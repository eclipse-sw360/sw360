/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.ektorp.support.View;

import java.util.Set;

/**
 * CRUD access for the VMProcessReporting class
 *
 * @author stefan.jaeger@evosoft.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'vmprocessreporting') emit(null, doc._id) }")
public class VMProcessReportingRepository extends DatabaseRepository<VMProcessReporting> {

    private static final String BY_START_DATE =
            "function(doc) {" +
                    "  if (doc.type == 'vmprocessreporting') {" +
                    "    emit(doc.startDate, doc._id);" +
                    "  } " +
                    "}";

    public VMProcessReportingRepository(DatabaseConnector db) {
        super(VMProcessReporting.class, db);

        initStandardDesignDocument();
    }

    @View(name = "bystartdate", map = BY_START_DATE)
    public VMProcessReporting getProcessReportingByStartDate(String startDate) {
        final Set<String> idList = queryForIdsAsValue("bystartdate", startDate);
        if (idList != null && idList.size() > 0)
            return get(CommonUtils.getFirst(idList));
        return null;
    }

}
