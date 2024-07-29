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

import static com.cloudant.client.api.query.Expression.eq;
import static com.cloudant.client.api.query.Operation.and;
import static com.cloudant.client.api.query.Operation.or;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.components.summary.ModerationRequestSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.cloudant.client.api.query.Expression;
import com.cloudant.client.api.query.PredicateExpression;
import com.cloudant.client.api.query.PredicatedOperation;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.query.Selector;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.Key.ComplexKey;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * CRUD access for the ModerationRequest class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
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

    public ModerationRequestRepository(DatabaseConnectorCloudant db) {
        super(ModerationRequest.class, db, new ModerationRequestSummary());
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byRequestingUsersDeptView", createMapReduce(REQUESTING_USERS_VIEW, null));
        views.put("countByModerationState", createMapReduce(COUNTBYMODERATIONSTATE, "_count"));
        views.put("countByRequester", createMapReduce(COUNTBYREQUESTER, "_count"));
        initStandardDesignDocument(views, db);
        createIndex("byModerators", new String[] {"moderators"}, db);
        createIndex("byDate", new String[] {"timestamp"}, db);
        createIndex("byComponentType", new String[] {"componentType"}, db);
        createIndex("byDocumentName", new String[] {"documentName"}, db);
        createIndex("byUsers", new String[] {"requestingUser"}, db);
        createIndex("byDepartment", new String[] {"requestingUserDepartment"}, db);
        createIndex("byModerationState", new String[] {"moderationState"}, db);
        createIndex("byId", new String[] {"_id"}, db);
        createIndex("byDocumentId", new String[] {"documentId"}, db);
    }

    public List<ModerationRequest> getRequestsByDocumentId(String documentId) {
        final Selector typeSelector = eq("type", "moderation");
        final Selector filterByModeratorSelector = eq("documentId", documentId);
        final Selector finalSelector = and(typeSelector, filterByModeratorSelector);
        QueryBuilder qb = new QueryBuilder(finalSelector);
        qb.useIndex("byDocumentId");
        List<ModerationRequest> mrs = getConnector().getQueryResult(qb.build(), ModerationRequest.class).getDocs();
        return mrs;
    }

    public List<ModerationRequest> getRequestsByModerator(String moderator) {
        final Selector typeSelector = eq("type", "moderation");
        final Selector filterByModeratorSelector = PredicatedOperation.elemMatch("moderators",
                PredicateExpression.eq(moderator));
        final Selector finalSelector = and(typeSelector, filterByModeratorSelector);
        QueryBuilder qb = new QueryBuilder(finalSelector);
        qb.useIndex("byModerators");
        List<ModerationRequest> mrs = getConnector().getQueryResult(qb.build(), ModerationRequest.class).getDocs();
        return makeSummaryFromFullDocs(SummaryType.SHORT, mrs);
    }

    public List<ModerationRequest> getRequestsByModeratorWithPaginationNoFilter(String moderator, PaginationData pageData) {
        final int rowsPerPage = pageData.getRowsPerPage();
        final boolean ascending = pageData.isAscending();
        final int skip = pageData.getDisplayStart();
        final Selector typeSelector = eq("type", "moderation");
        final Selector filterByModeratorSelector = PredicatedOperation.elemMatch("moderators",
                PredicateExpression.eq(moderator));
        final Selector finalSelector = and(typeSelector, filterByModeratorSelector);
        QueryBuilder qb = new QueryBuilder(finalSelector);
        qb.limit(rowsPerPage);
        qb.skip(skip);
        qb.useIndex("byDate");
        qb = ascending ? qb.sort(Sort.asc("timestamp")) : qb.sort(Sort.desc("timestamp"));
        return getConnector().getQueryResult(qb.build(), ModerationRequest.class).getDocs();
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
        String query = null;
        final Selector typeSelector = eq("type", "moderation");
        final Selector openModerationState = or(eq("moderationState", "PENDING"), eq("moderationState", "INPROGRESS"));
        final Selector closedModerationState = or(eq("moderationState", "APPROVED"), eq("moderationState", "REJECTED"));
        final Selector filterByModeratorSelector = PredicatedOperation.elemMatch("moderators",
                PredicateExpression.eq(moderator));
        final Selector emptyComponentTypeSelector = or(Expression.exists("componentType", false),
                eq("componentType", ""));
        final Selector commonSelector = and(typeSelector, open ? openModerationState : closedModerationState,
                filterByModeratorSelector);
        QueryBuilder qb = new QueryBuilder(commonSelector);
        if(rowsPerPage != -1) {
            qb.limit(rowsPerPage);
        }
        qb.skip(pageData.getDisplayStart());
        switch (sortColumnNo) {
        case -1:
            qb = qb.useIndex("byModerators");
            qb = ascending ? qb.sort(Sort.asc("moderators")) : qb.sort(Sort.desc("moderators"));
            query = qb.build();
            break;
        case 0:
            qb = qb.useIndex("byDate");
            qb = ascending ? qb.sort(Sort.asc("timestamp")) : qb.sort(Sort.desc("timestamp"));
            query = qb.build();
            break;
        case 1:
            qb = qb.useIndex("byComponentType");
            qb = ascending ? qb.sort(Sort.asc("componentType")) : qb.sort(Sort.desc("componentType"));
            query = qb.build();
            break;
        case 2:
            qb = qb.useIndex("byDocumentName");
            qb = ascending ? qb.sort(Sort.asc("documentName")) : qb.sort(Sort.desc("documentName"));
            query = qb.build();
            break;
        case 3:
            qb = qb.useIndex("byUsers");
            qb = ascending ? qb.sort(Sort.asc("requestingUser")) : qb.sort(Sort.desc("requestingUser"));
            query = qb.build();
            break;
        case 4:
            qb = qb.useIndex("byDepartment");
            qb = ascending ? qb.sort(Sort.asc("requestingUserDepartment"))
                    : qb.sort(Sort.desc("requestingUserDepartment"));
            query = qb.build();
            break;
        case 5:
            qb = qb.useIndex("byModerators");
            qb = ascending ? qb.sort(Sort.asc("moderators")) : qb.sort(Sort.desc("moderators"));
            query = qb.build();
            break;
        case 6:
            qb = qb.useIndex("byModerationState");
            qb = ascending ? qb.sort(Sort.asc("moderationState")) : qb.sort(Sort.desc("moderationState"));
            query = qb.build();
            break;
        default:
            break;
        }
        try {
            QueryResult<ModerationRequest> queryResult = getConnector().getQueryResult(query, ModerationRequest.class);
            modReqs = queryResult.getDocs();
            if (1 == sortColumnNo) {
                final Selector selectorCompType = and(typeSelector, open ? openModerationState : closedModerationState,
                        filterByModeratorSelector, emptyComponentTypeSelector);
                QueryBuilder emptyCTypeQb = new QueryBuilder(selectorCompType);
                emptyCTypeQb.limit(rowsPerPage);
                emptyCTypeQb.skip(pageData.getDisplayStart());
                QueryResult<ModerationRequest> queryResultWithoutSorting = getConnector()
                        .getQueryResult(emptyCTypeQb.build(), ModerationRequest.class);
                List<ModerationRequest> mods = queryResultWithoutSorting.getDocs();
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

    private List<ComplexKey> prepareKeys(String moderator, boolean ascending) {
        ComplexKey startKey, endKey;
        if (ascending) {
            startKey = Key.complex(new String[] { moderator });
            endKey = Key.complex(new String[] { moderator, "\ufff0" });
        } else {
            startKey = Key.complex(new String[] { moderator, "\ufff0" });
            endKey = Key.complex(new String[] { moderator });
        }
        return List.of(startKey, endKey);
    }

    public List<ModerationRequest> getRequestsByRequestingUser(String user) {
        final Selector typeSelector = eq("type", "moderation");
        final Selector filterByModeratorSelector = eq("requestingUser", user);
        final Selector finalSelector = and(typeSelector, filterByModeratorSelector);
        QueryBuilder qb = new QueryBuilder(finalSelector);
        qb.useIndex("byUsers");
        List<ModerationRequest> mrs = getConnector().getQueryResult(qb.build(), ModerationRequest.class).getDocs();
        return makeSummaryFromFullDocs(SummaryType.SHORT, mrs);
    }

    public List<ModerationRequest> getRequestsByRequestingUserWithPagination(String user, PaginationData pageData) {
        final int rowsPerPage = pageData.getRowsPerPage();
        final boolean ascending = pageData.isAscending();
        final int skip = pageData.getDisplayStart();
        final Selector typeSelector = eq("type", "moderation");
        final Selector filterByModeratorSelector = eq("requestingUser", user);
        final Selector finalSelector = and(typeSelector, filterByModeratorSelector);
        QueryBuilder qb = new QueryBuilder(finalSelector);
        qb.limit(rowsPerPage);
        qb.skip(skip);
        qb.useIndex("byUsers");
        qb = ascending ? qb.sort(Sort.asc("timestamp")) : qb.sort(Sort.desc("timestamp"));

        List<ModerationRequest> modReqs = Lists.newArrayList();
        try {
            QueryResult<ModerationRequest> queryResult = getConnector().getQueryResult(qb.build(), ModerationRequest.class);
            modReqs = queryResult.getDocs();
        } catch (Exception e) {
            log.error("Error getting moderation requests", e);
        }
        return modReqs;
    }

    public Map<String, Long> getCountByModerationState(String moderator) {
        Map<String, Long> countByModerationState = Maps.newHashMap();
        List<ComplexKey> keys = prepareKeys(moderator, true);
        ViewRequest<ComplexKey, Long> countReq = getConnector()
                .createQuery(ModerationRequest.class, "countByModerationState").newRequest(Key.Type.COMPLEX, Long.class)
                .startKey(keys.get(0)).endKey(keys.get(1)).group(true).groupLevel(2).reduce(true).build();
        try {
            ViewResponse<ComplexKey, Long> response = countReq.getResponse();
            if (null != response) {
                countByModerationState = response.getRows().stream().collect(Collectors.toMap(key -> {
                    String json = key.getKey().toJson();
                    String replace = json.replace("[", "").replace("]", "").replaceAll("\"", "");
                    List<String> moderatorToModStatus = new ArrayList<String>(Arrays.asList(replace.split(",")));
                    return moderatorToModStatus.get(1);
                }, val -> val.getValue()));
            }
        } catch (IOException e) {
            log.error("Error getting count of moderation requests based on moderation state", e);
        }
        return countByModerationState;
    }

    public Map<String, Long> getCountByRequester(String user) {
        Map<String, Long> countByModerationState = Maps.newHashMap();

        List<ComplexKey> keys = prepareKeys(user, true);
        ViewRequest<ComplexKey, Long> countReq = getConnector()
                .createQuery(ModerationRequest.class, "countByRequester").newRequest(Key.Type.COMPLEX, Long.class)
                .startKey(keys.get(0)).endKey(keys.get(1)).group(true).groupLevel(2).reduce(true).build();
        try {
            ViewResponse<ComplexKey, Long> response = countReq.getResponse();
            if (null != response) {
                countByModerationState = response.getRows().stream().collect(Collectors.toMap(key -> {
                    String json = key.getKey().toJson();
                    return json.replaceAll("[\\[\\]\"]", "");
                }, ViewResponse.Row::getValue));
            }
        } catch (IOException e) {
            log.error("Error getting count of moderation requests based on moderation state", e);
        }
        return countByModerationState;
    }

    public Set<String> getRequestingUserDepts() {
        Set<String> requestingUserDepts = Sets.newHashSet();
        ViewRequest<String, Object> query = getConnector()
                .createQuery(ModerationRequest.class, "byRequestingUsersDeptView")
                .newRequest(Key.Type.STRING, Object.class).includeDocs(false).build();
        try {
            requestingUserDepts = Sets.newTreeSet(CommonUtils.nullToEmptyList(query.getResponse().getKeys()).stream()
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        } catch (IOException e) {
            log.error("Error getting requesting users", e);
        }
        return requestingUserDepts;
    }
}
