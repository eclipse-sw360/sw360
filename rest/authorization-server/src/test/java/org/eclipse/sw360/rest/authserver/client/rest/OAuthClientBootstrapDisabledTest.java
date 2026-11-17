/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import org.eclipse.sw360.rest.authserver.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@TestPropertySource(properties = "sw360.security.client-bootstrap.enabled=false")
public class OAuthClientBootstrapDisabledTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate template;

    @Test
    public void bootstrapClient_disabledFlagDeniesAnonymousRequest() {
        assertThat(template.postForEntity(
                "/client-management/bootstrap", "{}", String.class).getStatusCode(),
                is(HttpStatus.FORBIDDEN));
    }
}
