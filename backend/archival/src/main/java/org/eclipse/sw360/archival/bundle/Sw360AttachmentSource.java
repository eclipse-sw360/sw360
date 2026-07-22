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

import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.services.archival.AttachmentMetadata;

import java.io.IOException;
import java.io.InputStream;

public final class Sw360AttachmentSource implements AttachmentSource {

    private final AttachmentConnector connector;
    private final AttachmentContent content;
    private final AttachmentMetadata metadata;
    private final long sizeBytes;

    public Sw360AttachmentSource(AttachmentConnector connector,
                                 AttachmentContent content,
                                 AttachmentMetadata metadata,
                                 long sizeBytes) {
        this.connector = connector;
        this.content = content;
        this.metadata = metadata;
        this.sizeBytes = sizeBytes;
    }

    @Override
    public AttachmentMetadata metadata() { return metadata; }

    @Override
    public long sizeBytes() { return sizeBytes; }

    @Override
    public InputStream open() throws IOException {
        try {
            return connector.unsafeGetAttachmentStream(content);
        } catch (SW360Exception e) {
            throw new IOException("failed to open attachment stream for "
                    + content.getId() + ": " + e.getMessage(), e);
        }
    }
}
