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

import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority.BASIC;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class GrantTypeClientCredentialsTestBase extends IntegrationTestBase {

    protected final String PARAMETER_GRANT_TYPE = "client_credentials";

    @Test
    public void should_connect_to_authorization_server_with_client_credentials() {
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void should_get_expected_response_headers() throws IOException {
        checkResponseBody();
    }

    @Test
    public void should_get_expected_jwt_attributes() throws IOException {
        checkJwtClaims(BASIC.getAuthority());
    }
}
