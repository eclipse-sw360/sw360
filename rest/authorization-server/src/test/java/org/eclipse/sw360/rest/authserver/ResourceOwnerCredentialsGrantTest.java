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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority.READ;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ResourceOwnerCredentialsGrantTest extends IntegrationTestBase {

    @Value("${local.server.port}")
    private int port;

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    private ResponseEntity<String> responseEntity;

    @Before
    public void before() {
        String parameters = "grant_type=password&username=%s&password=%s";
        responseEntity = getTokenWithParameters(String.format(parameters, testUserId, testUserPassword));
    }

    @Test
    public void should_connect_to_authorization_server_with_resource_owner_credentials() {
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void should_get_expected_response_headers() throws IOException {
        checkResponseBody(responseEntity);
    }

    @Test
    public void should_get_expected_jwt_attributes() throws IOException {
        JsonNode jwtClaimsJsonNode = checkJwtClaims(responseEntity, READ.getAuthority());
        assertThat(jwtClaimsJsonNode.get("user_name").asText(), is(testUserId));
    }
}
