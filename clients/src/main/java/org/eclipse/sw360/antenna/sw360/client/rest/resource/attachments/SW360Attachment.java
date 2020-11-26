/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360SimpleHalResource;
import org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException;

import java.nio.file.Path;
import java.util.Optional;

public final class SW360Attachment extends SW360SimpleHalResource {
    private String filename;
    private SW360AttachmentType attachmentType;
    private SW360AttachmentCheckStatus checkStatus;

    public SW360Attachment() {}

    public SW360Attachment(Path path, SW360AttachmentType attachmentType) {
        this(Optional.ofNullable(path)
                .map(Path::getFileName)
                .orElseThrow(() -> new SW360ClientException("Tried to add null path.")).toString(),
                attachmentType);
    }

    public SW360Attachment(String filename, SW360AttachmentType attachmentType) {
        this.filename = Optional.ofNullable(filename)
                .orElseThrow(() -> new SW360ClientException("Filename is not allowed to be null."));
        this.attachmentType = attachmentType;
        this.checkStatus = SW360AttachmentCheckStatus.NOTCHECKED;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getFilename() {
        return filename;
    }

    public SW360Attachment setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public SW360AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public SW360Attachment setAttachmentType(SW360AttachmentType attachmentType) {
        this.attachmentType = attachmentType;
        return this;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public SW360AttachmentCheckStatus getCheckStatus() {
        return checkStatus;
    }

    public SW360Attachment setCheckStatus(SW360AttachmentCheckStatus checkStatus) {
        this.checkStatus = checkStatus;
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SW360Attachment) || !super.equals(object)) return false;
        SW360Attachment that = (SW360Attachment) object;
        return java.util.Objects.equals(filename, that.filename) &&
                java.util.Objects.equals(attachmentType, that.attachmentType) &&
                java.util.Objects.equals(checkStatus, that.checkStatus);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), filename, attachmentType, checkStatus);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360Attachment;
    }
}
