/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.authserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer.CONFIG_CLIENT_ID;
import static org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer.CONFIG_CLIENT_SECRET;
import static org.eclipse.sw360.rest.authserver.security.Sw360SecurityEncryptor.decrypt;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Sw360AuthorizationServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev"})
public abstract class IntegrationTestBase {

    @Value("${local.server.port}")
    protected int port;

    @Value("${security.oauth2.client.client-id}")
    protected String clientId;

    @Value("${security.oauth2.client.client-secret}")
    protected String clientSecret;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    @MockBean
    protected ThriftClients thriftClients;

    protected User testUser;

    protected ResponseEntity<String> responseEntity;

    protected String getClientId() {
        return CONFIG_CLIENT_ID != null ? CONFIG_CLIENT_ID : clientId;
    }

    protected String getClientSecret() throws IOException {
        return CONFIG_CLIENT_SECRET != null ? decrypt(CONFIG_CLIENT_SECRET) : clientSecret;
    }

    @Before
    public void setup() throws TException {
        testUser = new User("mockedserviceuser@sw360.org", "qa");
        testUser.externalid = "service-mocked-by-mockito";
        testUser.fullname = "Mocked Service User";
        testUser.givenname = "Mocked";
        testUser.lastname = "Service User";
        testUser.userGroup = UserGroup.ADMIN;

        UserService.Client mockedUserService = mock(UserService.Client.class);
        when(mockedUserService.getByEmailOrExternalId(eq(testUser.email), anyString())).thenReturn(testUser);

        when(thriftClients.makeUserClient()).thenReturn(mockedUserService);
    }

    protected void checkResponseBody() throws IOException {
        String responseBody = responseEntity.getBody();

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat(responseBodyJsonNode.get("token_type").asText(), is("bearer"));
        assertThat(responseBodyJsonNode.get("scope").asText(), is(Sw360GrantedAuthority.READ.toString()));
        assertThat(responseBodyJsonNode.has("access_token"), is(true));
        assertThat(responseBodyJsonNode.has("expires_in"), is(true));
        assertThat(responseBodyJsonNode.has("jti"), is(true));
    }

    protected JsonNode checkJwtClaims(String... expectedAuthority) throws IOException {
        String responseBody = responseEntity.getBody();

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);
        assertThat(responseBodyJsonNode.has("access_token"), is(true));

        String accessToken = responseBodyJsonNode.get("access_token").asText();
        Jwt jwt = JwtHelper.decode(accessToken);
        String jwtClaims = jwt.getClaims();
        JsonNode jwtClaimsJsonNode = new ObjectMapper().readTree(jwtClaims);
        assertThat(jwtClaimsJsonNode.get("aud").get(0).asText(), is("sw360-REST-API"));
        assertThat(jwtClaimsJsonNode.get("client_id").asText(), is("trusted-sw360-client"));

        JsonNode scopesNode = jwtClaimsJsonNode.get("scope");
        List<String> actualScopes = new ArrayList<>();
        if (scopesNode.isArray()) {
            for (final JsonNode scopeNode : scopesNode) {
                actualScopes.add(scopeNode.asText());
            }
        } else {
            actualScopes.add(scopesNode.asText());
        }
        assertThat(actualScopes, containsInAnyOrder(Sw360GrantedAuthority.READ.toString()));

        JsonNode authoritiesJsonNode = jwtClaimsJsonNode.get("authorities");
        List<String> actualAuthorities = new ArrayList<>();
        if (authoritiesJsonNode.isArray()) {
            for (final JsonNode authorityTextNode : authoritiesJsonNode) {
                actualAuthorities.add(authorityTextNode.asText());
            }
        } else {
            actualAuthorities.add(authoritiesJsonNode.asText());
        }
        assertThat(actualAuthorities, containsInAnyOrder(expectedAuthority));

        return jwtClaimsJsonNode;
    }
}
