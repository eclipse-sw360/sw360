/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.fossology;

import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.services.fossology.FossologyProcessRequest;
import org.eclipse.sw360.datahandler.services.fossology.FossologyReleaseRequest;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class FossologyServiceRestClient implements FossologyClient {

    private static final String BASE = "/fossology/api/fossology";
    private static final ParameterizedTypeReference<Map<String, String>> STRING_MAP =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public FossologyServiceRestClient(RestClient restClient) {
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
    public ConfigContainer getFossologyConfig() {
        return call(() -> restClient.get().uri(BASE + "/config").retrieve().body(ConfigContainer.class));
    }

    @Override
    public RequestStatus setFossologyConfig(ConfigContainer config) {
        return call(() -> restClient.put().uri(BASE + "/config").body(config).retrieve().body(RequestStatus.class));
    }

    @Override
    public RequestStatus checkConnection() {
        return call(() -> restClient.get().uri(BASE + "/connection").retrieve().body(RequestStatus.class));
    }

    @Override
    public ExternalToolProcess process(FossologyProcessRequest request, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/process")
                .headers(h -> addUser(h, user))
                .body(request)
                .retrieve()
                .body(ExternalToolProcess.class));
    }

    @Override
    public RequestStatus markFossologyProcessOutdated(FossologyReleaseRequest request, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/process/outdated")
                .headers(h -> addUser(h, user))
                .body(request)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus triggerReportGenerationFossology(FossologyReleaseRequest request, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/process/report")
                .headers(h -> addUser(h, user))
                .body(request)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public Map<String, String> checkUnpackStatus(int uploadId) {
        return call(() -> restClient.get()
                .uri(BASE + "/unpack-status/{uploadId}", uploadId)
                .retrieve()
                .body(STRING_MAP));
    }

    @Override
    public Map<String, String> checkScanStatus(int scanJobId) {
        return call(() -> restClient.get()
                .uri(BASE + "/scan-status/{scanJobId}", scanJobId)
                .retrieve()
                .body(STRING_MAP));
    }
}
