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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
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
    public AttachmentContent getAttachmentContent(String id) throws SW360Exception {
        try {
            return attachmentClient.getAttachmentContent(id);
        } catch (TException e) {
            if (e instanceof SW360Exception sw360Exception) {
                throw sw360Exception;
            }
            throw new SW360Exception(e.getMessage());
        }
    }

    @Override
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents)
            throws TException {
        return attachmentClient.makeAttachmentContents(attachmentContents);
    }

    @Override
    public RequestSummary bulkDelete(List<String> ids) throws TException {
        return attachmentClient.bulkDelete(ids);
    }
}
