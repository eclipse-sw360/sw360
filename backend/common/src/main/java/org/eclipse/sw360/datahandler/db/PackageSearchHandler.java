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

import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

public class PackageSearchHandler {

    private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "packages",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if (!doc.type) return ret;" +
                    "    if (doc.type != 'package') return ret;" +
                    "    function idx(obj) {" +
                    "        for (var key in obj) {" +
                    "            switch (typeof obj[key]) {" +
                    "                case 'object':" +
                    "                    idx(obj[key]);" +
                    "                    break;" +
                    "                case 'function':" +
                    "                    break;" +
                    "                default:" +
                    "                    ret.add(obj[key]);" +
                    "                    break;" +
                    "            }" +
                    "        }" +
                    "    };" +
                    "    idx(doc);" +
                    "    if (doc.name) {  "+
                    "      ret.add(doc.name, {\"field\": \"name\"} );" +
                    "    }" +
                    "    if (doc.version) {  "+
                    "      ret.add(doc.version, {\"field\": \"version\"} );" +
                    "    }" +
                    "    if (doc.purl) {  "+
                    "         ret.add(doc.purl, {\"field\": \"purl\"} );" +
                    "    }" +
                    "    if (doc.releaseId) {  "+
                    "      ret.add(doc.releaseId, {\"field\": \"releaseId\"} );" +
                    "    }" +
                    "    if (doc.vcs) {  "+
                    "      ret.add(doc.vcs, {\"field\": \"vcs\"} );" +
                    "    }" +
                    "    if (doc.packageManager) {  "+
                    "      ret.add(doc.packageManager, {\"field\": \"packageManager\"} );" +
                    "    }" +
                    "    if (doc.packageType) {  "+
                    "      ret.add(doc.packageType, {\"field\": \"packageType\"} );" +
                    "    }" +
                    "    if (doc.createdBy) {  "+
                    "      ret.add(doc.createdBy, {\"field\": \"createdBy\"} );" +
                    "    }" +
                    "    if (doc.createdOn) {  "+
                    "      ret.add(doc.createdOn, {\"field\": \"createdOn\", \"type\": \"date\"} );" +
                    "    }" +
                    "    for (var i in doc.licenseIds) {" +
                    "      ret.add(doc.licenseIds[i], {\"field\": \"licenseIds\"} );" +
                    "    }" +
                    "    return ret;" +
                    "}");


    private final LuceneAwareDatabaseConnector connector;

    public PackageSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cloudantClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cloudantClient, dbName);
        connector.addView(luceneSearchView);
    }

    public List<Package> searchPackagesWithRestrictions(String text, final Map<String , Set<String>> subQueryRestrictions) {
        return connector.searchViewWithRestrictions(Package.class, luceneSearchView, text, subQueryRestrictions);
    }

    public List<Package> searchPackages(String searchText) {
        return connector.searchView(Package.class, luceneSearchView, prepareWildcardQuery(searchText));
    }

}
