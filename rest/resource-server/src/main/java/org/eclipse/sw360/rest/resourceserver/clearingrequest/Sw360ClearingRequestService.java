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
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
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

}
