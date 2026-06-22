/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.commonIO;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;

/**
 * Metadata operations for attachments (CouchDB document layer), without binary upload/download.
 */
public interface AttachmentMetadataOperations {

    AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException;

    AttachmentContent getAttachmentContent(String id) throws TException;

    RequestStatus deleteAttachmentContent(String id) throws TException;
}
