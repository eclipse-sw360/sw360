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

import org.eclipse.sw360.components.summary.ModerationRequestSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.client.api.views.ViewResponse.Row;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD access for the ModerationRequest class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class ModerationRequestRepository extends SummaryAwareRepository<ModerationRequest> {
    private static final String ALL = "function(doc) { if (doc.type == 'moderation') emit(null, doc._id) }";
    private static final String ALL_OPEN = "function(doc) { if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) emit(null, doc._id) }";
    private static final String ALL_CLOSED = "function(doc) { if (doc.type == 'moderation' && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) emit(null, doc._id) }";

    private static final String DOCUMENTS_VIEW = "function(doc) { " +
            "  if (doc.type == 'moderation') {" +
            "    emit(doc.documentId, doc._id);" +
            "  }" +
            "}";

    private static final String REQUESTING_USERS_VIEW = "function(doc) { " +
            "  if (doc.type == 'moderation') {" +
            "    emit(doc.requestingUserDepartment, null);" +
            "  }" +
            "}";

    private static final String USERS_VIEW = "function(doc) { " +
            "  if (doc.type == 'moderation') {" +
            "    emit(doc.requestingUser, doc);" +
            "    }" +
            "}";

    private static final String USERS_VIEW_OPEN = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) {" +
            "    emit(doc.requestingUser, doc);" +
            "    }" +
            "}";

    private static final String USERS_VIEW_CLOSED = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) {" +
            "    emit(doc.requestingUser, doc);" +
            "    }" +
            "}";

    private static final String MODERATORS_VIEW = "function(doc) {" +
            "  if (doc.type == 'moderation') {" +
            "    for(var i in doc.moderators) {" +
            "      emit(doc.moderators[i], doc);" +
            "    }" +
            "  }" +
            "}";

    private static final String MODERATORS_VIEW_FOR_SORTING_OPEN = "function(doc) {" +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) {" +
            "      if(doc.moderators) {" +
            "            emit(doc.moderators.join(), doc);" +
            "      } else {" +
            "            emit('', doc);" +
            "      }" +
            "  }" +
            "}";

    private static final String MODERATORS_VIEW_FOR_SORTING_CLOSED = "function(doc) {" +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) {" +
            "      if(doc.moderators) {" +
            "            emit(doc.moderators.join(), doc);" +
            "      } else {" +
            "            emit('', doc);" +
            "      }" +
            "  }" +
            "}";

    private static final String BYDATE_OPEN = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) {" +
            "    emit(new Date(doc.timestamp), doc);" +
            "    }" +
            "}";

    private static final String BYDATE_CLOSED = "function(doc) { " +
            "  if (doc.type == 'moderation'  && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) {" +
            "    emit(new Date(doc.timestamp), doc);" +
            "    }" +
            "}";

    private static final String BYDOCUMENTNAME_OPEN = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) {" +
            "    emit(doc.documentName, doc);" +
            "    }" +
            "}";

    private static final String BYDOCUMENTNAME_CLOSED = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) {" +
            "    emit(doc.documentName, doc);" +
            "    }" +
            "}";

    private static final String BYMODERATIONTATE_OPEN = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) {" +
            "    emit(doc.moderationState, doc);" +
            "    }" +
            "}";

    private static final String BYMODERATIONTATE_CLOSED = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) {" +
            "    emit(doc.moderationState, doc);" +
            "    }" +
            "}";

    private static final String BYCOMPONENTTYPE_OPEN = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) {" +
            "    emit(doc.componentType, doc);" +
            "    }" +
            "}";

    private static final String BYCOMPONENTTYPE_CLOSED = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) {" +
            "    emit(doc.componentType, doc);" +
            "    }" +
            "}";

    private static final String BYREQUESTINGUSERDEPRTMENT_OPEN = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'INPROGRESS' || doc.moderationState === 'PENDING')) {" +
            "    emit(doc.requestingUserDepartment, doc);" +
            "    }" +
            "}";

    private static final String BYREQUESTINGUSERDEPRTMENT_CLOSED = "function(doc) { " +
            "  if (doc.type == 'moderation' && (doc.moderationState === 'APPROVED' || doc.moderationState === 'REJECTED')) {" +
            "    emit(doc.requestingUserDepartment, doc);" +
            "    }" +
            "}";

    private static final String COUNTBYMODERATIONSTATE = "function(doc) {" +
            "  if (doc.type == 'moderation') {" +
            "    var moderationState = doc.moderationState;" +
            "    if(moderationState === \"INPROGRESS\" || moderationState === \"PENDING\") {" +
            "      emit(\"OPEN\", null);" +
            "    } else {" +
            "      emit(\"CLOSED\", null);" +
            "    }" +
            "  }" +
            "}";

    public ModerationRequestRepository(DatabaseConnectorCloudant db) {
        super(ModerationRequest.class, db, new ModerationRequestSummary());
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        views.put("allOpenRequests", createMapReduce(ALL_OPEN, null));
        views.put("allClosedRequests", createMapReduce(ALL_CLOSED, null));
        views.put("documents", createMapReduce(DOCUMENTS_VIEW, null));
        views.put("moderators", createMapReduce(MODERATORS_VIEW, null));
        views.put("users", createMapReduce(USERS_VIEW, null));
        views.put("openRequestsUsers", createMapReduce(USERS_VIEW_OPEN, null));
        views.put("closedRequestsUsers", createMapReduce(USERS_VIEW_CLOSED, null));
        views.put("openRequestsBydate", createMapReduce(BYDATE_OPEN, null));
        views.put("closedRequestsBydate", createMapReduce(BYDATE_CLOSED, null));
        views.put("openRequestsBydocumentname", createMapReduce(BYDOCUMENTNAME_OPEN, null));
        views.put("closedRequestsBydocumentname", createMapReduce(BYDOCUMENTNAME_CLOSED, null));
        views.put("openRequestsBymoderationstate", createMapReduce(BYMODERATIONTATE_OPEN, null));
        views.put("closedRequestsBymoderationstate", createMapReduce(BYMODERATIONTATE_CLOSED, null));
        views.put("openRequestsBycomponenttype", createMapReduce(BYCOMPONENTTYPE_OPEN, null));
        views.put("closedRequestsBycomponenttype", createMapReduce(BYCOMPONENTTYPE_CLOSED, null));
        views.put("openRequestsbyrequestinguserdept", createMapReduce(BYREQUESTINGUSERDEPRTMENT_OPEN, null));
        views.put("closedRequestsbyrequestinguserdept", createMapReduce(BYREQUESTINGUSERDEPRTMENT_CLOSED, null));
        views.put("openRequestsBymoderators", createMapReduce(MODERATORS_VIEW_FOR_SORTING_OPEN, null));
        views.put("closedRequestsBymoderators", createMapReduce(MODERATORS_VIEW_FOR_SORTING_CLOSED, null));
        views.put("byRequestingUsersDeptView", createMapReduce(REQUESTING_USERS_VIEW, null));
        views.put("countByModerationState", createMapReduce(COUNTBYMODERATIONSTATE, "_count"));
        initStandardDesignDocument(views, db);
    }

    public List<ModerationRequest> getRequestsByDocumentId(String documentId) {
        return queryView("documents", documentId);
    }

    public List<ModerationRequest> getRequestsByModerator(String moderator) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, queryView("moderators", moderator));
    }

    public Map<PaginationData, List<ModerationRequest>> getRequestsByModerator(String moderator, PaginationData pageData, boolean open) {
        Map<PaginationData, List<ModerationRequest>> paginatedModerations = queryViewWithPagination(moderator, pageData, open);
        List<ModerationRequest> moderationList = paginatedModerations.values().iterator().next();
        moderationList = makeSummaryFromFullDocs(SummaryType.SHORT, moderationList);
        paginatedModerations.put(pageData, moderationList);
        return paginatedModerations;
    }

    private Map<PaginationData, List<ModerationRequest>> queryViewWithPagination(String moderator, PaginationData pageData, boolean open) {
        final int rowsPerPage = pageData.getRowsPerPage();
        Map<PaginationData, List<ModerationRequest>> result = Maps.newHashMap();
        List<ModerationRequest> modReqs = Lists.newArrayList();
        final boolean ascending = pageData.isAscending();
        final int sortColumnNo = pageData.getSortColumnNumber();
        ViewRequestBuilder query;
        switch (sortColumnNo) {
        case -1:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsBymoderators" : "closedRequestsBymoderators");
            break;
        case 0:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsBydate" : "closedRequestsBydate");
            break;
        case 1:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsBycomponenttype" : "closedRequestsBycomponenttype");
            break;
        case 2:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsBydocumentname" : "closedRequestsBydocumentname");
            break;
        case 3:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsUsers" : "closedRequestsUsers");
            break;
        case 4:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsbyrequestinguserdept" : "closedRequestsbyrequestinguserdept");
            break;
        case 5:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsBymoderators" : "closedRequestsBymoderators");
            break;
        case 6:
            query = getConnector().createQuery(ModerationRequest.class,
                    open ? "openRequestsBymoderationstate" : "closedRequestsBymoderationstate");
            break;
        default:
            query = getConnector().createQuery(ModerationRequest.class, open ? "allOpenRequests" : "allClosedRequests");
            break;
        }
        ViewRequest<String, Object> request = null;
        if (rowsPerPage == -1) {
            request = query.newRequest(Key.Type.STRING, Object.class).descending(!ascending).includeDocs(true).build();
        } else {
            request = query.newPaginatedRequest(Key.Type.STRING, Object.class).rowsPerPage(rowsPerPage)
                    .descending(!ascending).includeDocs(true).build();
        }

        ViewResponse<String, Object> response = null;
        try {
            response = request.getResponse();
            int pageNo = pageData.getDisplayStart() / rowsPerPage;
            int i = 1;
            while (i <= pageNo) {
                response = response.nextPage();
                i++;
            }
            modReqs = response.getDocsAs(ModerationRequest.class);
        } catch (Exception e) {
            log.error("Error getting recent components", e);
        }
        pageData.setTotalRowCount(response.getTotalRowCount());
        result.put(pageData, modReqs);
        return result;
    }

    public List<ModerationRequest> getRequestsByRequestingUser(String user) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, queryView("users", user));
    }

    public Map<String, Long> getCountByModerationState() {
        Map<String, Long> countByModerationState = Maps.newHashMap();
        ViewRequest<String, Long> countReq = getConnector()
                .createQuery(ModerationRequest.class, "countByModerationState").newRequest(Key.Type.STRING, Long.class)
                .group(true).reduce(true).build();
        List<Row<String, Long>> rows;
        try {
            if (countReq.getResponse() != null) {
                rows = countReq.getResponse().getRows();
                for (Row<String, Long> row : rows) {
                    countByModerationState.put(row.getKey(), row.getValue());
                }
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
