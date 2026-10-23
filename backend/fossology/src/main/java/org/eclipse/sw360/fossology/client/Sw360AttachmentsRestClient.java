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
import org.eclipse.sw360.datahandler.attachments.AttachmentClients;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentContent;
import org.springframework.stereotype.Component;

/**
 * Calls the attachments backend for Fossology report storage.
 */
@Component
public class Sw360AttachmentsRestClient {

    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        AttachmentContent result = AttachmentClients.get().makeAttachmentContent(attachmentContent);
        if (result == null || result.getId() == null) {
            throw new TException("Failed to create attachment content via REST");
        }
        return result;
    }
}
