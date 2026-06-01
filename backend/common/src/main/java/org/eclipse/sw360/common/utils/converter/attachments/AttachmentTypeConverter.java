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

import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentType;

public final class AttachmentTypeConverter {

    private AttachmentTypeConverter() {}

    public static AttachmentType fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType thrift) {
        return EnumConverter.fromThrift(thrift, AttachmentType.class);
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType toThrift(AttachmentType pojo) {
        return EnumConverter.toThrift(pojo, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType.class);
    }
}
