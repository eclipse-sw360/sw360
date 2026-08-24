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

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link SmimeSigner} produces the detached S/MIME structure
 * expected by mail clients and that it degrades gracefully whenever signing is
 * not configured or not usable.
 */
class SmimeSignerTest {

    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEY_ALIAS = "sw360-mail";
    private static final String BODY = "Notification body of an SW360 e-mail.";

    private static KeyPair keyPair;
    private static X509Certificate certificate;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void createSigningIdentity() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        X500Name subject = new X500Name("CN=SW360 Test Mailer,E=noreply@sw360.example.org");
        Instant now = Instant.now();
        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(subject, BigInteger.valueOf(now.toEpochMilli()), Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(365, ChronoUnit.DAYS)), subject, keyPair.getPublic());
        certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        certificateBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_emailProtection));

        certificate = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate())));
    }

    private static Session session() {
        return Session.getInstance(new Properties());
    }

    private static MimeMessage unsignedMessage(Session session) throws Exception {
        MimeMessage message = new MimeMessage(session);
        message.setSubject("SW360 notification");
        message.setText(BODY);
        message.setFrom(new InternetAddress("noreply@sw360.example.org"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("user@example.org"));
        message.saveChanges();
        return message;
    }

    private Path writeKeystore(String alias, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), password.toCharArray(), new Certificate[]{certificate});

        Path keystoreFile = tempDir.resolve("smime-keystore.p12");
        try (OutputStream out = Files.newOutputStream(keystoreFile)) {
            keyStore.store(out, password.toCharArray());
        }
        return keystoreFile;
    }

    @Test
    void signProducesDetachedMultipartSignedMatchingTheReferenceFormat() throws Exception {
        Path keystoreFile = writeKeystore(KEY_ALIAS, KEYSTORE_PASSWORD);
        SmimeSigner signer = SmimeSigner.create(keystoreFile.toString(), KEYSTORE_PASSWORD, KEY_ALIAS, "", "SHA256").orElseThrow();

        Session session = session();
        MimeMessage signed = signer.sign(unsignedMessage(session), session);

        ContentType contentType = new ContentType(signed.getContentType());
        assertEquals("multipart/signed", contentType.getBaseType().toLowerCase());
        assertEquals("application/pkcs7-signature", contentType.getParameter("protocol"));
        assertEquals("sha-256", contentType.getParameter("micalg"));

        MimeMultipart multipart = (MimeMultipart) signed.getContent();
        assertEquals(2, multipart.getCount());
        assertEquals(BODY, multipart.getBodyPart(0).getContent());
        assertTrue(multipart.getBodyPart(1).getContentType().contains("pkcs7-signature"));
        assertEquals("smime.p7s", multipart.getBodyPart(1).getFileName());

        assertEquals("SW360 notification", signed.getSubject());
        assertEquals("noreply@sw360.example.org", signed.getFrom()[0].toString());
        assertEquals("user@example.org", signed.getRecipients(Message.RecipientType.TO)[0].toString());

        // Only the body and its Content-* headers are signed: envelope headers must
        // not be duplicated into the signed part, where clients would render them
        // as stray text.
        MimeBodyPart signedPart = (MimeBodyPart) multipart.getBodyPart(0);
        assertNull(signedPart.getHeader("Subject"));
        assertNull(signedPart.getHeader("From"));
        assertNull(signedPart.getHeader("To"));
        assertNull(signedPart.getHeader("Message-ID"));
        assertTrue(signedPart.getContentType().toLowerCase().startsWith("text/plain"));
    }

    @Test
    void htmlMailsAreSignedWithoutLosingTheirContentType() throws Exception {
        Path keystoreFile = writeKeystore(KEY_ALIAS, KEYSTORE_PASSWORD);
        SmimeSigner signer = SmimeSigner.create(keystoreFile.toString(), KEYSTORE_PASSWORD, KEY_ALIAS, "", "SHA256").orElseThrow();

        Session session = session();
        MimeMessage message = new MimeMessage(session);
        message.setSubject("SW360 clearing request");
        message.setContent("<html><body><p>" + BODY + "</p></body></html>", "text/html");
        message.setFrom(new InternetAddress("noreply@sw360.example.org"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("user@example.org"));
        message.saveChanges();

        MimeMessage signed = signer.sign(message, session);

        MimeMultipart multipart = (MimeMultipart) signed.getContent();
        MimeBodyPart signedPart = (MimeBodyPart) multipart.getBodyPart(0);
        assertTrue(signedPart.getContentType().toLowerCase().startsWith("text/html"));
        assertTrue(((String) signedPart.getContent()).contains(BODY));
    }

    @Test
    void producedSignatureVerifiesAgainstTheSignerCertificate() throws Exception {
        Path keystoreFile = writeKeystore(KEY_ALIAS, KEYSTORE_PASSWORD);
        SmimeSigner signer = SmimeSigner.create(keystoreFile.toString(), KEYSTORE_PASSWORD, KEY_ALIAS, "", "SHA256").orElseThrow();

        Session session = session();
        MimeMessage signed = signer.sign(unsignedMessage(session), session);

        // Re-parse the serialized message so the assertion covers what is
        // actually put on the wire rather than the in-memory object graph.
        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        signed.writeTo(serialized);
        MimeMessage received = new MimeMessage(session, new ByteArrayInputStream(serialized.toByteArray()));

        SMIMESigned smimeSigned = new SMIMESigned((MimeMultipart) received.getContent());
        Collection<SignerInformation> signers = smimeSigned.getSignerInfos().getSigners();
        assertEquals(1, signers.size());
        for (SignerInformation signerInformation : signers) {
            assertTrue(signerInformation.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build(certificate)));
        }
    }

    @Test
    void signLeavesTheSourceMessageUntouchedSoItCanBeReusedPerRecipient() throws Exception {
        Path keystoreFile = writeKeystore(KEY_ALIAS, KEYSTORE_PASSWORD);
        SmimeSigner signer = SmimeSigner.create(keystoreFile.toString(), KEYSTORE_PASSWORD, KEY_ALIAS, "", "SHA256").orElseThrow();

        Session session = session();
        MimeMessage template = unsignedMessage(session);

        MimeMessage first = signer.sign(template, session);
        MimeMessage second = signer.sign(template, session);

        assertNotSame(template, first);
        assertNotSame(first, second);
        assertTrue(template.getContentType().toLowerCase().startsWith("text/plain"));
        assertEquals(BODY, template.getContent());

        // The second signature must cover the original body, not the first signature.
        MimeMultipart secondContent = (MimeMultipart) second.getContent();
        assertEquals(2, secondContent.getCount());
        assertEquals(BODY, secondContent.getBodyPart(0).getContent());
    }

    @Test
    void signingIsDisabledWhenNotConfigured() {
        assertFalse(SmimeSigner.create("", "", "", "", "SHA256").isPresent());
        assertFalse(SmimeSigner.create(null, null, null, null, null).isPresent());
    }

    @Test
    void signingIsDisabledWhenOnlyOneOfPathAndPasswordIsSet() throws Exception {
        Path keystoreFile = writeKeystore(KEY_ALIAS, KEYSTORE_PASSWORD);
        assertFalse(SmimeSigner.create(keystoreFile.toString(), "", KEY_ALIAS, "", "SHA256").isPresent());
        assertFalse(SmimeSigner.create("", KEYSTORE_PASSWORD, KEY_ALIAS, "", "SHA256").isPresent());
    }

    @Test
    void signingIsDisabledWithoutThrowingWhenTheKeystoreIsUnusable() throws Exception {
        Path keystoreFile = writeKeystore(KEY_ALIAS, KEYSTORE_PASSWORD);

        Optional<SmimeSigner> missingFile = SmimeSigner.create(tempDir.resolve("absent.p12").toString(), KEYSTORE_PASSWORD, KEY_ALIAS, "", "SHA256");
        Optional<SmimeSigner> wrongPassword = SmimeSigner.create(keystoreFile.toString(), "not-the-password", KEY_ALIAS, "", "SHA256");
        Optional<SmimeSigner> unknownAlias = SmimeSigner.create(keystoreFile.toString(), KEYSTORE_PASSWORD, "no-such-alias", "", "SHA256");

        assertFalse(missingFile.isPresent());
        assertFalse(wrongPassword.isPresent());
        assertFalse(unknownAlias.isPresent());
    }

    @Test
    void keyAliasIsResolvedAutomaticallyWhenTheKeystoreHoldsASingleEntry() throws Exception {
        Path keystoreFile = writeKeystore(KEY_ALIAS, KEYSTORE_PASSWORD);
        assertTrue(SmimeSigner.create(keystoreFile.toString(), KEYSTORE_PASSWORD, "", "", "").isPresent());
    }

    @Test
    void signatureAlgorithmIsDerivedFromDigestAndKeyAlgorithm() {
        assertEquals("SHA256withRSA", SmimeSigner.signatureAlgorithm("SHA256", "RSA"));
        assertEquals("SHA256withRSA", SmimeSigner.signatureAlgorithm("sha-256", "RSA"));
        assertEquals("SHA256withRSA", SmimeSigner.signatureAlgorithm("", "RSA"));
        assertEquals("SHA512withECDSA", SmimeSigner.signatureAlgorithm("SHA-512", "EC"));
        assertEquals("Ed25519", SmimeSigner.signatureAlgorithm("SHA256", "Ed25519"));
    }
}
