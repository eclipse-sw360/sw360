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
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;

/**
 * CouchDB view of an ArchivalRecord.
 *
 * DatabaseConnectorCloudant serialises plain POJOs with Gson, so Jackson
 * annotations are ignored here and only the raw field names matter:
 *   - "id"       is lifted into the CouchDB _id and stripped from the body
 *   - "revision" is lifted into the CouchDB _rev and stripped from the body
 * Because both are stripped, neither survives a read, so docId keeps a durable
 * copy of the identifier inside the document body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchivalRecordDocument extends ArchivalRecord {

    /**
     * Must match this class's simple name: DatabaseConnectorCloudant.get() rejects
     * documents whose "type" field differs from the target class name, which would
     * silently turn every lookup into a null.
     */
    public static final String TYPE = "ArchivalRecordDocument";

    private String docId;

    private String revision;

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
