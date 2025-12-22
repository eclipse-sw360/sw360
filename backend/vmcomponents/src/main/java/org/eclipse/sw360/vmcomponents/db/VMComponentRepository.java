/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * CRUD access for the VMComponent class
 *
 * @author stefan.jaeger@evosoft.com
 */
@Component
public class VMComponentRepository extends DatabaseRepositoryCloudantClient<VMComponent> {

    private static final String ALL =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent') " +
                    "    emit(null, doc._id) " +
                    "}";

    private static final String BY_VMID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent') {" +
                    "    emit(doc.vmid, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LAST_UPDATE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.lastUpdateDate != null) {" +
                    "    emit(doc.lastUpdateDate, doc._id);" +
                    "  } " +
                    "}";

    private static final String ALL_VMIDS =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent') {" +
                    "    emit(null, doc.vmid);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_NAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.name != null) {" +
                    "    emit(doc.name.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_VENDOR_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.vendor != null) {" +
                    "    emit(doc.vendor.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_VERSION_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmcomponent' && doc.version != null) {" +
                    "    emit(doc.version.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    @Autowired
    public VMComponentRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_VM") DatabaseConnectorCloudant db
    ) {
        super(db, VMComponent.class);

        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byvmid", createMapReduce(BY_VMID_VIEW, null));
        views.put("bylastupdate", createMapReduce(BY_LAST_UPDATE_VIEW, null));
        views.put("all_vmids", createMapReduce(ALL_VMIDS, null));
        views.put("componentByName", createMapReduce(BY_LOWERCASE_NAME_VIEW, null));
        views.put("componentByVendor", createMapReduce(BY_LOWERCASE_VENDOR_VIEW, null));
        views.put("componentByVersion", createMapReduce(BY_LOWERCASE_VERSION_VIEW, null));
        initStandardDesignDocument(views, db);
    }

    public VMComponent getComponentByVmid(String vmid) {
        final Set<String> idList = queryForIdsAsValue("byvmid", vmid);
        if (idList != null && !idList.isEmpty())
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    public VMComponent getComponentByLastUpdate(String lastUpdateDate) {
        final Set<String> idList;
        if (lastUpdateDate == null) {
            idList = queryForIdsAsValue(getConnector()
                    .getPostViewQueryBuilder(VMComponent.class, "bylastupdate")
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
                .getPostViewQueryBuilder(VMComponent.class, "all_vmids")
                .build());
    }

    public Set<String> getComponentByLowercaseNamePrefix(String namePrefix) {
        return queryForIdsByPrefix("componentByName", namePrefix != null ? namePrefix.toLowerCase() : namePrefix);
    }

    public Set<String> getComponentByLowercaseVendorPrefix(String vendorPrefix) {
        return queryForIdsByPrefix("componentByVendor", vendorPrefix != null ? vendorPrefix.toLowerCase() : vendorPrefix);
    }

    public Set<String> getComponentByLowercaseVersionPrefix(String versionPrefix) {
        return queryForIdsByPrefix("componentByVersion", versionPrefix != null ? versionPrefix.toLowerCase() : versionPrefix);
    }
}
