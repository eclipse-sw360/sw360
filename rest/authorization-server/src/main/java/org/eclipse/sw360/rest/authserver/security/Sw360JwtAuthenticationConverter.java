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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Converts locally issued SW360 tokens and external IdP tokens into authorities for
 * authorization-server resource endpoints.
 *
 * <p>Besides the regular {@code scope} claim used by SW360-issued tokens, this converter
 * also understands group-based claims such as Keycloak's {@code userGroup} and
 * {@code groups}. That enables `/authorization/client-management` to authorize browser
 * Bearer tokens originating from a trusted external issuer.</p>
 */
@Component
@RequiredArgsConstructor
public class Sw360JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String USER_NAME = "user_name";
    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final String EMAIL = "email";
    private static final String GROUPS = "groups";
    private static final String USER_GROUP = "userGroup";
    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    private final Sw360GrantedAuthoritiesCalculator authoritiesCalculator;
    private final JwtGrantedAuthoritiesConverter scopesConverter = createScopesConverter();

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        String principalName = resolvePrincipal(jwt);
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.addAll(scopesConverter.convert(jwt));
        authorities.addAll(extractGroupAuthorities(jwt, principalName));
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private @NonNull Collection<? extends GrantedAuthority> extractGroupAuthorities(
            Jwt jwt, String principalName
    ) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (UserGroup group : extractUserGroups(jwt)) {
            org.eclipse.sw360.datahandler.services.users.User syntheticUser = new org.eclipse.sw360.datahandler.services.users.User()
                    .setEmail(principalName)
                    .setDepartment("jwt")
                    .setUserGroup(org.eclipse.sw360.datahandler.services.users.UserGroup.valueOf(group.name()));
            authorities.addAll(authoritiesCalculator.generateFromUser(syntheticUser));
        }
        return authorities;
    }

    private @NonNull Set<UserGroup> extractUserGroups(@NonNull Jwt jwt) {
        Set<UserGroup> groups = new LinkedHashSet<>();
        collectUserGroups(groups, jwt.getClaim(USER_GROUP));
        collectUserGroups(groups, jwt.getClaim(GROUPS));

        Object realmAccessClaim = jwt.getClaim(REALM_ACCESS);
        if (realmAccessClaim instanceof Map<?, ?> realmAccess) {
            collectUserGroups(groups, realmAccess.get(ROLES));
        }
        return groups;
    }

    private void collectUserGroups(Set<UserGroup> groups, Object claimValue) {
        if (claimValue instanceof Collection<?> values) {
            for (Object value : values) {
                toUserGroup(value).ifPresent(groups::add);
            }
            return;
        }
        toUserGroup(claimValue).ifPresent(groups::add);
    }

    private java.util.Optional<UserGroup> toUserGroup(Object rawValue) {
        String value = Objects.toString(rawValue, null);
        if (CommonUtils.isNullEmptyOrWhitespace(value)) {
            return java.util.Optional.empty();
        }

        String normalized = value.trim();
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
            normalized = normalized.substring(slashIndex + 1);
        }
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        try {
            return java.util.Optional.of(UserGroup.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return java.util.Optional.empty();
        }
    }

    private String resolvePrincipal(@NonNull Jwt jwt) {
        return firstNonBlank(
                jwt.getClaimAsString(USER_NAME),
                jwt.getClaimAsString(PREFERRED_USERNAME),
                jwt.getClaimAsString(EMAIL),
                jwt.getClaimAsString(JwtClaimNames.SUB)
        );
    }

    private static @NonNull JwtGrantedAuthoritiesConverter createScopesConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("");
        converter.setAuthoritiesClaimName("scope");
        return converter;
    }

    private String firstNonBlank(String @NonNull ... values) {
        for (String value : values) {
            if (!CommonUtils.isNullEmptyOrWhitespace(value)) {
                return value;
            }
        }
        return JwtClaimNames.SUB;
    }
}
