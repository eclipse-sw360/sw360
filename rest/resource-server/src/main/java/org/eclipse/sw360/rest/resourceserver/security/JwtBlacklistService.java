/*
 * Copyright Siemens AG, 2025-2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtBlacklistService {
    private static final Logger log = LogManager.getLogger(JwtBlacklistService.class);
    private final Set<String> blacklistedTokens = Collections.synchronizedSet(new HashSet<>());

    public void blacklistToken(String token) {
        if (token != null && !token.isEmpty()) {
            synchronized (blacklistedTokens) {
                blacklistedTokens.add(token);
                log.info("Token added to blacklist. Blacklist size: {}", blacklistedTokens.size());
            }
        }
    }

    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        boolean isBlacklisted = blacklistedTokens.contains(token);
        log.debug("Token blacklist check - Is blacklisted: {}", isBlacklisted);
        return isBlacklisted;
    }

    public void removeFromBlacklist(String token) {
        if (token != null && !token.isEmpty()) {
            synchronized (blacklistedTokens) {
                blacklistedTokens.remove(token);
                log.info("Token removed from blacklist. Blacklist size: {}", blacklistedTokens.size());
            }
        }
    }

    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}
