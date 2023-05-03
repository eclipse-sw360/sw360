/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ModerationSearchHandler;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.moderation.db.ModerationDatabaseHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final ModerationSearchHandler modSearchHandler;

    public ModerationHandler() throws IOException {
        handler = new ModerationDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
        modSearchHandler = new ModerationSearchHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
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
    public RequestStatus createSPDXDocumentRequest(SPDXDocument spdx, User user) throws TException {
        assertUser(user);
        assertNotNull(spdx);

        return handler.createRequest(spdx, user, false);
    }

    @Override
    public void createSPDXDocumentDeleteRequest(SPDXDocument spdx, User user) throws TException {
        assertUser(user);
        assertNotNull(spdx);

        handler.createRequest(spdx, user, true);
    }

    @Override
    public RequestStatus createSpdxDocumentCreationInfoRequest(DocumentCreationInformation documentCreationInfo, User user) throws TException {
        assertUser(user);
        assertNotNull(documentCreationInfo);

        return handler.createRequest(documentCreationInfo, user, false);
    }

    @Override
    public void createSpdxDocumentCreationInfoDeleteRequest(DocumentCreationInformation documentCreationInfo, User user) throws TException {
        assertUser(user);
        assertNotNull(documentCreationInfo);

        handler.createRequest(documentCreationInfo, user, true);
    }

    @Override
    public RequestStatus createSpdxPackageInfoRequest(PackageInformation packageInfo, User user) throws TException {
        assertUser(user);
        assertNotNull(packageInfo);

        return handler.createRequest(packageInfo, user, false);
    }

    @Override
    public void createSpdxPackageInfoDeleteRequest(PackageInformation packageInfo, User user) throws TException {
        assertUser(user);
        assertNotNull(packageInfo);

        handler.createRequest(packageInfo, user, true);
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

    @Override
    public ClearingRequest getClearingRequestByProjectId(String projectId, User user) throws TException {
        assertId(projectId);
        assertUser(user);

        return handler.getClearingRequestByProjectId(projectId, user);
    }

    @Override
    public Set<ClearingRequest> getMyClearingRequests(User user) throws TException {
        assertUser(user);

        return handler.getMyClearingRequests(user.getEmail());
    }

    @Override
    public Set<ClearingRequest> getClearingRequestsByBU(String businessUnit) throws TException {
        assertNotEmpty(businessUnit);

        return handler.getClearingRequestsByBU(businessUnit);
    }

    @Override
    public int getOpenCriticalCrCountByGroup(String group) throws TException {
        // user department is being passed here
        assertNotEmpty(group);
        return handler.getOpenCriticalCrCountByGroup(group);
    }

    @Override
    public String createClearingRequest(ClearingRequest clearingRequest, User user) throws TException {
        assertNotNull(clearingRequest);
        assertEmpty(clearingRequest.getId());
        assertUser(user);

        return handler.createClearingRequest(clearingRequest, user);
    }

    @Override
    public ClearingRequest getClearingRequestById(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.getClearingRequestById(id, user);
    }

    @Override
    public ClearingRequest getClearingRequestByIdForEdit(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.getClearingRequestByIdForEdit(id, user);
    }

    @Override
    public RequestStatus updateClearingRequest(ClearingRequest clearingRequest, User user, String projectUrl) throws TException {
        assertNotNull(clearingRequest);
        assertId(clearingRequest.getId());
        assertNotEmpty(projectUrl);
        assertUser(user);

        return handler.updateClearingRequest(clearingRequest, user, projectUrl);
    }

    @Override
    public void updateClearingRequestForProjectDeletion(Project project, User user) throws TException {
        assertNotNull(project);
        assertId(project.getClearingRequestId());
        assertUser(user);

        handler.updateClearingRequestForProjectDeletion(project, user);
    }

    @Override
    public void updateClearingRequestForChangeInProjectBU(String crId, String businessUnit, User user) throws TException {
        assertId(crId);
        assertNotNull(businessUnit);
        assertUser(user);

        handler.updateClearingRequestForChangeInProjectBU(crId, businessUnit, user);
    }

    @Override
    public RequestStatus addCommentToClearingRequest(String id, Comment comment, User user) throws TException {
        assertId(id);
        assertNotNull(comment);
        assertUser(user);

        return handler.addCommentToClearingRequest(id, comment, user);
    }

    @Override
    public List<ModerationRequest> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions)
            throws TException {
        return modSearchHandler.search(text, subQueryRestrictions);
    }

    @Override
    public Map<String, Long> getCountByModerationState(User user) throws TException {
        assertUser(user);
        return handler.getCountByModerationState(user.getEmail());
    }

    @Override
    public Map<PaginationData, List<ModerationRequest>> getRequestsByModeratorWithPagination(User user,
            PaginationData pageData, boolean open) throws TException {
        assertUser(user);

        return handler.getRequestsByModerator(user.getEmail(), pageData, open);
    }

    @Override
    public Set<String> getRequestingUserDepts() {
        return handler.getRequestingUserDepts();
    }
}
