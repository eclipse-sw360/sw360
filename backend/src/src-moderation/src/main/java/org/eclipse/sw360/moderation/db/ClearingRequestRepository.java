/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.ektorp.support.View;

/**
 * CRUD access for the ClearingRequest class
 *
 * @author abdul.mannankapti@siemens.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'clearingRequest') emit(null, doc._id) }")
public class ClearingRequestRepository extends DatabaseRepository<ClearingRequest> {

    private static final String BY_PROJECT_ID = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.projectId, doc);" +
            "    }" +
            "}";

    private static final String MY_CLEARING_REQUESTS = "function(doc) { " +
            "    if (doc.type == 'clearingRequest') {" +
            "        var acc = {};" +
            "        if (doc.requestingUser) {" +
            "            acc[doc.requestingUser] = 1;" +
            "        }" +
            "        if (doc.clearingTeam) {" +
            "            acc[doc.clearingTeam] = 1 ;" +
            "        }" +
            "        for (var i in acc) {" +
            "            emit(i, doc);" +
            "        }" +
            "    }" +
            "}";

    private static final String BY_BUSINESS_UNIT = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.projectBU, doc);" +
            "    }" +
            "}";

    public ClearingRequestRepository(DatabaseConnector db) {
        super(ClearingRequest.class, db);

        initStandardDesignDocument();
    }

    @View(name = "byProjectId", map = BY_PROJECT_ID)
    public ClearingRequest getClearingRequestByProjectId(String projectId) {
        List<ClearingRequest> requests = queryView("byProjectId", projectId);
        if (CommonUtils.isNotEmpty(requests)) {
            ClearingRequest request = requests.stream()
                    .findFirst().orElse(null);
            return request;
        }
        return null;
    }

    @View(name = "myClearingRequests", map = MY_CLEARING_REQUESTS)
    public Set<ClearingRequest> getMyClearingRequests(String user) {
        return new HashSet<ClearingRequest>(queryView("myClearingRequests", user));
    }

    @View(name = "byBusinessUnit", map = BY_BUSINESS_UNIT)
    public Set<ClearingRequest> getClearingRequestsByBU(String businessUnit) {
        return new HashSet<ClearingRequest>(queryView("byBusinessUnit", businessUnit));
    }
}
