/*
 * Copyright Siemens AG, 2019,2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import jakarta.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import static org.eclipse.sw360.rest.authserver.client.rest.OAuthClientController.UNUSED_REDIRECT_URI;

@Service
public class Sw360ClientDetailsService implements RegisteredClientRepository {

    private final Logger log = LogManager.getLogger(this.getClass());

    /**
     * Fallback access-token TTL (seconds) used when an
     * {@link OAuthClientEntity} has no per-client value. Sourced from the
     * {@code security.accesstoken.validity} property.
     */
    @Value("${security.accesstoken.validity:30}")
    private Integer defaultAccessTokenValiditySeconds;

    /**
     * Fallback refresh-token TTL (seconds) when the entity has no value.
     */
    @Value("${security.refreshtoken.validity:360}")
    private Integer defaultRefreshTokenValiditySeconds;

    @Autowired
    private OAuthClientRepository clientRepo;

    @Override
    public RegisteredClient findByClientId(@Nonnull String clientId) {
        log.debug("client registration findByClientId() called for client_id={}", clientId);
        return getByClientId(clientId);
    }

    private RegisteredClient getByClientId(String clientId) {
        OAuthClientEntity oce = clientRepo.getByClientId(clientId);
        if (oce == null) {
            // Returning null lets Spring Authorization Server produce a proper
            // 401 invalid_client response instead of a 500 NullPointerException.
            log.debug("No client registration found for client_id={}", clientId);
            return null;
        }

        // Add guards to create safe responses.
        Set<String> scopes = oce.getScope() != null ? oce.getScope() : Collections.emptySet();
        Set<String> redirectUris = oce.getRegisteredRedirectUri() != null && !oce.getRegisteredRedirectUri().isEmpty()
                ? oce.getRegisteredRedirectUri()
                : Collections.singleton(UNUSED_REDIRECT_URI);

        // Honour the per-client access/refresh token validity.
        int accessSeconds = (oce.getAccessTokenValiditySeconds() != null
                && oce.getAccessTokenValiditySeconds() > 0)
                ? oce.getAccessTokenValiditySeconds()
                : defaultAccessTokenValiditySeconds;
        int refreshSeconds = (oce.getRefreshTokenValiditySeconds() != null
                && oce.getRefreshTokenValiditySeconds() > 0)
                ? oce.getRefreshTokenValiditySeconds()
                : defaultRefreshTokenValiditySeconds;
        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(accessSeconds))
                .refreshTokenTimeToLive(Duration.ofSeconds(refreshSeconds))
                .build();

        RegisteredClient registeredClient = RegisteredClient
                .withId(oce.getClientId())
                .clientId(oce.getClientId())
                .clientSecret(oce.getClientSecret())
                .scopes(sc -> sc.addAll(scopes))
                .redirectUris(uri -> uri.addAll(redirectUris))
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantTypes(
                        grantType -> {
                            grantType.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                            grantType.add(AuthorizationGrantType.REFRESH_TOKEN);
                            grantType.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                        }
                ).clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(tokenSettings)
                .build();
        log.debug("Registered details for client_id={}: {}", clientId, registeredClient);
        return registeredClient;
    }

    @Override
    public RegisteredClient findById(@Nonnull String id) {
        log.debug("client registration findById() called with id={}", id);
        return getByClientId(id);
    }

    @Override
    public void save(@Nonnull RegisteredClient registeredClient) {
        log.debug("client registration save() called with client details: {}", registeredClient);
    }
}
