/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ComponentModerationRequestGenerator extends ModerationRequestGenerator<Component> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, Component updateComponent, Component actualComponent) {
        updateDocument = updateComponent;
        actualDocument = actualComponent;

        documentAdditions = new Component();
        documentDeletions = new Component();
        documentAdditions.setName(updateComponent.getName());
        documentAdditions.setId(actualComponent.getId());
        documentDeletions.setName(actualComponent.getName());
        documentDeletions.setId(actualComponent.getId());

        for (FieldAccessor<Component> field : PojoFields.of(Component.class)) {
            if (actualComponentValue(field) == null) {
                field.set(documentAdditions, field.get(updateComponent));
            } else if (field.get(updateComponent) == null) {
                field.set(documentDeletions, field.get(actualComponent));
            } else if (!field.get(actualComponent).equals(field.get(updateComponent))) {
                switch (field.name()) {
                    case "permissions":
                    case "documentState":
                    case "releases":
                    case "releaseIds":
                    case "mainLicenseIds":
                    case "languages":
                    case "operatingSystems":
                    case "vendorNames":
                        break;
                    case "attachments":
                        dealWithAttachments(field);
                        break;
                    case "roles":
                        dealWithCustomMap(field);
                        break;
                    case "externalIds":
                        dealWithStringtoStringMap(field);
                        break;
                    case "additionalData":
                        dealWithStringKeyedMap(field);
                        break;
                    default:
                        dealWithBaseTypes(field);
                }
            }
        }

        request.setComponentAdditions(documentAdditions);
        request.setComponentDeletions(documentDeletions);
        return request;
    }

    private Object actualComponentValue(FieldAccessor<Component> field) {
        return field.get(actualDocument);
    }
}
