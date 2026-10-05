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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
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
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.gte;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.lte;

/**
 * CRUD access for the ClearingRequest class
 *
 * @author abdul.mannankapti@siemens.com
 */
public class ClearingRequestRepository extends DatabaseRepositoryCloudantClient<ClearingRequest> {
    private static final Logger log = LogManager.getLogger(ClearingRequestRepository.class);

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
                ClearingRequest._Fields.TIMESTAMP.getFieldName(),
                ClearingRequest._Fields.PRIORITY.getFieldName(),
                ClearingRequest._Fields.CLEARING_TYPE.getFieldName(),
                ClearingRequest._Fields.REQUESTED_CLEARING_DATE.getFieldName(),
                ClearingRequest._Fields.AGREED_CLEARING_DATE.getFieldName(),
                ClearingRequest._Fields.MODIFIED_ON.getFieldName(),
                ClearingRequest._Fields.TIMESTAMP_OF_DECISION.getFieldName()
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
     * @param businessUnits Business units the user belongs to (primary and secondary)
     * @param pageData Pagination data
     * @return Map containing pagination data as key and list of clearing requests as value
     */
    public Map<PaginationData, List<ClearingRequest>> getRecentClearingRequestsWithPagination(
            String user, Set<String> businessUnits, PaginationData pageData) {
        Map<PaginationData, List<ClearingRequest>> result = Maps.newHashMap();

        final Map<String, Object> typeSelector = eq("type", "clearingRequest");
        final Map<String, Object> userOrBuSelector = getVisibilitySelector(user, businessUnits);
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
     * Builds the mandatory visibility clause: a clearing request is visible when the user
     * raised it, is its clearing team, or when it belongs to one of the user's business
     * units. Secondary departments are included so that filtering by a secondary BA-BL
     * returns results, matching the behaviour of the legacy clearing request page.
     * @param user User email
     * @param businessUnits Business units the user belongs to (primary and secondary)
     * @return Visibility selector
     */
    private static Map<String, Object> getVisibilitySelector(String user, Set<String> businessUnits) {
        List<Map<String, Object>> visibleConditions = new ArrayList<>();
        visibleConditions.add(eq(ClearingRequest._Fields.REQUESTING_USER.getFieldName(), user));
        visibleConditions.add(eq(ClearingRequest._Fields.CLEARING_TEAM.getFieldName(), user));
        CommonUtils.nullToEmptySet(businessUnits).stream()
                .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                .distinct()
                .forEach(bu -> visibleConditions.add(
                        eq(ClearingRequest._Fields.PROJECT_BU.getFieldName(), bu)));
        return or(visibleConditions);
    }

    /**
     * Search clearing requests by filters with pagination filtered by user access
     * @param user User email
     * @param businessUnits Business units the user belongs to (primary and secondary)
     * @param filterMap Map of field names to sets of values to match against
     * @param pageData Pagination data
     * @return Map containing pagination data as key and list of clearing requests as value
     */
    public Map<PaginationData, List<ClearingRequest>> searchClearingRequestsByFilters(
            String user, Set<String> businessUnits, Map<String, Set<String>> filterMap, PaginationData pageData) {

        final Map<String, Object> typeSelector = eq("type", "clearingRequest");
        final Map<String, Object> userOrBuSelector = getVisibilitySelector(user, businessUnits);
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

        // Date-range filters (e.g. "Last 7 days") are signalled via sentinel keys instead of
        // the standard field name, since the generic filterMap only supports eq/or matching.
        String dateField = getSingleValue(subQueryRestrictions, SW360Constants.CLEARING_REQUEST_DATE_FIELD_KEY);
        if (dateField != null) {
            String from = getSingleValue(subQueryRestrictions, SW360Constants.CLEARING_REQUEST_DATE_FROM_KEY);
            String to = getSingleValue(subQueryRestrictions, SW360Constants.CLEARING_REQUEST_DATE_TO_KEY);
            addDateRangeConditions(andConditions, dateField, from, to);
        }

        for (Map.Entry<String, Set<String>> entry : subQueryRestrictions.entrySet()) {
            String field = entry.getKey();
            Set<String> values = entry.getValue();

            if (SW360Constants.CLEARING_REQUEST_DATE_FIELD_KEY.equals(field)
                    || SW360Constants.CLEARING_REQUEST_DATE_FROM_KEY.equals(field)
                    || SW360Constants.CLEARING_REQUEST_DATE_TO_KEY.equals(field)) {
                continue;
            }

            if (values == null || values.isEmpty()) {
                continue;
            }

            List<String> nonEmptyValues = values.stream()
                    .filter(v -> v != null && !v.isEmpty())
                    .toList();

            if (nonEmptyValues.isEmpty()) {
                continue;
            }

            if (nonEmptyValues.size() == 1) {
                andConditions.add(eq(field, nonEmptyValues.get(0)));
            } else {
                andConditions.add(
                        or(nonEmptyValues.stream()
                                .map(v -> eq(field, v))
                                .toList())
                );
            }
        }

        return and(andConditions);
    }

    /**
     * Appends $gte / $lte selectors for the given ClearingRequest date field, choosing the
     * numeric or the lexicographic comparison depending on how the field is persisted.
     * @param andConditions Condition list to append to
     * @param dateField Name of the ClearingRequest field to filter on
     * @param from Inclusive lower bound, or {@code null} for an open lower bound
     * @param to Inclusive upper bound, or {@code null} for an open upper bound
     * @throws IllegalArgumentException if the field is not a supported date field or a bound
     *         cannot be parsed. Failing loudly is intentional: silently dropping the range
     *         would return documents outside the caller's requested window.
     */
    private void addDateRangeConditions(List<Map<String, Object>> andConditions, String dateField,
                                        String from, String to) {
        if (from == null && to == null) {
            return;
        }
        if (SW360Constants.CLEARING_REQUEST_EPOCH_DATE_FIELDS.contains(dateField)) {
            if (from != null) {
                andConditions.add(gte(dateField, parseEpochBound(from, dateField)));
            }
            if (to != null) {
                andConditions.add(lte(dateField, parseEpochBound(to, dateField)));
            }
        } else if (SW360Constants.CLEARING_REQUEST_ISO_DATE_FIELDS.contains(dateField)) {
            if (from != null) {
                andConditions.add(gte(dateField, from));
            }
            if (to != null) {
                andConditions.add(lte(dateField, to));
            }
        } else {
            log.error("Unsupported clearing request date range field: {}", dateField);
            throw new IllegalArgumentException(
                    "Unsupported clearing request date range field: " + dateField);
        }
    }

    private static long parseEpochBound(String value, String dateField) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Invalid epoch millis bound '{}' for clearing request date field: {}", value, dateField, e);
            throw new IllegalArgumentException(
                    "Invalid epoch millis bound '" + value + "' for clearing request date field: " + dateField, e);
        }
    }

    private static String getSingleValue(Map<String, Set<String>> filterMap, String key) {
        Set<String> values = filterMap.get(key);
        if (values == null) {
            return null;
        }
        return values.stream()
                .filter(v -> v != null && !v.isEmpty())
                .findFirst().orElse(null);
    }
}
