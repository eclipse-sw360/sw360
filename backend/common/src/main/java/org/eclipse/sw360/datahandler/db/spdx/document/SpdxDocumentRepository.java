/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db.spdx.document;

import org.eclipse.sw360.components.summary.SpdxDocumentSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;

import com.cloudant.client.api.model.DesignDocument.MapReduce;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpdxDocumentRepository extends SummaryAwareRepository<SPDXDocument> {

    private static final String ALL = "function(doc) { if (doc.type == 'SPDXDocument') emit(null, doc._id) }";

    public SpdxDocumentRepository(DatabaseConnectorCloudant db) {
        super(SPDXDocument.class, db, new SpdxDocumentSummary());
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<SPDXDocument> getSPDXDocumentSummary() {
        return makeSummary(SummaryType.SUMMARY, getAllIds());
    }

}
