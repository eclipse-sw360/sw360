/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import org.eclipse.sw360.components.summary.LicenseSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.licenses.License;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD access for the License class
 *
 * @author cedric.bodet@tngtech.com
 */
@Component
public class LicenseRepository extends SummaryAwareRepository<License> {

    private static final String ALL = "function(doc) { if (doc.type == 'license') emit(null, doc._id) }";
    private static final String BYNAME = "function(doc) { if(doc.type == 'license') { emit(doc.fullname, null) } }";
    private static final String BYSHORTNAME = "function(doc) { if(doc.type == 'license') { emit(doc._id, null) } }";
    private static final String BYLICENSETYPEID = "function(doc) { if(doc.type == 'license') { emit(doc.licenseTypeDatabaseId, null) } }";

    @Autowired
    public LicenseRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(License.class, db, new LicenseSummary());
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byname", createMapReduce(BYNAME, null));
        views.put("byshortname", createMapReduce(BYSHORTNAME, null));
        views.put("bylicensetypeid", createMapReduce(BYLICENSETYPEID, null));
        initStandardDesignDocument(views, db);
    }

    public List<License> searchByLicenseTypeId(String id) {
        return queryByPrefix("bylicensetypeid", id);
    }

    public List<License> searchByName(String name) {
        return queryByPrefix("byname", name);
    }

    public List<License> searchByShortName(String name) {
        return queryByPrefix("byshortname", name);
    }
    public List<License> searchByShortName(List<String> names) {
        return queryByIds("byshortname", names);
    }

    public List<License> getLicenseSummary() {
        return makeSummaryFromFullDocs(SummaryType.SUMMARY, queryView("byname"));
    }

    public List<License> getLicenseSummaryForExport() {
        return makeSummaryFromFullDocs(SummaryType.EXPORT_SUMMARY, queryView("byname"));
    }

}
