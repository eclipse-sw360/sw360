/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cvesearch;

import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class CveSearchServiceRestClient implements CveSearchClient {

    private static final String BASE = "/cvesearch/api/cvesearch";
    private static final ParameterizedTypeReference<Set<String>> STRING_SET =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public CveSearchServiceRestClient(RestClient restClient) {
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

    @Override
    public VulnerabilityUpdateStatus updateForRelease(String releaseId) {
        return postForBody(BASE + "/releases/" + releaseId, VulnerabilityUpdateStatus.class);
    }

    @Override
    public VulnerabilityUpdateStatus updateForComponent(String componentId) {
        return postForBody(BASE + "/components/" + componentId, VulnerabilityUpdateStatus.class);
    }

    @Override
    public VulnerabilityUpdateStatus updateForProject(String projectId) {
        return postForBody(BASE + "/projects/" + projectId, VulnerabilityUpdateStatus.class);
    }

    @Override
    public VulnerabilityUpdateStatus fullUpdate() {
        return postForBody(BASE + "/full-update", VulnerabilityUpdateStatus.class);
    }

    @Override
    public RequestStatus update() {
        return postForBody(BASE + "/update", RequestStatus.class);
    }

    @Override
    public Set<String> findCpes(String vendor, String product, String version) {
        return call(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/cpes")
                        .queryParam("vendor", vendor)
                        .queryParam("product", product)
                        .queryParam("version", version)
                        .build())
                .retrieve()
                .body(STRING_SET));
    }

    private <T> T postForBody(String path, Class<T> responseType) {
        return call(() -> restClient.post().uri(path).retrieve().body(responseType));
    }
}
