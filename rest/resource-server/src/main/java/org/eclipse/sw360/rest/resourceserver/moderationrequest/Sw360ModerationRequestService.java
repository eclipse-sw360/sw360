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

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
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

    /**
     * Get a moderation request by id if exists. Otherwise raise appropriate exception.
     *
     * @param requestId ID of moderation request to get
     * @return Moderation Request
     * @throws TException Appropriate exception if request does not exists or not accessible.
     */
    public ModerationRequest getModerationRequestById(String requestId) throws TException {
        try {
            return getThriftModerationClient().getModerationRequestById(requestId);
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested ModerationRequest not found");
            } else if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException(
                        "ModerationRequest or its Linked Project are restricted and / or not accessible");
            } else {
                log.error("Error fetching moderation request by id: " + sw360Exp.getMessage());
                throw sw360Exp;
            }
        }
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
        getThriftModerationClient().acceptRequest(request, moderatorComment, reviewer.getEmail());
        return ModerationState.APPROVED;
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
     * @param department Department of user
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
}
