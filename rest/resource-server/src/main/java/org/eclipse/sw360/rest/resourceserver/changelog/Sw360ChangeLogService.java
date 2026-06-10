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
import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.changelogs.ChangelogSortColumn;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class Sw360ChangeLogService {
    private static final Logger log = LogManager.getLogger(Sw360ChangeLogService.class);

    private final RestClient restClient;
    private final String CHANGELOGS_URI = "/changelogs/api/changelogs";

    public Sw360ChangeLogService(RestClient restClient){
        this.restClient = restClient;
    }

    public List<ChangeLogs> getChangeLogsByDocumentId(String docId, User sw360User) {
        return restClient.get()
        .uri(CHANGELOGS_URI+"/doc/"+docId)
        .header("X-User-Email", sw360User.getEmail())
        .header("X-User-Department", sw360User.getDepartment())
        .retrieve()
        .body(new ParameterizedTypeReference<List<ChangeLogs>>() {});
    }

    public Map<PaginationData, List<ChangeLogs>> getChangeLogsByDocumentIdPaginated(String docId, User sw360User, Pageable pageable) {
        PaginationData pageData = pageableToPaginationData(pageable);
        PaginatedResult<ChangeLogs> result = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(CHANGELOGS_URI + "/doc/" + docId + "/page")
            .queryParam("ascending", pageData.getAscending())
            .queryParam("displayStart", pageData.getDisplayStart())
            .queryParam("rowsPerPage", pageData.getRowsPerPage())
            .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
            .build())
        .header("X-User-Email", sw360User.getEmail())
        .header("X-User-Department", sw360User.getDepartment())
        .retrieve()
        .body(new ParameterizedTypeReference<PaginatedResult<ChangeLogs>>() {});

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
