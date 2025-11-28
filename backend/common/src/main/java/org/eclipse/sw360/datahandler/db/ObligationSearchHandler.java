/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

@Component
public class ObligationSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("obligations",
            new NouveauIndexFunction(
                "function(doc) {" +
                "    if(!doc.type || doc.type != 'obligation') return;" +
                "    if(doc.title !== undefined && doc.title != null && doc.title.length >0) {" +
                "      index('text', 'title', doc.title, {'store': true});" +
                "    }" +
                "    if(doc.text !== undefined && doc.text != null && doc.text.length >0) {" +
                "      index('text', 'text', doc.text, {'store': true});" +
                "    }" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    @Autowired
    public ObligationSearchHandler(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db,
            @Qualifier("COUCH_DB_DATABASE") String dbName,
            @Qualifier("LUCENE_SEARCH_LIMIT") int luceneSearchLimit
    ) throws IOException {
        // Creates the database connector and adds the lucene search view
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson(), luceneSearchLimit);
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
        connector.setResultLimit(luceneSearchLimit);
    }

    public List<Obligation> search(String searchText) {
        return connector.searchView(Obligation.class, luceneSearchView.getIndexName(),
                prepareWildcardQuery(searchText));
    }
}
