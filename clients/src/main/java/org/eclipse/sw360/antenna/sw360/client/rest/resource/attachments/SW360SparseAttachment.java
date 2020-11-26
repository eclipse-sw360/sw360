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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360SimpleHalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResourceUtility;

import java.util.Objects;
import java.util.Optional;

public final class SW360SparseAttachment extends SW360SimpleHalResource {
    private String filename;
    private SW360AttachmentType attachmentType;
    private String sha1;

    public String getFilename() {
        return filename;
    }

    public SW360SparseAttachment setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public SW360AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public SW360SparseAttachment setAttachmentType(SW360AttachmentType attachmentType) {
        this.attachmentType = attachmentType;
        return this;
    }

    /**
     * Returns the SHA1 hash sum of the represented attachment file. This
     * checksum is calculated automatically by SW360.
     *
     * @return the SHA1 checksum
     */
    public String getSha1() {
        return sha1;
    }

    public SW360SparseAttachment setSha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    @JsonIgnore
    public String getAttachmentId() {
        return Optional.ofNullable(getLinks())
                .map(LinkObjects::getSelf)
                .flatMap(SW360HalResourceUtility::getLastIndexOfSelfLink)
                .orElse("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360SparseAttachment) || !super.equals(o)) return false;
        SW360SparseAttachment that = (SW360SparseAttachment) o;
        return Objects.equals(filename, that.filename) &&
                Objects.equals(attachmentType, that.attachmentType) &&
                Objects.equals(sha1, that.sha1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), filename, attachmentType, sha1);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360SparseAttachment;
    }
}
