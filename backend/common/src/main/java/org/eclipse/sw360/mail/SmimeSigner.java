/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.mail;

import jakarta.activation.CommandMap;
import jakarta.activation.MailcapCommandMap;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Signs outgoing SW360 e-mails with S/MIME (CMS / PKCS#7).
 *
 * <p>The produced structure is a <em>detached</em> (clear-signed) message:</p>
 *
 * <pre>
 * Content-Type: multipart/signed; protocol="application/pkcs7-signature"; micalg="sha-256"
 *   part 0 -&gt; the original, unmodified message body
 *   part 1 -&gt; application/pkcs7-signature; name="smime.p7s" (base64)
 * </pre>
 *
 * <p>Clients without S/MIME support can therefore still read the mail body,
 * while S/MIME capable clients can verify the SW360 instance as the origin.</p>
 *
 * <h3>Enablement</h3>
 * <p>There is deliberately <b>no</b> {@code enable} switch. Signing is active
 * if and only if {@code MailUtil_smimeKeystorePath} and
 * {@code MailUtil_smimeKeystorePassword} are configured and the referenced
 * PKCS#12 file can actually be turned into a usable signing identity. Both
 * properties are empty by default, so signing is off out of the box.</p>
 *
 * <h3>Failure behaviour</h3>
 * <p>{@link #create} never throws. Every configuration or keystore problem is
 * reported through a single explicit {@code WARN} log line and results in an
 * empty {@link Optional}, i.e. SW360 keeps sending unsigned mail instead of
 * failing to start or silently dropping notifications.</p>
 */
public class SmimeSigner {

    private static final Logger log = LogManager.getLogger(SmimeSigner.class);

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String DEFAULT_DIGEST_ALGORITHM = "SHA256";

    /**
     * Content handlers required by Jakarta Activation to serialize the PKCS#7
     * parts. {@code bcjmail-jdk18on} ships these in its {@code META-INF/mailcap},
     * but they are registered defensively here as well: if anything else in the
     * container replaced the default {@link CommandMap}, sending would otherwise
     * fail with "no object DCH for MIME type application/pkcs7-signature".
     */
    private static final String[] SMIME_MAILCAPS = {
            "application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature",
            "application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime",
            "application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature",
            "application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime",
            "multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed"
    };

    /**
     * Headers owned by the signed multipart; never copied from the original.
     */
    private static final String[] CONTENT_HEADER_PREFIXES = {
            "Content-Type:", "Content-Transfer-Encoding:", "MIME-Version:"
    };

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        registerMailcaps();
    }

    private final PrivateKey signingKey;
    private final X509Certificate leafCertificate;
    private final JcaCertStore certificateStore;
    private final String signatureAlgorithm;

    private SmimeSigner(
            PrivateKey key,
            X509Certificate certificate,
            JcaCertStore certificateStore,
            String signatureAlgorithm
    ) {
        this.signingKey = key;
        this.leafCertificate = certificate;
        this.certificateStore = certificateStore;
        this.signatureAlgorithm = signatureAlgorithm;
    }

    private static void registerMailcaps() {
        CommandMap commandMap = CommandMap.getDefaultCommandMap();
        if (!(commandMap instanceof MailcapCommandMap mailcapCommandMap)) {
            log.warn("Default Jakarta Activation CommandMap is not a MailcapCommandMap ({}); " +
                    "cannot register S/MIME content handlers explicitly.", commandMap.getClass().getName());
            return;
        }
        for (String mailcap : SMIME_MAILCAPS) {
            mailcapCommandMap.addMailcap(mailcap);
        }
        CommandMap.setDefaultCommandMap(mailcapCommandMap);
    }

    /**
     * Builds a signer from the {@code MailUtil_smime*} configuration.
     *
     * @param keystorePath     path of the PKCS#12 file holding the signing identity
     * @param keystorePassword password protecting the PKCS#12 file
     * @param keyAlias         alias of the key entry; when blank the sole key entry is used
     * @param keyPassword      password of the key entry; when blank {@code keystorePassword} is reused
     * @param digestAlgorithm  digest to sign with; when blank {@code SHA256} is used
     * @return the signer, or {@link Optional#empty()} if signing is not configured or not usable
     */
    public static Optional<SmimeSigner> create(
            String keystorePath, String keystorePassword, String keyAlias,
            String keyPassword, String digestAlgorithm
    ) {
        if (CommonUtils.isNullEmptyOrWhitespace(keystorePath)
                && CommonUtils.isNullEmptyOrWhitespace(keystorePassword)) {
            log.info("S/MIME e-mail signing is not configured (MailUtil_smimeKeystorePath and " +
                    "MailUtil_smimeKeystorePassword are empty); outgoing mails will not be signed.");
            return Optional.empty();
        }
        if (CommonUtils.isNullEmptyOrWhitespace(keystorePath)) {
            log.warn("S/MIME e-mail signing disabled: MailUtil_smimeKeystorePassword is set but " +
                    "MailUtil_smimeKeystorePath is empty.");
            return Optional.empty();
        }
        if (CommonUtils.isNullEmptyOrWhitespace(keystorePassword)) {
            log.warn("S/MIME e-mail signing disabled: MailUtil_smimeKeystorePath is set but " +
                    "MailUtil_smimeKeystorePassword is empty.");
            return Optional.empty();
        }

        try {
            return load(keystorePath, keystorePassword, keyAlias, keyPassword, digestAlgorithm);
        } catch (Exception e) {
            log.warn("S/MIME e-mail signing disabled: could not build a signing identity from '{}': {}",
                    keystorePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static Optional<SmimeSigner> load(
            String keystorePath, String keystorePassword, String keyAlias,
            String keyPassword, String digestAlgorithm
    ) throws Exception {
        Path path = Paths.get(keystorePath);
        if (!Files.isReadable(path)) {
            log.warn("S/MIME e-mail signing disabled: keystore '{}' does not exist or is not readable.",
                    keystorePath);
            return Optional.empty();
        }

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (InputStream keystoreStream = new FileInputStream(path.toFile())) {
            keyStore.load(keystoreStream, keystorePassword.toCharArray());
        }

        String alias = resolveAlias(keyStore, keyAlias, keystorePath);
        if (alias == null) {
            return Optional.empty();
        }

        char[] entryPassword = (CommonUtils.isNullEmptyOrWhitespace(keyPassword)
                ? keystorePassword : keyPassword).toCharArray();
        java.security.Key key = keyStore.getKey(alias, entryPassword);
        if (!(key instanceof PrivateKey resolvedKey)) {
            log.warn("S/MIME e-mail signing disabled: alias '{}' in keystore '{}' does not hold a private key.",
                    alias, keystorePath);
            return Optional.empty();
        }

        List<X509Certificate> chain = readCertificateChain(keyStore, alias, keystorePath);
        if (chain.isEmpty()) {
            return Optional.empty();
        }
        X509Certificate leafCertificate = chain.getFirst();
        warnAboutCertificateSuitability(leafCertificate);

        String signatureAlgorithm = signatureAlgorithm(digestAlgorithm, resolvedKey.getAlgorithm());
        SmimeSigner signer = new SmimeSigner(resolvedKey, leafCertificate,
                new JcaCertStore(chain), signatureAlgorithm);

        log.info("S/MIME e-mail signing enabled using certificate '{}' (serial {}, valid until {}) " +
                        "from keystore '{}' with signature algorithm {}.",
                leafCertificate.getSubjectX500Principal().getName(),
                leafCertificate.getSerialNumber(), leafCertificate.getNotAfter(),
                keystorePath, signatureAlgorithm);
        return Optional.of(signer);
    }

    private static @Nullable String resolveAlias(
            KeyStore keyStore, String configuredAlias, String keystorePath
    ) throws java.security.KeyStoreException {
        if (!CommonUtils.isNullEmptyOrWhitespace(configuredAlias)) {
            if (!keyStore.isKeyEntry(configuredAlias)) {
                log.warn("S/MIME e-mail signing disabled: keystore '{}' has no key entry with alias '{}'. " +
                        "Available key aliases: {}.", keystorePath, configuredAlias, keyAliases(keyStore));
                return null;
            }
            return configuredAlias;
        }

        List<String> aliases = keyAliases(keyStore);
        if (aliases.isEmpty()) {
            log.warn("S/MIME e-mail signing disabled: keystore '{}' contains no key entry.", keystorePath);
            return null;
        }
        if (aliases.size() > 1) {
            log.warn("S/MIME e-mail signing disabled: keystore '{}' contains {} key entries {}; " +
                    "set MailUtil_smimeKeyAlias to select one.", keystorePath, aliases.size(), aliases);
            return null;
        }
        return aliases.getFirst();
    }

    private static @NonNull List<String> keyAliases(
            @NonNull KeyStore keyStore
    ) throws java.security.KeyStoreException {
        List<String> aliases = new ArrayList<>();
        Enumeration<String> allAliases = keyStore.aliases();
        while (allAliases.hasMoreElements()) {
            String alias = allAliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                aliases.add(alias);
            }
        }
        return aliases;
    }

    private static @NonNull List<X509Certificate> readCertificateChain(
            @NonNull KeyStore keyStore, String alias, String keystorePath
    ) throws java.security.KeyStoreException {
        Certificate[] rawChain = keyStore.getCertificateChain(alias);
        if (rawChain == null || rawChain.length == 0) {
            log.warn("S/MIME e-mail signing disabled: alias '{}' in keystore '{}' has no certificate chain.",
                    alias, keystorePath);
            return Collections.emptyList();
        }
        List<X509Certificate> chain = new ArrayList<>(rawChain.length);
        for (Certificate certificate : rawChain) {
            if (certificate instanceof X509Certificate x509Certificate) {
                chain.add(x509Certificate);
            }
        }
        if (chain.isEmpty()) {
            log.warn("S/MIME e-mail signing disabled: certificate chain of alias '{}' in keystore '{}' " +
                    "contains no X.509 certificate.", alias, keystorePath);
        }
        return chain;
    }

    /**
     * Logs non-fatal hints about a certificate that is unlikely to be accepted
     * by mail clients. Signing still proceeds, because rejecting the identity
     * here would silently degrade to unsigned mail for a purely advisory reason.
     */
    private static void warnAboutCertificateSuitability(X509Certificate certificate) {
        try {
            certificate.checkValidity(new Date());
        } catch (Exception e) {
            log.warn("S/MIME signing certificate '{}' is not valid at the current time: {}",
                    certificate.getSubjectX500Principal().getName(), e.getMessage());
        }

        boolean[] keyUsage = certificate.getKeyUsage();
        // Index 0 = digitalSignature, index 1 = nonRepudiation (RFC 5280).
        if (keyUsage != null && keyUsage.length > 1 && !keyUsage[0] && !keyUsage[1]) {
            log.warn("S/MIME signing certificate '{}' declares neither digitalSignature nor " +
                            "nonRepudiation key usage; mail clients may reject the signature.",
                    certificate.getSubjectX500Principal().getName());
        }

        try {
            List<String> extendedKeyUsage = certificate.getExtendedKeyUsage();
            // 1.3.6.1.5.5.7.3.4 = id-kp-emailProtection
            if (extendedKeyUsage != null && !extendedKeyUsage.contains("1.3.6.1.5.5.7.3.4")) {
                log.warn("S/MIME signing certificate '{}' does not declare the emailProtection " +
                                "extended key usage; mail clients may reject the signature.",
                        certificate.getSubjectX500Principal().getName());
            }
        } catch (java.security.cert.CertificateParsingException e) {
            log.warn("Could not read the extended key usage of the S/MIME signing certificate: {}",
                    e.getMessage());
        }
    }

    /**
     * Maps the configured digest and the private key algorithm onto a JCA
     * signature algorithm name, e.g. {@code SHA256} + {@code RSA} →
     * {@code SHA256withRSA}.
     */
    static @NonNull String signatureAlgorithm(String digestAlgorithm, String keyAlgorithm) {
        String digest = CommonUtils.isNullEmptyOrWhitespace(digestAlgorithm)
                ? DEFAULT_DIGEST_ALGORITHM
                : digestAlgorithm.trim().toUpperCase(Locale.ROOT).replace("-", "");
        String key = keyAlgorithm == null ? "" : keyAlgorithm.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            // EdDSA keys carry the digest in the algorithm itself.
            case "ED25519" -> "Ed25519";
            case "ED448" -> "Ed448";
            case "EC", "ECDSA" -> digest + "withECDSA";
            case "" -> digest + "withRSA";
            default -> digest + "with" + key;
        };
    }

    /**
     * Extracts the part of {@code original} that the signature covers.
     *
     * <p>Only the body and its {@code Content-*} headers are signed. Handing the
     * {@link MimeMessage} itself to BouncyCastle would additionally copy the
     * envelope headers ({@code From}, {@code To}, {@code Subject}, {@code Date},
     * {@code Message-ID}) into the signed body part, which duplicates them
     * inside the {@code multipart/signed} structure and is displayed as stray
     * text by some mail clients.</p>
     */
    private static @NonNull MimeBodyPart signableBodyPart(@NonNull MimeMessage original) throws MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setDataHandler(original.getDataHandler());
        copyHeader(original, bodyPart, "Content-Type");
        copyHeader(original, bodyPart, "Content-Transfer-Encoding");
        copyHeader(original, bodyPart, "Content-Disposition");
        return bodyPart;
    }

    private static void copyHeader(@NonNull MimeMessage from, MimeBodyPart to, String header) throws MessagingException {
        String value = from.getHeader(header, null);
        if (value != null) {
            to.setHeader(header, value);
        }
    }

    private static boolean isContentHeader(String headerLine) {
        for (String prefix : CONTENT_HEADER_PREFIXES) {
            if (headerLine.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a <b>new</b> message carrying the S/MIME signature of
     * {@code original}; {@code original} itself is left untouched, so the same
     * source message can safely be signed once per recipient.
     *
     * @param original the fully composed but unsigned message
     * @param session  session used to instantiate the resulting message
     * @return the signed message
     */
    public MimeMessage sign(MimeMessage original, Session session) throws MessagingException {
        MimeMultipart signedContent = generateSignedContent(original);

        MimeMessage signedMessage = new MimeMessage(session);
        Enumeration<String> headerLines = original.getAllHeaderLines();
        while (headerLines.hasMoreElements()) {
            String headerLine = headerLines.nextElement();
            if (!isContentHeader(headerLine)) {
                signedMessage.addHeaderLine(headerLine);
            }
        }
        signedMessage.setContent(signedContent);
        signedMessage.saveChanges();
        return signedMessage;
    }

    private MimeMultipart generateSignedContent(MimeMessage original) throws MessagingException {
        try {
            SignerInfoGenerator signerInfoGenerator = new JcaSimpleSignerInfoGeneratorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(signatureAlgorithm, signingKey, leafCertificate);

            // RFC5751_MICALGS yields micalg="sha-256"; the legacy RFC 3851 spelling
            // ("sha256") is rejected or ignored by current mail clients.
            SMIMESignedGenerator generator = new SMIMESignedGenerator(SMIMESignedGenerator.RFC5751_MICALGS);
            generator.addSignerInfoGenerator(signerInfoGenerator);
            generator.addCertificates(certificateStore);

            return generator.generate(signableBodyPart(original));
        } catch (Exception e) {
            throw new MessagingException("Could not create the S/MIME signature", e);
        }
    }
}
