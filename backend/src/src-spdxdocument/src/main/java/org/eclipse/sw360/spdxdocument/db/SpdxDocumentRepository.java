package org.eclipse.sw360.spdxdocument.db;

import org.eclipse.sw360.components.summary.ReleaseSummary;
import org.eclipse.sw360.components.summary.SpdxSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.UnpaginatedRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SpdxDocumentRepository extends SummaryAwareRepository<SPDXDocument> {

    private static final String ALL = "function(doc) { if (doc.type == 'SPDXDocument') emit(null, doc._id) }";

    public SpdxDocumentRepository(DatabaseConnectorCloudant db) {
        super(SPDXDocument.class, db, new SpdxSummary());
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<SPDXDocument> getSPDXDocumentSummary() {
        return makeSummary(SummaryType.SUMMARY, getAllIds());
    }

}