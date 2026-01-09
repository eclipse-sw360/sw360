/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationSortColumn;
import org.jetbrains.annotations.NotNull;

/**
 * CRUD access for the Obligation class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class TodoRepository extends DatabaseRepositoryCloudantClient<Obligation> {

    private static final String ALL = "function(doc) { if (doc.type == 'obligation') emit(null, doc._id) }";

    private static final String BY_LOWERCASE_OBLIGATION_TITLE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'obligation' && doc.title != null) {" +
                    "    emit(doc.title.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_OBLIGATION_TEXT_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'obligation' && doc.text != null) {" +
                    "    emit(doc.text.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_OBLIGATION_LEVEL_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'obligation' && doc.obligationLevel != null) {" +
                    "    emit(doc.obligationLevel.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    public TodoRepository(DatabaseConnectorCloudant db) {
        super(db, Obligation.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("obligationbytitle", createMapReduce(BY_LOWERCASE_OBLIGATION_TITLE_VIEW, null));
        views.put("obligationbytext", createMapReduce(BY_LOWERCASE_OBLIGATION_TEXT_VIEW, null));
        views.put("obligationbylevel", createMapReduce(BY_LOWERCASE_OBLIGATION_LEVEL_VIEW, null));
        initStandardDesignDocument(views, db);
    }

    public Map<PaginationData, List<Obligation>> getObligationsPaginated(PaginationData pageData) {
        String viewName = getViewFromPagination(pageData);

        List<Obligation> obligations = queryViewPaginated(viewName, pageData, false);

        return Collections.singletonMap(pageData, obligations);
    }

    private static @NotNull String getViewFromPagination(PaginationData pageData) {
        return switch (ObligationSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case ObligationSortColumn.BY_TEXT -> "obligationbytext";
            case ObligationSortColumn.BY_LEVEL -> "obligationbylevel";
            case null, default -> "obligationbytitle";
        };
    }
}
