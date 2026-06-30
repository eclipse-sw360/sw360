/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival.bundle;

import org.eclipse.sw360.services.archival.AttachmentMetadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * AttachmentSource backed by an in-memory byte array.
 * Suitable for tests and small files; use a streaming source for CouchDB attachments.
 */
public final class ByteArrayAttachmentSource implements AttachmentSource {

    private final AttachmentMetadata metadata;
    private final byte[] body;

    public ByteArrayAttachmentSource(AttachmentMetadata metadata, byte[] body) {
        this.metadata = metadata;
        this.body = body;
        if (metadata.getSizeBytes() != body.length) {
            metadata.setSizeBytes(body.length);
        }
    }

    @Override
    public AttachmentMetadata metadata() { return metadata; }

    @Override
    public long sizeBytes() { return body.length; }

    @Override
    public InputStream open() { return new ByteArrayInputStream(body); }
}
