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

import org.eclipse.sw360.common.utils.converter.common.CommentConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.projects.ClearingRequestConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Moderation for the project service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class ProjectModerator extends Moderator<Project._Fields, Project> {

    private static final Logger log = LogManager.getLogger(ProjectModerator.class);

    public RequestStatus updateProject(Project project, User user) {
        try {
            ModerationClients.get().createProjectRequest(
                    ProjectConverter.fromThrift(project), UserConverter.fromThrift(user));
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate project " + project.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteProject(Project project, User user) {
        try {
            ModerationClients.get().createProjectDeleteRequest(
                    ProjectConverter.fromThrift(project), UserConverter.fromThrift(user));
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate delete project " + project.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public String createClearingRequest(ClearingRequest clearingRequest, User user) {
        try {
            return ModerationClients.get().createClearingRequest(
                    ClearingRequestConverter.fromThrift(clearingRequest), UserConverter.fromThrift(user));
        } catch (SW360Exception e) {
            log.error("Could not create CR for Project: " + clearingRequest.getProjectId() + " by User " + user.getEmail(), e);
            return null;
        }
    }

    public RequestStatus addCommentToClearingRequest(String id, Comment comment, User user) {
        try {
            return RequestStatusConverter.toThrift(ModerationClients.get().addCommentToClearingRequest(
                    id, CommentConverter.fromThrift(comment), UserConverter.fromThrift(user)));
        } catch (SW360Exception e) {
            log.error("Failed to add comment in clearing request: " + id, e);
            return RequestStatus.FAILURE;
        }
    }

    public ClearingRequest getClearingRequestByProjectId(String projectId, User user) {
        try {
            return ClearingRequestConverter.toThrift(ModerationClients.get().getClearingRequestByProjectId(
                    projectId, UserConverter.fromThrift(user)));
        } catch (SW360Exception e) {
            log.error("Could not find CR for Project: " + projectId + " by User " + user.getEmail(), e);
            return null;
        }
    }

    public void updateClearingRequestForChangeInProjectBU(String crId, String businessUnit, User user) {
        try {
            ModerationClients.get().updateClearingRequestForChangeInProjectBU(
                    crId, businessUnit, UserConverter.fromThrift(user));
        } catch (SW360Exception e) {
            log.error("Failed to update project BU in CR : " + crId + ", by User " + user.getEmail(), e);
        }
    }

    public void unlinkClearingRequestForProjectDeletion(Project project, User user) {
        try {
            ModerationClients.get().updateClearingRequestForProjectDeletion(
                    ProjectConverter.fromThrift(project), UserConverter.fromThrift(user));
        } catch (SW360Exception e) {
            log.error("Failed to unlink CR : " + project.getClearingRequestId() + " for project: " + project.getId()
                    + ", by User " + user.getEmail(), e);
        }
    }

    public Project updateProjectFromModerationRequest(Project project, Project projectAdditions, Project projectDeletions){

        for (Project._Fields field : Project._Fields.values()) {
            if(!projectAdditions.isSet(field) && !projectDeletions.isSet(field)){
                continue;
            }

            if (field == Project._Fields.VISBILITY && projectAdditions != null && projectDeletions != null
                    && projectAdditions.getVisbility() == projectDeletions.getVisbility()) {
                continue;
            }

            switch (field) {
                case LINKED_PROJECTS:
                    project = updateEnumMap(
                            Project._Fields.LINKED_PROJECTS,
                            ProjectRelationship.class,
                            project,
                            projectAdditions,
                            projectDeletions);
                    break;
                case RELEASE_ID_TO_USAGE:
                    project = updateStringMap(
                            Project._Fields.RELEASE_ID_TO_USAGE,
                            project,
                            projectAdditions,
                            projectDeletions);
                    break;
                case ATTACHMENTS:
                    project.setAttachments( updateAttachments(
                            project.getAttachments(),
                            projectAdditions.getAttachments(),
                            projectDeletions.getAttachments()));
                    break;
                default:
                    project = updateBasicField(
                            field,
                            Project.metaDataMap.get(field),
                            project,
                            projectAdditions,
                            projectDeletions);
            }

        }
        return project;
    }
}
