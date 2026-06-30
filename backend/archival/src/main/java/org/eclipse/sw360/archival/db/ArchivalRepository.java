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
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;

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
}
