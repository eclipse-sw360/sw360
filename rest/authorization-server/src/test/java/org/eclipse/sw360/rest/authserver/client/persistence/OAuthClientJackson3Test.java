/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.authserver.client.persistence;

import com.ibm.cloud.cloudant.v1.model.Document;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class OAuthClientJackson3Test {

    @Test
    void shouldPreserveStoredOAuthClientDocument() {
        OAuthClientEntity entity = new OAuthClientEntity();
        entity.setId("client-id");
        entity.setRev("1-revision");
        entity.setClientId("client");
        entity.setClientSecret("encoded");
        entity.setDescription("description");
        entity.setSecretRequired(true);
        entity.setScoped(true);
        entity.setResourceIds(Set.of("resource"));
        entity.setAuthorizedGrantTypes(Set.of("client_credentials"));
        entity.setScope(Set.of("READ"));
        entity.setAuthorities(Set.of("READ"));
        entity.setRegisteredRedirectUri(Set.of("https://example.org"));
        entity.setAutoApproveScopes(Set.of("READ"));
        entity.setAccessTokenValiditySeconds(60);
        entity.setRefreshTokenValiditySeconds(120);
        entity.setOwnerEmail("user@example.org");
        DatabaseConnectorCloudant connector = mock(DatabaseConnectorCloudant.class, CALLS_REAL_METHODS);

        Document document = connector.getDocumentFromPojo(entity);
        OAuthClientEntity actual = connector.getPojoFromDocument(document, OAuthClientEntity.class);
        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

        assertEquals("client-id", document.getId());
        assertEquals("1-revision", document.getRev());
        assertEquals(mapper.readTree("""
                {"client_id":"client","client_secret":"encoded","description":"description",
                 "resource_ids":["resource"],"authorized_grant_types":["client_credentials"],
                 "authorities":["READ"],"scope":["READ"],"secretRequired":true,"scoped":true,
                 "redirect_uri":["https://example.org"],"access_token_validity":60,
                 "refresh_token_validity":120,"autoapprove":["READ"],"owner_email":"user@example.org"}
                """), mapper.readTree(mapper.writeValueAsString(document.getProperties())));
        assertEquals("client", document.getProperties().get("client_id"));
        assertEquals(entity.getClientId(), actual.getClientId());
        assertEquals(entity.getClientSecret(), actual.getClientSecret());
        assertEquals(entity.getScope(), actual.getScope());
        assertEquals(entity.getAuthorities(), actual.getAuthorities());
        assertEquals(entity.getOwnerEmail(), actual.getOwnerEmail());
        assertEquals(entity.getId(), actual.getId());
        assertEquals(entity.getRev(), actual.getRev());
    }

    @Test
    void shouldReadLegacyRoleObjectsAndOptionalFields() {
        String json = """
                {"_id":"id","_rev":"1-rev","client_id":"client","client_secret":null,
                 "description":null,"secretRequired":true,"scoped":false,
                 "authorities":["READ",{"role":"WRITE"}],
                 "resource_ids":[],"authorized_grant_types":["client_credentials"],
                 "scope":["READ"],"redirect_uri":[],"autoapprove":[]}
                """;

        OAuthClientEntity actual = JsonMapper.builderWithJackson2Defaults().build()
                .readValue(json, OAuthClientEntity.class);

        assertEquals("null", actual.getClientSecret());
        assertEquals("null", actual.getDescription());
        assertEquals(Set.of("READ", "WRITE"), actual.getAuthorities());
        assertEquals(Set.of("client_credentials"), actual.getAuthorizedGrantTypes());
        assertNull(actual.getOwnerEmail());
        assertNull(actual.getAccessTokenValiditySeconds());
    }
}
