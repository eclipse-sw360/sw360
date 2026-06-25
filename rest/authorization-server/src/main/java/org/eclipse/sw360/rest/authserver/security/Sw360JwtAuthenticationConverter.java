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

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.common.security.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.common.security.Sw360UserDetailsProvider;
import org.eclipse.sw360.rest.common.security.jwt.AbstractSw360JwtAuthenticationConverter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Converts locally issued SW360 tokens and external IdP tokens into authorities for
 * authorization-server resource endpoints.
 *
 * <p>All of the shared conversion logic lives in {@link AbstractSw360JwtAuthenticationConverter};
 * this subclass only contributes the authorization-server user lookup and scope handling. The
 * SW360 user is resolved from the JWT {@code email} claim and mapped to SW360 authorities, which
 * keeps {@code /authorization/client-management} aligned with the authoritative user record.</p>
 */
@Component
public class Sw360JwtAuthenticationConverter extends AbstractSw360JwtAuthenticationConverter {

    private final Sw360UserDetailsProvider userDetailsProvider;

    public Sw360JwtAuthenticationConverter(Sw360UserDetailsProvider userDetailsProvider) {
        super(scopesConverter(),
                Sw360GrantedAuthoritiesCalculator.CONFIG_WRITE_ACCESS_USERGROUP,
                Sw360GrantedAuthoritiesCalculator.CONFIG_ADMIN_ACCESS_USERGROUP);
        this.userDetailsProvider = userDetailsProvider;
    }

    @Override
    protected @Nullable User resolveUserByEmail(@Nullable String email) {
        if (email == null) {
            return null;
        }
        return userDetailsProvider.provideUserDetails(email, null);
    }

    @Override
    protected @Nullable User resolveUserByClientId(@Nullable String clientId) {
        if (clientId == null) {
            return null;
        }
        return userDetailsProvider.getUserFromClientId(clientId);
    }

    private static JwtGrantedAuthoritiesConverter scopesConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("");
        converter.setAuthoritiesClaimName(SCOPE);
        return converter;
    }
}
