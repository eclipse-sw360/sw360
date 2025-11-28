/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author johannes.najjar@tngtech.com
 */
@Component
public class LicenseTypeRepository extends DatabaseRepositoryCloudantClient<LicenseType> {
    private static final String ALL = "function(doc) { if (doc.type == 'licenseType') emit(null, doc._id) }";
    private static final String BYLICENSETYPE = "function(doc) { if(doc.type == 'licenseType') { emit(doc.licenseType.trim().toLowerCase(), null) } }";

    @Autowired
    public LicenseTypeRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, LicenseType.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("bylicensetype", createMapReduce(BYLICENSETYPE, null));
        initStandardDesignDocument(views, db);
    }

    public List<LicenseType> searchByLicenseType(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String searchLower = name.trim().toLowerCase();
        List<LicenseType> all = queryView("all");
        return all.stream()
                .filter(lt -> lt.getLicenseType() != null && lt.getLicenseType().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
    }
}
