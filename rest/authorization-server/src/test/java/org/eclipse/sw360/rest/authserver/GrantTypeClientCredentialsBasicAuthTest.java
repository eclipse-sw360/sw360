/*
* SPDX-FileCopyrightText: © 2024 Siemens AG
* SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver;
import org.eclipse.sw360.rest.authserver.IntegrationTestBase;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GrantTypeClientCredentialsBasicAuthTest extends IntegrationTestBase {

    private final String clientId = "trusted-sw360-client";
    private final String clientSecret = "sw360-secret";
    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void successfulTokenRetrieval() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret, StandardCharsets.UTF_8);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);
        String url = "http://localhost:" + port + "/oauth2/token";

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void unauthorizedDueToInvalidCredentials() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("invalidClientId", "invalidClientSecret", StandardCharsets.UTF_8);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);
        String url = "http://localhost:" + port + "/oauth2/token";

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void badRequestDueToMissingGrantType() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret, StandardCharsets.UTF_8);

        HttpEntity<String> request = new HttpEntity<>("", headers);
        String url = "http://localhost:" + port + "/oauth2/token";

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }
}