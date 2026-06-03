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

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link PasswordEncoder} that supports both the modern BCrypt-encoded client
 * secrets produced by this authorization server and the legacy raw-UUID
 * client secrets stored by the older Liferay-based SW360 portal in the same
 * {@code sw360oauthclients} CouchDB database.
 *
 * <p>{@link #encode(CharSequence)} always produces a BCrypt hash, so any new
 * secret created by the modern stack is stored encoded. {@link #matches(CharSequence,
 * String)} detects the BCrypt prefix and delegates to {@link
 * BCryptPasswordEncoder} for properly hashed values; otherwise it falls back
 * to a constant-time raw equality check so that pre-existing Liferay-era
 * clients continue to authenticate without manual CouchDB intervention.</p>
 *
 * <p>When a successful match happens against a legacy raw secret, the
 * presented raw value is stashed in {@link #UPGRADE_CTX} so the synchronous
 * {@link LegacyClientSecretUpgrader} listener can re-encode and persist it
 * immediately after authentication succeeds.</p>
 */
public class Sw360ClientSecretEncoder implements PasswordEncoder {
    /**
     * Carrier for a successfully-validated legacy raw secret. Set inside
     * {@link #matches(CharSequence, String)} when the stored value is not
     * BCrypt-prefixed and the raw equality check passes; consumed and cleared
     * by {@link LegacyClientSecretUpgrader}.
     */
    public static final ThreadLocal<String> UPGRADE_CTX = new ThreadLocal<>();

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // Always reset on entry so a previous request's leftover value can
        // never leak into a new authentication attempt.
        UPGRADE_CTX.remove();
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (looksLikeBcrypt(encodedPassword)) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }

        boolean matched = constantTimeEquals(rawPassword, encodedPassword);
        if (matched) {
            UPGRADE_CTX.set(rawPassword.toString());
        }
        return matched;
    }

    /**
     * @return {@code true} iff the supplied stored value looks like a BCrypt
     *         hash (one of the {@code $2a$}, {@code $2b$}, {@code $2y$}
     *         variants supported by {@link BCryptPasswordEncoder}).
     */
    public static boolean looksLikeBcrypt(String encoded) {
        return encoded != null
                && encoded.length() >= 4
                && encoded.charAt(0) == '$'
                && encoded.charAt(1) == '2'
                && (encoded.charAt(2) == 'a' || encoded.charAt(2) == 'b' || encoded.charAt(2) == 'y')
                && encoded.charAt(3) == '$';
    }

    /**
     * Cryptographically safe character sequence comparator which compares all
     * strings of given length, same or not, in constant time. Prevents guess
     * attacks.
     * @param a String one
     * @param b String two
     * @return Returns true if both the strings are equal.
     */
    private static boolean constantTimeEquals(CharSequence a, CharSequence b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
