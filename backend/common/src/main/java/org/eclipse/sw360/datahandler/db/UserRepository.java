/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.eq;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.and;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.exists;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.or;

import org.eclipse.sw360.components.summary.UserSummary;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import com.ibm.cloud.cloudant.v1.model.PostFindOptions;
import com.ibm.cloud.cloudant.v1.model.ViewResultRow;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CRUD access for the User class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author thomas.maier@evosoft.com
 */

public class UserRepository extends SummaryAwareRepository<User> {
    private static final String ALL = "function(doc) { if (doc.type == 'user') emit(null, doc._id) }";
    private static final String BYEXTERNALID = "function(doc) { if (doc.type == 'user' && doc.externalid) emit(doc.externalid.toLowerCase(), doc._id) }";
    private static final String BYAPITOKEN = "function(doc) { if (doc.type == 'user') " +
            "  for (var i in doc.restApiTokens) {" +
            "    emit(doc.restApiTokens[i].token, doc._id)" +
            "  }" +
            "}";
    private static final String BYOIDCCLIENTID = "function(doc) { if (doc.type == 'user') " +
            "  for (var i in doc.oidcClientInfos) {" +
            "    emit(i, doc._id)" +
            "  }" +
            "}";
    private static final String BYEMAIL = "function(doc) { " +
            "  if (doc.type == 'user') {" +
            "    emit(doc.email, doc._id); " +
            "    if (doc.formerEmailAddresses && Array.isArray(doc.formerEmailAddresses)) {" +
            "      var arr = doc.formerEmailAddresses;" +
            "      for (var i = 0; i < arr.length; i++){" +
            "        emit(arr[i], doc._id);" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    private static final String USERS_ALL_DEPARTMENT_VIEW = "function(doc) { " +
            "  if (doc.type == 'user') {" +
            "    emit(doc.department, null);" +
            "  }" +
            "}";

    private static final String USERS_ALL_EMAIL_VIEW = "function(doc) { " +
            "  if (doc.type == 'user') {" +
            "    emit(doc.email, null);" +
            "  }" +
            "}";

    private static final String USERS_ALL_SECONDARY_DEPARTMENT_VIEW = "function(doc) { " +
            "  if (doc.type == 'user') {" +
            "    for (var secondaryDepartmentsAndRole in doc.secondaryDepartmentsAndRoles) {" +
            "      try {" +
            "            var values = JSON.parse(doc.secondaryDepartmentsAndRoles[secondaryDepartmentsAndRole]);" +
            "            if(!isNaN(values)) {" +
            "               emit( secondaryDepartmentsAndRole, doc._id);" +
            "               continue;" +
            "            }" +
            "            for (var idx in values) {" +
            "              emit( secondaryDepartmentsAndRole, doc._id);" +
            "            }" +
            "      } catch(error) {" +
            "          emit( secondaryDepartmentsAndRole, doc._id);" +
            "      }" +
            "    }" +
            "  }" +
            "}";
    private static final String FIND_BY_SECONDARY_DEPARTMENT = "function(doc) { " +
            "  if (doc.type == 'user') {" +
            "    for (var secondaryDepartmentsAndRole in doc.secondaryDepartmentsAndRoles) {" +
            "      try {" +
            "            var values = JSON.parse(doc.secondaryDepartmentsAndRoles[secondaryDepartmentsAndRole]);" +
            "            if(!isNaN(values)) {" +
            "               emit( secondaryDepartmentsAndRole, doc.email);" +
            "               continue;" +
            "            }" +
            "            for (var idx in values) {" +
            "              emit( secondaryDepartmentsAndRole, doc.email);" +
            "            }" +
            "      } catch(error) {" +
            "          emit( secondaryDepartmentsAndRole, doc.email);" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    public UserRepository(DatabaseConnectorCloudant databaseConnector) {
        super(User.class, databaseConnector, new UserSummary());
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byExternalId", createMapReduce(BYEXTERNALID, null));
        views.put("byApiToken", createMapReduce(BYAPITOKEN, null));
        views.put("byOidcClientId", createMapReduce(BYOIDCCLIENTID, null));
        views.put("byEmail", createMapReduce(BYEMAIL, null));
        views.put("userDepartments", createMapReduce(USERS_ALL_DEPARTMENT_VIEW, null));
        views.put("userSecondaryDepartments", createMapReduce(USERS_ALL_SECONDARY_DEPARTMENT_VIEW, null));
        views.put("find_by_secondaryDepartments", createMapReduce(FIND_BY_SECONDARY_DEPARTMENT, null));
        views.put("userEmails", createMapReduce(USERS_ALL_EMAIL_VIEW, null));
        initStandardDesignDocument(views, databaseConnector);
        createIndex("byEmailUser", new String[] {"email"}, databaseConnector);
        createIndex("byDepartment", new String[] {"department"}, databaseConnector);
        createIndex("byFirstName", new String[] {"givenname"}, databaseConnector);
        createIndex("byLastName", new String[] {"lastname"}, databaseConnector);
        createIndex("byActiveStatus", new String[] {"deactivated"}, databaseConnector);
        createIndex("byUserGroup", new String[] {"userGroup"}, databaseConnector);
        createIndex("bySecondaryDepartmentsAndRoles", new String[] {"secondaryDepartmentsAndRoles"}, databaseConnector);
    }

    @Override
    public List<User> get(Collection<String> ids) {
        return getConnector().get(User.class, ids, true);
    }

