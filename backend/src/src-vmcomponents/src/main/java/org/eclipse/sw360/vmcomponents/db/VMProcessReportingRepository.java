/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
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
