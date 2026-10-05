/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Sw360ClientSecretEncoderTest {
    private final Sw360ClientSecretEncoder encoder = new Sw360ClientSecretEncoder();

    @AfterEach
    public void cleanup() {
        Sw360ClientSecretEncoder.UPGRADE_CTX.remove();
    }

    @Test
    public void encode_alwaysProducesBcrypt() {
        String encoded = encoder.encode("plaintext-secret");
        assertTrue(Sw360ClientSecretEncoder.looksLikeBcrypt(encoded),
                "encoded value must be a BCrypt hash but was: " + encoded);
    }

    @Test
    public void matches_bcryptStored_delegatesToBcrypt_andDoesNotPopulateUpgradeCtx() {
        String stored = new BCryptPasswordEncoder().encode("plaintext-secret");
        assertTrue(encoder.matches("plaintext-secret", stored));
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get(),
                "modern matches must not stash anything for upgrade");
    }

    @Test
    public void matches_bcryptStored_wrongSecret_returnsFalse() {
        String stored = new BCryptPasswordEncoder().encode("plaintext-secret");
        assertFalse(encoder.matches("wrong-secret", stored));
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get());
    }

    @Test
    public void matches_legacyRawStored_correctSecret_populatesUpgradeCtx() {
        String stored = "legacy-fixture-not-a-real-secret";
        assertTrue(encoder.matches(stored, stored));
        assertEquals(stored, Sw360ClientSecretEncoder.UPGRADE_CTX.get(),
                "legacy match must stash raw secret for the upgrader");
    }

    @Test
    public void matches_legacyRawStored_wrongSecret_doesNotPopulateUpgradeCtx() {
        // gitleaks:allow - this is a deliberately synthetic, never-deployed test fixture
        assertFalse(encoder.matches("wrong-secret", "legacy-fixture-not-a-real-secret"));
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get());
    }

    @Test
    public void matches_resetsUpgradeCtxOnEntry() {
        Sw360ClientSecretEncoder.UPGRADE_CTX.set("stale-leftover-value");
        encoder.matches("anything", new BCryptPasswordEncoder().encode("anything-else"));
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get());
    }

    @Test
    public void matches_nullInputs_returnFalse() {
        assertFalse(encoder.matches(null, "anything"));
        assertFalse(encoder.matches("anything", null));
    }

    @Test
    public void looksLikeBcrypt_recognizesAllPrefixes() {
        assertTrue(Sw360ClientSecretEncoder.looksLikeBcrypt("$2a$10$abcdefghij"));
        assertTrue(Sw360ClientSecretEncoder.looksLikeBcrypt("$2b$10$abcdefghij"));
        assertTrue(Sw360ClientSecretEncoder.looksLikeBcrypt("$2y$10$abcdefghij"));
        assertFalse(Sw360ClientSecretEncoder.looksLikeBcrypt(null));
        assertFalse(Sw360ClientSecretEncoder.looksLikeBcrypt(""));
        assertFalse(Sw360ClientSecretEncoder.looksLikeBcrypt("plaintext-uuid"));
        assertFalse(Sw360ClientSecretEncoder.looksLikeBcrypt("$1$$"));
    }
}
