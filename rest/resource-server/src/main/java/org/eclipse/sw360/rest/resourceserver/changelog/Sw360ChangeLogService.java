/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.changelog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangelogSortColumn;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Sw360ChangeLogService {
    private static final Logger log = LogManager.getLogger(Sw360ChangeLogService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    private ChangeLogsService.Iface getThriftChangeLogClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/changelogs/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ChangeLogsService.Client(protocol);
    }

    public List<ChangeLogs> getChangeLogsByDocumentId(String docId, User sw360User) throws TException {
        return getThriftChangeLogClient().getChangeLogsByDocumentId(sw360User, docId);
    }

    public Map<PaginationData, List<ChangeLogs>> getChangeLogsByDocumentIdPaginated(String docId, User sw360User, Pageable pageable) throws TException {
        PaginationData pageData = pageableToPaginationData(pageable);
        return getThriftChangeLogClient().getChangeLogsByDocumentIdPaginated(sw360User, docId, pageData);
    }

    /**
     * Converts a Pageable object to a PaginationData object.
     *
     * @param pageable the Pageable object to convert
     * @return a PaginationData object representing the pagination information
     */
    private static PaginationData pageableToPaginationData(@NotNull Pageable pageable) {
        ChangelogSortColumn column = ChangelogSortColumn.BY_CHANGE_TIMESTAMP;
        boolean ascending = false;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            ascending = order.isAscending();
        }
        return new PaginationData().setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize()).setSortColumnNumber(column.getValue()).setAscending(ascending);
    }
}
