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
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestSize;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ClearingRequestService {
    private static final Logger log = LogManager.getLogger(Sw360ClearingRequestService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    private ModerationService.Iface getThriftModerationClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/moderation/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ModerationService.Client(protocol);
    }

    public ClearingRequest getClearingRequestByProjectId(String projectId, User sw360User) throws TException {
        try {
            return getThriftModerationClient().getClearingRequestByProjectId(projectId, sw360User);
        } catch (SW360Exception sw360Exp) {
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
            return getThriftModerationClient().getClearingRequestById(id, sw360User);
        } catch (SW360Exception sw360Exp) {
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
        Set<ClearingRequest> clearingrequests = new HashSet<>(getThriftModerationClient().getMyClearingRequests(sw360User));
        clearingrequests.addAll(getThriftModerationClient().getClearingRequestsByBU(sw360User.getDepartment()));
        if (state!= null) {
            clearingrequests = clearingrequests.parallelStream().filter(cr -> { return cr.getClearingState() == state; }).collect(Collectors.toSet());
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

        ModerationService.Iface clearingRequestClient = getThriftModerationClient();
        RequestStatus requestStatus = clearingRequestClient.addCommentToClearingRequest(crId, comment, sw360User);
        if (requestStatus != RequestStatus.SUCCESS) {
            throw new TException("Error adding comment to clearing request");
        }
        return getClearingRequestById(crId, sw360User);
    }

    public RequestStatus updateClearingRequest(ClearingRequest clearingRequest, User sw360User, String baseUrl, String projectId) throws TException {
        ModerationService.Iface sw360ModerationClient = getThriftModerationClient();
        String projectUrl = baseUrl + "/projects/-/project/detail/" + projectId;

        RequestStatus requestStatus;
        requestStatus = sw360ModerationClient.updateClearingRequest(clearingRequest, sw360User, projectUrl);

        if (requestStatus == RequestStatus.FAILURE) {
            throw new RuntimeException("Clearing Request with id '" + clearingRequest.getId() + " cannot be updated.");
        }
        return requestStatus;
    }

    public void updateClearingRequestForChangeInClearingSize(String crId, ClearingRequestSize size) throws TException{
        try {
            getThriftModerationClient().updateClearingRequestForChangeInClearingSize(crId, size);
        } catch (SW360Exception e) {
            log.error("Error updating clearing request for change in clearing size: " + e.getMessage());
        }
    }
}
