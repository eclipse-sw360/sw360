/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security.key;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for {@link KeyManager} covering the bootstrap-order contract:
 * <ol>
 *   <li>Load from configured filesystem path when the file exists.</li>
 *   <li>The {@code kid} is deterministic (same keystore → same thumbprint).</li>
 *   <li>Fall back to the bundled classpath resource when the file is absent.</li>
 *   <li>Fail fast with {@link IllegalStateException} when neither source is
 *       available.</li>
 * </ol>
 */
class KeyManagerTest {

    private static final String DEFAULT_PASSWORD = "sw360SecretKey";

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link KeyManager} instance with injected field values so we
     * can control the keystore path and password without a Spring context.
     */
    private KeyManager keyManagerWithPath(String path) {
        KeyManager km = new KeyManager();
        ReflectionTestUtils.setField(km, "secretKey", DEFAULT_PASSWORD);
        ReflectionTestUtils.setField(km, "keystorePath", path);
        return km;
    }

    /**
     * Copies the bundled classpath JKS to the given target path so the test
     * can exercise the "load from filesystem" code path.
     */
    private void seedKeystoreFile(Path target) throws IOException {
        try (InputStream in = KeyManagerTest.class.getResourceAsStream("/jwt-keystore.jks")) {
            assertThat(in).as("Bundled /jwt-keystore.jks must be present on test classpath").isNotNull();
            Files.copy(in, target);
        }
    }

    // -------------------------------------------------------------------------
    // Test cases
    // -------------------------------------------------------------------------

    @Test
    void loadsKeystoreFromConfiguredPath(@TempDir Path tempDir)
            throws Exception {
        Path ksFile = tempDir.resolve("jwt-keystore.jks");
        seedKeystoreFile(ksFile);

        KeyManager km = keyManagerWithPath(ksFile.toString());
        RSAKey rsaKey = km.rsaKey();

        assertThat(rsaKey).isNotNull();
        assertThat(rsaKey.getKeyID()).isNotEmpty();
    }

    @Test
    void kidIsDeterministicAcrossInstances(@TempDir Path tempDir)
            throws Exception {
        Path ksFile = tempDir.resolve("jwt-keystore.jks");
        seedKeystoreFile(ksFile);

        String path = ksFile.toString();
        String kid1 = keyManagerWithPath(path).rsaKey().getKeyID();
        String kid2 = keyManagerWithPath(path).rsaKey().getKeyID();

        assertThat(kid1)
                .as("kid must be identical across instances loading the same keystore")
                .isEqualTo(kid2);
    }

    @Test
    void fallsBackToClasspathWhenFileMissing(@TempDir Path tempDir)
            throws Exception {
        // Point at a non-existent file – KeyManager must use the classpath fallback.
        String nonExistentPath = tempDir.resolve("missing.jks").toString();
        KeyManager km = keyManagerWithPath(nonExistentPath);

        RSAKey rsaKey = km.rsaKey();

        assertThat(rsaKey).isNotNull();
        assertThat(rsaKey.getKeyID()).isNotEmpty();
        // The classpath fallback should have been copied to tempDir/missing.jks.
        assertThat(new java.io.File(nonExistentPath))
                .as("KeyManager should seed the persistent path from the classpath fallback")
                .exists();
    }

    @Test
    void failsFastWhenNoKeystoreAvailable(@TempDir Path tempDir) {
        String nonExistentPath = tempDir.resolve("missing.jks").toString();

        // Spy to suppress the classpath fallback.
        KeyManager km = spy(keyManagerWithPath(nonExistentPath));
        doReturn(null).when(km).getClasspathKeystore();

        assertThatThrownBy(km::rsaKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No JWT signing keystore found");
    }

    @Test
    void rsaKeyUsesThumbprintAsKid(@TempDir Path tempDir)
            throws Exception {
        Path ksFile = tempDir.resolve("jwt-keystore.jks");
        seedKeystoreFile(ksFile);

        RSAKey rsaKey = keyManagerWithPath(ksFile.toString()).rsaKey();

        // Verify the kid matches the RFC 7638 thumbprint of the public key.
        String expectedKid = new RSAKey.Builder(rsaKey.toRSAPublicKey())
                .keyIDFromThumbprint()
                .build()
                .getKeyID();
        assertThat(rsaKey.getKeyID())
                .as("kid must be the RFC 7638 thumbprint of the public key")
                .isEqualTo(expectedKid);
    }

    @Test
    void failsFastWhenKeystorePasswordIsWrong(@TempDir Path tempDir)
            throws Exception {
        Path ksFile = tempDir.resolve("jwt-keystore.jks");
        seedKeystoreFile(ksFile);

        KeyManager km = keyManagerWithPath(ksFile.toString());
        ReflectionTestUtils.setField(km, "secretKey", "wrong-password");

        assertThatThrownBy(km::rsaKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not load JWT signing keystore")
                .hasMessageContaining("JWT_SECRETKEY");
    }
}
