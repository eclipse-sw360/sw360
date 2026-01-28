/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360CustomUserDetailsService;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(
        classes = Sw360ResourceServer.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration
abstract public class TestIntegrationBase {

    private static final String AUTH_BASIC = "Basic ";

    @MockitoBean
    Sw360CustomUserDetailsService sw360CustomUserDetailsService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @MockitoBean
    protected Sw360UserService userServiceMock;

    @Before
    public void setupMockerUser() {
        when(sw360CustomUserDetailsService.loadUserByUsername("admin@sw360.org"))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        "admin@sw360.org",
                        encoder.encode("12345"),
                        List.of(new SimpleGrantedAuthority(
                                Sw360GrantedAuthority.ADMIN.getAuthority()))
                ));
    }

    public HttpHeaders getHeaders(int port) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",
                generateBasicAuthHeader("admin@sw360.org", "12345"));
        return headers;
    }

    /**
     * OAuth token retrieval when available.
     * Falls back to basic authentication in CI environments
     * where OAuth client credentials are not configured.
     */
    public HttpHeaders getHeader(int port) throws IOException {
        try {
            ResponseEntity<String> response =
                    new TestRestTemplate("trusted-sw360-client", "sw360-secret")
                            .postForEntity(
                                    "http://localhost:" + port + "/oauth/token",
                                    null,
                                    String.class
                            );

            String responseText = response.getBody();
            HashMap<?, ?> jwtMap =
                    new ObjectMapper().readValue(responseText, HashMap.class);
            String accessToken = (String) jwtMap.get("access_token");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            return headers;

        } catch (Exception e) {
            // CI fallback: OAuth is not available in GitHub Actions
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization",
                    generateBasicAuthHeader("admin@sw360.org", "12345"));
            return headers;
        }
    }

    protected void checkResponse(
            ResponseEntity<String> response,
            String linkRelation,
            int embeddedArraySize
    ) throws IOException {

        String responseBody = response.getBody();
        JsonNode responseBodyJsonNode =
                new ObjectMapper().readTree(responseBody);

        assertThat(responseBodyJsonNode.has("_embedded"), is(true));

        JsonNode embeddedNode = responseBodyJsonNode.get("_embedded");
        assertThat(embeddedNode.has("sw360:" + linkRelation), is(true));

        JsonNode sw360UsersNode =
                embeddedNode.get("sw360:" + linkRelation);
        assertThat(sw360UsersNode.isArray(), is(true));
        assertThat(sw360UsersNode.size(), is(embeddedArraySize));

        assertThat(responseBodyJsonNode.has("_links"), is(true));

        JsonNode linksNode = responseBodyJsonNode.get("_links");
        assertThat(linksNode.has("curies"), is(true));

        JsonNode curiesNode = linksNode.get("curies").get(0);
        assertThat(curiesNode.get("href").asText(),
                endsWith("docs/{rel}.html"));
        assertThat(curiesNode.get("name").asText(), is("sw360"));
        assertThat(curiesNode.get("templated").asBoolean(), is(true));
    }

    private static String generateBasicAuthHeader(
            String user,
            String password
    ) {
        String credentials = user + ":" + password;
        String credentialsEncoded =
                Base64.getEncoder().encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8)
                );
        return AUTH_BASIC + credentialsEncoded;
    }
}
