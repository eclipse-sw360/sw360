package org.eclipse.sw360.spdxdocumentcreationinfo.db;

import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneSearchView;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector.prepareWildcardQuery;

public class SpdxDocumentCreationInfoSearchHandler {

    private static final LuceneSearchView luceneSearchView
            = new LuceneSearchView("lucene", "spdxDocumentCreationInformation",
            "function(doc) {" +
                    "  if(doc.type == 'spdxDocumentCreationInformation') { " +
                    "      var ret = new Document();" +
                    "      ret.add(doc._id);  " +
                    "      return ret;" +
                    "  }" +
                    "}");

    private final LuceneAwareDatabaseConnector connector;

    public SpdxDocumentCreationInfoSearchHandler(Supplier<HttpClient> httpClient, Supplier<CloudantClient> cClient, String dbName) throws IOException {
        connector = new LuceneAwareDatabaseConnector(httpClient, cClient, dbName);
        connector.addView(luceneSearchView);
    }

    public List<DocumentCreationInformation> search(String searchText) {
        return connector.searchView(DocumentCreationInformation.class, luceneSearchView, prepareWildcardQuery(searchText));
    }
}
