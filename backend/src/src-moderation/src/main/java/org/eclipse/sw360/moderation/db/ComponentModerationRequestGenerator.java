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

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ComponentModerationRequestGenerator extends ModerationRequestGenerator<Component._Fields, Component> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, Component updateComponent, Component actualComponent){
        updateDocument = updateComponent;
        actualDocument = actualComponent;

        documentAdditions = new Component();
        documentDeletions = new Component();
        //required fields:
        documentAdditions.setName(updateComponent.getName());
        documentAdditions.setId(actualComponent.getId());
        documentDeletions.setName(actualComponent.getName());
        documentDeletions.setId(actualComponent.getId());

        for (Component._Fields field : Component._Fields.values()) {

            if(actualComponent.getFieldValue(field) == null){
                documentAdditions.setFieldValue(field, updateComponent.getFieldValue(field));

            } else if (updateComponent.getFieldValue(field) == null){
                documentDeletions.setFieldValue(field, actualComponent.getFieldValue(field));

            } else if(!actualComponent.getFieldValue(field).equals(updateComponent.getFieldValue(field))) {
                switch (field) {
                    //ignored fields
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    //releases and related fields are not updated via moderation requests
                    case RELEASES:
                    case RELEASE_IDS:
                    case MAIN_LICENSE_IDS:
                    case LANGUAGES:
                    case OPERATING_SYSTEMS:
                    case VENDOR_NAMES:
                        break;
                    case ATTACHMENTS:
                        dealWithAttachments(Component._Fields.ATTACHMENTS);
                        break;
                    case ROLES:
                        dealWithCustomMap(Component._Fields.ROLES);
                        break;
                    case EXTERNAL_IDS:
                        dealWithStringtoStringMap(Component._Fields.EXTERNAL_IDS);
                        break;
                    case ADDITIONAL_DATA:
                        dealWithStringKeyedMap(Component._Fields.ADDITIONAL_DATA);
                        break;
                    default:
                        dealWithBaseTypes(field, Component.metaDataMap.get(field));
                }
            }
        }

        request.setComponentAdditions(documentAdditions);
        request.setComponentDeletions(documentDeletions);
        return request;
    }
}
