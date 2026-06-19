/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cvesearch;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class Sw360CveSearchService {

    private static final Logger log = LogManager.getLogger(Sw360CveSearchService.class);

    private static final String CVESEARCH_URI = "/cvesearch/api/cvesearch";

    private final RestClient restClient;

    public Sw360CveSearchService(RestClient restClient) {
        this.restClient = restClient;
    }

    public VulnerabilityUpdateStatus updateForRelease(String releaseId) {
        return postForBody(CVESEARCH_URI + "/releases/" + releaseId, VulnerabilityUpdateStatus.class);
    }

    public VulnerabilityUpdateStatus updateForComponent(String componentId) {
        return postForBody(CVESEARCH_URI + "/components/" + componentId, VulnerabilityUpdateStatus.class);
    }

    public VulnerabilityUpdateStatus updateForProject(String projectId) {
        return postForBody(CVESEARCH_URI + "/projects/" + projectId, VulnerabilityUpdateStatus.class);
    }

    public VulnerabilityUpdateStatus fullUpdate() {
        return postForBody(CVESEARCH_URI + "/full-update", VulnerabilityUpdateStatus.class);
    }

    public RequestStatus update() {
        return postForBody(CVESEARCH_URI + "/update", RequestStatus.class);
    }

    public Set<String> findCpes(String vendor, String product, String version) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(CVESEARCH_URI + "/cpes")
                        .queryParam("vendor", vendor)
                        .queryParam("product", product)
                        .queryParam("version", version)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Set<String>>() {});
    }

    private <T> T postForBody(String path, Class<T> responseType) {
        try {
            return restClient.post()
                    .uri(path)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            log.error("CVE search backend call failed: {}", path, e);
            throw e;
        }
    }
}
