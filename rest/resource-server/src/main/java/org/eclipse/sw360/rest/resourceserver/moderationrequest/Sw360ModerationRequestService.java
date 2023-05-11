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
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ModerationRequestService {
    private static final Logger log = LogManager.getLogger(Sw360ModerationRequestService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    private ModerationService.Iface getThriftModerationClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/moderation/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ModerationService.Client(protocol);
    }

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

    public List<ModerationRequest> getRequestsByModerator(User sw360User, Pageable pageable) throws TException {
        PaginationData pageData = pageableToPaginationData(pageable);
        return getThriftModerationClient().getRequestsByModeratorWithPaginationNoFilter(sw360User, pageData);
    }

    /**
     * Get total count of moderation requests with user as a moderator.
     *
     * @param sw360User Moderator
     * @return Count of moderation requests
     * @throws TException
     */
    public long getTotalCountOfRequests(User sw360User) throws TException {
        Map<String, Long> countInfo = getThriftModerationClient().getCountByModerationState(sw360User);
        long totalCount = 0;
        totalCount += countInfo.get("OPEN");
        totalCount += countInfo.get("CLOSED");
        return totalCount;
    }

    private static @NotNull PaginationData pageableToPaginationData(@NotNull Pageable pageable) {
        PaginationData pageData = new PaginationData();
        pageData.setRowsPerPage(pageable.getPageSize());
        pageData.setDisplayStart((int) pageable.getOffset());
        pageData.setSortColumnNumber(-1);
        return pageData;
    }

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
            paginationData.setTotalRowCount(countInfo.get("OPEN"));
        } else {
            paginationData.setTotalRowCount(countInfo.get("CLOSED"));
        }
        moderationData.put(paginationData, moderationRequests);
        return moderationData;
    }
}
