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

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

public class ObligationElementSearchHandler {

	private static final LuceneSearchView luceneSearchView = new LuceneSearchView("lucene", "obligationelements",
			"function(doc) {" + "  if(doc.type == 'obligationElement') { " + "      var ret = new Document();"
					+ "      ret.add(doc.langElement);  " + "      ret.add(doc.action);  "
					+ "      ret.add(doc.object);  " + "      return ret;" + "  }" + "}");

	private final LuceneAwareDatabaseConnector connector;

	public ObligationElementSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cCLient,
			String dbName) throws IOException {
		// Creates the database connector and adds the lucene search view
		connector = new LuceneAwareDatabaseConnector(httpClient, cCLient, dbName);
		connector.addView(luceneSearchView);
		connector.setResultLimit(DatabaseSettings.LUCENE_SEARCH_LIMIT);
	}

	public List<ObligationElement> search(String searchText) {
		return connector.searchView(ObligationElement.class, luceneSearchView, prepareWildcardQuery(searchText));
	}

}
