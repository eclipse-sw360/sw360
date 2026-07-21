/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostFindOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.and;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.eq;

public class LicenseDBSyncReportRepository extends DatabaseRepositoryCloudantClient<LicenseDBSyncReport> {

    private static final String ALL = "function(doc) { if (doc.type == 'licensedb-sync-report') emit(null, doc._id) }";
    private static final String SYNC_REPORT_IDX = "syncReportIdx";

    public LicenseDBSyncReportRepository(DatabaseConnectorCloudant db) {
        super(db, LicenseDBSyncReport.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);

        createIndex(SYNC_REPORT_IDX, "byTypeStatusEndDate",
                new String[]{"type", "status", "endDate"}, db);
    }

    public LicenseDBSyncReport getLatestReport() {
        return queryLatest(null);
    }

    public LicenseDBSyncReport getLatestSuccessfulReport() {
        return queryLatest("SUCCESS");
    }

    private LicenseDBSyncReport queryLatest(String statusFilter) {
        Map<String, Object> typeSelector = eq("type", "licensedb-sync-report");
        Map<String, Object> selector;
        if (statusFilter != null) {
            Map<String, Object> statusSelector = eq("status", statusFilter);
            selector = and(List.of(typeSelector, statusSelector));
        } else {
            selector = typeSelector;
        }

        PostFindOptions query = getConnector().getQueryBuilder()
                .selector(selector)
                .useIndex(Collections.singletonList(SYNC_REPORT_IDX))
                .addSort(Collections.singletonMap("endDate", "desc"))
                .limit(1L)
                .build();

        List<LicenseDBSyncReport> results = getConnector().getQueryResult(query, LicenseDBSyncReport.class);
        return results.isEmpty() ? null : results.get(0);
    }
}
