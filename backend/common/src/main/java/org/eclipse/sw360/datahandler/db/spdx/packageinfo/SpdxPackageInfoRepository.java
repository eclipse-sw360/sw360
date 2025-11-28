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
package org.eclipse.sw360.datahandler.db.spdx.packageinfo;

import org.eclipse.sw360.components.summary.PackageInformationSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpdxPackageInfoRepository extends SummaryAwareRepository<PackageInformation> {

    private static final String ALL = "function(doc) { if (doc.type == 'packageInformation') emit(null, doc._id) }";

    @Autowired
    public SpdxPackageInfoRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_SPDX") DatabaseConnectorCloudant db
    ) {
        super(PackageInformation.class, db, new PackageInformationSummary());
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<PackageInformation> getPackageInformationSummary() {
        return makeSummary(SummaryType.SUMMARY, getAllIds());
    }

}
