/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.attachments;

import org.eclipse.sw360.datahandler.services.attachments.FilledAttachment;

public final class FilledAttachmentConverter {

    private FilledAttachmentConverter() {}

    public static FilledAttachment fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment thrift) {
        if (thrift == null) {
            return null;
        }
        FilledAttachment pojo = new FilledAttachment();
        if (thrift.isSetAttachment()) {
            pojo.setAttachment(org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.fromThrift(thrift.getAttachment()));
        }
        if (thrift.isSetAttachmentContent()) {
            pojo.setAttachmentContent(org.eclipse.sw360.common.utils.converter.attachments.AttachmentContentConverter.fromThrift(thrift.getAttachmentContent()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment toThrift(FilledAttachment pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment thrift = new org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment();
        if (pojo.getAttachment() != null) {
            thrift.setAttachment(org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.toThrift(pojo.getAttachment()));
        }
        if (pojo.getAttachmentContent() != null) {
            thrift.setAttachmentContent(org.eclipse.sw360.common.utils.converter.attachments.AttachmentContentConverter.toThrift(pojo.getAttachmentContent()));
        }
        return thrift;
    }
}
