/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * CRUD access for the VMProcessReporting class
 *
 * @author stefan.jaeger@evosoft.com
 */
@Component
public class VMProcessReportingRepository extends DatabaseRepositoryCloudantClient<VMProcessReporting> {

    private static final String ALL =
            "function(doc) {" +
                    "  if (doc.type == 'vmprocessreporting')" +
                    "    emit(null, doc._id) " +
                    "}";

    private static final String BY_START_DATE =
            "function(doc) {" +
                    "  if (doc.type == 'vmprocessreporting') {" +
                    "    emit(doc.startDate, doc._id);" +
                    "  } " +
                    "}";

    @Autowired
    public VMProcessReportingRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_VM") DatabaseConnectorCloudant db
    ) {
        super(db, VMProcessReporting.class);

        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("bystartdate", createMapReduce(BY_START_DATE, null));
        initStandardDesignDocument(views, db);
    }

    public VMProcessReporting getProcessReportingByStartDate(String startDate) {
        final Set<String> idList = queryForIdsAsValue("bystartdate", startDate);
        if (idList != null && !idList.isEmpty())
            return get(CommonUtils.getFirst(idList));
        return null;
    }

}
