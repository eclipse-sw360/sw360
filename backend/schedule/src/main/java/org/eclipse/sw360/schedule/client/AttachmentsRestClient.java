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
public class AttachmentsRestClient {

    private static final Logger log = LogManager.getLogger(AttachmentsRestClient.class);

    private static final String CLEANUP_PATH = "/attachments/api/attachments/cleanup/filesystem";

    private final RestClient restClient;

    public AttachmentsRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public RequestStatus deleteOldAttachmentFromFileSystem() {
        try {
            RequestStatus body = restClient.post()
                    .uri(CLEANUP_PATH)
                    .retrieve()
                    .body(RequestStatus.class);
            return body != null ? body : RequestStatus.FAILURE;
        } catch (RestClientException e) {
            log.error("Attachment filesystem cleanup REST call failed: {}", CLEANUP_PATH, e);
            return RequestStatus.FAILURE;
        }
    }
}
