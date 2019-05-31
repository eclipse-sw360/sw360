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
package org.eclipse.sw360.rest.authserver.client.persistence;

import org.eclipse.sw360.rest.authserver.IntegrationTestBase;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Of course it would be possible to test only the repo down to the db without
 * the spring context. But this way we use the convenience of springs property
 * loading and overwriting in tests.
 *
 * Currently we are only testing the setup anyway, since we do not have custom
 * repo logic yet - beside the ektorp supported CRUD logic.
 *
 * ATTENTION: This test should be executed manually when a couchdb is running.
 * Make sure to not have the OAuthClientRepository as Mockito mock in the
 * context.
 */
public class OAuthClientRepositoryTest extends IntegrationTestBase {

    @Autowired
    private OAuthClientRepository uut;

    @Before
    public void setup() {
        uut.getAll().stream().forEach(uut::remove);
    }

    // just to satisfy subclass expectations of JUnit
    @Test
    public void testNoop() {
        assertTrue(true);
    }

    // @Test
    public void testInsertNewClient() {
        // given:
        String clientId = "foo";
        OAuthClientEntity client = new OAuthClientEntity();
        client.setId(clientId);

        // when:
        uut.add(client);

        // then:
        List<OAuthClientEntity> actualClients = uut.getAll();
        assertThat(actualClients.size(), is(1));
        assertThat(actualClients.get(0).getId(), is(clientId));
    }

}
