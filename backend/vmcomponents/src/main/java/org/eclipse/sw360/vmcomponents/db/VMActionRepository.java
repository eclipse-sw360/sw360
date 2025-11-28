/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * CRUD access for the VMAction class
 *
 * @author stefan.jaeger@evosoft.com
 */
@Component
public class VMActionRepository extends DatabaseRepositoryCloudantClient<VMAction> {

    private static final String ALL =
            "function(doc) {" +
                    "  if (doc.type == 'vmaction') " +
                    "    emit(null, doc._id) " +
                    "}";

    private static final String BY_VMID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmaction') {" +
                    "    emit(doc.vmid, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LAST_UPDATE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmaction' && doc.lastUpdateDate != null) {" +
                    "    emit(doc.lastUpdateDate, doc._id);" +
                    "  } " +
                    "}";

    private static final String ALL_VMIDS =
            "function(doc) {" +
                    "  if (doc.type == 'vmaction') {" +
                    "    emit(null, doc.vmid);" +
                    "  } " +
                    "}";

    @Autowired
    public VMActionRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_VM") DatabaseConnectorCloudant db
    ) {
        super(db, VMAction.class);

        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byvmid", createMapReduce(BY_VMID_VIEW, null));
        views.put("bylastupdate", createMapReduce(BY_LAST_UPDATE_VIEW, null));
        views.put("all_vmids", createMapReduce(ALL_VMIDS, null));
        initStandardDesignDocument(views, db);
    }

    public VMAction getActionByVmid(String vmid) {
        final Set<String> idList = queryForIdsAsValue("byvmid", vmid);
        if (idList != null && !idList.isEmpty())
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    public VMAction getActionByLastUpdate(String lastUpdateDate) {
        final Set<String> idList;
        if (lastUpdateDate == null){
            idList = queryForIdsAsValue(getConnector()
                    .getPostViewQueryBuilder(VMAction.class, "bylastupdate")
                    .descending(true)
                    .build());
        } else {
            idList = queryForIdsAsValue("bylastupdate", lastUpdateDate);
        }
        if (idList != null && !idList.isEmpty())
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    public Set<String> getAllVmids() {
        return queryForIdsAsValue(getConnector()
                .getPostViewQueryBuilder(VMAction.class, "all_vmids")
                .build());
    }
}
