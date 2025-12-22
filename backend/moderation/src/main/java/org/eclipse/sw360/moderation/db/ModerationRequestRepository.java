/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.components.summary.ModerationRequestSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostFindOptions;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import com.ibm.cloud.cloudant.v1.model.ViewResult;
import com.ibm.cloud.cloudant.v1.model.ViewResultRow;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.and;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.elemMatch;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.eq;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.exists;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.or;

/**
 * CRUD access for the ModerationRequest class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
@Component
public class ModerationRequestRepository extends SummaryAwareRepository<ModerationRequest> {
    private static final String ALL = "function(doc) { if (doc.type == 'moderation') emit(null, doc._id) }";
    private static final String REQUESTING_USERS_VIEW = "function(doc) { " +
            "  if (doc.type == 'moderation') {" +
            "    emit(doc.requestingUserDepartment, null);" +
            "  }" +
            "}";
    private static final String COUNTBYMODERATIONSTATE = "function(doc) {" +
            "    if (doc.type == 'moderation') {" +
            "        var moderationState = doc.moderationState;" +
            "        for(var i in doc.moderators)" +
            "        {" +
            "          if (moderationState === \"INPROGRESS\" || moderationState === \"PENDING\") {" +
            "            emit([doc.moderators[i], \"OPEN\"], null);" +
            "          } else {" +
            "            emit([doc.moderators[i], \"CLOSED\"], null);" +
            "          }" +
            "        }" +
            "    }" +
            "}";

    private static final String COUNTBYREQUESTER = "function(doc) {" +
            "    if (doc.type == 'moderation') {" +
            "        emit([doc.requestingUser], null);" +
            "    }" +
            "}";

    private static final String MR_BY_MODERATORS_IDX = "MrByModeratorsIdx";
    private static final String MR_BY_DATE_IDX = "MrByDateIdx";
    private static final String MR_BY_COMPONENT_TYPE_IDX = "MrByComponentTypeIdx";
    private static final String MR_BY_DOCUMENT_NAME_IDX = "MrByDocumentNameIdx";
    private static final String MR_BY_USERS_IDX = "MrByUsersIdx";
    private static final String MR_BY_DEPARTMENT_IDX = "MrByDepartmentIdx";
    private static final String MR_BY_MODERATION_STATE_IDX = "MrByModerationStateIdx";
    private static final String MR_BY_DOCUMENT_ID_IDX = "MrByDocumentIdIdx";

    @Autowired
    public ModerationRequestRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(ModerationRequest.class, db, new ModerationRequestSummary());
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byRequestingUsersDeptView", createMapReduce(REQUESTING_USERS_VIEW, null));
        views.put("countByModerationState", createMapReduce(COUNTBYMODERATIONSTATE, "_count"));
        views.put("countByRequester", createMapReduce(COUNTBYREQUESTER, "_count"));
        initStandardDesignDocument(views, db);
        createIndex(MR_BY_MODERATORS_IDX, "byModerators", new String[] {"moderators"}, db);
        createIndex(MR_BY_DATE_IDX, "byDate", new String[] {"timestamp"}, db);
        createIndex(MR_BY_COMPONENT_TYPE_IDX, "byComponentType", new String[] {"componentType"}, db);
        createIndex(MR_BY_DOCUMENT_NAME_IDX, "byDocumentName", new String[] {"documentName"}, db);
        createIndex(MR_BY_USERS_IDX, "byUsers", new String[] {"requestingUser"}, db);
        createIndex(MR_BY_DEPARTMENT_IDX, "byDepartment", new String[] {"requestingUserDepartment"}, db);
        createIndex(MR_BY_MODERATION_STATE_IDX, "byModerationState", new String[] {"moderationState"}, db);
        createIndex(MR_BY_DOCUMENT_ID_IDX, "byDocumentId", new String[] {"documentId"}, db);
    }

    public List<ModerationRequest> getRequestsByDocumentId(String documentId) {
        final Map<String, Object> typeSelector = eq("type", "moderation");
        final Map<String, Object> filterByModeratorSelector = eq("documentId", documentId);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, filterByModeratorSelector));
        PostFindOptions qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(MR_BY_DOCUMENT_ID_IDX))
                .build();
        List<ModerationRequest> mrs = getConnector().getQueryResult(qb, ModerationRequest.class);
        return mrs;
    }

    public List<ModerationRequest> getRequestsByModerator(String moderator) {
        final Map<String, Object> typeSelector = eq("type", "moderation");
        final Map<String, Object> filterByModeratorSelector = elemMatch("moderators", moderator);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, filterByModeratorSelector));
        PostFindOptions qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(MR_BY_MODERATORS_IDX))
                .build();
        List<ModerationRequest> mrs = getConnector().getQueryResult(qb, ModerationRequest.class);
        return makeSummaryFromFullDocs(SummaryType.SHORT, mrs);
    }

    public List<ModerationRequest> getRequestsByModeratorWithPaginationNoFilter(String moderator, PaginationData pageData) {
        final int rowsPerPage = pageData.getRowsPerPage();
        final boolean ascending = pageData.isAscending();
        final int skip = pageData.getDisplayStart();
        final Map<String, Object> typeSelector = eq("type", "moderation");
        final Map<String, Object> filterByModeratorSelector = elemMatch("moderators", moderator);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, filterByModeratorSelector));
        PostFindOptions qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .limit(rowsPerPage)
                .skip(skip)
                .useIndex(Collections.singletonList(MR_BY_DATE_IDX))
                .addSort(Collections.singletonMap("timestamp", ascending ? "asc" : "desc"))
                .build();
        return getConnector().getQueryResult(qb, ModerationRequest.class);
    }

    public Map<PaginationData, List<ModerationRequest>> getRequestsByModerator(String moderator, PaginationData pageData, boolean open) {
        Map<PaginationData, List<ModerationRequest>> paginatedModerations = queryViewWithPagination(moderator, pageData, open);
        List<ModerationRequest> moderationList = paginatedModerations.values().iterator().next();
        moderationList = makeSummaryFromFullDocs(SummaryType.SHORT, moderationList);
        paginatedModerations.put(pageData, moderationList);
        return paginatedModerations;
    }

    public Map<PaginationData, List<ModerationRequest>> getRequestsByModeratorAllDetails(String moderator, PaginationData pageData, boolean open) {
        Map<PaginationData, List<ModerationRequest>> paginatedModerations = queryViewWithPagination(moderator, pageData, open);
        List<ModerationRequest> moderationList = paginatedModerations.values().iterator().next();
        paginatedModerations.put(pageData, moderationList);
        return paginatedModerations;
    }

    private Map<PaginationData, List<ModerationRequest>> queryViewWithPagination(String moderator,
            PaginationData pageData, boolean open) {
        final int rowsPerPage = pageData.getRowsPerPage();
        Map<PaginationData, List<ModerationRequest>> result = Maps.newHashMap();
        List<ModerationRequest> modReqs = Lists.newArrayList();
        final boolean ascending = pageData.isAscending();
        final int sortColumnNo = pageData.getSortColumnNumber();
        PostFindOptions query = null;
        final Map<String, Object> typeSelector = eq("type", "moderation");
        final Map<String, Object> openModerationState = or(List.of(eq("moderationState", "PENDING"), eq("moderationState", "INPROGRESS")));
        final Map<String, Object> closedModerationState = or(List.of(eq("moderationState", "APPROVED"), eq("moderationState", "REJECTED")));
        final Map<String, Object> filterByModeratorSelector = elemMatch("moderators", moderator);
        final Map<String, Object> emptyComponentTypeSelector = or(List.of(exists("componentType", false),
                eq("componentType", "")));
        final Map<String, Object> commonSelector = and(List.of(typeSelector, open ? openModerationState : closedModerationState,
                filterByModeratorSelector));
        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(commonSelector);
        if(rowsPerPage != -1) {
            qb.limit(rowsPerPage);
        }
        qb.skip(pageData.getDisplayStart());
        switch (sortColumnNo) {
            case -1, 5:
                qb.useIndex(Collections.singletonList(MR_BY_MODERATORS_IDX))
                        .addSort(Collections.singletonMap("moderators", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 0:
                qb.useIndex(Collections.singletonList(MR_BY_DATE_IDX))
                        .addSort(Collections.singletonMap("timestamp", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 1:
                qb.useIndex(Collections.singletonList(MR_BY_COMPONENT_TYPE_IDX))
                        .addSort(Collections.singletonMap("componentType", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 2:
                qb.useIndex(Collections.singletonList(MR_BY_DOCUMENT_NAME_IDX))
                        .addSort(Collections.singletonMap("documentName", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 3:
                qb.useIndex(Collections.singletonList(MR_BY_USERS_IDX))
                        .addSort(Collections.singletonMap("requestingUser", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 4:
                qb.useIndex(Collections.singletonList(MR_BY_DEPARTMENT_IDX))
                        .addSort(Collections.singletonMap("requestingUserDepartment", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 6:
                qb.useIndex(Collections.singletonList(MR_BY_MODERATION_STATE_IDX))
                        .addSort(Collections.singletonMap("moderationState", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            default:
                break;
        }
        try {
            modReqs = getConnector().getQueryResult(query, ModerationRequest.class);
            if (1 == sortColumnNo) {
                final Map<String, Object> selectorCompType = and(List.of(typeSelector, open ? openModerationState : closedModerationState,
                        filterByModeratorSelector, emptyComponentTypeSelector));
                PostFindOptions.Builder emptyCTypeQb = getConnector().getQueryBuilder()
                        .selector(selectorCompType)
                        .limit(rowsPerPage)
                        .skip(pageData.getDisplayStart());
                List<ModerationRequest> mods = getConnector()
                        .getQueryResult(emptyCTypeQb.build(), ModerationRequest.class);
                if (ascending) {
                    mods.addAll(modReqs);
                    modReqs = mods;
                } else {
                    modReqs.addAll(mods);
                }
            }
        } catch (Exception e) {
            log.error("Error getting moderation requests", e);
        }
        result.put(pageData, modReqs);
        return result;
    }

    private List<String[]> prepareKeys(String moderator, boolean ascending) {
        String[] startKey, endKey;
        if (ascending) {
            startKey = new String[] { moderator };
            endKey = new String[] { moderator, "\ufff0" };
        } else {
            startKey = new String[] { moderator, "\ufff0" };
            endKey = new String[] { moderator };
        }
        return List.of(startKey, endKey);
    }

    public List<ModerationRequest> getRequestsByRequestingUser(String user) {
        final Map<String, Object> typeSelector = eq("type", "moderation");
        final Map<String, Object> filterByModeratorSelector = eq("requestingUser", user);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, filterByModeratorSelector));
        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(MR_BY_USERS_IDX));
        List<ModerationRequest> mrs = getConnector().getQueryResult(qb.build(), ModerationRequest.class);
        return makeSummaryFromFullDocs(SummaryType.SHORT, mrs);
    }

    public List<ModerationRequest> getRequestsByRequestingUserWithPagination(String user, PaginationData pageData) {
        final int rowsPerPage = pageData.getRowsPerPage();
        final boolean ascending = pageData.isAscending();
        final int skip = pageData.getDisplayStart();
        final Map<String, Object> typeSelector = eq("type", "moderation");
        final Map<String, Object> filterByModeratorSelector = eq("requestingUser", user);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, filterByModeratorSelector));
        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .limit(rowsPerPage)
                .skip(skip)
                .useIndex(Collections.singletonList(MR_BY_USERS_IDX))
                .addSort(Collections.singletonMap("timestamp", ascending ? "asc" : "desc"));

        List<ModerationRequest> modReqs = Lists.newArrayList();
        try {
            modReqs = getConnector().getQueryResult(qb.build(), ModerationRequest.class);
        } catch (Exception e) {
            log.error("Error getting moderation requests", e);
        }
        return modReqs;
    }

    public Map<String, Long> getCountByModerationState(String moderator) {
        Map<String, Long> countByModerationState = Maps.newHashMap();
        List<String[]> keys = prepareKeys(moderator, true);
        PostViewOptions countReq = getConnector()
                .getPostViewQueryBuilder(ModerationRequest.class, "countByModerationState")
                .startKey(keys.get(0))
                .endKey(keys.get(1))
                .group(true)
                .groupLevel(2)
                .descending(false)
                .reduce(true).build();
        try {
            ViewResult response = getConnector().getPostViewQueryResponse(countReq);
            if (null != response) {
                countByModerationState = response.getRows().stream().collect(Collectors.toMap(key -> {
                    String json = key.getKey().toString();
                    String replace = json.replace("[", "").replace("]", "").replaceAll("\"", "");
                    List<String> moderatorToModStatus = new ArrayList<>(Arrays.asList(replace.split(",")));
                    return moderatorToModStatus.get(1);
                }, val -> Long.parseLong(val.getValue().toString())));
            }
        } catch (ServiceResponseException e) {
            log.error("Error getting count of moderation requests based on moderation state", e);
        }
        return countByModerationState;
    }

    public Map<String, Long> getCountByRequester(String user) {
        Map<String, Long> countByModerationState = Maps.newHashMap();

        List<String[]> keys = prepareKeys(user, true);
        PostViewOptions countReq = getConnector()
                .getPostViewQueryBuilder(ModerationRequest.class, "countByRequester")
                .startKey(keys.get(0)).endKey(keys.get(1))
                .descending(false).group(true).groupLevel(2).reduce(true).build();
        try {
            ViewResult response = getConnector().getPostViewQueryResponse(countReq);
            if (null != response) {
                countByModerationState = response.getRows().stream().collect(Collectors.toMap(key -> {
                    String json = key.getKey().toString();
                    return json.replaceAll("[\\[\\]\"]", "");
                }, val -> Long.parseLong(val.getValue().toString())));
            }
        } catch (ServiceResponseException e) {
            log.error("Error getting count of moderation requests based on moderation state", e);
        }
        return countByModerationState;
    }

    public Set<String> getRequestingUserDepts() {
        Set<String> requestingUserDepts = Sets.newHashSet();
        PostViewOptions query = getConnector()
                .getPostViewQueryBuilder(ModerationRequest.class, "byRequestingUsersDeptView")
                .includeDocs(false).build();
        try {
            requestingUserDepts = getConnector().getPostViewQueryResponse(query).getRows()
                    .stream()
                    .map(ViewResultRow::getKey)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toCollection(Sets::newTreeSet));
        } catch (ServiceResponseException e) {
            log.error("Error getting requesting users", e);
        }
        return requestingUserDepts;
    }
}
