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

import org.junit.Before;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.io.IOException;

/**
 * A POST request for an access token with grant type 'password' and custom auth
 * header should be possible.
 */
public class GrantTypePasswordCustomHeaderPostTest extends GrantTypePasswordTestBase {

    @Before
    public void before() throws IOException {
        String url = "http://localhost:" + String.valueOf(port) + "/oauth/token?grant_type=" + PARAMETER_GRANT_TYPE
                + "&client_id=" + testClient.getClientId() + "&client_secret=" + testClient.getClientSecret();

        // since we do not have a proxy that sets the header during test, we set it
        // already on client-side
        HttpHeaders headers = new HttpHeaders();
        headers.add("authenticated-email", adminTestUser.email);

        responseEntity = new TestRestTemplate().postForEntity(url, new HttpEntity<>(headers), String.class);
    }
}
