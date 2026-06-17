/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting;
import org.eclipse.sw360.datahandler.common.CommonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRUD access for the VMProcessReporting class
 *
 * @author stefan.jaeger@evosoft.com
 */
public class VMProcessReportingRepository extends DatabaseRepositoryCloudantClient<VMProcessReporting> {

    private static final String ALL =
            "function(doc) {" +
                    "  if (doc.type == 'vmprocessreporting')" +
                    "    emit(null, doc._id) " +
                    "}";

    private static final String BY_START_DATE =
            """
                    function(doc) {
                      if (
                          doc.type == 'vmprocessreporting'
                          && doc.startDate && doc.startDate.length > 0
                          && doc.elementType && doc.elementType.length > 0
                      ) {
                        emit([doc.elementType, doc.startDate], doc._id);
                      }
                    }""";

    private static final String BY_SUCCESS_END_DATE =
            """
                    function(doc) {
                      if (
                          doc.type == 'vmprocessreporting'
                          && doc.endDate && doc.endDate.length > 0
                          && doc.elementType && doc.elementType.length > 0
                      ) {
                        emit([doc.elementType, doc.endDate], doc._id);
                      }
                    }""";

    public VMProcessReportingRepository(DatabaseConnectorCloudant db) {
        super(db, VMProcessReporting.class);

        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("bystartdate", createMapReduce(BY_START_DATE, null));
        views.put("bySuccessEndDate", createMapReduce(BY_SUCCESS_END_DATE, null));
        initStandardDesignDocument(views, db);
    }

    /**
     * Get {@link VMProcessReporting} document for given element and startDate.
     *
     * @param startDate   the startDate to look up
     * @param elementType simple class name of the element type
     *                    (e.g. {@code "VMComponent"});
     */
    public VMProcessReporting getProcessReportingByStartDate(String startDate, String elementType) {
        List<Object> complexKeysList = Collections.singletonList(new String[]{elementType, startDate});

        Set<String> idList = queryForIdsAsValue("bystartdate",
                complexKeysList);
        if (CommonUtils.isNullOrEmptyCollection(idList)) {
            return null;
        }
        for (String id : idList) {
            VMProcessReporting candidate = get(id);
            if (candidate != null && elementType.equals(candidate.getElementType())) {
                return candidate;
            }
        }
        return null;
    }

    public VMProcessReporting getLastSuccessfulProcessByElementType(String elementType) {
        PaginationData pageData = new PaginationData()
                .setRowsPerPage(1).setDisplayStart(0).setAscending(false);

        List<VMProcessReporting> results = queryViewWithComplexKeysPaginated(
                "bySuccessEndDate", elementType, pageData);
        return results.isEmpty() ? null : results.getFirst();
    }
}
