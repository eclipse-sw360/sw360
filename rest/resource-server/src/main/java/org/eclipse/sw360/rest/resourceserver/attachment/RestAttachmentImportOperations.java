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

import java.util.List;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.importer.AttachmentImportOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestAttachmentImportOperations implements AttachmentImportOperations {

    private final SW360AttachmentBackendService attachmentBackendService;

    @Override
    public AttachmentContent getAttachmentContent(String id) throws SW360Exception {
        return attachmentBackendService.getAttachmentContent(id);
    }

    @Override
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        return attachmentBackendService.makeAttachmentContents(attachmentContents);
    }

    @Override
    public RequestSummary bulkDelete(List<String> ids) throws TException {
        return attachmentBackendService.bulkDelete(ids);
    }
}
