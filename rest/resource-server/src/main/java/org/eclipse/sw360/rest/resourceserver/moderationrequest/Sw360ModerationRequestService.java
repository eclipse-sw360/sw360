/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 2023, Siemens AG. Part of the SW360 Portal Project.
 * SPDX-FileContributor: Gaurav Mishra <mishra.gaurav@siemens.com>
 */
package org.eclipse.sw360.rest.resourceserver.moderationrequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class Sw360ModerationRequestService {
    private static final Logger log = LogManager.getLogger(Sw360ModerationRequestService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public static boolean isOpenModerationRequest(@NotNull ModerationRequest moderationRequest) {
        return moderationRequest.getModerationState() == ModerationState.PENDING || moderationRequest.getModerationState() == ModerationState.INPROGRESS;
    }

    private ModerationService.Iface getThriftModerationClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/moderation/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ModerationService.Client(protocol);
    }
    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }

    public ProjectService.Iface getThriftProjectClient() throws TTransportException {
        ProjectService.Iface projectClient = new ThriftClients().makeProjectClient();
        return projectClient;
    }

    private LicenseService.Iface getThriftLicenseClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/licenses/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new LicenseService.Client(protocol);
    }

    private SPDXDocumentService.Iface getThriftSPDXDocumentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/spdxdocument/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new SPDXDocumentService.Client(protocol);
    }

    private DocumentCreationInformationService.Iface getThriftDocumentCreationInfo()  throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/spdxdocumentcreationinfo/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new DocumentCreationInformationService.Client(protocol);
    }

    private PackageInformationService.Iface getThriftPackageInfo()  throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/spdxpackageinfo/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new PackageInformationService.Client(protocol);
    }
    private UserService.Iface getThriftUserClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new UserService.Client(protocol);
    }

    /**
     * Get a moderation request by id if exists. Otherwise raise appropriate exception.
     *
     * @param requestId ID of moderation request to get
     * @return Moderation Request
     * @throws TException Appropriate exception if request does not exists or not accessible.
     */
    public ModerationRequest getModerationRequestById(String requestId) throws TException, TApplicationException {
        ModerationRequest moderationRequest = null;
        try {
            moderationRequest = getThriftModerationClient().getModerationRequestById(requestId);
        } catch (TApplicationException tAppExp) {
            log.error("TApplicationException while fetching moderation request by ID: {}. Exception: {}",
                      requestId, tAppExp.getMessage(), tAppExp);
            throw new ResourceNotFoundException("Requested ModerationRequest not found: " + requestId, tAppExp);
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                log.warn("ModerationRequest not found with ID: {}", requestId);
                throw new ResourceNotFoundException("Requested ModerationRequest not found: " + requestId, sw360Exp);
            } else if (sw360Exp.getErrorCode() == 403) {
                log.warn("Access denied for ModerationRequest or its linked project with ID: {}", requestId);
                throw new AccessDeniedException("ModerationRequest or its Linked Project are restricted and/or not accessible", sw360Exp);
            } else {
                log.error("Unhandled SW360Exception while fetching moderation request by ID: {}. Exception: {}",
                          requestId, sw360Exp.getMessage(), sw360Exp);
                throw sw360Exp;
            }
        } catch (Exception ex) {
            log.error("Unexpected exception while fetching moderation request by ID: {}. Exception: {}",
                      requestId, ex.getMessage(), ex);
            throw new RuntimeException("An unexpected error occurred while fetching the ModerationRequest: " + requestId, ex);
        }
        return moderationRequest;
    }


    /**
     * Get paginated list of moderation requests where user is one of the
     * moderators.
     *
     * @param sw360User Moderator
     * @param pageable  Pageable information from request
     * @return Paginated list of moderation requests.
     * @throws TException Exception in case of error.
     */
    public List<ModerationRequest> getRequestsByModerator(User sw360User, Pageable pageable) throws TException {
        PaginationData pageData = pageableToPaginationData(pageable);
        return getThriftModerationClient().getRequestsByModeratorWithPaginationNoFilter(sw360User, pageData);
    }

    /**
     * Get paginated list of moderation requests where user is the requester.
     * @param sw360User Requester
     * @param pageable  Pageable information from request
     * @return Paginated list of moderation requests.
     * @throws TException Exception in case of error.
     */
    public Map<PaginationData, List<ModerationRequest>> getRequestsByRequestingUser(
            User sw360User, Pageable pageable
    ) throws TException {
        PaginationData pageData = pageableToPaginationData(pageable);
        ModerationService.Iface client = getThriftModerationClient();

        List<ModerationRequest> moderationList = client.
                getRequestsByRequestingUserWithPagination(sw360User, pageData);
        Map<String, Long> countInfo = client.getCountByRequester(sw360User);
        pageData.setTotalRowCount(countInfo.getOrDefault(sw360User.getEmail(), 0L));

        Map<PaginationData, List<ModerationRequest>> result = new HashMap<>();
        result.put(pageData, moderationList);
        return result;
    }

    /**
     * Get total count of moderation requests with user as a moderator.
     *
     * @param sw360User Moderator
     * @return Count of moderation requests
     * @throws TException Throws exception in case of error
     */
    public long getTotalCountOfRequests(User sw360User) throws TException {
        Map<String, Long> countInfo = getThriftModerationClient().getCountByModerationState(sw360User);
        long totalCount = 0;
        totalCount += countInfo.getOrDefault("OPEN", 0L);
        totalCount += countInfo.getOrDefault("CLOSED", 0L);
        return totalCount;
    }

    /**
     * Get total count of moderation requests with user as a moderator and specific requesting user.
     *
     * @param moderator Moderator
     * @param requestingUser Requesting user
     * @return Count of moderation requests
     * @throws TException Throws exception in case of error
     */
    public long getTotalCountByModerationStateAndRequestingUser(User moderator, User requestingUser) throws TException {
        Map<String, Long> countInfo = getThriftModerationClient().getCountByModerationStateAndRequestingUser(moderator, requestingUser);
        long totalCount = 0L;
        totalCount += countInfo.getOrDefault("OPEN", 0L);
        totalCount += countInfo.getOrDefault("CLOSED", 0L);
        return totalCount;
    }

    /**
     * Convert Pageable from spring request to PaginationData
     *
     * @param pageable Pageable from request
     * @return Converted PaginationData
     */
    private static @NotNull PaginationData pageableToPaginationData(@NotNull Pageable pageable) {
        PaginationData pageData = new PaginationData();
        pageData.setRowsPerPage(pageable.getPageSize());
        pageData.setDisplayStart((int) pageable.getOffset());
        pageData.setSortColumnNumber(-1);
        return pageData;
    }

    /**
     * Get list of moderation requests based on moderation state(open/closed)
     * where user is one of the moderators.
     *
     * @param sw360user  Moderator
     * @param pageable   Pagination information
     * @param open       State is open?
     * @param allDetails Need all details?
     * @return Map of pagination information and list of moderation requests
     * @throws TException Exception in case of error.
     */
    public Map<PaginationData, List<ModerationRequest>> getRequestsByState(User sw360user, Pageable pageable,
                                                                           boolean open, boolean allDetails) throws TException {
        PaginationData pageData = pageableToPaginationData(pageable);
        ModerationService.Iface client = getThriftModerationClient();
        Map<PaginationData, List<ModerationRequest>> moderationData;
        if (allDetails) {
            moderationData = client.getRequestsByModeratorWithPaginationAllDetails(sw360user, pageData, open);
        } else {
            moderationData = client.getRequestsByModeratorWithPagination(sw360user, pageData, open);
        }
        Map<String, Long> countInfo = client.getCountByModerationState(sw360user);
        PaginationData paginationData = moderationData.keySet().iterator().next();
        List<ModerationRequest> moderationRequests = moderationData.remove(paginationData);
        if (open) {
            paginationData.setTotalRowCount(countInfo.getOrDefault("OPEN", 0L));
        } else {
            paginationData.setTotalRowCount(countInfo.getOrDefault("CLOSED", 0L));
        }
        moderationData.put(paginationData, moderationRequests);
        return moderationData;
    }

    /**
     * Set moderation state of moderation request to ACCEPTED
     *
     * @param request          Request to accept
     * @param moderatorComment Comments from moderator
     * @param reviewer         Reviewer
     * @return Current status of request
     * @throws TException Exception in case of error.
     */
    public ModerationState acceptRequest(ModerationRequest request, String moderatorComment, @NotNull User reviewer)
            throws TException {
        User userFromRequest = getUserFromRequest(request);
        RequestStatus actionStatus = null;

        try {
            switch (request.getDocumentType()) {
                case COMPONENT: {
                    ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
                    if (request.isRequestDocumentDelete()) {
                        actionStatus = sw360ComponentClient.deleteComponent(request.getDocumentId(), reviewer);
                    } else {
                        actionStatus = sw360ComponentClient.updateComponentFromModerationRequest(
                                request.getComponentAdditions(), request.getComponentDeletions(), reviewer);
                    }
                }
                break;
                case PROJECT: {
                    ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
                    if (request.isRequestDocumentDelete()) {
                        actionStatus = sw360ProjectClient.deleteProject(request.getDocumentId(), reviewer);
                    } else {
                        actionStatus = sw360ProjectClient.updateProjectFromModerationRequest(request.getProjectAdditions(),
                                request.getProjectDeletions(), reviewer);
                    }
                }
                break;
                case RELEASE: {
                    ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
                    if (request.isRequestDocumentDelete()) {
                        actionStatus = sw360ComponentClient.deleteRelease(request.getDocumentId(), reviewer);
                        if (actionStatus.equals(RequestStatus.SUCCESS)) {
                            SW360Utils.removeReleaseVulnerabilityRelation(request.getDocumentId(), userFromRequest);
                        }
                    } else {
                        actionStatus = sw360ComponentClient.updateReleaseFromModerationRequest(
                                request.getReleaseAdditions(), request.getReleaseDeletions(), reviewer);
                    }
                }
                break;
                case LICENSE: {
                    LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
                    actionStatus = sw360LicenseClient.updateLicenseFromModerationRequest(request.getLicenseAdditions(),
                            request.getLicenseDeletions(), reviewer, userFromRequest);
                }
                break;
                case SPDX_DOCUMENT: {
                    SPDXDocumentService.Iface sw360SPDXClient = getThriftSPDXDocumentClient();
                    if (request.isRequestDocumentDelete()) {
                        actionStatus = sw360SPDXClient.deleteSPDXDocument(request.getDocumentId(), reviewer);
                    } else {
                        actionStatus = sw360SPDXClient.updateSPDXDocumentFromModerationRequest(
                                request.getSPDXDocumentAdditions(), request.getSPDXDocumentDeletions(), reviewer);
                    }
                }
                break;
                case SPDX_DOCUMENT_CREATION_INFO: {
                    DocumentCreationInformationService.Iface documentCreationInfoClient = getThriftDocumentCreationInfo();
                    if (request.isRequestDocumentDelete()) {
                        actionStatus = documentCreationInfoClient.deleteDocumentCreationInformation(request.getDocumentId(),
                                reviewer);
                    } else {
                        actionStatus = documentCreationInfoClient.updateDocumentCreationInfomationFromModerationRequest(
                                request.getDocumentCreationInfoAdditions(), request.getDocumentCreationInfoDeletions(),
                                reviewer);
                    }
                }
                break;
                case SPDX_PACKAGE_INFO: {
                    PackageInformationService.Iface packageInfoClient = getThriftPackageInfo();
                    if (request.isRequestDocumentDelete()) {
                        actionStatus = packageInfoClient.deletePackageInformation(request.getDocumentId(), reviewer);
                    } else {
                        actionStatus = packageInfoClient.updatePackageInfomationFromModerationRequest(
                                request.getPackageInfoAdditions(), request.getPackageInfoDeletions(), reviewer);
                    }
                }
                break;
            }
        } catch (SW360Exception sw360Exp) {
            log.error("Failed to process the moderation request." + sw360Exp.getMessage());
            throw sw360Exp;
        }

        if (actionStatus != null && actionStatus.equals(RequestStatus.SUCCESS)) {
            getThriftModerationClient().acceptRequest(request, moderatorComment, reviewer.getEmail());
            return ModerationState.APPROVED;
        } else {
            return ModerationState.REJECTED;
        }
    }

    private User getUserFromRequest(ModerationRequest request) {
        return new User(request.getId(), request.getModerators().toString());
    }

    public User getUserByEmail(String email) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByEmail(email);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByEmailOrExternalId(org.eclipse.sw360.datahandler.thrift.users.UserService.Iface userClient,
            String userIdentifier, String string) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByEmailOrExternalId(userIdentifier, userIdentifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set moderation state of the moderation request to REJECTED
     *
     * @param request          Moderation request to reject
     * @param moderatorComment Comment from moderator
     * @param reviewer         Reviewer
     * @return Current status of the moderation request
     * @throws TException Exception in case of error.
     */
    public ModerationState rejectRequest(@NotNull ModerationRequest request, String moderatorComment,
                                         @NotNull User reviewer) throws TException {
        getThriftModerationClient().refuseRequest(request.getId(), moderatorComment, reviewer.getEmail());
        return ModerationState.REJECTED;
    }

    /**
     * Update the moderation request with the new comment when POSTPONE action
     * is performed and update its state to INPROGRESS.
     *
     * @param request          Moderation request to postpone
     * @param moderatorComment Comment from moderator
     * @return Current status of the moderation request
     * @throws TException Exception in case of error.
     */
    public ModerationState postponeRequest(@NotNull ModerationRequest request,
                                           String moderatorComment) throws TException {
        request.setModerationState(ModerationState.INPROGRESS);
        request.setCommentDecisionModerator(moderatorComment);
        getThriftModerationClient().updateModerationRequest(request);
        return ModerationState.INPROGRESS;
    }

    /**
     * Remove the user from moderator list of the moderation request.
     *
     * @param request  Moderation request to edit
     * @param reviewer User to remove
     * @return Pending status if removal successful
     * @throws SW360Exception Throws exception if user is last moderator of the request.
     */
    public ModerationState removeMeFromModerators(@NotNull ModerationRequest request, @NotNull User reviewer)
            throws SW360Exception {
        RemoveModeratorRequestStatus status = RemoveModeratorRequestStatus.FAILURE;
        try {
            status = getThriftModerationClient().removeUserFromAssignees(request.getId(), reviewer);
        } catch (TException e) {
            log.error("Error in Moderation ", e);
        }
        if (status == RemoveModeratorRequestStatus.LAST_MODERATOR) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT,
                    "You are the last moderator for this request - " +
                    "you are not allowed to unsubscribe.");
        } else if (status == RemoveModeratorRequestStatus.FAILURE) {
            throw new SW360Exception("Failed to remove from moderator list.");
        }
        return ModerationState.PENDING;
    }

    /**
     * Assign a moderation request to the user.
     *
     * @param request  Moderation request to assign
     * @param reviewer User to assign MR to
     * @return Moderation state in-progress if assignment successful.
     * @throws TException Throws exception in case of errors.
     */
    public ModerationState assignRequest(@NotNull ModerationRequest request, @NotNull User reviewer)
            throws TException {
        if (!request.getModerators().contains(reviewer.getEmail())) {
            throw new AccessDeniedException("User is not assigned as a moderator for the request.");
        } else if (!isOpenModerationRequest(request)) {
            throw new InvalidParameterException("Moderation request is not in open state.");
        }
        getThriftModerationClient().setInProgress(request.getId(), reviewer);
        return ModerationState.INPROGRESS;
    }

    /**
     * Get open critical CR count by user department
     *
     * @param group Department of user
     * @return Count of open critical CRs
     * @throws TException Throws exception in case of errors.
     */
    public Integer getOpenCriticalCrCountByGroup(String group) {
        try {
            return getThriftModerationClient().getOpenCriticalCrCountByGroup(group);
        } catch (TException e) {
            log.error("Error in getting open critical CR count by group: ", e);
            return 0;
        }
    }

    public RequestStatus deleteModerationRequestInfo(@NotNull User sw360User, @NotNull String id,
			@NotNull ModerationRequest moderationRequest)
            throws TTransportException, TException {
        RequestStatus requestStatus = null;
        Set<String> moderators = moderationRequest.getModerators();
        String requestingUser = moderationRequest.getRequestingUser();
        ModerationState moderationState = moderationRequest.getModerationState();

        if (moderators.contains(sw360User.getEmail())) {
            if (moderationState == ModerationState.REJECTED || moderationState == ModerationState.APPROVED) {
                requestStatus = getThriftModerationClient().deleteModerationRequest(id, sw360User);
            }
        } else if (!sw360User.getEmail().equals(requestingUser)) {
            if (moderationState == ModerationState.PENDING || moderationState == ModerationState.INPROGRESS) {
                requestStatus = RequestStatus.FAILURE;
            }
        } else {
            requestStatus = getThriftModerationClient().deleteModerationRequest(id, sw360User);
        }

        return requestStatus;
    }
}
