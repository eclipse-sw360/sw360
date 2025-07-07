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
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceComparatorGenerator;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserSortColumn;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.prepareFuzzyQuery;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

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
                "    if (!doc.type || doc.type != 'user') return;" +
                "    if (doc.givenname && typeof(doc.givenname) == 'string' && doc.givenname.length > 0) {" +
                "      index('text', 'givenname', doc.givenname, {'store': true});" +
                "    }" +
                "    if (doc.lastname && typeof(doc.lastname) == 'string' && doc.lastname.length > 0) {" +
                "      index('text', 'lastname', doc.lastname, {'store': true});" +
                "    }" +
                "    if (doc.email && typeof(doc.email) == 'string' && doc.email.length > 0) {" +
                "      index('text', 'email', doc.email, {'store': true});" +
                "    }" +
                "    if (doc.userGroup && typeof(doc.userGroup) == 'string' && doc.userGroup.length > 0) {" +
                "      index('text', 'userGroup', doc.userGroup, {'store': true});" +
                "    }" +
                "    if (doc.department && typeof(doc.department) == 'string' && doc.department.length > 0) {" +
                "      index('text', 'department', doc.department, {'store': true});" +
                "    }" +
                "}"));

    private final NouveauLuceneAwareDatabaseConnector connector;

    public UserSearchHandler(Cloudant client, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        // Creates the database connector and adds the lucene search view
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
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
        ResourceComparatorGenerator<User> resourceComparatorGenerator = new ResourceComparatorGenerator<>();
        UserSortColumn sortBy = UserSortColumn.findByValue(pageData.getSortColumnNumber());
        Comparator<User> comparator;

        try {
            comparator = switch (sortBy) {
                case UserSortColumn.BY_GIVENNAME ->
                        resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_USER, User._Fields.GIVENNAME.getFieldName());
                case UserSortColumn.BY_LASTNAME ->
                        resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_USER, User._Fields.LASTNAME.getFieldName());
                case UserSortColumn.BY_EMAIL ->
                        resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_USER, User._Fields.EMAIL.getFieldName());
                case UserSortColumn.BY_STATUS ->
                        resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_USER, User._Fields.DEACTIVATED.getFieldName());
                case UserSortColumn.BY_DEPARTMENT ->
                        resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_USER, User._Fields.DEPARTMENT.getFieldName());
                case UserSortColumn.BY_ROLE ->
                        resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_USER, User._Fields.USER_GROUP.getFieldName());
                case null, default -> null; // sort by score
            };
        } catch (ResourceClassNotFoundException e) {
            comparator = null;
        }

        Map<PaginationData, List<User>> resultUserList = connector
                .searchViewWithRestrictions(User.class,
                        luceneUserSearchView.getIndexName(), text, subQueryRestrictions,
                        pageData, null, pageData.isAscending());

        PaginationData respPageData = resultUserList.keySet().iterator().next();
        List<User> usersList = resultUserList.values().iterator().next();
        if (comparator != null) {
            usersList.sort(comparator);
        }

        return Collections.singletonMap(respPageData, usersList);
    }
}
