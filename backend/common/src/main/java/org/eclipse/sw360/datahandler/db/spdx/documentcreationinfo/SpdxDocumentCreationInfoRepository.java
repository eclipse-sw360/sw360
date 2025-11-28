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
package org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo;

import org.eclipse.sw360.components.summary.DocumentCreationInformationSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpdxDocumentCreationInfoRepository extends SummaryAwareRepository<DocumentCreationInformation> {

    private static final String ALL = "function(doc) { if (doc.type == 'documentCreationInformation') emit(null, doc._id) }";

    @Autowired
    public SpdxDocumentCreationInfoRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_SPDX") DatabaseConnectorCloudant db
    ) {
        super(DocumentCreationInformation.class, db, new DocumentCreationInformationSummary());
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<DocumentCreationInformation> getDocumentCreationInformationSummary() {
        return makeSummary(SummaryType.SUMMARY, getAllIds());
    }

}
