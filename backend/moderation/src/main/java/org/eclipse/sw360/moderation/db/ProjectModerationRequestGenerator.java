/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.services.projects.Project;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ProjectModerationRequestGenerator extends ModerationRequestGenerator<Project> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, Project updateProject, Project actualProject) {
        updateDocument = updateProject;
        actualDocument = actualProject;

        if (request.getProjectAdditions() != null) {
            documentAdditions = request.getProjectAdditions();
        } else {
            documentAdditions = new Project();
        }
        if (request.getProjectDeletions() != null) {
            documentDeletions = request.getProjectDeletions();
        } else {
            documentDeletions = new Project();
        }
        documentAdditions.setName(updateProject.getName());
        documentAdditions.setId(actualProject.getId());
        documentDeletions.setName(actualProject.getName());
        documentDeletions.setId(actualProject.getId());

        for (FieldAccessor<Project> field : PojoFields.of(Project.class)) {
            if (isBoolOrInt(field.type())) {
                if (field.get(actualDocument) != field.get(updateDocument)) {
                    field.set(documentAdditions, field.get(updateDocument));
                    field.set(documentDeletions, field.get(actualDocument));
                }
            } else if (field.get(actualProject) == null) {
                field.set(documentAdditions, field.get(updateProject));
            } else if (field.get(updateProject) == null) {
                field.set(documentDeletions, field.get(actualProject));
            } else if (!field.get(actualProject).equals(field.get(updateProject))) {
                switch (field.name()) {
                    case "permissions":
                    case "documentState":
                    case "releaseClearingStateSummary":
                        break;
                    case "attachments":
                        dealWithAttachments(field);
                        break;
                    case "linkedProjects":
                        dealWithEnumMap(field);
                        break;
                    case "releaseIdToUsage":
                        dealWithStringKeyedMap(field);
                        break;
                    case "externalIds":
                        dealWithStringtoStringMap(field);
                        break;
                    case "additionalData":
                        dealWithStringKeyedMap(field);
                        break;
                    case "externalUrls":
                        dealWithStringtoStringMap(field);
                        break;
                    case "roles":
                        dealWithCustomMap(field);
                        break;
                    default:
                        dealWithBaseTypes(field);
                }
            }
        }

        request.setProjectAdditions(documentAdditions);
        request.setProjectDeletions(documentDeletions);
        return request;
    }
}
