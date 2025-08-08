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
import org.eclipse.sw360.datahandler.thrift.users.UserSortColumn;
import org.jetbrains.annotations.NotNull;

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

    private static final String USER_BY_EMAIL_IDX = "UserByEmailIdx";
    private static final String USER_BY_DEPARTMENT_IDX = "UserByDepartmentIdx";
    private static final String USER_BY_FIRST_NAME_IDX = "UserByFirstNameIdx";
    private static final String USER_BY_LAST_NAME_IDX = "UserByLastNameIdx";
    private static final String USER_BY_ACTIVE_STATUS_IDX = "UserByActiveStatusIdx";
    private static final String USER_BY_GROUP_IDX = "UserByGroupIdx";
    private static final String USER_BY_SECONDARY_DEPT_IDX = "UserBySecondaryDeptIdx";
    private static final String USER_BY_ALL_IDX = "UserByAllIdx";

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
        createIndex(USER_BY_EMAIL_IDX, "byEmailUser", new String[] {"email"}, databaseConnector);
        createIndex(USER_BY_DEPARTMENT_IDX, "byDepartment", new String[] {"department"}, databaseConnector);
        createIndex(USER_BY_FIRST_NAME_IDX, "byFirstName", new String[] {"givenname"}, databaseConnector);
        createIndex(USER_BY_LAST_NAME_IDX, "byLastName", new String[] {"lastname"}, databaseConnector);
        createIndex(USER_BY_ACTIVE_STATUS_IDX, "byActiveStatus", new String[] {"deactivated"}, databaseConnector);
        createIndex(USER_BY_GROUP_IDX, "byUserGroup", new String[] {"userGroup"}, databaseConnector);
        createIndex(USER_BY_SECONDARY_DEPT_IDX,
                "bySecondaryDepartmentsAndRoles", new String[] {"secondaryDepartmentsAndRoles"}, databaseConnector);

        createIndex(USER_BY_ALL_IDX, "usersByAll", new String[] {
                User._Fields.GIVENNAME.getFieldName(),
                User._Fields.LASTNAME.getFieldName(),
                User._Fields.DEPARTMENT.getFieldName(),
                User._Fields.EMAIL.getFieldName(),
                User._Fields.USER_GROUP.getFieldName(),
        }, databaseConnector);
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

    public Set<String> getEmailsBySecondaryDepartmentName(String departmentName) {
        final Set<String> emails = queryForIdsAsValue("find_by_secondaryDepartments", departmentName);
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

    public Map<PaginationData, List<User>> searchUsersByExactValues(Map<String,Set<String>> subQueryRestrictions, PaginationData pageData) {
        final Map<String, Object> typeSelector = eq("type", "user");
        final Map<String, Object> restrictionsSelector = getQueryFromRestrictions(subQueryRestrictions);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, restrictionsSelector));

        final Map<String, String> sortSelector = getSortSelector(pageData);

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(USER_BY_ALL_IDX));

        List<User> users = getConnector().getQueryResultPaginated(
                qb, User.class, pageData, sortSelector
        );

        return Collections.singletonMap(pageData, users);
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(PaginationData pageData) {
        return queryViewWithPagination(pageData);
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
        final UserSortColumn sortBy = UserSortColumn.findByValue(pageData.getSortColumnNumber());
        final Map<String, String> sortSelector = getSortSelector(pageData);
        List<User> users = Lists.newArrayList();
        Map<PaginationData, List<User>> result = Maps.newHashMap();

        final Map<String, Object> typeSelector = eq("type", "user");

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(typeSelector);

        if (sortBy == UserSortColumn.BY_STATUS) {
            qb.useIndex(Collections.singletonList(USER_BY_ACTIVE_STATUS_IDX));
        } else {
            qb.useIndex(Collections.singletonList(USER_BY_ALL_IDX));
        }

        try {
            users = getConnector().getQueryResultPaginated(qb, User.class, pageData, sortSelector);
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

    private Map<String, Object> getQueryFromRestrictions(Map<String, Set<String>> subQueryRestrictions) {
        List<Map<String, Object>> andConditions = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : subQueryRestrictions.entrySet()) {
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value.isEmpty()) {
                        continue;
                    }
                    andConditions.add(eq(entry.getKey(), value));
                }
            }
        }
        return and(andConditions);
    }

    private static @NotNull Map<String, String> getSortSelector(PaginationData pageData) {
        boolean ascending = pageData.isAscending();
        return switch (UserSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case UserSortColumn.BY_LASTNAME ->
                    Collections.singletonMap("lastname", ascending ? "asc" : "desc");
            case UserSortColumn.BY_EMAIL ->
                    Collections.singletonMap("email", ascending ? "asc" : "desc");
            case UserSortColumn.BY_STATUS ->
                    Collections.singletonMap("deactivated", ascending ? "asc" : "desc");
            case UserSortColumn.BY_DEPARTMENT ->
                    Collections.singletonMap("department", ascending ? "asc" : "desc");
            case UserSortColumn.BY_ROLE ->
                    Collections.singletonMap("userGroup", ascending ? "asc" : "desc");
            case null, default ->
                    Collections.singletonMap("givenname", ascending ? "asc" : "desc"); // Default sort by name
        };
    }
}
