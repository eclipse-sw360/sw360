/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common.security.jwt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.rest.common.security.Sw360GrantedAuthoritiesUtils;
import org.eclipse.sw360.rest.common.security.TokenCapabilityAuthorities;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared JWT-to-authentication conversion used by both the SW360 authorization server and
 * resource server.
 *
 * <p>The conversion flow is centralized here so the two REST stacks cannot drift apart:</p>
 * <ol>
 *     <li>extract the {@code email}/{@code client_id} claims from the token,</li>
 *     <li>resolve the backing SW360 {@link User} (delegated via {@link #resolveUser}),</li>
 *     <li>validate the resolved user (delegated via {@link #validateUser}),</li>
 *     <li>combine scope-derived authorities with the SW360 group authorities
 *         ({@code READ}/{@code WRITE}/{@code ADMIN}) produced by
 *         {@link Sw360GrantedAuthoritiesUtils}.</li>
 * </ol>
 *
 * <p>Subclasses only provide the stack-specific pieces (user lookup strategy, the configured
 * write/admin user groups, the scope-claim converter and, where required, strict user
 * validation or token-capability handling).</p>
 */
public abstract class AbstractSw360JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String EMAIL = "email";
    public static final String USER_NAME = "user_name";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String CLIENT_ID = "client_id";
    public static final String SCOPE = "scope";
    public static final List<String> IDENTIFIABLE_CLAIMS = List.of(USER_NAME, PREFERRED_USERNAME, EMAIL, JwtClaimNames.SUB);

    private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter;
    private final UserGroup writeAccessUserGroup;
    private final UserGroup adminAccessUserGroup;

    public static final String USER_IS_DEACTIVATED_OR_NOT_AVAILABLE = "User is deactivated or not available.";

    private static final Logger log = LogManager.getLogger(AbstractSw360JwtAuthenticationConverter.class);

    protected AbstractSw360JwtAuthenticationConverter(
            @NonNull JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter,
            UserGroup writeAccessUserGroup,
            UserGroup adminAccessUserGroup
    ) {
        this.scopeAuthoritiesConverter = scopeAuthoritiesConverter;
        this.writeAccessUserGroup = writeAccessUserGroup;
        this.adminAccessUserGroup = adminAccessUserGroup;
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        User sw360User = resolveUser(extractEmailFromJwt(jwt), extractClientIdFromJwt(jwt));
        validateUser(sw360User);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Collection<GrantedAuthority> scopeAuthorities = scopeAuthoritiesConverter.convert(jwt);
        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);
        }
        if (sw360User != null) {
            authorities.addAll(mapAuthorities(sw360User, jwt));
        }

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, authorities, resolvePrincipal(jwt));
        authentication.setDetails(sw360User);
        return authentication;
    }

    /**
     * Resolve the SW360 {@link User} backing the given token claims.
     *
     * @param email    email derived from the token (may be {@code null})
     * @param clientId client id derived from the token (may be {@code null})
     * @return the resolved user, or {@code null} when no user could be resolved
     */
    protected @Nullable User resolveUser(@Nullable String email, @Nullable String clientId) {
        User sw360User = null;
        if (CommonUtils.isNotNullEmptyOrWhitespace(email)) {
            sw360User = resolveUserByEmail(email);
        }

        if (sw360User == null && CommonUtils.isNotNullEmptyOrWhitespace(clientId)) {
            sw360User = resolveUserByClientId(clientId);
        }
        return sw360User;
    }

    /**
     * Resolve a user by email. Subclasses may override when email-based lookup is supported.
     */
    protected @Nullable User resolveUserByEmail(@Nullable String email) {
        return null;
    }

    /**
     * Resolve a user by client id. Subclasses may override when client-id lookup is supported.
     */
    protected @Nullable User resolveUserByClientId(@Nullable String clientId) {
        return null;
    }

    /**
     * Map a resolved SW360 user to granted authorities. Defaults to the shared
     * {@code READ}/{@code WRITE}/{@code ADMIN} group mapping; subclasses may extend it (e.g. to
     * merge API-token capabilities).
     */
    protected Collection<GrantedAuthority> mapAuthorities(@NonNull User user, @NonNull Jwt jwt) {
        Collection<GrantedAuthority> tokenCapabilities =
                TokenCapabilityAuthorities.fromJwtScopeClaim(jwt.getClaim(SCOPE));
        List<GrantedAuthority> grantedAuthorities = groupAuthorities(user);
        log.debug("User {} has group authorities {} and token capabilities {} for client {}",
                user.getEmail(), grantedAuthorities, tokenCapabilities, jwt.getClaim(CLIENT_ID));
        return TokenCapabilityAuthorities.mergeForTokenAuthentication(grantedAuthorities, tokenCapabilities);
    }

    /**
     * The shared SW360 group-to-authority mapping using this stack's configured user groups.
     */
    protected final List<GrantedAuthority> groupAuthorities(@NonNull User user) {
        return Sw360GrantedAuthoritiesUtils.generateFromUser(user, writeAccessUserGroup, adminAccessUserGroup);
    }

    /**
     * Validate the resolved user. If user is {@code null} or deactivated, the request is rejected.
     */
    protected void validateUser(@Nullable User sw360User) {
        if (sw360User == null || Boolean.TRUE.equals(sw360User.getDeactivated())) {
            throw new BadCredentialsException(USER_IS_DEACTIVATED_OR_NOT_AVAILABLE);
        }
    }

    /**
     * Resolve the principal name for the authentication token. Defaults to the first non-blank
     * of {@code user_name}, {@code preferred_username}, {@code email} and {@code sub}.
     */
    protected String resolvePrincipal(@NonNull Jwt jwt) {
        return firstNonBlankClaim(jwt);
    }

    /**
     * Extract the SW360 user email from a token: prefers the {@code email} claim and falls back
     * to {@code user_name}.
     */
    public static @Nullable String extractEmailFromJwt(@NonNull Jwt jwt) {
        String email = jwt.getClaimAsString(EMAIL);
        if (CommonUtils.isNullEmptyOrWhitespace(email)) {
            email = jwt.getClaimAsString(USER_NAME);
        }
        return CommonUtils.isNullEmptyOrWhitespace(email) ? null : email;
    }

    /**
     * Extract a single client id from a token. The claim may be a string or a collection; the
     * first non-blank value is returned.
     */
    public static @Nullable String extractClientIdFromJwt(@NonNull Jwt jwt) {
        Object clientIdClaim = jwt.getClaim(CLIENT_ID);
        if (clientIdClaim instanceof Collection<?> clientIdCollection) {
            for (Object candidate : clientIdCollection) {
                String value = Objects.toString(candidate, null);
                if (CommonUtils.isNotNullEmptyOrWhitespace(value)) {
                    return value;
                }
            }
            return null;
        }
        String value = Objects.toString(clientIdClaim, null);
        return CommonUtils.isNotNullEmptyOrWhitespace(value) ? value : null;
    }

    /**
     * Return the value of the first claim (in order of {@code IDENTIFIABLE_CLAIMS})
     * that has a non-blank value, falling back to the {@code sub} claim.
     */
    protected static String firstNonBlankClaim(@NonNull Jwt jwt) {
        for (String claimName : IDENTIFIABLE_CLAIMS) {
            String value = jwt.getClaimAsString(claimName);
            if (CommonUtils.isNotNullEmptyOrWhitespace(value)) {
                return value;
            }
        }
        return jwt.getClaimAsString(JwtClaimNames.SUB);
    }
}
