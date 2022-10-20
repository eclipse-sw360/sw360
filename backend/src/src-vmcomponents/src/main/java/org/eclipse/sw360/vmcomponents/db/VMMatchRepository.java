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
