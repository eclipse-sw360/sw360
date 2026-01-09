/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.listener;

import org.eclipse.sw360.keycloak.event.listener.service.Sw360KeycloakAdminEventService;
import org.eclipse.sw360.keycloak.event.listener.service.Sw360KeycloakUserEventService;
import org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Custom Event Listener Provider for SW360.
 * @author smruti.sahoo@siemens.com
 */
public class Sw360CustomEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(Sw360CustomEventListenerProvider.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    Sw360UserService userService;
    Sw360KeycloakAdminEventService keycloakAdminEventService;
    Sw360KeycloakUserEventService keycloakUserEventService;

    public Sw360CustomEventListenerProvider(KeycloakSession keycloakSession) {
        this.userService = new Sw360UserService();
        keycloakAdminEventService = new Sw360KeycloakAdminEventService(userService, objectMapper, keycloakSession);
        keycloakUserEventService = new Sw360KeycloakUserEventService(userService, objectMapper, keycloakSession);
    }

    /**
     * This method is called when an admin event is triggered.
     * @param event to be triggered
     * @param includeRepresentation when false, event listener should NOT include representation field in the resulting
     *                              action
     */
    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        log.debugf("CustomEventListenerSW360:::onEvent(_,_) called!" ,event.toString());
        log.debugf("onEvent() -->Resource Type::: ", event.getResourceType());
        if (ResourceType.USER.equals(event.getResourceType())) {
            if (OperationType.CREATE.equals(event.getOperationType())) {
                keycloakAdminEventService.createUserOperation(event);
            } else if (OperationType.UPDATE.equals(event.getOperationType())) {
                keycloakAdminEventService.updateUserOperation(event);
            } else if (OperationType.ACTION.equals(event.getOperationType())) {
                keycloakAdminEventService.actionUserOperation(event);
            } else {
                log.debug("User Details:::(? Event)" + event.getOperationType());
            }
        } else if (ResourceType.GROUP_MEMBERSHIP.equals(event.getResourceType())) {
            keycloakAdminEventService.groupMembershipOperationAdminEvent(event);
        }
    }

    /**
     * This method is called when a non-admin event is triggered.
     * @param event to be triggered
     */
    @Override
    public void onEvent(Event event) {
        log.debug("CustomEventListenerSW360:::onEvent(_) called!");
        log.debugf("Event client Id: %s, Event Type: %s", event.getClientId(), event.getType());
        if (EventType.REGISTER.equals(event.getType())) {
            keycloakUserEventService.userRegistrationEvent(event);
        } else if (EventType.LOGIN.equals(event.getType())) {
            keycloakUserEventService.userLoginEvent(event);
        }
        log.debug("CustomEventListenerSW360:::Exiting onEvent(_)::");
    }

    public void close() {
        log.debug("CustomEventListenerSW360:::close() called!");
    }

}
