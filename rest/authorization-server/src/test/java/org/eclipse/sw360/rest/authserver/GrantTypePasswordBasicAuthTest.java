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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.IOException;

/**
 * A POST request for an access token with grant type 'password' and basic auth
 * should be possible.
 */
public class GrantTypePasswordBasicAuthTest extends GrantTypePasswordTestBase {

    @Value("${sw360.test-user-id}")
    protected String testUserId;

    @Value("${sw360.test-user-password}")
    protected String testUserPassword;

    @Before
    public void before() throws IOException {
        String url = "http://localhost:" + String.valueOf(port) + "/oauth/token?grant_type=" + PARAMETER_GRANT_TYPE
                + "&username=" + testUserId + "&password=" + testUserPassword;

        responseEntity = new TestRestTemplate(clientId, clientSecret).postForEntity(url, null, String.class);
    }

}
