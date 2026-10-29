/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.entitlement;

import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.users.User;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Moderation for the component service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class ComponentModerator extends Moderator {

    private static final Logger log = LogManager.getLogger(ComponentModerator.class);

    public RequestStatus updateComponent(Component component, User user) {
        try {
            ModerationClients.get().createComponentRequest(component, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate component " + component.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public org.eclipse.sw360.datahandler.services.components.Component updateComponentFromModerationRequest(
            org.eclipse.sw360.datahandler.services.components.Component component,
            org.eclipse.sw360.datahandler.services.components.Component componentAdditions,
            org.eclipse.sw360.datahandler.services.components.Component componentDeletions) {
        if (componentAdditions == null) {
            componentAdditions = new org.eclipse.sw360.datahandler.services.components.Component();
        }
        if (componentDeletions == null) {
            componentDeletions = new org.eclipse.sw360.datahandler.services.components.Component();
        }

        for (FieldAccessor<org.eclipse.sw360.datahandler.services.components.Component> field : PojoFields
                .of(org.eclipse.sw360.datahandler.services.components.Component.class)) {
            if (!field.isSet(componentAdditions) && !field.isSet(componentDeletions)) {
                continue;
            }

            switch (field.name()) {
                case "id":
                case "revision":
                case "type":
                case "createdBy":
                case "createdOn":
                case "permissions":
                case "documentState":
                    // Releases and aggregates:
                case "releases":
                case "releaseIds":
                case "mainLicenseIds":
                case "languages":
                case "operatingSystems":
                case "vendorNames":
                    break;
                case "attachments":
                    component.setAttachments(updatePojoAttachments(
                            component.getAttachments(),
                            componentAdditions.getAttachments(),
                            componentDeletions.getAttachments()));
                    break;
                default:
                    applyBasicField(field, component, componentAdditions, componentDeletions);
            }
        }
        return component;
    }
    public RequestStatus deleteComponent(Component component, User user) {
        try {
            ModerationClients.get().createComponentDeleteRequest(component, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate delete component " + component.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

}
