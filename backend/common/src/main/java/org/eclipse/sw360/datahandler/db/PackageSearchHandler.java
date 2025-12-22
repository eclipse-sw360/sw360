/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.common.utils.SearchUtils.OBJ_ARRAY_TO_STRING_INDEX;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

@Component
public class PackageSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("packages",
            new NouveauIndexFunction(
                "function(doc) {" +
                OBJ_ARRAY_TO_STRING_INDEX +
                "    if (!doc.type || doc.type != 'package') return;" +
                "    if (doc.name && typeof(doc.name) == 'string' && doc.name.length > 0) {" +
                "      index('text', 'name', doc.name, {'store': true});"+
                "    }" +
                "    if (doc.version && typeof(doc.version) == 'string' && doc.version.length > 0) {" +
                "      index('text', 'version', doc.version, {'store': true});"+
                "    }" +
                "    if (doc.purl && typeof(doc.purl) == 'string' && doc.purl.length > 0) {" +
                "      index('text', 'purl', doc.purl, {'store': true});"+
                "    }" +
                "    if (doc.releaseId && typeof(doc.releaseId) == 'string' && doc.releaseId.length > 0) {" +
                "      index('text', 'releaseId', doc.releaseId, {'store': true});"+
                "    }" +
                "    if (doc.vcs && typeof(doc.vcs) == 'string' && doc.vcs.length > 0) {" +
                "      index('text', 'vcs', doc.vcs, {'store': true});"+
                "    }" +
                "    if (doc.packageManager && typeof(doc.packageManager) == 'string' && doc.packageManager.length > 0) {" +
                "      index('text', 'packageManager', doc.packageManager, {'store': true});"+
                "    }" +
                "    if (doc.packageType && typeof(doc.packageType) == 'string' && doc.packageType.length > 0) {" +
                "      index('text', 'packageType', doc.packageType, {'store': true});"+
                "    }" +
                "    if (doc.createdBy && typeof(doc.createdBy) == 'string' && doc.createdBy.length > 0) {" +
                "      index('text', 'createdBy', doc.createdBy, {'store': true});"+
                "    }" +
                "    if(doc.createdOn && doc.createdOn.length) {"+
                "      var dt = new Date(doc.createdOn);"+
                "      var formattedDt = `${dt.getFullYear()}${(dt.getMonth()+1).toString().padStart(2,'0')}${dt.getDate().toString().padStart(2,'0')}`;" +
                "      index('double', 'createdOn', Number(formattedDt), {'store': true});"+
                "    }" +
                "    arrayToStringIndex(doc.licenseIds, 'licenseIds');" +
                "}"));


    private final NouveauLuceneAwareDatabaseConnector connector;

    @Autowired
    public PackageSearchHandler(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db,
            @Qualifier("COUCH_DB_DATABASE") String dbName,
            @Qualifier("LUCENE_SEARCH_LIMIT") int luceneSearchLimit
    ) throws IOException {
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson(), luceneSearchLimit);
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
    }

    public List<Package> searchPackagesWithRestrictions(String text, final Map<String , Set<String>> subQueryRestrictions) {
        return connector.searchViewWithRestrictionsWithAnd(Package.class, luceneSearchView.getIndexName(),
                text, subQueryRestrictions);
    }

    public List<Package> searchPackages(String searchText) {
        return connector.searchView(Package.class, luceneSearchView.getIndexName(),
                prepareWildcardQuery(searchText));
    }

    public List<Package> searchAccessiblePackages(String text, final Map<String,
            Set<String>> subQueryRestrictions, User user ){
        List<Package> resultPackageList = connector.searchViewWithRestrictionsWithAnd(Package.class,
                luceneSearchView.getIndexName(), text, subQueryRestrictions);
        return resultPackageList;
    }
}
