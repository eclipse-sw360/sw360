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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;

import com.cloudant.client.api.model.DesignDocument.MapReduce;

/**
 * CRUD access for the ClearingRequest class
 *
 * @author abdul.mannankapti@siemens.com
 */
public class ClearingRequestRepository extends DatabaseRepositoryCloudantClient<ClearingRequest> {
    private static final String ALL = "function(doc) { if (doc.type == 'clearingRequest') emit(null, doc._id) }";

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

    public ClearingRequestRepository(DatabaseConnectorCloudant db) {
        super(db, ClearingRequest.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byProjectId", createMapReduce(BY_PROJECT_ID, null));
        views.put("myClearingRequests", createMapReduce(MY_CLEARING_REQUESTS, null));
        views.put("byBusinessUnit", createMapReduce(BY_BUSINESS_UNIT, null));
        initStandardDesignDocument(views, db);
    }

    public ClearingRequest getClearingRequestByProjectId(String projectId) {
        List<ClearingRequest> requests = queryView("byProjectId", projectId);
        if (CommonUtils.isNotEmpty(requests)) {
            ClearingRequest request = requests.stream()
                    .findFirst().orElse(null);
            return request;
        }
        return null;
    }

    public Set<ClearingRequest> getMyClearingRequests(String user) {
        return new HashSet<ClearingRequest>(queryView("myClearingRequests", user));
    }

    public Set<ClearingRequest> getClearingRequestsByBU(String businessUnit) {
        return new HashSet<ClearingRequest>(queryView("byBusinessUnit", businessUnit));
    }
}
