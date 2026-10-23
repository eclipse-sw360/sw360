/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.changelogs;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class ChangeLogsServiceRestClient implements ChangeLogsClient {

    private static final String BASE = "/changelogs/api/changelogs";
    private static final ParameterizedTypeReference<List<ChangeLogs>> CHANGELOG_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginatedResult<ChangeLogs>> CHANGELOG_PAGE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ChangeLogsServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            headers.set("X-User-Email", user.getEmail());
        }
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
    }

    @Override
    public List<ChangeLogs> getChangeLogsByDocumentId(String docId, User user) {
        return call(() -> restClient.get()
                .uri(BASE + "/doc/" + docId)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(CHANGELOG_LIST));
    }

    @Override
    public PaginatedResult<ChangeLogs> getChangeLogsByDocumentIdPaginated(
            String docId, User user, PaginationData pageData) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/doc/" + docId + "/page")
                        .queryParam("ascending", pageData.getAscending())
                        .queryParam("displayStart", pageData.displayStartOrZero())
                        .queryParam("rowsPerPage", pageData.rowsPerPageOrZero())
                        .queryParam("sortColumnNumber", pageData.getSortColumnNumber())
                        .build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(CHANGELOG_PAGE));
    }
}
