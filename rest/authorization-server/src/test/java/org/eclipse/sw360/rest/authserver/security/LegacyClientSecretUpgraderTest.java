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

import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LegacyClientSecretUpgraderTest {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Mock
    private OAuthClientRepository repo;
    private LegacyClientSecretUpgrader upgrader;

    @BeforeEach
    public void setUp() {
        upgrader = new LegacyClientSecretUpgrader(repo, bcrypt);
    }

    @AfterEach
    public void cleanup() {
        Sw360ClientSecretEncoder.UPGRADE_CTX.remove();
    }

    @Test
    public void onSuccess_legacyContext_reFetchesEntityAndPersistsBcrypt() {
        String clientId = "legacy-bot";
        String rawSecret = "legacy-fixture-secret";
        Sw360ClientSecretEncoder.UPGRADE_CTX.set(rawSecret);
        OAuthClientEntity fresh = new OAuthClientEntity();
        fresh.setClientId(clientId);
        fresh.setClientSecret(rawSecret);
        when(repo.getByClientId(clientId)).thenReturn(fresh);
        upgrader.onApplicationEvent(buildSuccessEvent(clientId));
        // The entity must be re-fetched immediately before update so the _rev is fresh.
        verify(repo).getByClientId(clientId);
        ArgumentCaptor<OAuthClientEntity> persisted = ArgumentCaptor.forClass(OAuthClientEntity.class);
        verify(repo).update(persisted.capture());
        assertTrue(Sw360ClientSecretEncoder.looksLikeBcrypt(persisted.getValue().getClientSecret()),
                "persisted secret must be BCrypt-encoded but was: " + persisted.getValue().getClientSecret());
        assertTrue(bcrypt.matches(rawSecret, persisted.getValue().getClientSecret()),
                "BCrypt hash must verify against the original raw secret");
        // Thread-local must always be cleared so the next request starts clean.
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get());
    }

    @Test
    public void onSuccess_alreadyBcryptInDatabase_doesNotPersist() {
        String clientId = "legacy-bot";
        String rawSecret = "legacy-fixture-secret";
        Sw360ClientSecretEncoder.UPGRADE_CTX.set(rawSecret);
        OAuthClientEntity fresh = new OAuthClientEntity();
        fresh.setClientId(clientId);
        // Concurrent upgrade already happened on another node.
        fresh.setClientSecret(bcrypt.encode(rawSecret));
        when(repo.getByClientId(clientId)).thenReturn(fresh);
        upgrader.onApplicationEvent(buildSuccessEvent(clientId));
        verify(repo).getByClientId(clientId);
        verify(repo, never()).update(any());
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get());
    }

    @Test
    public void onSuccess_noUpgradeContext_isNoOp() {
        // No call to UPGRADE_CTX.set(...) - simulates a modern bcrypt path.
        upgrader.onApplicationEvent(buildSuccessEvent("any-client"));
        verify(repo, never()).getByClientId(any());
        verify(repo, never()).update(any());
    }

    @Test
    public void onSuccess_nonClientAuthentication_isNoOp() {
        Sw360ClientSecretEncoder.UPGRADE_CTX.set("legacy-fixture-secret");
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(
                new UsernamePasswordAuthenticationToken("user@sw360.org", "pwd", Collections.emptyList()));
        upgrader.onApplicationEvent(event);
        verify(repo, never()).getByClientId(any());
        verify(repo, never()).update(any());
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get());
    }

    @Test
    public void onSuccess_repoUpdateThrows_swallowsAndClearsContext() {
        String clientId = "legacy-bot";
        Sw360ClientSecretEncoder.UPGRADE_CTX.set("legacy-fixture-secret");
        OAuthClientEntity fresh = new OAuthClientEntity();
        fresh.setClientId(clientId);
        fresh.setClientSecret("legacy-fixture-secret");
        when(repo.getByClientId(clientId)).thenReturn(fresh);
        doThrow(new RuntimeException("simulated 409 conflict")).when(repo).update(any());
        // Must NOT propagate - token response should still succeed.
        upgrader.onApplicationEvent(buildSuccessEvent(clientId));
        assertNull(Sw360ClientSecretEncoder.UPGRADE_CTX.get());
    }

    private AuthenticationSuccessEvent buildSuccessEvent(String clientId) {
        RegisteredClient registered = RegisteredClient.withId(clientId)
                .clientId(clientId)
                .clientSecret("placeholder")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("READ")
                .build();
        OAuth2ClientAuthenticationToken token = new OAuth2ClientAuthenticationToken(
                registered, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, null);
        return new AuthenticationSuccessEvent(token);
    }
}
