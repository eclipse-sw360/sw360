/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import org.eclipse.sw360.rest.authserver.IntegrationTestBase;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.security.Sw360ClientSecretEncoder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OAuthClientBootstrapControllerTest extends IntegrationTestBase {

    private static final String BOOTSTRAP_PATH = "/client-management/bootstrap";

    private static final String CLIENT_ID = "bootstrap-client";

    private static final String CLIENT_SECRET = "bootstrap-secret";

    private static final String VALID_REQUEST = """
            {"client_id":"bootstrap-client","client_secret":"bootstrap-secret",
             "redirect_uri":["https://example.org/callback"]}
            """;

    @Autowired
    private TestRestTemplate template;

    @Test
    public void bootstrapClient_validRequestCreatesHashedClientWithoutDisclosingSecret() throws Exception {
        when(clientRepo.getByClientId(CLIENT_ID)).thenReturn(null);
        doAnswer((InvocationOnMock invocation) -> true)
                .when(clientRepo).add(any(OAuthClientEntity.class));

        ResponseEntity<String> response = post(VALID_REQUEST);

        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
        assertFalse(response.getBody() != null && response.getBody().contains(CLIENT_SECRET));

        ArgumentCaptor<OAuthClientEntity> captor = ArgumentCaptor.forClass(OAuthClientEntity.class);
        verify(clientRepo).add(captor.capture());
        OAuthClientEntity saved = captor.getValue();
        assertThat(saved.getId(), is(CLIENT_ID));
        assertThat(saved.getClientId(), is(CLIENT_ID));
        assertTrue(Sw360ClientSecretEncoder.looksLikeBcrypt(saved.getClientSecret()));
        assertTrue(encoder.matches(CLIENT_SECRET, saved.getClientSecret()));
        assertThat(saved.getAuthorizedGrantTypes(), containsInAnyOrder("authorization_code"));
        assertThat(saved.getScope(), containsInAnyOrder("openid", "ADMIN"));
        assertThat(saved.getRegisteredRedirectUri(), containsInAnyOrder("https://example.org/callback"));
        assertThat(saved.getResourceIds(), containsInAnyOrder("sw360-REST-API"));
        assertThat(saved.getAutoApproveScopes(), containsInAnyOrder("true"));
        assertThat(saved.getAuthorities(), is(nullValue()));
        assertThat(saved.getOwnerEmail(), is(nullValue()));
        assertThat(saved.getAccessTokenValiditySeconds(), is(nullValue()));
        assertThat(saved.getRefreshTokenValiditySeconds(), is(nullValue()));
        assertFalse(saved.isSecretRequired());
        assertFalse(saved.isScoped());
    }

    @Test
    public void bootstrapClient_missingFieldsOrInvalidRedirectUriReturnsBadRequest() throws Exception {
        List<String> invalidRequests = List.of(
                "{\"client_secret\":\"secret\",\"redirect_uri\":[\"https://example.org/callback\"]}",
                "{\"client_id\":\"id\",\"redirect_uri\":[\"https://example.org/callback\"]}",
                "{\"client_id\":\"id\",\"client_secret\":\"secret\"}",
                "{\"client_id\":\"id\",\"client_secret\":\"secret\",\"redirect_uri\":[]}",
                "{\"client_id\":\"id\",\"client_secret\":\"secret\",\"redirect_uri\":[\"relative/path\"]}",
                "{\"client_id\":\"id\",\"client_secret\":\"secret\",\"redirect_uri\":[\"https://example.org/a b\"]}"
        );

        for (String body : invalidRequests) {
            assertThat(post(body).getStatusCode(), is(HttpStatus.BAD_REQUEST));
        }
        verify(clientRepo, never()).add(any(OAuthClientEntity.class));
    }

    @Test
    public void bootstrapClient_existingClientReturnsConflict() throws Exception {
        when(clientRepo.getByClientId(CLIENT_ID)).thenReturn(new OAuthClientEntity());

        ResponseEntity<String> response = post(VALID_REQUEST);

        assertThat(response.getStatusCode(), is(HttpStatus.CONFLICT));
        verify(clientRepo, never()).add(any(OAuthClientEntity.class));
    }

    private ResponseEntity<String> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return template.postForEntity(BOOTSTRAP_PATH, new HttpEntity<>(body, headers), String.class);
    }
}
