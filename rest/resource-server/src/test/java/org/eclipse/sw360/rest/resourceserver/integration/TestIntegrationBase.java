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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SpringBootTest(classes = Sw360ResourceServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration
abstract public class TestIntegrationBase {

    public HttpHeaders getHeaders(int port) throws IOException {
        ResponseEntity<String> response =
                new TestRestTemplate("trusted-sw360-client", "sw360-secret")
                        .postForEntity("http://localhost:" + port + "/oauth/token?grant_type=password&username=admin@sw360.org&password=sw360-password",
                                null,
                                String.class);

        String responseText = response.getBody();
        HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
        String accessToken = (String) jwtMap.get("access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }

    protected void checkResponse(ResponseEntity<String> response, String linkRelation, int embeddedArraySize) throws IOException {
        String responseBody = response.getBody();
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat(responseBodyJsonNode.has("_embedded"), is(true));

        JsonNode embeddedNode = responseBodyJsonNode.get("_embedded");
        assertThat(embeddedNode.has("sw360:" + linkRelation), is(true));

        JsonNode sw360UsersNode = embeddedNode.get("sw360:" + linkRelation);
        assertThat(sw360UsersNode.isArray(),is(true));
        assertThat(sw360UsersNode.size(),is(embeddedArraySize));

        assertThat(responseBodyJsonNode.has("_links"), is(true));

        JsonNode linksNode = responseBodyJsonNode.get("_links");
        assertThat(linksNode.has("curies"), is(true));

        JsonNode curiesNode = linksNode.get("curies").get(0);
        assertThat(curiesNode.get("href").asText(), endsWith("docs/{rel}.html"));
        assertThat(curiesNode.get("name").asText(), is("sw360"));
        assertThat(curiesNode.get("templated").asBoolean(), is(true));
    }
}
