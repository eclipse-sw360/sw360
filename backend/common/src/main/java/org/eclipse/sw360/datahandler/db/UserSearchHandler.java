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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserSortColumn;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareWildcardQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class UserSearchHandler extends BaseNouveauSearchHandler<User> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    /**
     * Fields indexed for user search, grouped by index category.
     *
     * <ul>
     *   <li><b>standard</b>: {@code givenname}, {@code lastname} - full prefix-search support.</li>
     *   <li><b>simple</b>: {@code email} (email analyzer), {@code department} (keyword),
     *       {@code userGroup} (keyword)</li>
     *   <li><b>date</b>: {@code createdOn} - stored as sortable yyyyMMdd double.</li>
     * </ul>
     */
    private static final List<IndexField> USER_FIELDS = List.of(
            IndexField.standard("givenname"),
            IndexField.standard("lastname"),
            IndexField.simple("email", "email"),
            IndexField.simple("department", "keyword"),
            IndexField.simple("userGroup", "keyword"),
            IndexField.date("createdOn")
    );

    /**
     * Custom JS: index {@code primaryRoles} as a concatenated text blob.
     */
    private static final String USER_CUSTOM_JS =
            "    arrayToStringIndex(doc.primaryRoles, 'primaryroles');";

    /**
     * Analyzer overrides that are not auto-generated from {@link #USER_FIELDS}.
     * <ul>
     *   <li>{@code primaryroles_sort} -> {@code keyword} (created by {@code arrayToStringIndex})</li>
     * </ul>
     */
    private static final Map<String, String> USER_CUSTOM_ANALYZERS = Map.of(
            "primaryroles_sort", "keyword"
    );

    // -------------------------------------------------------------------------
    //  Design document
    // -------------------------------------------------------------------------

    private static final BuiltIndexDefinition USER_INDEX_DEFINITION = buildIndexFunction(
            "user",
            null,  // No empty-aware token needed for users
            USER_FIELDS,
            USER_CUSTOM_JS,
            USER_CUSTOM_ANALYZERS,
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;

    public UserSearchHandler(Cloudant client, String dbName) throws IOException {
        super(User.class, "users", USER_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    // -------------------------------------------------------------------------
    //  Public search API (matching ProjectSearchHandler pattern)
    // -------------------------------------------------------------------------

    /**
     * Search with pagination and filter restrictions (primary method)
     */
    public Map<PaginationData, List<User>> search(final Map<String, Set<String>> subQueryRestrictions, PaginationData pageData) {
        return baseSearch(connector, subQueryRestrictions, pageData);
    }

    /**
     * Search with free-text and field restrictions (AND logic for both)
     */
    public Map<PaginationData, List<User>> search(String text, final Map<String, Set<String>> subQueryRestrictions, PaginationData pageData) {
        return baseSearch(connector, subQueryRestrictions, pageData);
    }

    /**
     * Search by free-text term only (simple wildcard query)
     */
    public List<User> search(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return Collections.emptyList();
        }
        // Clean up email addresses (split at '@')
        String cleanedText = cleanUp(searchText);
        return connector.searchView(User.class, getIndexName(),
                prepareWildcardQuery(cleanedText));
    }

    /**
     * Search by free-text and restrictions without pagination (returns all matching)
     */
    public List<User> search(String text, final Map<String, Set<String>> subQueryRestrictions) {
        if (text == null) {
            text = "";
        }
        String cleanedText = text.isEmpty() ? "" : cleanUp(text);
        return connector.searchViewWithRestrictionsWithAnd(User.class, getIndexName(),
                cleanedText, subQueryRestrictions);
    }

    /**
     * Search users by a free-text term matched against givenname, lastname, or email (OR logic).
     * Convenience method for name/email search with pagination.
     */
    public Map<PaginationData, List<User>> searchByNameOrEmail(String searchText, PaginationData pageData) {
        Map<String, Set<String>> subQueryRestrictions = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(searchText)) {
            String cleanedText = cleanUp(searchText);
            subQueryRestrictions.put(User._Fields.GIVENNAME.getFieldName(), Collections.singleton(cleanedText));
            subQueryRestrictions.put(User._Fields.LASTNAME.getFieldName(), Collections.singleton(cleanedText));
            subQueryRestrictions.put(User._Fields.EMAIL.getFieldName(), Collections.singleton(cleanedText));
        }
        return baseSearchOr(connector, subQueryRestrictions, pageData);
    }

    /**
     * Simple free-text search (non-paginated) matching givenname, lastname, or email.
     * Returns users sorted by relevance score.
     */
    public List<User> searchByNameAndEmail(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return Collections.emptyList();
        }
        String cleanedText = cleanUp(searchText);
        return connector.searchAndSortByScore(User.class, getIndexName(), cleanedText);
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    /**
     * Map sort column number to Lucene sort field names with tie-breaking.
     * Similar to ProjectSearchHandler pattern.
     */
    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
        return switch (UserSortColumn.findByValue(sortColumnNumber)) {
            case UserSortColumn.BY_GIVENNAME -> List.of("givenname_sort", "lastname_sort", "email_sort");
            case UserSortColumn.BY_LASTNAME -> List.of("lastname_sort", "givenname_sort", "email_sort");
            case UserSortColumn.BY_EMAIL -> List.of("email_sort", "givenname_sort", "lastname_sort");
            case UserSortColumn.BY_DEPARTMENT -> List.of("department_sort", SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort");
            case UserSortColumn.BY_STATUS -> List.of(SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort");
            case UserSortColumn.BY_ROLE -> List.of("primaryroles_sort", SCORE_SORTING_FIELD, "givenname_sort", "lastname_sort");
            // Default sort by scoring
            case UserSortColumn.BY_SCORE -> List.of(SCORE_SORTING_FIELD);
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    // -------------------------------------------------------------------------
    //  Helper methods
    // -------------------------------------------------------------------------

    /**
     * Clean up search text for email handling.
     * Lucene splits email addresses at '@' when indexing,
     * so we only search for the username part (before the '@').
     */
    private String cleanUp(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return "";
        }
        return searchText.split("@")[0];
    }
}
