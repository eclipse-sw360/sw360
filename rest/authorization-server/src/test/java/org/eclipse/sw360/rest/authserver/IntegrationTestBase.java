/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Sw360AuthorizationServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev"})
abstract public class IntegrationTestBase {

    @Value("${local.server.port}")
    protected int port;

    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;

    protected ResponseEntity<String> getTokenWithParameters(String parameters) {
        String url = "http://localhost:" + port + "/oauth/token?" + parameters;
        return new TestRestTemplate(clientId, clientSecret).postForEntity(url, null, String.class);
    }

    protected void checkResponseBody(ResponseEntity<String> responseEntity) throws IOException {
        String responseBody = responseEntity.getBody();

        assertThat(HttpStatus.OK, is(responseEntity.getStatusCode()));

        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat(responseBodyJsonNode.get("token_type").asText(), is("bearer"));
        assertThat(responseBodyJsonNode.get("scope").asText(), is("all"));
        assertThat(responseBodyJsonNode.has("access_token"), is(true));
        assertThat(responseBodyJsonNode.has("expires_in"), is(true));
        assertThat(responseBodyJsonNode.has("jti"), is(true));
    }

    protected JsonNode checkJwtClaims(ResponseEntity<String> responseEntity, String expectedAuthority) throws IOException {
        String responseBody = responseEntity.getBody();

        assertThat(HttpStatus.OK, is(responseEntity.getStatusCode()));

        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);
        assertThat(responseBodyJsonNode.has("access_token"), is(true));

        String accessToken = responseBodyJsonNode.get("access_token").asText();
        Jwt jwt = JwtHelper.decode(accessToken);
        String jwtClaims = jwt.getClaims();
        JsonNode jwtClaimsJsonNode = new ObjectMapper().readTree(jwtClaims);
        assertThat(jwtClaimsJsonNode.get("aud").get(0).asText(), is("sw360-REST-API"));
        assertThat(jwtClaimsJsonNode.get("client_id").asText(), is("trusted-sw360-client"));

        JsonNode scopeNode = jwtClaimsJsonNode.get("scope");
        assertThat(scopeNode.get(0).asText(), is("all"));
        assertThat(scopeNode.size(), is(1));

        JsonNode authoritiesJsonNode = jwtClaimsJsonNode.get("authorities");
        assertThat(authoritiesJsonNode.get(0).asText(), is(expectedAuthority));

        return jwtClaimsJsonNode;
    }
}
