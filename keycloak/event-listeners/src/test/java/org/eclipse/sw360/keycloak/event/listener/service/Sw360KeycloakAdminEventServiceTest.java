/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.event.listener.service;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.keycloak.common.Sw360UserService;
import org.junit.jupiter.api.Test;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import static org.eclipse.sw360.keycloak.common.KeycloakConstants.ProviderService.LISTENER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class Sw360KeycloakAdminEventServiceTest {
    private final Sw360UserService users = mock(Sw360UserService.class);
    private final Sw360KeycloakAdminEventService service = new Sw360KeycloakAdminEventService(
            users, JsonMapper.builderWithJackson2Defaults().build(), mock(KeycloakSession.class));

    @Test
    void shouldConvertUserRepresentation() {
        AdminEvent event = new AdminEvent();
        event.setResourceType(ResourceType.USER);
        event.setRepresentation("""
                {"email":"user@example.org","firstName":"Example","lastName":"User",
                 "attributes":{"department":["Engineering"],"externalid":["external"]},
                 "ignoredField":"ignored"}
                """);

        service.updateUserOperation(event);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(users).createOrUpdateUser(user.capture(), eq(LISTENER));
        assertEquals("user@example.org", user.getValue().getEmail());
        assertEquals("Example User", user.getValue().getFullname());
    }

    @Test
    void shouldNotUpdateUserForMalformedRepresentation() {
        AdminEvent event = new AdminEvent();
        event.setResourceType(ResourceType.USER);
        event.setRepresentation("{");

        service.updateUserOperation(event);

        verify(users, never()).createOrUpdateUser(org.mockito.ArgumentMatchers.any(), eq(LISTENER));
    }
}
