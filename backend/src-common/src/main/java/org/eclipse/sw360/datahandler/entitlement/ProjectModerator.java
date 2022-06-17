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
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;

/**
 * Moderation for the project service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class ProjectModerator extends Moderator<Project._Fields, Project> {

    private static final Logger log = LogManager.getLogger(ProjectModerator.class);


    public ProjectModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public ProjectModerator(){
        super(new ThriftClients());
    }

    public RequestStatus updateProject(Project project, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createProjectRequest(project, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate project " + project.getId() + " for User " + user.getEmail(), e);
            return  RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteProject(Project project, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createProjectDeleteRequest(project, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete project " + project.getId() + " for User " + user.getEmail(), e);
            return  RequestStatus.FAILURE;
        }
    }

    public String createClearingRequest(ClearingRequest clearingRequest, User user) {
        ModerationService.Iface client = thriftClients.makeModerationClient();
        try {
            return client.createClearingRequest(clearingRequest, user);
        } catch (TException e) {
            log.error("Could not create CR for Project: " + clearingRequest.getProjectId() + " by User " + user.getEmail(), e);
            return null;
        }
    }

    public RequestStatus addCommentToClearingRequest(String id, Comment comment, User user) {
        ModerationService.Iface client = thriftClients.makeModerationClient();
        try {
            return client.addCommentToClearingRequest(id, comment, user);
        } catch (TException e) {
            log.error("Failed to add comment in clearing request: " + id, e);
            return RequestStatus.FAILURE;
        }
    }

    public ClearingRequest getClearingRequestByProjectId(String projectId, User user) {
        ModerationService.Iface client = thriftClients.makeModerationClient();
        try {
            return client.getClearingRequestByProjectId(projectId, user);
        } catch (TException e) {
            log.error("Could not find CR for Project: " + projectId + " by User " + user.getEmail(), e);
            return null;
        }
    }


    public void unlinkClearingRequestForProjectDeletion(Project project, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.updateClearingRequestForProjectDeletion(project, user);
        } catch (TException e) {
            log.error("Failed to unlink CR : " + project.getClearingRequestId() + " for project: " + project.getId() + ", by User " + user.getEmail(), e);
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
