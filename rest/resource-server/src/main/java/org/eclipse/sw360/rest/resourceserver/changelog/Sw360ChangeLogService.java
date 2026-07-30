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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.changelogs.ChangeLogsClient;
import org.eclipse.sw360.datahandler.changelogs.ChangeLogsClients;
import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.changelogs.ChangelogSortColumn;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class Sw360ChangeLogService {
    private static final Logger log = LogManager.getLogger(Sw360ChangeLogService.class);

    private ChangeLogsClient client() {
        return ChangeLogsClients.get();
    }

    public List<ChangeLogs> getChangeLogsByDocumentId(String docId, User sw360User) {
        return client().getChangeLogsByDocumentId(docId, UserConverter.fromThrift(sw360User));
    }

    public Map<PaginationData, List<ChangeLogs>> getChangeLogsByDocumentIdPaginated(String docId, User sw360User, Pageable pageable) {
        PaginationData pageData = pageableToPaginationData(pageable);
        PaginatedResult<ChangeLogs> result = client().getChangeLogsByDocumentIdPaginated(
                docId, UserConverter.fromThrift(sw360User), pageData);

        if (result == null) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(result.getPaginationData(), result.getData());
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
                .setRowsPerPage(pageable.getPageSize()).setSortColumnNumber(column.ordinal()).setAscending(ascending);
    }
}
