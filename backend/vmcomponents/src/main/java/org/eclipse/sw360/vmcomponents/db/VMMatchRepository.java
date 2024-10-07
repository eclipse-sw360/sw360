/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.ektorp.support.View;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * CRUD access for the VMMatch class
 *
 * @author stefan.jaeger@evosoft.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'vmmatch') emit(null, doc._id) }")
public class VMMatchRepository extends DatabaseRepository<VMMatch> {

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

    public VMMatchRepository(DatabaseConnector db) {
        super(VMMatch.class, db);

        initStandardDesignDocument();
    }

    @View(name = "byids", map = BY_IDs_VIEW)
    public VMMatch getMatchByIds(String releaseId, String vmComponentId) {
        final Set<String> idList = queryForIdsAsComplexValue("byids", releaseId, vmComponentId);
        if (idList != null && idList.size() > 0)
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    @View(name = "byComponentId", map = BY_COMPONENT_ID_VIEW)
    public List<VMMatch> getMatchesByComponentIds(Collection<String> componentIds) {
        return queryByIds("byComponentId", componentIds);
    }


}