    public User getByExternalId(String externalId) {
        if(externalId == null || "".equals(externalId)) {
            // liferay contains the setup user with externalId=="" and we do not want to match him or any other one with empty externalID
            return null;
        }
        final Set<String> userIds = queryForIdsAsValue("byExternalId", externalId.toLowerCase());
        return getUserFromIds(userIds);
    }

    public User getByEmail(String email) {
        final Set<String> userIds = queryForIdsAsValue("byEmail", email);
        return getUserFromIds(userIds);
    }

    public Set<String> getEmailsByDepartmentName(String key) {
        final Set<String> emails = queryForIdsAsValue("find_by_secondaryDepartments", key);
        return emails;
    }

    public User getByApiToken(String token) {
        final Set<String> userIds = queryForIdsAsValue("byApiToken", token);
        return getUserFromIds(userIds);
    }

    private User getUserFromIds(Set<String> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            return get(CommonUtils.getFirst(userIds));
        } else {
            return null;
        }
    }

    public Set<String> getUserDepartments() {
        return getResultBasedOnQuery("userDepartments");
    }

    public Set<String> getUserSecondaryDepartments() {
        return getResultBasedOnQuery("userSecondaryDepartments");
    }

    public Set<String> getUserEmails() {
        return getResultBasedOnQuery("userEmails");
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(PaginationData pageData) {
        Map<PaginationData, List<User>> paginatedUsers = queryViewWithPagination(pageData);
        List<User> userList = paginatedUsers.values().iterator().next();
        paginatedUsers.put(pageData, userList);
        return paginatedUsers;
    }

    private Set<String> getResultBasedOnQuery(String queryName) {
        Set<String> userResults = Sets.newHashSet();
        PostViewOptions query = getConnector().getPostViewQueryBuilder(User.class, queryName)
                .includeDocs(false).build();
        try {
            userResults = getConnector().getPostViewQueryResponse(query).getRows()
                    .stream()
                    .map(ViewResultRow::getKey)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        } catch (ServiceConfigurationError e) {
            log.error("Error getting record of users based on queryName - " + queryName, e);
        }
        return userResults;
    }

    private Map<PaginationData, List<User>> queryViewWithPagination(PaginationData pageData) {
        final int rowsPerPage = pageData.getRowsPerPage();
        Map<PaginationData, List<User>> result = Maps.newHashMap();
        List<User> users = Lists.newArrayList();
        final boolean ascending = pageData.isAscending();
        final int sortColumnNo = pageData.getSortColumnNumber();
        PostFindOptions query = null;
        final Map<String, Object> typeSelector = Collections.singletonMap("type",
                Collections.singletonMap("$eq", "user"));
        final Map<String, Object> emptySecondaryDepartmentsAndRolesSelector = or(
                List.of(exists("secondaryDepartmentsAndRoles", false), eq("secondaryDepartmentsAndRoles", "")));
        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(typeSelector);
        if (rowsPerPage != -1) {
            qb.limit(rowsPerPage);
        }
        qb.skip(pageData.getDisplayStart());

        switch (sortColumnNo) {
            case -1:
            case 2:
                qb.useIndex(Collections.singletonList("byEmailUser"))
                        .addSort(Collections.singletonMap("email", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 0:
                qb.useIndex(Collections.singletonList("byFirstName"))
                        .addSort(Collections.singletonMap("givenname", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 1:
                qb.useIndex(Collections.singletonList("byLastName"))
                        .addSort(Collections.singletonMap("lastname", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 3:
                qb.useIndex(Collections.singletonList("byActiveStatus"))
                        .addSort(Collections.singletonMap("deactivated", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 4:
                qb.useIndex(Collections.singletonList("byDepartment"))
                        .addSort(Collections.singletonMap("department", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 5:
                qb.useIndex(Collections.singletonList("byUserGroup"))
                        .addSort(Collections.singletonMap("userGroup", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            case 6:
                if (ascending) {
                    qb.skip(0);
                }
                qb.useIndex(Collections.singletonList("bySecondaryDepartmentsAndRoles"))
                        .addSort(Collections.singletonMap("secondaryDepartmentsAndRoles", ascending ? "asc" : "desc"));
                query = qb.build();
                break;
            default:
                break;
        }

        try {
            users = getConnector().getQueryResult(query, User.class);

            if (sortColumnNo == 6) {
                final Map<String, Object> selectorSecondaryGroupsAndRoles = and(List.of(typeSelector,
                        emptySecondaryDepartmentsAndRolesSelector));
                PostFindOptions.Builder emptySecondaryGroupsAndRolesQb = getConnector().getQueryBuilder()
                        .selector(selectorSecondaryGroupsAndRoles);
                emptySecondaryGroupsAndRolesQb.skip(pageData.getDisplayStart());
                if (rowsPerPage != -1) {
                    emptySecondaryGroupsAndRolesQb.limit(rowsPerPage);
                }
                List<User> userList = getConnector()
                        .getQueryResult(emptySecondaryGroupsAndRolesQb.build(), User.class);
                if (ascending) {
                    userList.addAll(users);
                    users = userList;
                } else {
                    users.addAll(userList);
                }
            }
        } catch (Exception e) {
            log.error("Error getting users", e);
        }
        result.put(pageData, users);
        return result;
    }

    public User getByOidcClientId(String clientId) {
        final Set<String> userIds = queryForIdsAsValue("byOidcClientId", clientId);
        return getUserFromIds(userIds);
    }
}
