/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver;

import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * A POST request for an access token with grant type 'client_credentials' and
 * custom auth header should NOT be possible.
 */
public class GrantTypeClientCredentialsCustomHeaderAuthTest extends IntegrationTestBase {

    @Test
    public void should_not_login_with_custom_header() throws IOException {
        String url = "http://localhost:" + String.valueOf(port)
                + "/oauth/token?grant_type=client_credentials&client_id=" + testClient.getClientId() + "&client_secret="
                + testClient.getClientSecret();

        // since we do not have a proxy that sets the header during test, we set it
        // already on client-side
        HttpHeaders headers = new HttpHeaders();
        headers.add("authenticated-email", adminTestUser.email);

        responseEntity = new TestRestTemplate().postForEntity(url, new HttpEntity<>(headers), String.class);

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

}
