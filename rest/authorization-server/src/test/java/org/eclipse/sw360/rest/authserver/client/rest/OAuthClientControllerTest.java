/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import org.eclipse.sw360.rest.authserver.IntegrationTestBase;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.security.Sw360ClientSecretEncoder;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class OAuthClientControllerTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate template;

    @Test
    public void testGetAll_basicAuth_unknownUser_fail() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth("my-unknown-user", "12345").exchange(
                new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")), String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void testGetAll_basicAuth_admin_success() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345")
                .exchange(new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")),
                        String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testGetAll_basicAuth_normal_fail() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth(normalTestUser.getEmail(), "12345")
                .exchange(new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")),
                        String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    // ---- New tests for the plaintext-disclosure / scope-restriction fix ----

    @Test
    public void createClient_admin_returnsPlaintextSecretAndPersistsBcrypt()
            throws RestClientException, URISyntaxException, java.io.IOException {
        // given: repo.add() simulates CouchDB assigning an _id, and getByClientId echoes the persisted entity
        stubRepoAddAndLookup();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        String body = "{\"description\":\"new bot\",\"scope\":[\"READ\",\"WRITE\"],"
                + "\"access_token_validity\":3600,\"refresh_token_validity\":3600}";

        // when:
        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode response = new ObjectMapper().readTree(responseEntity.getBody());
        String disclosedSecret = response.get("client_secret").asText();
        assertNotNull("client_secret must be present on creation response", disclosedSecret);
        assertFalse("disclosed client_secret must be plaintext, not a BCrypt hash: " + disclosedSecret,
                Sw360ClientSecretEncoder.looksLikeBcrypt(disclosedSecret));
        assertThat(response.get("scope").toString(), is(oneOf("[\"READ\",\"WRITE\"]", "[\"WRITE\",\"READ\"]")));
    }

    @Test
    public void createClient_admin_invalidScope_returns400()
            throws RestClientException, URISyntaxException {
        stubRepoAddAndLookup();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        String body = "{\"description\":\"bad-scope\",\"scope\":[\"ADMIN\"]}";

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void createClient_admin_normalizesScopeAliases()
            throws RestClientException, URISyntaxException, java.io.IOException {
        stubRepoAddAndLookup();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        // 'scope_read' is the SCOPE_-prefixed lowercase Spring convention.
        String body = "{\"description\":\"normalize-me\",\"scope\":[\"scope_read\"]}";

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode response = new ObjectMapper().readTree(responseEntity.getBody());
        assertThat(response.get("scope").toString(), is("[\"READ\"]"));
    }

    @Test
    public void getAllClients_admin_doesNotLeakStoredSecrets()
            throws RestClientException, URISyntaxException, java.io.IOException {
        OAuthClientEntity legacy = new OAuthClientEntity();
        legacy.setClientId("legacy-bot");
        legacy.setClientSecret("abc123-uuid"); // raw plaintext from Liferay era
        OAuthClientEntity modern = new OAuthClientEntity();
        modern.setClientId("modern-bot");
        modern.setClientSecret("$2a$10$abcdefghijabcdefghij..");
        when(clientRepo.getAll()).thenReturn(List.of(legacy, modern));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(headers, HttpMethod.GET, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode array = new ObjectMapper().readTree(responseEntity.getBody());
        for (JsonNode item : array) {
            assertThat(item.get("client_secret"), is(notNullValue()));
            assertThat("must never expose stored secrets via GET /client-management",
                    item.get("client_secret").asText(),
                    is(OAuthClientResource.HIDDEN_SECRET));
        }
        // hamcrest sanity check across all entries
        assertThat(List.of(array.get(0).get("client_secret").asText(),
                        array.get(1).get("client_secret").asText()),
                everyItem(is(OAuthClientResource.HIDDEN_SECRET)));
    }

    @Test
    public void updateClient_admin_doesNotRotateOrDiscloseSecret()
            throws RestClientException, URISyntaxException, java.io.IOException {
        String existingClientId = "existing-bot";
        OAuthClientEntity existing = new OAuthClientEntity();
        existing.setClientId(existingClientId);
        existing.setClientSecret("$2a$10$abcdefghijabcdefghij..");
        when(clientRepo.getByClientId(existingClientId)).thenReturn(existing);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        String body = "{\"client_id\":\"" + existingClientId + "\",\"description\":\"updated\","
                + "\"scope\":[\"READ\"]}";

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode response = new ObjectMapper().readTree(responseEntity.getBody());
        assertThat("update path must never re-disclose the stored secret",
                response.get("client_secret").asText(), is(OAuthClientResource.HIDDEN_SECRET));
    }

    // ---- Tests for owner_email association (option B / oidcClientInfos mirror) ----

    @Test
    public void createClient_admin_unknownOwnerEmail_returns400()
            throws RestClientException, URISyntaxException {
        stubRepoAddAndLookup();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        // ghost@example.com is not stubbed in IntegrationTestBase, so the
        // user service mock returns null and the controller must reject.
        String body = "{\"description\":\"bot for ghost\",\"scope\":[\"READ\"],"
                + "\"owner_email\":\"ghost@example.com\"}";

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void createClient_admin_defaultsOwnerEmailToCaller()
            throws RestClientException, URISyntaxException, java.io.IOException {
        stubRepoAddAndLookup();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        String body = "{\"description\":\"bot owned by caller\",\"scope\":[\"READ\"]}";

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode response = new ObjectMapper().readTree(responseEntity.getBody());
        assertThat(response.get("owner_email").asText(), is(adminTestUser.getEmail()));
    }

    @Test
    public void createClient_admin_explicitOwnerEmail_acceptsKnownUser()
            throws RestClientException, URISyntaxException, java.io.IOException {
        stubRepoAddAndLookup();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        // normalTestUser is also stubbed in IntegrationTestBase; admin can
        // mint a client on their behalf.
        String body = "{\"description\":\"bot for normal user\",\"scope\":[\"READ\",\"WRITE\"],"
                + "\"owner_email\":\"" + normalTestUser.getEmail() + "\"}";

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode response = new ObjectMapper().readTree(responseEntity.getBody());
        assertThat(response.get("owner_email").asText(), is(normalTestUser.getEmail()));
    }

    @Test
    public void updateClient_admin_ignoresOwnerEmailChange()
            throws RestClientException, URISyntaxException, java.io.IOException {
        String existingClientId = "owned-bot";
        OAuthClientEntity existing = new OAuthClientEntity();
        existing.setClientId(existingClientId);
        existing.setClientSecret("$2a$10$abcdefghijabcdefghij..");
        existing.setOwnerEmail(adminTestUser.getEmail());
        when(clientRepo.getByClientId(existingClientId)).thenReturn(existing);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        // Try to repoint ownership; the controller must silently drop it.
        String body = "{\"client_id\":\"" + existingClientId + "\",\"description\":\"updated\","
                + "\"scope\":[\"READ\"],\"owner_email\":\"" + normalTestUser.getEmail() + "\"}";

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(body, headers, HttpMethod.POST, new URI("/client-management")), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode response = new ObjectMapper().readTree(responseEntity.getBody());
        assertThat("owner_email is immutable on update",
                response.get("owner_email").asText(), is(adminTestUser.getEmail()));
    }

    @Test
    public void deleteClient_admin_returnsHiddenSecretAndKeepsRecord()
            throws RestClientException, URISyntaxException, java.io.IOException {
        String existingClientId = "to-delete";
        OAuthClientEntity existing = new OAuthClientEntity();
        existing.setClientId(existingClientId);
        existing.setClientSecret("$2a$10$abcdefghijabcdefghij..");
        existing.setOwnerEmail(adminTestUser.getEmail());
        when(clientRepo.getByClientId(existingClientId)).thenReturn(existing);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth(adminTestUser.getEmail(), "12345").exchange(
                new RequestEntity<>(headers, HttpMethod.DELETE,
                        new URI("/client-management/" + existingClientId)), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        JsonNode response = new ObjectMapper().readTree(responseEntity.getBody());
        assertThat(response.get("client_secret").asText(), is(OAuthClientResource.HIDDEN_SECRET));
    }

    /**
     * Wires the mocked {@link org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository}
     * so that the controller's create flow can complete: {@code add} synthesizes an {@code _id},
     * and {@code getByClientId} echoes the in-memory entity used by the request.
     */
    private void stubRepoAddAndLookup() {
        final OAuthClientEntity[] holder = new OAuthClientEntity[1];
        try {
            doAnswer((InvocationOnMock inv) -> {
                OAuthClientEntity e = inv.getArgument(0);
                e.setId(UUID.randomUUID().toString());
                holder[0] = e;
                return null;
            }).when(clientRepo).add(any(OAuthClientEntity.class));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception unreachable) {
            throw new AssertionError(unreachable);
        }
        when(clientRepo.getByClientId(any(String.class))).thenAnswer(inv -> holder[0]);
    }
}
