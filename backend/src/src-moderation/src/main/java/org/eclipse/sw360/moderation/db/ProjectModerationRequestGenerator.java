/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.moderation.db;

import org.apache.thrift.protocol.TType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ProjectModerationRequestGenerator extends ModerationRequestGenerator<Project._Fields, Project> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, Project updateProject, Project actualProject){
        updateDocument = updateProject;
        actualDocument = actualProject;

        documentAdditions = new Project();
        documentDeletions = new Project();
        //required fields:
        documentAdditions.setName(updateProject.getName());
        documentAdditions.setId(actualProject.getId());
        documentDeletions.setName(actualProject.getName());
        documentDeletions.setId(actualProject.getId());

        for (Project._Fields field : Project._Fields.values()) {

            if(Project.metaDataMap.get(field).valueMetaData.type == TType.BOOL ||
                    Project.metaDataMap.get(field).valueMetaData.type == TType.I32){
                if(actualDocument.getFieldValue(field) != updateDocument.getFieldValue(field)){
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                }
            } else if(actualProject.getFieldValue(field) == null){
                documentAdditions.setFieldValue(field, updateProject.getFieldValue(field));

            } else if (updateProject.getFieldValue(field) == null){
                documentDeletions.setFieldValue(field,actualProject.getFieldValue(field));

            } else if(!actualProject.getFieldValue(field).equals(updateProject.getFieldValue(field))) {
                switch (field) {
                    //ignored fields and concluded fields
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case RELEASE_CLEARING_STATE_SUMMARY:
                        break;
                    case ATTACHMENTS:
                        dealWithAttachments(Project._Fields.ATTACHMENTS);
                        break;
                    case LINKED_PROJECTS:
                        dealWithEnumMap(Project._Fields.LINKED_PROJECTS, ProjectRelationship.class);
                        break;
                    case RELEASE_ID_TO_USAGE:
                        dealWithStringKeyedMap(Project._Fields.RELEASE_ID_TO_USAGE);
                        break;
                    case EXTERNAL_IDS:
                        dealWithStringtoStringMap(Project._Fields.EXTERNAL_IDS);
                        break;
                    case ADDITIONAL_DATA:
                        dealWithStringKeyedMap(Project._Fields.ADDITIONAL_DATA);
                        break;
                    case ROLES:
                        dealWithCustomMap(Project._Fields.ROLES);
                        break;
                    default:
                        dealWithBaseTypes(field, Project.metaDataMap.get(field));
                }
            }
        }

        request.setProjectAdditions(documentAdditions);
        request.setProjectDeletions(documentDeletions);
        return request;
    }
}
