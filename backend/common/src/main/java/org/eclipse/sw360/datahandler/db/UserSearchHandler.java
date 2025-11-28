/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserSortColumn;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.common.utils.SearchUtils.OBJ_ARRAY_TO_STRING_INDEX;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareFuzzyQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

@Component
public class UserSearchHandler {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("users",
            new NouveauIndexFunction("function(doc) {" +
                "  if (doc.type == 'user') { " +
                "    if (doc.givenname && typeof(doc.givenname) == 'string' && doc.givenname.length > 0) {" +
                "      index('text', 'givenname', doc.givenname, {'store': true});" +
                "    }" +
                "    if (doc.lastname && typeof(doc.lastname) == 'string' && doc.lastname.length > 0) {" +
                "      index('text', 'lastname', doc.lastname, {'store': true});" +
                "    }" +
                "    if (doc.email && typeof(doc.email) == 'string' && doc.email.length > 0) {" +
                "      index('text', 'email', doc.email, {'store': true});" +
                "    }" +
                "  }" +
                "}"));

    private static final NouveauIndexDesignDocument luceneUserSearchView
        = new NouveauIndexDesignDocument("usersearch",
            new NouveauIndexFunction("function(doc) {" +
                OBJ_ARRAY_TO_STRING_INDEX +
                "    if (!doc.type || doc.type != 'user') return;" +
                "    if (doc.givenname && typeof(doc.givenname) == 'string' && doc.givenname.length > 0) {" +
                "      index('text', 'givenname', doc.givenname, {'store': true});" +
                "      index('string', 'givenname_sort', doc.givenname);" +
                "    }" +
                "    if (doc.lastname && typeof(doc.lastname) == 'string' && doc.lastname.length > 0) {" +
                "      index('text', 'lastname', doc.lastname, {'store': true});" +
                "      index('string', 'lastname_sort', doc.lastname);" +
                "    }" +
                "    if (doc.email && typeof(doc.email) == 'string' && doc.email.length > 0) {" +
                "      index('text', 'email', doc.email, {'store': true});" +
                "      index('string', 'email_sort', doc.email);" +
                "    }" +
                "    if (doc.userGroup && typeof(doc.userGroup) == 'string' && doc.userGroup.length > 0) {" +
                "      index('text', 'userGroup', doc.userGroup, {'store': true});" +
                "    }" +
                "    if (doc.department && typeof(doc.department) == 'string' && doc.department.length > 0) {" +
                "      index('text', 'department', doc.department, {'store': true});" +
                "      index('string', 'department_sort', doc.department);" +
                "    }" +
                "    if (doc.deactivated && typeof(doc.deactivated) == 'boolean') {" +
                "      index('double', 'deactivated', doc.deactivated ? 0 : 1);" +
                "    }" +
                "    arrayToStringIndex(doc.primaryRoles, 'primaryroles');" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    @Autowired
    public UserSearchHandler(
            @Qualifier("CLOUDANT_DB_CONNECTOR_USERS") DatabaseConnectorCloudant db,
            @Qualifier("COUCH_DB_USERS") String dbName,
            @Qualifier("LUCENE_SEARCH_LIMIT") int luceneSearchLimit
    ) throws IOException {
        // Creates the database connector and adds the lucene search view
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson(), luceneSearchLimit);
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        searchView.addNouveau(luceneUserSearchView, gson);
        connector.addDesignDoc(searchView);
    }

    private String cleanUp(String searchText) {
        // Lucene seems to split email addresses at an '@' when indexing
        // so in this case we only search for the username in front of the '@'
        return searchText.split("@")[0];
    }

    public List<User> searchByNameAndEmail(String searchText) {
        // Query the search view for the provided text
        if(searchText == null) {
            searchText = "";
        }
        String queryString = prepareFuzzyQuery(cleanUp(searchText));
        return connector.searchAndSortByScore(User.class, luceneSearchView.getIndexName(), queryString);
    }

    public Map<PaginationData, List<User>> search(String text, final Map<String, Set<String>> subQueryRestrictions, @Nonnull PaginationData pageData) {
        String sortColumn = getSortColumnName(pageData);
        return connector.searchViewWithRestrictionsWithAnd(User.class,
                luceneUserSearchView.getIndexName(), text, subQueryRestrictions,
                pageData, sortColumn, pageData.isAscending());
    }

    /**
     * Convert sort column number back to sorting column name. This function makes sure to use the string column (with
     * `_sort` suffix) for text indexes.
     * @param pageData Pagination Data from the request.
     * @return Sort column name. Defaults to givenname_sort
     */
    private static @Nonnull String getSortColumnName(@Nonnull PaginationData pageData) {
        return switch (UserSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case UserSortColumn.BY_LASTNAME -> "lastname_sort";
            case UserSortColumn.BY_EMAIL -> "email_sort";
            case UserSortColumn.BY_STATUS -> "deactivated";
            case UserSortColumn.BY_DEPARTMENT -> "department_sort";
            case UserSortColumn.BY_ROLE -> "primaryroles_sort";
            case null -> "givenname_sort";
            default -> "givenname_sort";
        };
    }
}
