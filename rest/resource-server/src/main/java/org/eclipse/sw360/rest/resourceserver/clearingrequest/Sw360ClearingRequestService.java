/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.clearingrequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.CommentConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.projects.ClearingRequestConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.clients.users.UsersClient;
import org.eclipse.sw360.datahandler.moderation.ModerationClient;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Sw360ClearingRequestService {
    private static final Logger log = LogManager.getLogger(Sw360ClearingRequestService.class);

    private final UsersClient usersClient;

    private ModerationClient moderationClient() {
        return ModerationClients.get();
    }

    public ClearingRequest getClearingRequestByProjectId(String projectId, User sw360User) throws TException {
        try {
            return ClearingRequestConverter.toThrift(
                    moderationClient().getClearingRequestByProjectId(projectId, UserConverter.fromThrift(sw360User)));
        } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested ClearingRequest not found");
            } else if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException(
                        "ClearingRequest or its Linked Project are restricted and / or not accessible");
            } else {
                log.error("Error fetching clearing request by project id: " + sw360Exp.getMessage());
                throw sw360Exp;
            }
        }
    }

    public ClearingRequest getClearingRequestById(String id, User sw360User) throws TException {
        try {
            return ClearingRequestConverter.toThrift(
                    moderationClient().getClearingRequestById(id, UserConverter.fromThrift(sw360User)));
        } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested ClearingRequest not found");
            } else if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException(
                        "ClearingRequest or its Linked Project are restricted and / or not accessible");
            } else {
                log.error("Error fetching clearing request by id: " + sw360Exp.getMessage());
                throw sw360Exp;
            }
        }
    }

    public Set<ClearingRequest> getMyClearingRequests(User sw360User, ClearingRequestState state) throws TException {
        ModerationClient client = moderationClient();
        var userPojo = UserConverter.fromThrift(sw360User);
        Set<ClearingRequest> clearingrequests = client.getMyClearingRequests(userPojo).stream()
                .map(ClearingRequestConverter::toThrift)
                .collect(Collectors.toCollection(HashSet::new));
        clearingrequests.addAll(client.getClearingRequestsByBU(sw360User.getDepartment()).stream()
                .map(ClearingRequestConverter::toThrift)
                .collect(Collectors.toSet()));
        if (state != null) {
            clearingrequests = clearingrequests.parallelStream()
                    .filter(cr -> cr.getClearingState() == state)
                    .collect(Collectors.toSet());
        }
        return clearingrequests;
    }

    public ClearingRequest addCommentToClearingRequest(String crId, Comment comment, User sw360User) throws TException {
        if (crId == null || crId.isBlank()) {
            throw new IllegalArgumentException("Clearing request ID cannot be null or empty.");
        }
        if (comment.getText() == null || comment.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty.");
        }

        comment.setCommentedBy(sw360User.getEmail());
        comment.setText(comment.getText().trim());

        RequestStatus requestStatus = RequestStatusConverter.toThrift(
                moderationClient().addCommentToClearingRequest(
                        crId, CommentConverter.fromThrift(comment), UserConverter.fromThrift(sw360User)));
        if (requestStatus != RequestStatus.SUCCESS) {
            throw new TException("Error adding comment to clearing request");
        }
        return getClearingRequestById(crId, sw360User);
    }

    public RequestStatus updateClearingRequest(ClearingRequest clearingRequest, User sw360User, String baseUrl, String projectId) throws TException {
        String projectUrl = baseUrl + "/projects/-/project/detail/" + projectId;

        RequestStatus requestStatus = RequestStatusConverter.toThrift(
                moderationClient().updateClearingRequest(
                        ClearingRequestConverter.fromThrift(clearingRequest),
                        UserConverter.fromThrift(sw360User),
                        projectUrl));

        if (requestStatus == RequestStatus.FAILURE) {
            throw new RuntimeException("Clearing Request with id '" + clearingRequest.getId() + " cannot be updated.");
        }
        return requestStatus;
    }

    public void convertTimestampAndEmail(ClearingRequest clearingRequest) throws TException {
        List<Comment> comments = clearingRequest.getComments();
        if (comments != null && !comments.isEmpty()) {
            for (Comment comment : comments) {
                String convertTimestampToDateTime = convertTimestampToDateTime(comment.getCommentedOn());
                comment.setDateTime(convertTimestampToDateTime);

                String email = comment.getCommentedBy();
                if (email != null && !email.isEmpty()) {
                    String convertEmailToUsername = getUserNameByEmail(email);
                    comment.setUsername(convertEmailToUsername);
                }
            }
        }
    }

    public String getUserNameByEmail(String userEmail) {
        org.eclipse.sw360.datahandler.services.users.User sw360User = usersClient.getByEmail(userEmail);
        return sw360User.getFullname();
    }

    public static String convertTimestampToDateTime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        String iso8601Format = DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(instant);
        return iso8601Format;
    }

    public Map<PaginationData, List<ClearingRequest>> getRecentClearingRequestsWithPagination(
            User sw360User, Pageable pageable) throws TException {
        PaginationData pageData = pageableToPaginationData(pageable);
        PaginatedResult<org.eclipse.sw360.datahandler.services.projects.ClearingRequest> page =
                moderationClient().getRecentClearingRequestsWithPagination(
                        UserConverter.fromThrift(sw360User), PaginationDataConverter.fromThrift(pageData));
        return toClearingRequestPageMap(page, pageData);
    }

    public Map<PaginationData, List<ClearingRequest>> searchClearingRequestsByFilters(
            User sw360User, Map<String, Set<String>> filterMap, Pageable pageable) throws TException {
        PaginationData pageData = pageableToPaginationData(pageable);
        PaginatedResult<org.eclipse.sw360.datahandler.services.projects.ClearingRequest> page =
                moderationClient().searchClearingRequestsByFilters(
                        UserConverter.fromThrift(sw360User), filterMap, PaginationDataConverter.fromThrift(pageData));
        return toClearingRequestPageMap(page, pageData);
    }

    private static Map<PaginationData, List<ClearingRequest>> toClearingRequestPageMap(
            PaginatedResult<org.eclipse.sw360.datahandler.services.projects.ClearingRequest> page,
            PaginationData fallback) {
        Map<PaginationData, List<ClearingRequest>> result = new HashMap<>();
        PaginationData pd = page != null && page.getPaginationData() != null
                ? PaginationDataConverter.toThrift(page.getPaginationData())
                : fallback;
        List<ClearingRequest> list = page == null || page.getData() == null
                ? new ArrayList<>()
                : new ArrayList<>(page.getData().stream().map(ClearingRequestConverter::toThrift).toList());
        result.put(pd, list);
        return result;
    }

    /**
     * Converts a Pageable object to a PaginationData object for clearing requests.
     *
     * @param pageable the Pageable object to convert
     * @return a PaginationData object representing the pagination information
     */
    private static PaginationData pageableToPaginationData(@NotNull Pageable pageable) {
        boolean ascending = false;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            ascending = order.isAscending();
        }

        return new PaginationData()
                .setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize())
                .setAscending(ascending);
    }
}
