/*
SPDX-FileCopyrightText: © 2024,2026 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver.security.key;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;

/**
 * Loads the RSA signing key from a persistent JKS keystore so that the JWT
 * signing key (and its {@code kid}) is stable across authorization-server
 * restarts and across HA nodes that share the same keystore volume.
 *
 * <h3>Bootstrap order</h3>
 * <ol>
 *   <li>If {@code jwt.keystore.path} exists on the filesystem → load it.</li>
 *   <li>Otherwise copy the bundled {@code /jwt-keystore.jks} classpath
 *       resource to {@code jwt.keystore.path} (best-effort) and load it.
 *       The copy makes the keystore persistent across future restarts so that
 *       the authorization server does not switch signing keys on every JVM
 *       start.</li>
 *   <li>If neither the file nor the classpath resource is available →
 *       fail fast with a clear error message pointing at the helper script
 *       {@code tools/generateJwtStore.sh}.</li>
 * </ol>
 *
 * <h3>Key ID ({@code kid})</h3>
 * The {@code kid} is derived from the RFC 7638 public-key thumbprint, so the
 * same keystore always produces the same {@code kid} value on every JVM start
 * and on every HA node. Resource-server JWKS caches therefore remain valid
 * across authorization-server restarts without a manual cache flush.
 */
@Component
public class KeyManager {

    private static final Logger log = LogManager.getLogger(KeyManager.class);

    /** Alias used when the JKS was generated with {@code tools/generateJwtStore.sh}. */
    private static final String KEY_ALIAS = "jwt";

    /** Classpath resource bundled inside the WAR as a development / CI fallback. */
    private static final String CLASSPATH_KEYSTORE = "/jwt-keystore.jks";

    @Value("${jwt.secretkey:sw360SecretKey}")
    private String secretKey;

    /**
     * Filesystem path of the persistent JWT signing keystore.
     * Defaults to {@code /etc/sw360/jwt-keystore.jks} (the same directory that
     * is mounted as a named Docker volume, keeping the keystore across
     * container restarts).  Override via {@code jwt.keystore.path} in
     * {@code application.yml} or {@code @TestPropertySource} in tests.
     */
    @Value("${jwt.keystore.path:" + CommonUtils.SYSTEM_CONFIGURATION_PATH + "/jwt-keystore.jks}")
    private String keystorePath;

    /**
     * Returns the RSA signing key loaded from the persistent keystore.
     *
     * <p>The returned {@link RSAKey} has a deterministic {@code kid} derived
     * from the RFC 7638 thumbprint of the public key.</p>
     *
     * @throws IllegalStateException if no keystore can be found or loaded.
     */
    public RSAKey rsaKey() throws KeyStoreException, UnrecoverableKeyException,
            NoSuchAlgorithmException, CertificateException, IOException, JOSEException {
        KeyStore keyStore = loadKeyStore(keystorePath, secretKey);
        Certificate certificate = keyStore.getCertificate(KEY_ALIAS);
        if (certificate == null) {
            throw new IllegalStateException(
                    "Keystore at '" + keystorePath + "' has no entry with alias '" +
                            KEY_ALIAS + "'.");
        }
        RSAPublicKey  publicKey  = (RSAPublicKey)  certificate.getPublicKey();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(KEY_ALIAS, secretKey.toCharArray());
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE, 30);
        // Derive a deterministic JWK 'kid' from the RFC 7638 thumbprint of the
        // public key. The same keystore therefore always produces the same kid,
        // so resource-server JWKS caches stay valid across authorization-server
        // restarts (and across nodes in an HA deployment sharing the same keystore).
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .expirationTime(cl.getTime())
                .keyIDFromThumbprint()
                .build();
    }

    /**
     * Loads the JKS keystore following the bootstrap order described in the
     * class-level Javadoc.
     */
    private KeyStore loadKeyStore(String keystoreFile, String password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        File ksFile = new File(keystoreFile);

        if (ksFile.exists()) {
            log.debug("Loading JWT signing keystore from {}", keystoreFile);
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                loadKeyStoreData(keyStore, fis, password, keystoreFile);
            }
            return keyStore;
        }

        // Persistent keystore not found – try classpath fallback.
        InputStream classpathStream = getClasspathKeystore();
        if (classpathStream == null) {
            throw new IllegalStateException(
                    "No JWT signing keystore found and no bundled classpath fallback. " +
                            "Check docs to generate new one at appropriate location.");
        }

        // Best-effort: persist the classpath keystore so future restarts are stable.
        try (InputStream seedStream = classpathStream) {
            File parentDir = ksFile.getParentFile();
            if (parentDir != null) {
                boolean created = parentDir.mkdirs();
                if (!created && !parentDir.exists()) {
                    log.warn("Could not create parent directory '{}' for JWT keystore.", parentDir.getAbsolutePath());
                }
            }
            Files.copy(seedStream, ksFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Seeded JWT signing keystore from bundled classpath fallback to '{}'.", keystoreFile);
        } catch (IOException e) {
            log.warn("Could not persist bundled JWT keystore to '{}': {}. " +
                     "Using classpath copy for this JVM run only.", keystoreFile, e.getMessage());
        }

        // Load from file if the copy succeeded; otherwise reload from classpath.
        if (ksFile.exists()) {
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                loadKeyStoreData(keyStore, fis, password, keystoreFile);
            }
        } else {
            try (InputStream fallback = getClasspathKeystore()) {
                if (fallback == null) {
                    throw new IllegalStateException(
                            "No JWT signing keystore found and no bundled classpath fallback. " +
                                    "Check docs to generate new one at appropriate location.");
                }
                loadKeyStoreData(keyStore, fallback, password, CLASSPATH_KEYSTORE);
            }
        }
        return keyStore;
    }

    private void loadKeyStoreData(KeyStore keyStore, InputStream keyStoreStream,
            String password, String source)
            throws NoSuchAlgorithmException, CertificateException {
        try {
            keyStore.load(keyStoreStream, password.toCharArray());
        } catch (IOException e) {
            log.error("Failed to load JWT signing keystore from '{}'. " +
                    "The keystore password may be wrong or the keystore is invalid/corrupted.", source, e);
            throw new IllegalStateException(
                    "Could not load JWT signing keystore from '" + source +
                    "'. Check JWT_SECRETKEY / jwt.secretkey and keystore integrity.", e);
        }
    }

    /**
     * Returns an {@link InputStream} for the bundled classpath JKS resource,
     * or {@code null} if the resource is not present on the classpath.
     *
     * <p>Extracted as a protected, overridable method so unit tests can
     * simulate the absence of the classpath fallback without manipulating the
     * actual classpath.</p>
     */
    protected InputStream getClasspathKeystore() {
        return KeyManager.class.getResourceAsStream(CLASSPATH_KEYSTORE);
    }
}
