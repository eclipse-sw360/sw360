/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.vmcomponents;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class VMComponentsServiceRestClient implements VMComponentsClient {

    private static final Logger log = LogManager.getLogger(VMComponentsServiceRestClient.class);
    private static final String BASE = "/vmcomponents/api/vmcomponents";

    private final RestClient restClient;

    public VMComponentsServiceRestClient(RestClient restClient) {
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
    public RequestStatus synchronizeComponents() {
        return postForRequestStatus(BASE + "/synchronize");
    }

    @Override
    public RequestStatus triggerReverseMatch() {
        return postForRequestStatus(BASE + "/reverse-match");
    }

    private RequestStatus postForRequestStatus(String path) {
        try {
            RequestSummary body = call(() -> restClient.post().uri(path).retrieve().body(RequestSummary.class));
            if (body != null && body.getRequestStatus() != null) {
                return body.getRequestStatus();
            }
            return RequestStatus.FAILURE;
        } catch (RuntimeException e) {
            log.error("VM components REST call failed: {}", path, e);
            return RequestStatus.FAILURE;
        }
    }
}
