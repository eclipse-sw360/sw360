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

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareFuzzyQuery;

public class UserSearchHandler {

	private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "users",
			"function(doc) {" + "  if(doc.type == 'user') { " + "      var ret = new Document();"
					+ "      ret.add(doc.givenname);  " + "      ret.add(doc.lastname);  "
					+ "      ret.add(doc.email);  " + "      return ret;" + "  }" + "}");

	private static final LuceneSearchView luceneUserSearchView = new LuceneSearchView("lucene", "usersearch",
			"function(doc) {" + "    var ret = new Document();" + "    if (!doc.type) return ret;"
					+ "    if (doc.type != 'user') return ret;" + "    function idx(obj) {"
					+ "        for (var key in obj) {" + "            switch (typeof obj[key]) {"
					+ "                case 'object':" + "                    idx(obj[key]);"
					+ "                    break;" + "                case 'function':" + "                    break;"
					+ "                default:" + "                    ret.add(obj[key]);"
					+ "                    break;" + "            }" + "        }" + "    };" + "    idx(doc);"
					+ "    if (doc.givenname) {  " + "      ret.add(doc.givenname, {\"field\": \"givenname\"} );"
					+ "    }" + "    if (doc.lastname) {  " + "      ret.add(doc.lastname, {\"field\": \"lastname\"} );"
					+ "    }" + "    if (doc.email) {  " + "      ret.add(doc.email, {\"field\": \"email\"} );"
					+ "    }" + "    if (doc.userGroup) {  "
					+ "      ret.add(doc.userGroup, {\"field\": \"userGroup\"} );" + "    }"
					+ "    if (doc.department) {  " + "      ret.add(doc.department, {\"field\": \"department\"} );"
					+ "    }" + "    return ret;" + "}");

	private final LuceneAwareDatabaseConnector connector;

	public UserSearchHandler(DatabaseConnector databaseConnector, Supplier<CloudantClient> cClient) throws IOException {
		// Creates the database connector and adds the lucene search view
		connector = new LuceneAwareDatabaseConnector(databaseConnector, cClient);
		connector.addView(luceneSearchView);
		connector.addView(luceneUserSearchView);
	}

	private String cleanUp(String searchText) {
		// Lucene seems to split email addresses at an '@' when indexing
		// so in this case we only search for the user name in front of the '@'
		return searchText.split("@")[0];
	}

	public List<User> searchByNameAndEmail(String searchText) {
		// Query the search view for the provided text
		if (searchText == null) {
			searchText = "";
		}
		String queryString = prepareFuzzyQuery(cleanUp(searchText));
		return connector.searchView(User.class, luceneSearchView, queryString);
	}

	public List<User> search(String text, final Map<String, Set<String>> subQueryRestrictions) {
		return connector.searchViewWithRestrictions(User.class, luceneUserSearchView, text, subQueryRestrictions);
	}
}
