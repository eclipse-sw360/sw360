/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.archival;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivePreview;
import org.eclipse.sw360.datahandler.services.archival.ArchiveRequest;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class ArchivalServiceRestClient implements ArchivalClient {

    private static final String BASE = "/archival/api/archival";
    private static final ParameterizedTypeReference<List<ArchivalRecord>> RECORD_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public ArchivalServiceRestClient(RestClient restClient) {
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
        if (user.getUserGroup() != null) {
            headers.set("X-User-Group", user.getUserGroup().name());
        }
    }

    @Override
    public ArchivePreview preview(ArchiveRequest request, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/preview")
                .headers(h -> addUser(h, user))
                .body(request)
                .retrieve()
                .body(ArchivePreview.class));
    }

    @Override
    public InputStream archive(ArchiveRequest request, User user) {
        // exchange with close=false leaves the connection open so the body can be
        // streamed straight through to the browser; the caller closes the stream.
        return call(() -> restClient.post()
                .uri(BASE + "/archive")
                .headers(h -> addUser(h, user))
                .body(request)
                .exchange((req, res) -> res.getBody(), false));
    }

    @Override
    public List<ArchivalRecord> listRecords() {
        return call(() -> restClient.get()
                .uri(BASE + "/records")
                .retrieve()
                .body(RECORD_LIST));
    }

    @Override
    public ArchivalRecord getRecord(String id) {
        return call(() -> restClient.get()
                .uri(BASE + "/records/{id}", id)
                .retrieve()
                .body(ArchivalRecord.class));
    }

    @Override
    public void deleteRecord(String id) {
        call(() -> restClient.delete()
                .uri(BASE + "/records/{id}", id)
                .retrieve()
                .toBodilessEntity());
    }
}
