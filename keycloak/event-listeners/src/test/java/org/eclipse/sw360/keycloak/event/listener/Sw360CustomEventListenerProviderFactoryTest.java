/*
 * Copyright Siemens AG, 2024-2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.event.listener;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

/**
 * JUnit tests for Sw360CustomEventListenerProviderFactory.
 * Tests factory lifecycle and configuration handling.
 */
public class Sw360CustomEventListenerProviderFactoryTest {

    private Sw360CustomEventListenerProviderFactory factory;

    @Mock
    private KeycloakSession mockSession;

    @Mock
    private KeycloakSessionFactory mockSessionFactory;

    @Mock
    private Config.Scope mockConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        factory = new Sw360CustomEventListenerProviderFactory();
    }

    @Test
    public void testFactoryBasics() {
        assertEquals("sw360-add-user-to-couchdb", factory.getId());
        EventListenerProvider provider = factory.create(mockSession);
        assertNotNull(provider);
        assertTrue(provider instanceof Sw360CustomEventListenerProvider);
    }

    @Test
    public void testLifecycle() {
        factory.init(mockConfig);
        factory.postInit(mockSessionFactory);
        assertNotNull(factory.create(mockSession));
        factory.close();
        assertTrue(true);
    }
}
