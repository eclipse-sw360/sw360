/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TokenCapabilityAuthorities {

    public static final String TOKEN_READ = "TOKEN_READ";
    public static final String TOKEN_WRITE = "TOKEN_WRITE";
    public static final String WRITE = "WRITE";

    private static final Set<GrantedAuthority> READ_ONLY_AUTHORITIES =
            Set.of(new SimpleGrantedAuthority(TOKEN_READ));
    private static final Set<GrantedAuthority> READ_WRITE_AUTHORITIES =
            Set.of(new SimpleGrantedAuthority(TOKEN_READ), new SimpleGrantedAuthority(TOKEN_WRITE));

    private TokenCapabilityAuthorities() {
    }

    public static Set<GrantedAuthority> readWrite() {
        return READ_WRITE_AUTHORITIES;
    }

    public static Set<GrantedAuthority> fromAuthorityNames(Collection<String> authorityNames) {
        if (CommonUtils.isNullOrEmptyCollection(authorityNames)) {
            return READ_ONLY_AUTHORITIES;
        }

        ScopeFlags scopeFlags = scopeFlags(authorityNames);
        if (scopeFlags.hasWrite()) {
            return readWrite();
        }
        return READ_ONLY_AUTHORITIES;
    }

    private static ScopeFlags scopeFlags(Collection<String> authorityNames) {
        boolean read = false;
        boolean write = false;
        for (String authorityName : authorityNames) {
            if (authorityName == null) {
                continue;
            }
            switch (authorityName.toUpperCase(Locale.ROOT)) {
                case "READ", "SCOPE_READ" -> read = true;
                case "WRITE", "SCOPE_WRITE" -> {
                    read = true;
                    write = true;
                }
                default -> {
                    // Unknown scopes/authorities are intentionally ignored.
                }
            }
        }
        return new ScopeFlags(read, write);
    }

    public static Set<GrantedAuthority> fromJwtScopeClaim(Object scopeClaim) {
        if (scopeClaim instanceof String scopeClaimString) {
            if (scopeClaimString.isBlank()) {
                return readWrite();
            }
            String[] values = scopeClaimString.trim().split("\\s+");
            return fromJwtScopeValues(List.of(values));
        }

        if (scopeClaim instanceof Collection<?> scopeClaimCollection) {
            Set<String> normalized = new LinkedHashSet<>();
            for (Object value : scopeClaimCollection) {
                if (value != null) {
                    normalized.add(value.toString());
                }
            }
            return fromJwtScopeValues(normalized);
        }

        return readWrite();
    }

    private static Set<GrantedAuthority> fromJwtScopeValues(Collection<String> scopeValues) {
        ScopeFlags scopeFlags = scopeFlags(scopeValues);
        if (!scopeFlags.hasRead()) {
            return readWrite();
        }
        if (scopeFlags.hasWrite()) {
            return readWrite();
        }
        return READ_ONLY_AUTHORITIES;
    }

    public static Set<GrantedAuthority> merge(Collection<? extends GrantedAuthority> first,
                                               Collection<? extends GrantedAuthority> second) {
        Set<GrantedAuthority> merged = new HashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return Collections.unmodifiableSet(merged);
    }

    public static Set<GrantedAuthority> mergeForTokenAuthentication(Collection<? extends GrantedAuthority> userAuthorities,
                                                                     Collection<? extends GrantedAuthority> tokenCapabilities) {
        return merge(userAuthorities, tokenCapabilities);
    }

    private record ScopeFlags(boolean hasRead, boolean hasWrite) {
    }
}
