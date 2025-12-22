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

import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseSortColumn;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
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
@Component
public class ReleaseSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("releases",
            new NouveauIndexFunction(
                "function(doc) {" +
                "  if(doc.type == 'release') {" +
                "    if (doc.name && typeof(doc.name) == 'string' && doc.name.length > 0) {" +
                "      index('text', 'name', doc.name, {'store': true});" +
                "      index('string', 'name_sort', doc.name);" +
                "    }" +
                "    if (doc.version && typeof(doc.version) == 'string' && doc.version.length > 0) {" +
                "      index('text', 'version', doc.version, {'store': true});" +
                "      index('string', 'version_sort', doc.version);" +
                "    }" +
                "    if(doc.createdOn && doc.createdOn.length) {"+
                "      var dt = new Date(doc.createdOn);"+
                "      var formattedDt = `${dt.getFullYear()}${(dt.getMonth()+1).toString().padStart(2,'0')}${dt.getDate().toString().padStart(2,'0')}`;" +
                "      index('double', 'createdOn', Number(formattedDt), {'store': true});"+
                "    }" +
                "    index('text', 'id', doc._id, {'store': true});" +
                "  }" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    @Autowired
    public ReleaseSearchHandler(
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

    public Map<PaginationData, List<Release>> search(String searchText, PaginationData pageData) {
        String sortColumn = getSortColumnName(pageData);
        Map<PaginationData, List<Release>> resultReleaseList = connector
                .searchViewWithRestrictionsWithAnd(Release.class,
                        luceneSearchView.getIndexName(), null,
                        Map.of(Release._Fields.NAME.getFieldName(),
                                Collections.singleton(prepareWildcardQuery(searchText))
                        ),
                        pageData, sortColumn, pageData.isAscending());

        PaginationData respPageData = resultReleaseList.keySet().iterator().next();
        List<Release> releaseList = resultReleaseList.values().iterator().next();

        return Collections.singletonMap(respPageData, releaseList);
    }

    /**
     * Convert sort column number back to sorting column name. This function makes sure to use the string column (with
     * `_sort` suffix) for text indexes.
     * @param pageData Pagination Data from the request.
     * @return Sort column name. Defaults to createdOn
     */
    private static @Nonnull String getSortColumnName(@Nonnull PaginationData pageData) {
        return switch (ReleaseSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case ReleaseSortColumn.BY_NAME -> "name_sort";
            case ReleaseSortColumn.BY_VERSION -> "version_sort";
            case null -> "createdOn";
            default -> "createdOn";
        };
    }
}
