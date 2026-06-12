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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CveSearchRestClient {

    private static final Logger log = LogManager.getLogger(CveSearchRestClient.class);

    private static final String UPDATE_PATH = "/cvesearch/api/cvesearch/update";

    private final RestClient restClient;

    public CveSearchRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public RequestStatus update() {
        try {
            RequestStatus body = restClient.post()
                    .uri(UPDATE_PATH)
                    .retrieve()
                    .body(RequestStatus.class);
            return body != null ? body : RequestStatus.FAILURE;
        } catch (RestClientException e) {
            log.error("CVE search update REST call failed: {}", UPDATE_PATH, e);
            return RequestStatus.FAILURE;
        }
    }
}
