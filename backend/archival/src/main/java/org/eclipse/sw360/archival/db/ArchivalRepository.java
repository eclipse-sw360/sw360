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

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.Document;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArchivalRepository extends DatabaseRepositoryCloudantClient<ArchivalRecordDocument> {

    private static final String ALL =
            "function(doc) { if (doc.type == '" + ArchivalRecordDocument.TYPE + "') emit(null, doc._id) }";

    private static final String BY_BUNDLE_ID =
            "function(doc) {" +
            "  if (doc.type == '" + ArchivalRecordDocument.TYPE + "' && doc.bundleId != null) {" +
            "    emit(doc.bundleId, doc._id);" +
            "  }" +
            "}";

    private static final String BY_ENTITY_TYPE =
            "function(doc) {" +
            "  if (doc.type == '" + ArchivalRecordDocument.TYPE + "' && doc.entityType != null) {" +
            "    emit(doc.entityType, doc._id);" +
            "  }" +
            "}";

    public ArchivalRepository(DatabaseConnectorCloudant db) {
        super(db, ArchivalRecordDocument.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byBundleId", createMapReduce(BY_BUNDLE_ID, null));
        views.put("byEntityType", createMapReduce(BY_ENTITY_TYPE, null));
        initStandardDesignDocument(views, db);
    }

    public List<ArchivalRecordDocument> getByBundleId(String bundleId) {
        Set<String> ids = queryForIdsAsValue("byBundleId", bundleId);
        return get(ids);
    }

    public List<ArchivalRecordDocument> getByEntityType(String entityType) {
        Set<String> ids = queryForIdsAsValue("byEntityType", entityType);
        return get(ids);
    }

    /**
     * A document read back from CouchDB has no id or revision: the connector strips
     * both out of the body on write and only re-injects them for Thrift structs.
     * Restore them from the durable docId and the document's current revision so the
     * write actually lands instead of failing on an empty id.
     */
    public void updateRecord(ArchivalRecordDocument doc) throws SW360Exception {
        String id = doc.getDocId() != null ? doc.getDocId() : doc.getId();
        if (id == null || id.isBlank()) {
            throw new SW360Exception("cannot update an archival record without an id");
        }
        Document current = getConnector().getDocument(id);
        doc.setId(id);
        doc.setDocId(id);
        doc.setRevision(current.getRev());
        update(doc);
    }
}
