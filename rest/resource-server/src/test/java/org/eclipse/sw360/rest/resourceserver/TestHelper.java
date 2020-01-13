/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 * Copyright Bosch Software Innovations GmbH, 2018.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;

import java.util.Collections;
import java.util.List;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TestHelper {

    static public void checkResponse(String responseBody, String linkRelation, int embeddedArraySize) throws IOException {
        TestHelper.checkResponse(responseBody, linkRelation, embeddedArraySize, null);
    }

    static public void checkResponse(String responseBody, String linkRelation, int embeddedArraySize, List<String> fields) throws IOException {
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat(responseBodyJsonNode.has("_embedded"), is(true));

        JsonNode embeddedNode = responseBodyJsonNode.get("_embedded");
        assertThat(embeddedNode.has("sw360:" + linkRelation), is(true));

        JsonNode sw360UsersNode = embeddedNode.get("sw360:" + linkRelation);
        assertThat(sw360UsersNode.isArray(),is(true));
        assertThat(sw360UsersNode.size(),is(embeddedArraySize));
        if(fields != null && embeddedArraySize > 0) {
            JsonNode itemNode = sw360UsersNode.get(0);
            for(String field:fields) {
                assertTrue(itemNode.has(field));
            }
        }

        assertThat(responseBodyJsonNode.has("_links"), is(true));

        JsonNode linksNode = responseBodyJsonNode.get("_links");
        assertThat(linksNode.has("curies"), is(true));

        JsonNode curiesNode = linksNode.get("curies").get(0);
        assertThat(curiesNode.get("href").asText(), endsWith("docs/{rel}.html"));
        assertThat(curiesNode.get("name").asText(), is("sw360"));
        assertThat(curiesNode.get("templated").asBoolean(), is(true));
    }

    public static String getAccessToken(MockMvc mockMvc, String username, String password) throws Exception {
        String authorizationHeaderValue = "Basic "
                + new String(Base64Utils.encode("trusted-sw360-client:sw360-secret".getBytes()));

        MockHttpServletResponse response = mockMvc
                .perform(post("/oauth/token")
                        .header("Authorization", authorizationHeaderValue)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("client_id", "trusted-sw360-client")
                        .param("client_secret", "sw360-secret")
                        .param("username", username)
                        .param("password", password)
                        .param("grant_type", "password")
                        .param("scope", "all"))
                .andReturn().getResponse();

        return new ObjectMapper()
                .readValue(response.getContentAsByteArray(), OAuthToken.class)
                .accessToken;
    }

    public static void handleBatchDeleteResourcesResponse(ResponseEntity<String> response, String resourceId, int statusCode) throws IOException {
        handleBatchDeleteResourcesResponse(response, Collections.singletonList(new MultiStatus(resourceId, HttpStatus.valueOf(statusCode))));
    }

    public static void handleBatchDeleteResourcesResponse(ResponseEntity<String> response, List<MultiStatus> responseStatusList) throws IOException {
        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());

        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        assertThat(responseNode.isArray(), is(true));
        assertThat(responseNode.size(), is(responseStatusList.size()));

        for (int i = 0; i < responseStatusList.size(); i++) {
            MultiStatus multiStatus = responseStatusList.get(i);
            JsonNode jsonResult = responseNode.get(i);
            assertThat(jsonResult.get("status").asInt(), is(multiStatus.getStatusCode()));
            assertThat(jsonResult.get("resourceId").asText(), is(multiStatus.getResourceId()));
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OAuthToken {
        @JsonProperty("access_token")
        public String accessToken;
    }

}
