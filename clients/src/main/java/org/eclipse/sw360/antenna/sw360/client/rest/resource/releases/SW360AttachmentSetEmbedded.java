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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.releases;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Embedded;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360SparseAttachment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@JsonDeserialize(as = SW360AttachmentSetEmbedded.class)
public class SW360AttachmentSetEmbedded implements Embedded {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("sw360:attachments")
    private Set<SW360SparseAttachment> attachments;

    public Set<SW360SparseAttachment> getAttachments() {
        return Optional.ofNullable(attachments)
                .map(HashSet::new)
                .orElse(new HashSet<>());
    }

    public SW360AttachmentSetEmbedded setAttachments(Set<SW360SparseAttachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360AttachmentSetEmbedded that = (SW360AttachmentSetEmbedded) o;
        return Objects.equals(attachments, that.attachments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachments);
    }
}