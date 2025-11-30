/*
 * Copyright TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseObligationList;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;

/**
 * CRUD access for the LicenseObligationList class
 *
 * 
 */
public class LicenseObligationListRepository extends DatabaseRepositoryCloudantClient<LicenseObligationList> {

    private static final String BY_LICENSE_ID =
            "function(doc) {" +
                    "  if (doc.type == 'licenseObligationList') {" +
                    "    emit(doc.licenseId, null);" +
                    "  }" +
                    "}";

    private static final String ALL = "function(doc) { if (doc.type == 'licenseObligationList') emit(null, doc._id) }";

    public LicenseObligationListRepository(DatabaseConnectorCloudant db) {
        super(db, LicenseObligationList.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("byLicenseId", createMapReduce(BY_LICENSE_ID, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public Optional<LicenseObligationList> getObligationByLicenseid(String licenseId) {
        return queryView("byLicenseId", licenseId).stream().findFirst();
    }
}
