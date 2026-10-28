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

import org.eclipse.sw360.datahandler.services.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;

public interface AttachmentImportOperations {

    AttachmentContent getAttachmentContent(String id);

    List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents);

    RequestSummary bulkDelete(List<String> ids);
}
