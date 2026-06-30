/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology.client;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Sw360AttachmentsRestClient {

    private static final String CONTENTS_URI = "/attachments/api/attachments/contents";

    private final RestClient restClient;

    public Sw360AttachmentsRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojo =
                new org.eclipse.sw360.datahandler.services.attachments.AttachmentContent()
                        .setFilename(attachmentContent.getFilename())
                        .setContentType(attachmentContent.isSetContentType() ? attachmentContent.getContentType() : null)
                        .setOnlyRemote(attachmentContent.isSetOnlyRemote() ? attachmentContent.isOnlyRemote() : null)
                        .setRemoteUrl(attachmentContent.isSetRemoteUrl() ? attachmentContent.getRemoteUrl() : null)
                        .setPartsCount(attachmentContent.isSetPartsCount() ? attachmentContent.getPartsCount() : null);

        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent result = restClient.post()
                .uri(CONTENTS_URI)
                .body(pojo)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.attachments.AttachmentContent.class);

        if (result == null || result.getId() == null) {
            throw new TException("Failed to create attachment content via REST");
        }
        attachmentContent.setId(result.getId());
        if (result.getRevision() != null) {
            attachmentContent.setRevision(result.getRevision());
        }
        return attachmentContent;
    }
}
