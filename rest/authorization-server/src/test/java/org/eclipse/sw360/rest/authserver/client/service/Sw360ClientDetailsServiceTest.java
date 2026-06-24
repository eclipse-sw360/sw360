/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.service;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class Sw360ClientDetailsServiceTest {
    @Mock
    private OAuthClientRepository clientRepo;
    @InjectMocks
    private Sw360ClientDetailsService service;
    private OAuthClientEntity entity;
    @BeforeEach
    public void setUp() {
        // @Value-injected fallbacks are not populated by @InjectMocks; supply
        // sane defaults via reflection so the TokenSettings builder doesn't
        // hit a null Integer when an entity omits the per-client validity.
        ReflectionTestUtils.setField(service, "defaultAccessTokenValiditySeconds", 1800);
        ReflectionTestUtils.setField(service, "defaultRefreshTokenValiditySeconds", 3600);
        entity = new OAuthClientEntity();
        entity.setClientId("bot-client");
        entity.setClientSecret("$2a$10$abcdefghijabcdefghij..");
    }
    /**
     * Regression test for the NullPointerException that was returned as
     * HTTP 500 from POST /oauth2/token when the OAuth client document had
     * {@code registered_redirect_uri = null} (always true for client_credentials
     * clients minted by the modern /client-management controller).
     */
    @Test
    public void findByClientId_nullRedirectUris_buildsClientWithoutThrowing() {
        entity.setScope(Set.of("READ", "WRITE"));
        entity.setRegisteredRedirectUri(null);
        when(clientRepo.getByClientId("bot-client")).thenReturn(entity);
        RegisteredClient registered = service.findByClientId("bot-client");
        assertThat(registered, is(notNullValue()));
        assertThat(registered.getClientId(), is("bot-client"));
        assertThat(registered.getScopes(), containsInAnyOrder("READ", "WRITE"));
        assertThat("redirect URIs must have fallback sentinel for client_credentials clients",
                registered.getRedirectUris(), contains("https://localhost/unused-redirect"));
        assertThat(registered.getAuthorizationGrantTypes(),
                containsInAnyOrder(
                        AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN,
                        AuthorizationGrantType.CLIENT_CREDENTIALS));
    }
    @Test
    public void findByClientId_nullScopes_buildsClientWithEmptyScopes() {
        entity.setScope(null);
        entity.setRegisteredRedirectUri(null);
        when(clientRepo.getByClientId("bot-client")).thenReturn(entity);
        RegisteredClient registered = service.findByClientId("bot-client");
        assertThat(registered, is(notNullValue()));
        assertThat(registered.getScopes(), is(empty()));
        assertThat(registered.getRedirectUris(), contains("https://localhost/unused-redirect"));
    }
    @Test
    public void findByClientId_unknownClient_returnsNullInsteadOfNpe() {
        when(clientRepo.getByClientId("does-not-exist")).thenReturn(null);
        // Returning null is the contract Spring Authorization Server expects
        // so it can emit a proper 401 invalid_client error.
        assertThat(service.findByClientId("does-not-exist"), is(nullValue()));
    }

    /**
     * Regression: without this, Spring Authorization Server's default
     * 5-minute access-token TTL silently overrides the per-client value
     * persisted in the OAuth client document.
     */
    @Test
    public void findByClientId_honorsPerClientAccessTokenValidity() {
        entity.setAccessTokenValiditySeconds(3600);
        entity.setRefreshTokenValiditySeconds(7200);
        entity.setScope(Set.of("READ"));
        when(clientRepo.getByClientId("bot-client")).thenReturn(entity);
        RegisteredClient registered = service.findByClientId("bot-client");
        assertThat(registered.getTokenSettings().getAccessTokenTimeToLive(), is(Duration.ofSeconds(3600)));
        assertThat(registered.getTokenSettings().getRefreshTokenTimeToLive(), is(Duration.ofSeconds(7200)));
    }

    @Test
    public void findByClientId_fallsBackToConfiguredDefaultsWhenEntityHasNoValidity() {
        entity.setAccessTokenValiditySeconds(null);
        entity.setRefreshTokenValiditySeconds(null);
        entity.setScope(Set.of("READ"));
        when(clientRepo.getByClientId("bot-client")).thenReturn(entity);
        RegisteredClient registered = service.findByClientId("bot-client");
        assertThat(registered.getTokenSettings().getAccessTokenTimeToLive(), is(Duration.ofSeconds(1800)));
        assertThat(registered.getTokenSettings().getRefreshTokenTimeToLive(), is(Duration.ofSeconds(3600)));
    }
}
