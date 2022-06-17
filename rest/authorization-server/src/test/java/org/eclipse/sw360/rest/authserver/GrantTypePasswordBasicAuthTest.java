/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver;

import org.junit.Before;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.io.IOException;

/**
 * A POST request for an access token with grant type 'password' and basic auth
 * should be possible.
 */
public class GrantTypePasswordBasicAuthTest extends GrantTypePasswordTestBase {

    @Before
    public void before() throws IOException {
        String url = "http://localhost:" + String.valueOf(port) + "/oauth/token?grant_type=" + PARAMETER_GRANT_TYPE
                + "&username=" + adminTestUser.email + "&password=password-not-checked-in-test-without-liferay";

        responseEntity = new TestRestTemplate(testClient.getClientId(), testClient.getClientSecret()).postForEntity(url,
                null, String.class);
    }

}
