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

import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;

import java.util.List;
import java.util.Map;

/**
 * Everything we need to bundle one entity into the TAR.
 * documents: filename -> serialised JSON bytes (e.g. project.json, changelogs.json).
 * attachments: lazy handles streamed in directly without loading into memory.
 */
public final class CollectedEntity {

    private final String entityId;
    private final String entityName;
    private final String entityVersion;
    private final ArchivalEntityType entityType;
    private final Map<String, byte[]> documents;
    private final List<AttachmentSource> attachments;

    public CollectedEntity(String entityId,
                           String entityName,
                           String entityVersion,
                           ArchivalEntityType entityType,
                           Map<String, byte[]> documents,
                           List<AttachmentSource> attachments) {
        this.entityId = entityId;
        this.entityName = entityName;
        this.entityVersion = entityVersion;
        this.entityType = entityType;
        this.documents = documents;
        this.attachments = attachments;
    }

    public String entityId() { return entityId; }
    public String entityName() { return entityName; }
    public String entityVersion() { return entityVersion; }
    public ArchivalEntityType entityType() { return entityType; }
    public Map<String, byte[]> documents() { return documents; }
    public List<AttachmentSource> attachments() { return attachments; }
}
