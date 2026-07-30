/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Nouveau search handler for ClearingRequests with paginated results.
 */
public class ClearingRequestSearchHandler extends BaseNouveauSearchHandler<ClearingRequest> {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final List<IndexField> CLEARING_REQUEST_FIELDS = List.of(
            IndexField.simple("clearingState", "keyword"),
            IndexField.simple("projectBU"),
            IndexField.simple("requestingUser", "email"),
            IndexField.simple("clearingTeam", "email"),
            IndexField.doubleField("timestamp")
    );

    private static final BuiltIndexDefinition CLEARING_REQUEST_INDEX = buildIndexFunction(
            "clearingRequest",
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            CLEARING_REQUEST_FIELDS,
            null,
            Map.of(),
            "standard"
    );

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ClearingRequestSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(ClearingRequest.class, "clearingRequests", CLEARING_REQUEST_INDEX);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

        public Map<PaginationData, List<ClearingRequest>> search(
            final Map<String, Set<String>> subQueryRestrictions, PaginationData pageData) {
        Map<PaginationData, List<ClearingRequest>> result = baseSearch(connector, subQueryRestrictions, pageData);

        PaginationData respPageData = result.keySet().iterator().next();
        List<ClearingRequest> items = result.values().iterator().next();

        return Collections.singletonMap(respPageData, items);
    }

    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
        return switch (sortColumnNumber) {
            case 0 -> List.of("clearingState_sort", "-timestamp");
            case 1 -> List.of("projectBU_sort", "-timestamp");
            case 2 -> List.of("requestingUser_sort", "-timestamp");
            case 3 -> List.of("clearingTeam_sort", "-timestamp");
            case -1 -> List.of("timestamp");
            default -> List.of(SCORE_SORTING_FIELD);
        };
    }
}
