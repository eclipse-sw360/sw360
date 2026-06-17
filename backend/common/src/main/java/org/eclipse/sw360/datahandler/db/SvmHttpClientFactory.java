/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * Factory for pre-built, shared {@link CloseableHttpClient} instances configured for
 * the SVM service.
 *
 * <p>Two singleton clients are provided:
 * <ul>
 *   <li>{@link #getTrustedClient()} - one-way TLS (trust store only); used for plain
 *       GET requests such as fetching component-mappings or vulnerability data.</li>
 *   <li>{@link #getMutualTlsClient()} - mutual TLS; includes the SW360 PKCS12 client
 *       certificate in addition to the trust store, used for monitoring-list uploads.</li>
 * </ul>
 *
 * <p>Both clients are thread-safe singletons. Do <em>not</em> close them; they are
 * managed for the lifetime of the application.
 *
 * <h3>Key store / trust store files</h3>
 * All JKS files are resolved by {@link CommonUtils#loadResource}: the file is first
 * looked up under {@code /etc/sw360/<filename>}, then on the classpath. Only the base
 * file name (not an absolute path) is configured in {@code sw360.properties}.
 *
 * <h3>Trust-store lookup order</h3>
 * <ol>
 *   <li>JKS file referenced by {@code svm.sw360.truststore.filename} in
 *       {@code /etc/sw360/sw360.properties}.</li>
 *   <li>JVM default trust store when the property is absent or empty.</li>
 * </ol>
 */
public final class SvmHttpClientFactory {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final Logger log = LogManager.getLogger(SvmHttpClientFactory.class);

    /**
     * PKCS12 client-certificate key store for mutual TLS.
     */
    private static final String KEY_STORE_FILENAME;
    private static final char[] KEY_STORE_PASSPHRASE;

    /**
     * Optional JKS trust store - empty string means "use JVM default".
     */
    private static final String TRUST_STORE_FILENAME;
    private static final char[] TRUST_STORE_PASSWORD;

    /**
     * Shared client for one-way TLS (trust store only).
     */
    private static final CloseableHttpClient TRUSTED_CLIENT;

    /**
     * Shared client for mutual TLS (client cert + trust store).
     * {@code null} when {@code svm.sw360.certificate.filename} is not configured -
     * in that case {@link #getMutualTlsClient()} throws a clear {@link IllegalStateException}.
     */
    private static final CloseableHttpClient MUTUAL_TLS_CLIENT;

    static {
        Properties props = CommonUtils.loadProperties(SvmHttpClientFactory.class, PROPERTIES_FILE_PATH);

        KEY_STORE_FILENAME = props.getProperty("svm.sw360.certificate.filename", "");
        KEY_STORE_PASSPHRASE = props.getProperty("svm.sw360.certificate.passphrase", "").toCharArray();
        TRUST_STORE_FILENAME = props.getProperty("svm.sw360.truststore.filename", "");
        TRUST_STORE_PASSWORD = props.getProperty("svm.sw360.truststore.password", "changeit").toCharArray();

        // One-way TLS client - always built; uses JVM default cacerts when no trust
        // store is configured, so it never fails due to missing SVM configuration.
        CloseableHttpClient trustedClient;
        try {
            trustedClient = buildClient(buildTrustOnlySslContext());
        } catch (IOException e) {
            log.error("SVM: failed to build trusted HTTP client - SVM GET endpoints will be unavailable", e);
            trustedClient = null;
        }
        TRUSTED_CLIENT = trustedClient;

        // Mutual-TLS client - only built when a client certificate is configured.
        // Skipped silently when the property is absent (SVM not in use).
        CloseableHttpClient mutualTlsClient = null;
        if (!CommonUtils.isNullEmptyOrWhitespace(KEY_STORE_FILENAME)) {
            try {
                mutualTlsClient = buildClient(buildMutualTlsSslContext());
            } catch (IOException e) {
                log.error("SVM: failed to build mutual-TLS HTTP client - monitoring-list upload will be unavailable", e);
            }
        } else {
            log.debug("SVM: svm.sw360.certificate.filename not set - mutual TLS client not created");
        }
        MUTUAL_TLS_CLIENT = mutualTlsClient;
    }

    private SvmHttpClientFactory() {
        // Utility class - not instantiable.
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the shared {@link CloseableHttpClient} for one-way TLS connections
     * to SVM endpoints (trust store only, no client certificate).
     *
     * <p>The returned instance is a thread-safe singleton. Do <em>not</em> close it.
     */
    public static CloseableHttpClient getTrustedClient() {
        return TRUSTED_CLIENT;
    }

    /**
     * Returns the shared {@link CloseableHttpClient} for mutual TLS connections
     * to SVM endpoints (client certificate + trust store).
     *
     * <p>The returned instance is a thread-safe singleton. Do <em>not</em> close it.
     *
     * @throws IllegalStateException when {@code svm.sw360.certificate.filename}
     *                               is not configured.
     */
    public static CloseableHttpClient getMutualTlsClient() {
        if (MUTUAL_TLS_CLIENT == null) {
            throw new IllegalStateException("SVMML: Mutual TLS client not configured " +
                    "(svm.sw360.certificate.filename not set)");
        }
        return MUTUAL_TLS_CLIENT;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static CloseableHttpClient buildClient(SSLContext sslContext) {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder
                .create()
                .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext))
                .build();
        return HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    /**
     * Builds an {@link SSLContext} that only loads the configured trust store.
     * If {@code svm.sw360.truststore.filename} is not set, the JVM default
     * ({@code cacerts}) is used automatically.
     */
    private static SSLContext buildTrustOnlySslContext() throws IOException {
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            applyTrustStore(builder);
            return builder.build();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException
                 | CertificateException e) {
            throw new IOException("SVM: Failed to build TLS SSL context for SVM connection", e);
        }
    }

    /**
     * Builds an {@link SSLContext} that loads the PKCS12 client certificate
     * ({@code svm.sw360.certificate.filename}) together with the trust store.
     */
    private static SSLContext buildMutualTlsSslContext() throws IOException {
        try {
            SSLContextBuilder builder = SSLContexts.custom();

            // --- Client certificate from PKCS12 ---
            byte[] keyStoreBytes = CommonUtils.loadResource(SvmHttpClientFactory.class,
                    KEY_STORE_FILENAME)
                    .orElseThrow(
                            () -> new IOException(
                                    "SVMML: Cannot find client key store '" + KEY_STORE_FILENAME + "'"));

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream in = new ByteArrayInputStream(keyStoreBytes)) {
                keyStore.load(in, KEY_STORE_PASSPHRASE);
            } catch (IOException e) {
                throw new IOException("SVMML: Failed to open SVM client key store '"
                        + KEY_STORE_FILENAME + "'", e);
            }

            try {
                builder.loadKeyMaterial(keyStore, KEY_STORE_PASSPHRASE);
            } catch (UnrecoverableKeyException e) {
                throw new IOException("SVMML: Cannot extract private key from SVM client key store '"
                        + KEY_STORE_FILENAME + "'", e);
            }

            applyTrustStore(builder);
            return builder.build();

        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException
                 | CertificateException e) {
            throw new IOException("Failed to build mutual TLS SSL context for SVM connection", e);
        }
    }

    /**
     * Conditionally loads the configured JKS trust store into {@code builder}.
     * When {@code svm.sw360.truststore.filename} is absent or empty, the builder
     * is left unchanged and the JVM default trust store is used automatically.
     */
    private static void applyTrustStore(
            SSLContextBuilder builder
    ) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (CommonUtils.isNullEmptyOrWhitespace(TRUST_STORE_FILENAME)) {
            log.debug("SVM: svm.sw360.truststore.filename not set - using JVM default cacerts");
            return;
        }
        log.debug("SVM: loading trust store from {}", TRUST_STORE_FILENAME);
        byte[] trustStoreBytes = CommonUtils.loadResource(SvmHttpClientFactory.class,
                TRUST_STORE_FILENAME)
                .orElseThrow(
                        () -> new IOException("SVM: Cannot read SVM trust store: "
                                + TRUST_STORE_FILENAME));
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream in = new ByteArrayInputStream(trustStoreBytes)) {
            trustStore.load(in, TRUST_STORE_PASSWORD);
        }
        builder.loadTrustMaterial(trustStore, null);
    }
}
