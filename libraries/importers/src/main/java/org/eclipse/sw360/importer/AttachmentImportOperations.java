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

public interface AttachmentImportOperations {

    AttachmentContent getAttachmentContent(String id) throws SW360Exception;

    List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException;

    RequestSummary bulkDelete(List<String> ids) throws TException;
}
