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

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.ektorp.support.View;

import java.util.Set;

/**
 * CRUD access for the VMPriority class
 *
 * @author stefan.jaeger@evosoft.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'vmpriority') emit(null, doc._id) }")
public class VMPriorityRepository extends DatabaseRepository<VMPriority> {

    private static final String BY_VMID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmpriority') {" +
                    "    emit(doc.vmid, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LAST_UPDATE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vmpriority' && doc.lastUpdateDate != null) {" +
                    "    emit(doc.lastUpdateDate, doc._id);" +
                    "  } " +
                    "}";

    private static final String ALL_VMIDS =
            "function(doc) {" +
                    "  if (doc.type == 'vmpriority') {" +
                    "    emit(null, doc.vmid);" +
                    "  } " +
                    "}";

    public VMPriorityRepository(DatabaseConnector db) {
        super(VMPriority.class, db);

        initStandardDesignDocument();
    }

    @View(name = "byvmid", map = BY_VMID_VIEW)
    public VMPriority getPriorityByVmid(String vmid) {
        final Set<String> idList = queryForIdsAsValue("byvmid", vmid);
        if (idList != null && idList.size() > 0)
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    @View(name = "bylastupdate", map = BY_LAST_UPDATE_VIEW)
    public VMPriority getPriorityByLastUpdate(String lastUpdateDate) {
        final Set<String> idList;
        if (lastUpdateDate == null){
            idList = getAllIdsByView("bylastupdate", true);
        } else {
            idList = queryForIdsAsValue("bylastupdate", lastUpdateDate);
        }
        if (idList != null && idList.size() > 0)
            return get(CommonUtils.getFirst(idList));
        return null;
    }

    @View(name = "all_vmids", map = ALL_VMIDS)
    public Set<String> getAllVmids() {
        return queryForIdsAsValue(createQuery("all_vmids"));
    }


}
