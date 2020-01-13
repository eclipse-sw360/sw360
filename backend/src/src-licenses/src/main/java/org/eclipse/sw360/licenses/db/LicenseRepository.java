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
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.Collection;
import java.util.List;

/**
 * CRUD access for the License class
 *
 * @author cedric.bodet@tngtech.com
 */
@Views({
        @View(name = "all", map = "function(doc) { if (doc.type == 'license') emit(null, doc._id) }"),
        @View(name = "byname", map = "function(doc) { if(doc.type == 'license') { emit(doc.fullname, doc) } }"),
        @View(name = "byshortname", map = "function(doc) { if(doc.type == 'license') { emit(doc._id, doc) } }")
})
public class LicenseRepository extends SummaryAwareRepository<License> {

    public LicenseRepository(DatabaseConnector db) {
        super(License.class, db, new LicenseSummary());

        initStandardDesignDocument();
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
