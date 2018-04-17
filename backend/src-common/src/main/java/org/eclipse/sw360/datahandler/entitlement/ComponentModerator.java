/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.entitlement;

import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 * Moderation for the component service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class ComponentModerator extends Moderator<Component._Fields, Component> {

    private static final Logger log = Logger.getLogger(ComponentModerator.class);

    public ComponentModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public ComponentModerator() {
        super(new ThriftClients());
    }

    public RequestStatus updateComponent(Component component, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createComponentRequest(component, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate component " + component.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public Component updateComponentFromModerationRequest(Component component,
                                                          Component componentAdditions,
                                                          Component componentDeletions){

        for (Component._Fields field : Component._Fields.values()) {
            if(componentAdditions.getFieldValue(field) == null && componentDeletions.getFieldValue(field) == null){
                continue;
            }

            switch (field) {
                case ID:
                case REVISION:
                case TYPE:
                case CREATED_BY:
                case CREATED_ON:
                case PERMISSIONS:
                case DOCUMENT_STATE:
                    //Releases and aggregates:
                case RELEASES:
                case RELEASE_IDS:
                case MAIN_LICENSE_IDS:
                case LANGUAGES:
                case OPERATING_SYSTEMS:
                case VENDOR_NAMES:
                    break;
                case ATTACHMENTS:
                    component.setAttachments( updateAttachments(
                            component.getAttachments(),
                            componentAdditions.getAttachments(),
                            componentDeletions.getAttachments()));
                    break;
                default:
                    component = updateBasicField(field, Component.metaDataMap.get(field), component, componentAdditions, componentDeletions);
            }

        }
        return component;
    }
    public RequestStatus deleteComponent(Component component, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createComponentDeleteRequest(component, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete component " + component.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

}
