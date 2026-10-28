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
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.SW360ExceptionConverter;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.importer.AttachmentImportOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestAttachmentImportOperations implements AttachmentImportOperations {

    private final SW360AttachmentBackendService attachmentBackendService;
    private final AttachmentTypeBridge attachmentTypeBridge;

    @Override
    public AttachmentContent getAttachmentContent(String id) {
        try {
            return attachmentTypeBridge.toPojo(attachmentBackendService.getAttachmentContent(id));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw SW360ExceptionConverter.fromThrift(e);
        }
    }

    @Override
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) {
        try {
            return attachmentBackendService.makeAttachmentContents(attachmentContents.stream()
                    .map(attachmentTypeBridge::toThrift)
                    .collect(Collectors.toList())).stream()
                    .map(attachmentTypeBridge::toPojo)
                    .collect(Collectors.toList());
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    @Override
    public RequestSummary bulkDelete(List<String> ids) {
        try {
            return RequestSummaryConverter.fromThrift(attachmentBackendService.bulkDelete(ids));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }
}
