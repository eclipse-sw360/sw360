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
import java.util.Collections;
import java.util.ArrayList;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestPriority;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;

import com.google.common.collect.Maps;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostFindOptions;

import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.eq;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.and;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.or;

/**
 * CRUD access for the ClearingRequest class
 *
 * @author abdul.mannankapti@siemens.com
 */
public class ClearingRequestRepository extends DatabaseRepositoryCloudantClient<ClearingRequest> {
    private static final String ALL = "function(doc) { if (doc.type == 'clearingRequest') emit(null, doc._id) }";

    private static final String BY_PROJECT_ID = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.projectId, null);" +
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
            "            emit(i, null);" +
            "        }" +
            "    }" +
            "}";

    private static final String BY_BUSINESS_UNIT = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.projectBU, null);" +
            "    }" +
            "}";

    private static final String BY_PRIORITY = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.priority, null);" +
            "    }" +
            "}";

    private static final String BY_CREATED_ON = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.timestamp, null);" +
            "    }" +
            "}";

    private static final String BY_STATUS = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.clearingState, null);" +
            "    }" +
            "}";

    private static final String BY_REQUESTING_USER = "function(doc) { " +
            "  if (doc.type == 'clearingRequest') {" +
            "    emit(doc.requestingUser, null);" +
            "    }" +
            "}";

    private static final String CLEARING_REQUEST_BY_ALL_IDX = "ClearingRequestByAllIdx";

    public ClearingRequestRepository(DatabaseConnectorCloudant db) {
        super(db, ClearingRequest.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byProjectId", createMapReduce(BY_PROJECT_ID, null));
        views.put("myClearingRequests", createMapReduce(MY_CLEARING_REQUESTS, null));
        views.put("byBusinessUnit", createMapReduce(BY_BUSINESS_UNIT, null));
        views.put("byPriority", createMapReduce(BY_PRIORITY, null));
        views.put("byCreatedOn", createMapReduce(BY_CREATED_ON, null));
        views.put("byStatus", createMapReduce(BY_STATUS, null));
        views.put("byRequestingUser", createMapReduce(BY_REQUESTING_USER, null));
        initStandardDesignDocument(views, db);

        createIndex(CLEARING_REQUEST_BY_ALL_IDX, "clearingReqByAll", new String[] {
                ClearingRequest._Fields.PROJECT_ID.getFieldName(),
                ClearingRequest._Fields.REQUESTING_USER.getFieldName(),
                ClearingRequest._Fields.CLEARING_TEAM.getFieldName(),
                ClearingRequest._Fields.PROJECT_BU.getFieldName(),
                ClearingRequest._Fields.CLEARING_STATE.getFieldName(),
                ClearingRequest._Fields.TIMESTAMP.getFieldName()
        }, db);
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

    public Integer getOpenCriticalClearingRequestCount(String group) {
        Set<ClearingRequest> criticalCr = new HashSet<ClearingRequest>(queryView("byPriority", ClearingRequestPriority.CRITICAL.name()));
        // filter the CLOSED / REJECTED and CR belong to same group as user
        return (int) CommonUtils.nullToEmptySet(criticalCr).stream()
                .filter(cr -> !(ClearingRequestState.CLOSED.equals(cr.getClearingState()) || ClearingRequestState.REJECTED.equals(cr.getClearingState()))
                        && cr.getProjectBU().trim().toUpperCase().startsWith(group.trim().toUpperCase()))
                .distinct().count();
    }

    /**
     * Get recent clearing requests with pagination filtered by user access
     * @param user User email
     * @param businessUnit User's business unit
     * @param pageData Pagination data
     * @return Map containing pagination data as key and list of clearing requests as value
     */
    public Map<PaginationData, List<ClearingRequest>> getRecentClearingRequestsWithPagination(
            String user, String businessUnit, PaginationData pageData) {
        Map<PaginationData, List<ClearingRequest>> result = Maps.newHashMap();
        
        final Map<String, Object> typeSelector = eq("type", "clearingRequest");
        final Map<String, Object> userOrBuSelector = or(List.of(
                eq(ClearingRequest._Fields.REQUESTING_USER.getFieldName(), user),
                eq(ClearingRequest._Fields.CLEARING_TEAM.getFieldName(), user),
                eq(ClearingRequest._Fields.PROJECT_BU.getFieldName(), businessUnit)
        ));
        final Map<String, Object> finalSelector = and(List.of(typeSelector, userOrBuSelector));
        
        final Map<String, String> sortSelector = Collections.singletonMap(
                ClearingRequest._Fields.TIMESTAMP.getFieldName(), 
                pageData.isAscending() ? "asc" : "desc"
        );
        
        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(CLEARING_REQUEST_BY_ALL_IDX));
        
        List<ClearingRequest> clearingRequests = getConnector().getQueryResultPaginated(
                qb, ClearingRequest.class, pageData, sortSelector
        );
        
        result.put(pageData, clearingRequests);
        return result;
    }

    /**
     * Search clearing requests by filters with pagination filtered by user access
     * @param user User email
     * @param businessUnit User's business unit
     * @param filterMap Map of field names to sets of values to match against
     * @param pageData Pagination data
     * @return Map containing pagination data as key and list of clearing requests as value
     */
    public Map<PaginationData, List<ClearingRequest>> searchClearingRequestsByFilters(
            String user, String businessUnit, Map<String, Set<String>> filterMap, PaginationData pageData) {
        
        final Map<String, Object> typeSelector = eq("type", "clearingRequest");
        final Map<String, Object> userOrBuSelector = or(List.of(
                eq(ClearingRequest._Fields.REQUESTING_USER.getFieldName(), user),
                eq(ClearingRequest._Fields.CLEARING_TEAM.getFieldName(), user),
                eq(ClearingRequest._Fields.PROJECT_BU.getFieldName(), businessUnit)
        ));
        final Map<String, Object> restrictionsSelector = getQueryFromRestrictions(filterMap);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, userOrBuSelector, restrictionsSelector));

        final Map<String, String> sortSelector = Collections.singletonMap(
                ClearingRequest._Fields.TIMESTAMP.getFieldName(), 
                pageData.isAscending() ? "asc" : "desc"
        );

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(CLEARING_REQUEST_BY_ALL_IDX));

        List<ClearingRequest> clearingRequests = getConnector().getQueryResultPaginated(
                qb, ClearingRequest.class, pageData, sortSelector
        );

        return Collections.singletonMap(pageData, clearingRequests);
    }

    private Map<String, Object> getQueryFromRestrictions(Map<String, Set<String>> subQueryRestrictions) {
        List<Map<String, Object>> andConditions = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : subQueryRestrictions.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                String fieldValue = entry.getValue().stream().findFirst().orElse("");
                if (!fieldValue.isEmpty()) {
                    andConditions.add(eq(entry.getKey(), fieldValue));
                }
            }
        }
        return and(andConditions);
    }
}
