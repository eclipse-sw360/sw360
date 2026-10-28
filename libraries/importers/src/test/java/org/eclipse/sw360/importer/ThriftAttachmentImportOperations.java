/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.importer;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentContentConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.SW360ExceptionConverter;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;

/**
 * Test adapter bridging legacy Thrift attachment client mocks to {@link AttachmentImportOperations}.
 */
public class ThriftAttachmentImportOperations implements AttachmentImportOperations {

    private final AttachmentService.Iface attachmentClient;

    public ThriftAttachmentImportOperations(AttachmentService.Iface attachmentClient) {
        this.attachmentClient = attachmentClient;
    }

    @Override
    public AttachmentContent getAttachmentContent(String id) {
        try {
            return AttachmentContentConverter.fromThrift(attachmentClient.getAttachmentContent(id));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw SW360ExceptionConverter.fromThrift(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    @Override
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) {
        try {
            return attachmentClient.makeAttachmentContents(attachmentContents.stream()
                    .map(AttachmentContentConverter::toThrift)
                    .collect(Collectors.toList())).stream()
                    .map(AttachmentContentConverter::fromThrift)
                    .collect(Collectors.toList());
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    @Override
    public RequestSummary bulkDelete(List<String> ids) {
        try {
            return RequestSummaryConverter.fromThrift(attachmentClient.bulkDelete(ids));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }
}
