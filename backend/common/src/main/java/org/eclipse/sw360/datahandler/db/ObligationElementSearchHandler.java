/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import java.io.IOException;
import java.util.List;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

public class ObligationElementSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("obligationelements",
            new NouveauIndexFunction(
                "function(doc) {" +
                "  if(doc.type == 'obligationElement') {" +
                "    if (doc.langElement && typeof(doc.langElement) == 'string' && doc.langElement.length > 0) {" +
                "      index('text', 'langElement', doc.langElement, {'store': true});" +
                "    }" +
                "    if (doc.action && typeof(doc.action) == 'string' && doc.action.length > 0) {" +
                "      index('text', 'action', doc.action, {'store': true});" +
                "    }" +
                "    if (doc.object && typeof(doc.object) == 'string' && doc.object.length > 0) {" +
                "      index('text', 'object', doc.object, {'store': true});" +
                "    }" +
                "  }" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ObligationElementSearchHandler(Cloudant cClient, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        // Creates the database connector and adds the lucene search view
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
        connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
    }

    public List<ObligationElement> search(String searchText) {
        return connector.searchView(ObligationElement.class, luceneSearchView.getIndexName(),
                prepareWildcardQuery(searchText));
    }
}
