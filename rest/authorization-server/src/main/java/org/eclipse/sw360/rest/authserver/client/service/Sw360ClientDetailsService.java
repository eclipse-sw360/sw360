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

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Service;

@Service
public class Sw360ClientDetailsService implements RegisteredClientRepository {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Value("${security.accesstoken.validity}")
    private Integer accessTokenValidity;

    private final OAuthClientRepository clientRepo;

    public Sw360ClientDetailsService(OAuthClientRepository clientRepo) {
        this.clientRepo = clientRepo;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        log.debug("client registration findbyClientId() called!!");
        return getByClientId(clientId);
    }

    private RegisteredClient getByClientId(String clientId) {
        OAuthClientEntity oce = clientRepo.getByClientId(clientId);
        Set<String> scopes = oce.getScope();
        RegisteredClient registeredClient = RegisteredClient
                .withId(oce.getClientId())
                .clientId(oce.getClientId())
                .clientSecret(oce.getClientSecret())
                .scopes(sc -> sc.addAll(scopes))
                .redirectUris(redirectUri -> redirectUri.addAll(oce.getRegisteredRedirectUri()))
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
                .build();
        log.debug("Registered details: {}", registeredClient.toString());
        return registeredClient;
    }

    @Override
    public RegisteredClient findById(String clientId) {
        log.debug("client registration findbyId() called!! with id: {}", clientId);
        return getByClientId(clientId);
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        log.debug("client registration save() called!! with client details: {}", registeredClient.toString());
    }
}