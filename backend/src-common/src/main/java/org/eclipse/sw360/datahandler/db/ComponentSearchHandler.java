/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Class for accessing the Lucene connector on the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentSearchHandler {

    private static final Logger log = LogManager.getLogger(ComponentSearchHandler.class);

    private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "components",
            "function(doc) {" +
                    "    var ret = new Document();" +
                    "    if(!doc.type) return ret;" +
                    "    if(doc.type != 'component') return ret;" +
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
                    "    for(var i in doc.categories) {" +
                    "      ret.add(doc.categories[i], {\"field\": \"categories\"} );" +
                    "    }" +
                    "    for(var i in doc.languages) {" +
                    "      ret.add(doc.languages[i], {\"field\": \"languages\"} );" +
                    "    }" +
                    "    for(var i in doc.softwarePlatforms) {" +
                    "      ret.add(doc.softwarePlatforms[i], {\"field\": \"softwarePlatforms\"} );" +
                    "    }" +
                    "    for(var i in doc.operatingSystems) {" +
                    "      ret.add(doc.operatingSystems[i], {\"field\": \"operatingSystems\"} );" +
                    "    }" +
                    "    for(var i in doc.vendorNames) {" +
                    "      ret.add(doc.vendorNames[i], {\"field\": \"vendorNames\"} );" +
                    "    }" +
                    "    for(var i in doc.mainLicenseIds) {" +
                    "      ret.add(doc.mainLicenseIds[i], {\"field\": \"mainLicenseIds\"} );" +
                    "    }" +
                    "        ret.add(doc.componentType, {\"field\": \"componentType\"} );" +
                    "    if(doc.name !== undefined && doc.name != null && doc.name.length >0) {  "+
                    "      ret.add(doc.name, {\"field\": \"name\"} );" +
                    "    }" +
                    "    if(doc.createdBy && doc.createdBy.length) {  "+
                    "      ret.add(doc.createdBy, {\"field\": \"createdBy\"} );" +
                    "    }" +
                    "    if(doc.createdOn && doc.createdOn.length) {  "+
                    "      ret.add(doc.createdOn, {\"field\": \"createdOn\", \"type\": \"date\"} );" +
                    "    }" +
                    "    return ret;" +
                    "}");


    private final LuceneAwareDatabaseConnector connector;

    public ComponentSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cClient, dbName);
        connector.addView(luceneSearchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<Component> search(String text, final Map<String , Set<String > > subQueryRestrictions ){
        return connector.searchViewWithRestrictions(Component.class, luceneSearchView, text, subQueryRestrictions);
    }

}
