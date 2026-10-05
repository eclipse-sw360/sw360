/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.jwt;

import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.common.security.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.common.security.jwt.AbstractSw360JwtAuthenticationConverter;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Validates the user and extracts the roles from the JWT token, converting them into
 * {@link GrantedAuthority}s. The shared conversion flow lives in
 * {@link AbstractSw360JwtAuthenticationConverter}; this subclass adds the resource-server user
 * lookup (by email, then client id), strict user validation and API-token capability merging.
 *
 * @author smruti.sahoo@siemens.com
 */
@Profile("!SECURITY_MOCK")
@Component
public class Sw360JWTAccessTokenConverter extends AbstractSw360JwtAuthenticationConverter {

    private static final Logger log = LogManager.getLogger(Sw360JWTAccessTokenConverter.class);

    @Value("${jwt.auth.converter.principle-attribute:email}")
    private String principleAttribute;

    @Autowired
    private Sw360UserService userService;

    public Sw360JWTAccessTokenConverter() {
        super(new JwtGrantedAuthoritiesConverter(),
                Sw360GrantedAuthoritiesCalculator.CONFIG_WRITE_ACCESS_USERGROUP,
                Sw360GrantedAuthoritiesCalculator.CONFIG_ADMIN_ACCESS_USERGROUP);
    }

    @Override
    protected @Nullable User resolveUserByEmail(@Nullable String email) {
        if (!CommonUtils.isNotNullEmptyOrWhitespace(email)) {
            return null;
        }
        try {
            return userService.getUserByEmail(email);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    protected @Nullable User resolveUserByClientId(@Nullable String clientId) {
        if (!CommonUtils.isNotNullEmptyOrWhitespace(clientId)) {
            return null;
        }
        try {
            return userService.getUserFromClientId(clientId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    protected String resolvePrincipal(@NonNull Jwt jwt) {
        String claimName = principleAttribute != null ? principleAttribute : JwtClaimNames.SUB;
        return jwt.getClaim(claimName);
    }
}
