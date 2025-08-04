/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

/**
 * Lucene search for the Release class
 *
 * @author thomas.maier@evosoft.com
 */
public class ReleaseSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("releases",
            new NouveauIndexFunction(
                "function(doc) {" +
                "  if(doc.type == 'release') {" +
                "    if (doc.name && typeof(doc.name) == 'string' && doc.name.length > 0) {" +
                "      index('text', 'name', doc.name, {'store': true});" +
                "    }" +
                "    if (doc.version && typeof(doc.version) == 'string' && doc.version.length > 0) {" +
                "      index('text', 'version', doc.version, {'store': true});" +
                "    }" +
                "    index('text', 'id', doc._id, {'store': true});" +
                "  }" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ReleaseSearchHandler(Cloudant cClient, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
    }

    public Map<PaginationData, List<Release>> search(String searchText, PaginationData pageData) {
        Map<PaginationData, List<Release>> resultReleaseList = connector
                .searchViewWithRestrictions(Release.class,
                        luceneSearchView.getIndexName(), null,
                        Map.of(Release._Fields.NAME.getFieldName(),
                                Collections.singleton(prepareWildcardQuery(searchText))
                        ),
                        pageData, null, pageData.isAscending());

        PaginationData respPageData = resultReleaseList.keySet().iterator().next();
        List<Release> releaseList = resultReleaseList.values().iterator().next();

        return Collections.singletonMap(respPageData, releaseList);
    }
}
