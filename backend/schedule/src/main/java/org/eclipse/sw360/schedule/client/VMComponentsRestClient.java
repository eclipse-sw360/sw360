/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.schedule.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VMComponentsRestClient {

    private static final Logger log = LogManager.getLogger(VMComponentsRestClient.class);

    private static final String SYNCHRONIZE_PATH = "/vmcomponents/api/vmcomponents/synchronize";
    private static final String REVERSE_MATCH_PATH = "/vmcomponents/api/vmcomponents/reverse-match";

    private final RestClient restClient;

    public VMComponentsRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public RequestStatus synchronizeComponents() {
        return postForRequestStatus(SYNCHRONIZE_PATH);
    }

    public RequestStatus triggerReverseMatch() {
        return postForRequestStatus(REVERSE_MATCH_PATH);
    }

    private RequestStatus postForRequestStatus(String path) {
        try {
            RequestSummary body = restClient.post()
                    .uri(path)
                    .retrieve()
                    .body(RequestSummary.class);
            if (body != null && body.getRequestStatus() != null) {
                return body.getRequestStatus();
            }
            return RequestStatus.FAILURE;
        } catch (RestClientException e) {
            log.error("VM components REST call failed: {}", path, e);
            return RequestStatus.FAILURE;
        }
    }
}
