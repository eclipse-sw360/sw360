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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

public class ArchivalDatabaseHandler {

    private final ArchivalRepository repository;

    public ArchivalDatabaseHandler(Cloudant client, String dbName) throws MalformedURLException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        repository = new ArchivalRepository(db);
    }

    public ArchivalDatabaseHandler(ArchivalRepository repository) {
        this.repository = repository;
    }

    public ArchivalRecord add(ArchivalRecord record) throws SW360Exception {
        ArchivalRecordDocument doc = ArchivalRecordDocument.fromRecord(record);
        repository.add(doc);
        return doc.toRecord();
    }

    public ArchivalRecord get(String id) {
        ArchivalRecordDocument doc = repository.get(id);
        return doc == null ? null : doc.toRecord();
    }

    public List<ArchivalRecord> getAll() {
        return repository.getAll().stream()
                .map(ArchivalRecordDocument::toRecord)
                .collect(Collectors.toList());
    }

    public List<ArchivalRecord> getByBundleId(String bundleId) {
        return repository.getByBundleId(bundleId).stream()
                .map(ArchivalRecordDocument::toRecord)
                .collect(Collectors.toList());
    }

    public boolean delete(String id) {
        return repository.remove(id);
    }

    /**
     * Re-fetches the existing doc to keep its _rev, copies the new field values,
     * and writes it back. Returns true if the row existed and was updated.
     */
    public boolean update(ArchivalRecord record) {
        ArchivalRecordDocument existing = repository.get(record.getId());
        if (existing == null) return false;

        existing.setBundleId(record.getBundleId());
        existing.setEntityId(record.getEntityId());
        existing.setEntityName(record.getEntityName());
        existing.setEntityType(record.getEntityType());
        existing.setStatus(record.getStatus());
        existing.setArchivedBy(record.getArchivedBy());
        existing.setArchivedAt(record.getArchivedAt());
        existing.setRestoredBy(record.getRestoredBy());
        existing.setRestoredAt(record.getRestoredAt());
        existing.setAttachmentCount(record.getAttachmentCount());
        existing.setComment(record.getComment());

        repository.update(existing);
        return true;
    }
}
