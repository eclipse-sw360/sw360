/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.sw360.services.archival.ArchivalRecord;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchivalRecordDocument extends ArchivalRecord {

    public static final String TYPE = "archivalRecord";

    @JsonProperty("_id")
    private String docId;

    @JsonProperty("_rev")
    private String revision;

    @JsonProperty("type")
    private String type = TYPE;

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision = revision; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public static ArchivalRecordDocument fromRecord(ArchivalRecord r) {
        ArchivalRecordDocument d = new ArchivalRecordDocument();
        d.setId(r.getId());
        d.setDocId(r.getId());
        d.setBundleId(r.getBundleId());
        d.setEntityId(r.getEntityId());
        d.setEntityName(r.getEntityName());
        d.setEntityType(r.getEntityType());
        d.setStatus(r.getStatus());
        d.setArchivedBy(r.getArchivedBy());
        d.setArchivedAt(r.getArchivedAt());
        d.setRestoredBy(r.getRestoredBy());
        d.setRestoredAt(r.getRestoredAt());
        d.setAttachmentCount(r.getAttachmentCount());
        d.setComment(r.getComment());
        return d;
    }

    public ArchivalRecord toRecord() {
        ArchivalRecord r = new ArchivalRecord();
        r.setId(getDocId());
        r.setBundleId(getBundleId());
        r.setEntityId(getEntityId());
        r.setEntityName(getEntityName());
        r.setEntityType(getEntityType());
        r.setStatus(getStatus());
        r.setArchivedBy(getArchivedBy());
        r.setArchivedAt(getArchivedAt());
        r.setRestoredBy(getRestoredBy());
        r.setRestoredAt(getRestoredAt());
        r.setAttachmentCount(getAttachmentCount());
        r.setComment(getComment());
        return r;
    }
}
