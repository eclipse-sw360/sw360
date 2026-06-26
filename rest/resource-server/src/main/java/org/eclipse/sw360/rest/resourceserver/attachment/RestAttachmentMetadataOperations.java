/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.attachment;

import org.apache.thrift.TException;
import org.eclipse.sw360.commonIO.AttachmentMetadataOperations;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestAttachmentMetadataOperations implements AttachmentMetadataOperations {

    private final SW360AttachmentBackendService attachmentBackendService;

    @Override
    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        return attachmentBackendService.makeAttachmentContent(attachmentContent);
    }

    @Override
    public AttachmentContent getAttachmentContent(String id) throws TException {
        return attachmentBackendService.getAttachmentContent(id);
    }

    @Override
    public RequestStatus deleteAttachmentContent(String id) throws TException {
        return attachmentBackendService.deleteAttachmentContent(id);
    }
}
