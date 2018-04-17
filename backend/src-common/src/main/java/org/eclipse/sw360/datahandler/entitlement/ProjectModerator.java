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
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 * Moderation for the project service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class ProjectModerator extends Moderator<Project._Fields, Project> {

    private static final Logger log = Logger.getLogger(ProjectModerator.class);


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

    public Project updateProjectFromModerationRequest(Project project, Project projectAdditions, Project projectDeletions){

        for (Project._Fields field : Project._Fields.values()) {
            if(!projectAdditions.isSet(field) && !projectDeletions.isSet(field)){
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
