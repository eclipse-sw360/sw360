package org.eclipse.sw360.spdxpackageinfo.db;

import org.eclipse.sw360.components.summary.DocumentCreationInformationSummary;
import org.eclipse.sw360.components.summary.PackageInformationSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.spdx.packageinformation.*;

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

public class SpdxPackageInfoRepository extends SummaryAwareRepository<PackageInformation> {

    private static final String ALL = "function(doc) { if (doc.type == 'spdxPackageInformation') emit(null, doc._id) }";

    public SpdxPackageInfoRepository(DatabaseConnectorCloudant db) {
        super(PackageInformation.class, db, new PackageInformationSummary());
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<PackageInformation> getPackageInformationSummary() {
        return makeSummary(SummaryType.SUMMARY, getAllIds());
    }

}