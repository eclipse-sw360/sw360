/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.apache.thrift.TException;
import org.eclipse.sw360.commonIO.AttachmentMetadataOperations;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;

public class AttachmentDatabaseHandlerMetadataOperations implements AttachmentMetadataOperations {

    private final AttachmentDatabaseHandler handler;

    public AttachmentDatabaseHandlerMetadataOperations(AttachmentDatabaseHandler handler) {
        this.handler = handler;
    }

    @Override
    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        try {
            return handler.add(attachmentContent);
        } catch (SW360Exception e) {
            throw new TException(e);
        }
    }

    @Override
    public AttachmentContent getAttachmentContent(String id) throws TException {
        try {
            return handler.getAttachmentContent(id);
        } catch (SW360Exception e) {
            throw new TException(e);
        }
    }

    @Override
    public RequestStatus deleteAttachmentContent(String id) throws TException {
        return handler.deleteAttachmentContent(id);
    }
}
