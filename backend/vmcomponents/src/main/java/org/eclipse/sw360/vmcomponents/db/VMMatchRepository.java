/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRUD access for the VMMatch class
 *
 * @author stefan.jaeger@evosoft.com
 */
@Component
public class VMMatchRepository extends DatabaseRepositoryCloudantClient<VMMatch> {

    private static final String ALL =
            "function (doc) {" +
                    "  if (doc.type == 'vmmatch')" +
                    "    emit(null, doc._id) " +
                    "}";

    private static final String BY_IDs_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmmatch') {" +
                    "    emit([doc.releaseId, doc.vmComponentId], doc);" +
                    "  } " +
                    "}";

    private static final String BY_COMPONENT_ID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmmatch') {" +
                    "    emit(doc.vmComponentId, doc);" +
                    "  } " +
                    "}";

    @Autowired
    public VMMatchRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_VM") DatabaseConnectorCloudant db
    ) {
        super(db, VMMatch.class);

        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byids", createMapReduce(BY_IDs_VIEW, null));
        views.put("byComponentId", createMapReduce(BY_COMPONENT_ID_VIEW, null));
        initStandardDesignDocument(views, db);
    }

    public VMMatch getMatchByIds(String releaseId, String vmComponentId) {
        final Set<String> idList = queryForIdsAsComplexValue("byids", releaseId, vmComponentId);
        if (idList != null && !idList.isEmpty())
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    public List<VMMatch> getMatchesByComponentIds(Collection<String> componentIds) {
        return queryByIds("byComponentId", componentIds);
    }
}
