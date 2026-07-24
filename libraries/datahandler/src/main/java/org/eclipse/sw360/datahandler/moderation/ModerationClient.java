/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.moderation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.Comment;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the moderation backend service.
 * 
 * Callers (for example entitlement moderators and the public REST layer) use this
 * interface instead of the former Thrift {@code ModerationService.Iface}. Types are
 * service-api POJOs. The live implementation talks to the moderation WAR over HTTP;
 * see {@link ModerationServiceRestClient} and {@link ModerationClients}.
 */
public interface ModerationClient {

    RequestStatus createComponentRequest(Component component, User user);

    RequestStatus createReleaseRequest(Release release, User user);

    RequestStatus createReleaseRequestForEcc(Release release, User user);

    RequestStatus createProjectRequest(Project project, User user);

    RequestStatus createLicenseRequest(License license, User user);

    RequestStatus createSPDXDocumentRequest(SPDXDocument spdx, User user);

    RequestStatus createSpdxDocumentCreationInfoRequest(DocumentCreationInformation documentCreationInfo, User user);

    RequestStatus createSpdxPackageInfoRequest(PackageInformation packageInfo, User user);

    void createUserRequest(User user);

    void createComponentDeleteRequest(Component component, User user);

    void createReleaseDeleteRequest(Release release, User user);

    void createProjectDeleteRequest(Project project, User user);

    void createSPDXDocumentDeleteRequest(SPDXDocument spdx, User user);

    void createSpdxDocumentCreationInfoDeleteRequest(DocumentCreationInformation documentCreationInfo, User user);

    void createSpdxPackageInfoDeleteRequest(PackageInformation packageInfo, User user);

    List<ModerationRequest> getModerationRequestByDocumentId(String documentId);

    ModerationRequest getModerationRequestById(String id);

    RequestStatus updateModerationRequest(ModerationRequest moderationRequest);

    RequestStatus acceptRequest(ModerationRequest request, String moderationDecisionComment, String reviewer);

    void refuseRequest(String requestId, String moderationDecisionComment, String reviewer);

    RemoveModeratorRequestStatus removeUserFromAssignees(String requestId, User user);

    void cancelInProgress(String requestId);

    void setInProgress(String requestId, User user);

    void deleteRequestsOnDocument(String documentId);

    List<ModerationRequest> getRequestsByModerator(User user);

    List<ModerationRequest> getRequestsByModeratorWithPaginationNoFilter(User user, PaginationData pageData);

    PaginatedResult<ModerationRequest> getRequestsByModeratorWithPagination(User user, PaginationData pageData,
            boolean open);

    PaginatedResult<ModerationRequest> getRequestsByModeratorWithPaginationAllDetails(User user,
            PaginationData pageData, boolean open);

    List<ModerationRequest> getRequestsByRequestingUser(User user);

    List<ModerationRequest> getRequestsByRequestingUserWithPagination(User user, PaginationData pageData);

    RequestStatus deleteModerationRequest(String id, User user);

    RequestStatus deleteClearingRequest(String id, User user);

    String createClearingRequest(ClearingRequest clearingRequest, User user);

    RequestStatus updateClearingRequest(ClearingRequest clearingRequest, User user, String projectUrl);

    Set<ClearingRequest> getMyClearingRequests(User user);

    Set<ClearingRequest> getClearingRequestsByBU(String businessUnit);

    ClearingRequest getClearingRequestByProjectId(String projectId, User user);

    void updateClearingRequestForProjectDeletion(Project project, User user);

    void updateClearingRequestForChangeInProjectBU(String crId, String businessUnit, User user);

    ClearingRequest getClearingRequestById(String id, User user);

    ClearingRequest getClearingRequestByIdForEdit(String id, User user);

    RequestStatus addCommentToClearingRequest(String id, Comment comment, User user);

    List<ModerationRequest> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData);

    List<ModerationRequest> searchModerationRequestsByExactValues(Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData);

    Map<String, Long> getCountByModerationState(User user);

    Map<String, Long> getCountByRequester(User user);

    Map<String, Long> getCountByModerationStateAndRequestingUser(User moderator, User requestingUser);

    Set<String> getRequestingUserDepts();

    int getOpenCriticalCrCountByGroup(String group);

    PaginatedResult<ClearingRequest> getRecentClearingRequestsWithPagination(User user, PaginationData pageData);

    PaginatedResult<ClearingRequest> searchClearingRequestsByFilters(User user, Map<String, Set<String>> filterMap,
            PaginationData pageData);
}
