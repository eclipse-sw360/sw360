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

package org.eclipse.sw360.moderation;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.moderation.db.ModerationDatabaseHandler;

import java.net.MalformedURLException;
import java.util.List;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;


/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ModerationHandler implements ModerationService.Iface {

    private final ModerationDatabaseHandler handler;
    /*private final DocumentDatabaseHandler documentHandler;*/

    public ModerationHandler() throws MalformedURLException {
        handler = new ModerationDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
        /*documentHandler = new DocumentDatabaseHandler(DatabaseSettings.COUCH_DB_URL, DatabaseSettings.COUCH_DB_DATABASE);*/
    }

    @Override
    public RequestStatus createComponentRequest(Component component, User user) throws TException {
        assertUser(user);
        assertNotNull(component);

        return handler.createRequest(component, user, false);
    }

    @Override
    public RequestStatus createReleaseRequest(Release release, User user) throws TException {
        assertUser(user);
        assertNotNull(release);

        return handler.createRequest(release, user, false);
    }

    @Override
    public RequestStatus createReleaseRequestForEcc(Release release, User user) throws TException {
        assertUser(user);
        assertNotNull(release);

        return handler.createRequest(release, user, false, handler.getEccModeratorsProvider());
    }

    @Override
    public RequestStatus createProjectRequest(Project project, User user) throws TException {
        assertUser(user);
        assertNotNull(project);

        return handler.createRequest(project, user, false);
    }

    @Override
    public RequestStatus createLicenseRequest(License license, User user) throws TException {
        assertUser(user);
        assertNotNull(license);

        return handler.createRequest(license, user);
    }

    @Override
    public void createUserRequest(User user) throws TException {
        assertUser(user);

        handler.createRequest(user);
    }

    @Override
    public void createComponentDeleteRequest(Component component, User user) throws TException {
        assertUser(user);
        assertNotNull(component);

        handler.createRequest(component, user, true);
    }

    @Override
    public void createReleaseDeleteRequest(Release release, User user) throws TException {
        assertUser(user);
        assertNotNull(release);

        handler.createRequest(release, user, true);
    }

    @Override
    public void createProjectDeleteRequest(Project project, User user) throws TException {
        assertUser(user);
        assertNotNull(project);

        handler.createRequest(project, user, true);
    }

    @Override
    public List<ModerationRequest> getModerationRequestByDocumentId(String documentId) throws TException {
        assertId(documentId);

        return handler.getRequestByDocumentId(documentId);
    }

    @Override
    public RequestStatus acceptRequest(ModerationRequest request, String moderationComment, String reviewer) throws TException {
        handler.acceptRequest(request, moderationComment, reviewer);
        return RequestStatus.SUCCESS;
    }

    @Override
    public RequestStatus updateModerationRequest(ModerationRequest moderationRequest) throws TException {
        handler.updateModerationRequest(moderationRequest);
        return RequestStatus.SUCCESS;
    }

    @Override
    public ModerationRequest getModerationRequestById(String id) throws TException {
        return handler.getRequest(id);
    }

    @Override
    public void refuseRequest(String requestId, String moderationDecisionComment, String reviewer) throws TException {
        handler.refuseRequest(requestId, moderationDecisionComment, reviewer);
    }

    @Override
    public RemoveModeratorRequestStatus removeUserFromAssignees(String requestId, User user) throws TException {
        ModerationRequest request = handler.getRequest(requestId);
        if(request.getModerators().size()==1){
            return RemoveModeratorRequestStatus.LAST_MODERATOR;
        }
        request.getModerators().remove(user.getEmail());
        request.setModerationState(ModerationState.PENDING);
        request.unsetReviewer();
        handler.updateModerationRequest(request);
        return RemoveModeratorRequestStatus.SUCCESS;
    }

    @Override
    public void cancelInProgress(String requestId) throws TException {
        ModerationRequest request = handler.getRequest(requestId);
        request.setModerationState(ModerationState.PENDING);
        request.unsetReviewer();
        handler.updateModerationRequest(request);
    }

    @Override
    public void setInProgress(String requestId, User user) throws TException {
        ModerationRequest request = handler.getRequest(requestId);
        request.setModerationState(ModerationState.INPROGRESS);
        request.setReviewer(user.getEmail());
        handler.updateModerationRequest(request);
    }

    @Override
    public void deleteRequestsOnDocument(String documentId) throws TException {
        assertId(documentId);

        handler.deleteRequestsOnDocument(documentId);
    }

    @Override
    public RequestStatus deleteModerationRequest(String id, User user) throws SW360Exception{
        assertUser(user);

        return handler.deleteModerationRequest(id,user);
    }

    @Override
    public List<ModerationRequest> getRequestsByModerator(User user) throws TException {
        assertUser(user);

        return handler.getRequestsByModerator(user.getEmail());
    }

    @Override
    public List<ModerationRequest> getRequestsByRequestingUser(User user) throws TException {
        assertUser(user);

        return handler.getRequestsByRequestingUser(user.getEmail());
    }

}
