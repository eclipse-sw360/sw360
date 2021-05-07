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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;

import com.cloudant.client.api.model.DesignDocument.MapReduce;

/**
 * @author johannes.najjar@tngtech.com
 */
public class LicenseTypeRepository extends DatabaseRepositoryCloudantClient<LicenseType> {
    private static final String ALL = "function(doc) { if (doc.type == 'licenseType') emit(null, doc._id) }";

    public LicenseTypeRepository(DatabaseConnectorCloudant db) {
        super(db, LicenseType.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }
}
