/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 * With contributions by Siemens Healthcare Diagnostics Inc, 2018.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.components.summary.ProjectSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectData;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.jetbrains.annotations.NotNull;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostFindOptions;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.all;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.elemMatch;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.eq;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.and;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.or;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_ADMIN_PRIVATE_ACCESS_ENABLED;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getBUFromOrganisation;

/**
 * CRUD access for the Project class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author thomas.maier@evosoft.com
 * @author ksoranko@verifa.io
 */
@Component
public class ProjectRepository extends SummaryAwareRepository<Project> {
    private static final String ALL = "function(doc) { if (doc.type == 'project') emit(null, doc._id) }";

    private static final String FULL_MY_PROJECTS_VIEW =
            "function(doc) {\n" +
                    "  if (doc.type == 'project') {\n" +
                    "    var acc = {};\n" +
                    "    if(doc.createdBy)\n" +
                    "      acc[doc.createdBy]=1;\n" +
                    "    if(doc.leadArchitect)\n" +
                    "      acc[doc.leadArchitect]=1;\n" +
                    "    for(var i in doc.moderators) { \n" +
                    "      acc[doc.moderators[i]]=1;\n" +
                    "    }\n" +
                    "    for(var i in doc.contributors) {\n" +
                    "      acc[doc.contributors[i]]=1;\n" +
                    "    }\n" +
                    "    if(doc.projectOwner)\n" +
                    "      acc[doc.projectOwner]=1;\n" +
                    "    if(doc.projectResponsible)\n" +
                    "      acc[doc.projectResponsible]=1;\n" +
                    "    for(var i in doc.securityResponsibles) {\n" +
                    "      acc[doc.securityResponsibles[i]]=1;\n" +
                    "    }\n" +
                    "    for(var i in acc){\n" +
                    "      emit(i,doc._id);\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

    private static final String MY_ACCESSIBLE_PROJECTS_COUNT =
            "function(doc) {\n" +
            "  if (doc.type == 'project') {\n" +
            "    var acc = {};\n" +
            "    var meAndModerator = doc.visbility == \"ME_AND_MODERATORS\";\n" +
            "    if(doc.createdBy && (meAndModerator || doc.visbility == \"PRIVATE\"))\n" +
            "      acc[doc.createdBy]=1;\n" +
            "    if(doc.leadArchitect && meAndModerator)\n" +
            "      acc[doc.leadArchitect]=1;\n" +
            "  if(meAndModerator) {\n" +
            "    for(var i in doc.moderators) {\n" +
            "        acc[doc.moderators[i]]=1;\n" +
            "    }\n" +
            "    if(meAndModerator) {\n" +
            "    for(var i in doc.contributors) {\n" +
            "        acc[doc.contributors[i]]=1;\n" +
            "    }\n" +
            "    }\n" +
            "    if(doc.projectResponsible && meAndModerator)\n" +
            "      acc[doc.projectResponsible]=1;\n" +
            "    }\n" +
            "    for(var i in acc){\n" +
            "      emit(i,null);\n" +
            "    }\n" +
            "    if(doc.visbility == \"EVERYONE\"){\n" +
            "      emit(\"everyone\",null)\n" +
            "    }\n" +
            "    if(doc.visbility == \"BUISNESSUNIT_AND_MODERATORS\") {\n" +
            "        emit(doc.businessUnit,null);\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String ACCESSIBLE_PROJECTS_COUNT_FOR_CA_AND_ABOVE =
            "function(doc) {\n" +
            "  if (doc.type == 'project') {\n" +
            "    var acc = {};\n" +
            "    var meAndModerator = doc.visbility == \"ME_AND_MODERATORS\";\n" +
            "    if(doc.createdBy && (meAndModerator || doc.visbility == \"PRIVATE\"))\n" +
            "      acc[doc.createdBy]=1;\n" +
            "    if(doc.leadArchitect && meAndModerator)\n" +
            "      acc[doc.leadArchitect]=1;\n" +
            "  if(meAndModerator) {\n" +
            "    for(var i in doc.moderators) {\n" +
            "        acc[doc.moderators[i]]=1;\n" +
            "    }\n" +
            "    if(meAndModerator) {\n" +
            "    for(var i in doc.contributors) {\n" +
            "        acc[doc.contributors[i]]=1;\n" +
            "    }\n" +
            "    }\n" +
            "    if(doc.projectResponsible && meAndModerator)\n" +
            "      acc[doc.projectResponsible]=1;\n" +
            "    }\n" +
            "    for(var i in acc){\n" +
            "      emit(i,null);\n" +
            "    }\n" +
            "    if(doc.visbility == \"EVERYONE\"){\n" +
            "      emit(\"everyone\",null)\n" +
            "    }\n" +
            "    if(doc.visbility == \"BUISNESSUNIT_AND_MODERATORS\") {\n" +
            "        emit(\"bu\",null);\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String BU_PROJECTS_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.businessUnit, doc._id);" +
                    "  }" +
                    "}";

    private static final String BY_NAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.name, doc._id);" +
                    "  }" +
                    "}";

    private static final String BY_TAG_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.tag, doc._id);" +
                    "  }" +
                    "}";

    private static final String BY_GROUP_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.businessUnit, doc._id);" +
                    "  }" +
                    "}";

    private static final String BY_TYPE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.projectType, doc._id);" +
                    "  }" +
                    "}";

    private static final String BY_STATE_VIEW =
            "function(doc) {" +
            "    if (doc.type == 'project') {" +
            "      if(doc.clearingState == 'OPEN')" +
            "      {" +
            "        emit(doc.state+0, doc._id);" +
            "      } else if(doc.clearingState == 'IN_PROGRESS'){" +
            "        emit(doc.state+1, doc._id);" +
            "      }else if(doc.clearingState == 'CLOSED'){" +
            "        emit(doc.state+2, doc._id);" +
            "      }" +
            "    }" +
            "}";


    private static final String BY_RELEASE_ID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    for(var i in doc.releaseIdToUsage) {" +
                    "      emit(i, doc._id);" +
                    "    }" +
                    "  }" +
                    "}";


    private static final String FULL_BY_RELEASE_ID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    for(var i in doc.releaseIdToUsage) {" +
                    "      emit(i, doc._id);" +
                    "    }" +
                    "  }" +
                    "}";

    private static final String BY_PACKAGE_ID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project' && doc.packageIds) {" +
                    "    for(var i in doc.packageIds) {" +
                    "      emit(i, doc._id);" +
                    "    }" +
                    "  }" +
                    "}";

    private static final String BY_LINKING_PROJECT_ID_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    for(var i in doc.linkedProjects) {" +
                    "      emit(i, doc._id);" +
                    "    }" +
                    "  }" +
                    "}";

    private static final String BY_EXTERNAL_IDS =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    for (var externalId in doc.externalIds) {" +
                    "      try {" +
                    "            var values = JSON.parse(doc.externalIds[externalId]);" +
                    "            if(!isNaN(values)) {" +
                    "               emit( [externalId, doc.externalIds[externalId]], doc._id);" +
                    "               continue;" +
                    "            }" +
                    "            for (var idx in values) {" +
                    "              emit( [externalId, values[idx]], doc._id);" +
                    "            }" +
                    "      } catch(error) {" +
                    "          emit( [externalId, doc.externalIds[externalId]], doc._id);" +
                    "      }" +
                    "    }" +
                    "  }" +
                    "}";

    private static final String PROJECT_BY_NAME_IDX = "ProjectByNameIdx";
    private static final String PROJECT_BY_DESC_IDX = "ProjectByDescIdx";
    private static final String PROJECT_BY_RESPONSIBLE_IDX = "ProjectByResponsibleIdx";
    private static final String PROJECT_BY_CREATED_ON_IDX = "ProjectByCreatedOnIdx";
    private static final String PROJECT_BY_ALL_IDX = "ProjectByAllIdx";
    public static final String PAGINATION_IDX = "PaginationIdx";

    @Autowired
    public ProjectRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(Project.class, db, new ProjectSummary());
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("byname", createMapReduce(BY_NAME_VIEW, null));
        views.put("bygroup", createMapReduce(BY_GROUP_VIEW, null));
        views.put("bytag", createMapReduce(BY_TAG_VIEW, null));
        views.put("bytype", createMapReduce(BY_TYPE_VIEW, null));
        views.put("byState", createMapReduce(BY_STATE_VIEW, null));
        views.put("byreleaseid", createMapReduce(BY_RELEASE_ID_VIEW, null));
        views.put("byPackageId", createMapReduce(BY_PACKAGE_ID_VIEW, null));
        views.put("fullbyreleaseid", createMapReduce(FULL_BY_RELEASE_ID_VIEW, null));
        views.put("bylinkingprojectid", createMapReduce(BY_LINKING_PROJECT_ID_VIEW, null));
        views.put("fullmyprojects", createMapReduce(FULL_MY_PROJECTS_VIEW, null));
        views.put("buprojects", createMapReduce(BU_PROJECTS_VIEW, null));
        views.put("byexternalids", createMapReduce(BY_EXTERNAL_IDS, null));
        views.put("all", createMapReduce(ALL, null));
        views.put("myfullprojectscount", createMapReduce(MY_ACCESSIBLE_PROJECTS_COUNT, "_count"));
        views.put("myfullprojectscountca", createMapReduce(ACCESSIBLE_PROJECTS_COUNT_FOR_CA_AND_ABOVE, "_count"));
        initStandardDesignDocument(views, db);
        createIndex(PROJECT_BY_NAME_IDX, "byName",
                new String[] {Project._Fields.TYPE.getFieldName(), Project._Fields.NAME.getFieldName()}, db);
        createIndex(PROJECT_BY_DESC_IDX, "byDesc",
                new String[] {Project._Fields.TYPE.getFieldName(), Project._Fields.DESCRIPTION.getFieldName()}, db);
        createIndex(PROJECT_BY_RESPONSIBLE_IDX, "byProjectResponsible",
                new String[] {Project._Fields.TYPE.getFieldName(), Project._Fields.PROJECT_RESPONSIBLE.getFieldName()}, db);
        createIndex(PROJECT_BY_CREATED_ON_IDX, "byCreatedOn",
                new String[] {Project._Fields.TYPE.getFieldName(), Project._Fields.CREATED_ON.getFieldName()}, db);
        createIndex(PAGINATION_IDX, "byTypePagination",
                new String[] {"type", "_id"}, db);

        createIndex(PROJECT_BY_ALL_IDX, "projByAll", new String[] {
                Project._Fields.NAME.getFieldName(),
                Project._Fields.TAG.getFieldName(),
                Project._Fields.PROJECT_TYPE.getFieldName(),
                Project._Fields.OWNER_GROUP.getFieldName(),
                Project._Fields.VERSION.getFieldName(),
                Project._Fields.PROJECT_RESPONSIBLE.getFieldName(),
                Project._Fields.STATE.getFieldName(),
                Project._Fields.CLEARING_STATE.getFieldName(),
                Project._Fields.ADDITIONAL_DATA.getFieldName(),
                Project._Fields.DESCRIPTION.getFieldName()
        }, db);
    }

    public List<Project> searchByName(String name, User user, SummaryType summaryType) {
        Set<String> searchIds = queryForIdsByPrefix("byname", name);
        return makeSummaryFromFullDocs(summaryType, filterAccessibleProjectsByIds(user, searchIds));
    }

    public List<Project> searchByNameAndVersion(String name, String version) {
        List<Project> projectsMatchingName = queryView("byname", name);
        List<Project> projectsMatchingNameAndVersion = projectsMatchingName.stream()
                .filter(p -> isNullOrEmpty(version) ? isNullOrEmpty(p.getVersion()) : version.equals(p.getVersion()))
                .collect(Collectors.toList());
        return makeSummaryFromFullDocs(SummaryType.SHORT, projectsMatchingNameAndVersion);
    }

    public Set<Project> searchByReleaseId(String id, User user) {
        return searchByReleaseId(Collections.singleton(id), user);
    }

    public Set<Project> searchByReleaseId(Set<String> ids, User user) {
        Set<String> searchIds = queryForIdsAsValue("byreleaseid", ids);
        return getAccessibleProjectSummary(user, searchIds);
    }

    public int getCountByReleaseIds(Set<String> ids) {
        Set<String> searchIds = queryForIdsAsValue("byreleaseid", ids);
        return searchIds.size();
    }

    public Set<Project> searchByPackageId(String id, User user) {
        return searchByPackageIds(Collections.singleton(id), user);
    }

    public Set<Project> searchByPackageIds(Set<String> ids, User user) {
        Set<String> projectIds = queryForIdsAsValue("byPackageId", ids);
        return getAccessibleProjectSummary(user, projectIds);
    }

    public int getCountByPackageId(String id) {
        Set<String> projectIds = queryForIdsAsValue("byPackageId", Collections.singleton(id));
        return projectIds.size();
    }

    public Set<Project> searchByReleaseId(String id) {
        Set<String> projectIds = queryForIdsAsValue("fullbyreleaseid", id);
        return getFullDocsById(projectIds);
    }

    public Set<Project> searchByReleaseId(Set<String> ids) {
        Set<String> projectIds = queryForIdsAsValue("fullbyreleaseid", ids);
        return getFullDocsById(projectIds);
    }

    public Set<Project> searchByLinkingProjectId(String id, User user) {
        Set<String> searchIds = queryForIdsByPrefix("bylinkingprojectid", id);
        return getAccessibleProjectSummary(user, searchIds);
    }

    public int getCountByProjectId(String id) {
        Set<String> searchIds = queryForIdsByPrefix("bylinkingprojectid", id);
        return searchIds.size();
    }

    public Set<Project> searchByLinkingProjectId(String id) {
        return new HashSet<>(queryView("bylinkingprojectid", id));
    }

    private Set<Project> getMyProjects(String user) {
        Set<String> myProjectsIds = queryForIdsAsValue("fullmyprojects", user);
        return getFullDocsById(myProjectsIds);
    }

    public List<Project> getMyProjectsSummary(String user) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, getMyProjects(user));
    }

    public List<Project> getMyProjectsFull(String user) {
        return new ArrayList<>(getMyProjects(user));
    }

    public List<Project> getBUProjects(String organisation) {
        // Filter BU to first three blocks
        String bu = getBUFromOrganisation(organisation);
        return queryByPrefix("buprojects", bu);
    }

    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) {
        RepositoryUtils repositoryUtils = new RepositoryUtils();
        Set<String> searchIds = repositoryUtils.searchByExternalIds(this, "byexternalids", externalIds);
        return filterAccessibleProjectsByIds(user, searchIds);
    }

    public List<Project> getBUProjectsSummary(String organisation) {
        return makeSummaryFromFullDocs(SummaryType.SUMMARY, getBUProjects(organisation));
    }


    public List<Project> getAccessibleProjectsSummary(User user, VendorRepository vendorRepository) {
        return makeSummaryFromFullDocs(SummaryType.SUMMARY, getAccessibleProjects(user, vendorRepository));
    }

    public Map<PaginationData, List<Project>> getAccessibleProjectsSummary(User user, PaginationData pageData) {
        final ProjectSortColumn sortBy = ProjectSortColumn.findByValue(pageData.getSortColumnNumber());
        final Map<String, String> sortSelector = getSortSelector(pageData);
        List<Project> projects = new ArrayList<>();
        Map<PaginationData, List<Project>> result = Maps.newHashMap();

        Map<String, Object> finalSelector = getAccessibleProjectSelector(user, Map.of());

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector);
        String queryViewName = null;

        switch (sortBy) {
            case ProjectSortColumn.BY_DESCRIPTION:
                qb.useIndex(Collections.singletonList(PROJECT_BY_DESC_IDX));
                break;
            case ProjectSortColumn.BY_RESPONSIBLE:
                qb.useIndex(Collections.singletonList(PROJECT_BY_RESPONSIBLE_IDX));
                break;
            case ProjectSortColumn.BY_CREATEDON:
                qb.useIndex(Collections.singletonList(PROJECT_BY_CREATED_ON_IDX));
            case ProjectSortColumn.BY_STATE:
                queryViewName = "byState";
                break;
            case null:
            default: // By default, sort by name
                qb.useIndex(Collections.singletonList(PROJECT_BY_NAME_IDX));
                break;
        }
        try {
            if (queryViewName != null) {
                projects = queryViewPaginated(queryViewName, pageData, false);
            } else {
                projects = getConnector().getQueryResultPaginated(qb, Project.class, pageData, sortSelector);
            }
        } catch (Exception e) {
            log.error("Error getting projects", e);
        }
        result.put(pageData, projects);
        return result;
    }

    @NotNull
    public Set<Project> getAccessibleProjects(User user, VendorRepository vendorRepository) {
        /** This implementation requires multiple DB requests and has its logic distributed in multiple places **/
//        final Set<Project> buProjects = new HashSet<>(getBUProjects(organisation));
//        final Set<Project> myProjects = getMyProjects(user);
//        return Sets.union(buProjects, myProjects);

        /** I have only one day left in the project so I try this, but if there is time this should be reviewed
         *  The ideal solution would be to make a smarter query with a combined key, but I do not know how easy
         *  this is to refactor if say an enum value gets renamed...
         * **/
        final List<Project> all = getAll();
        return all.stream().filter(ProjectPermissions.isVisible(user)::test).map(project -> {
            vendorRepository.fillVendor(project);
            return project;
        }).collect(Collectors.toSet());
    }

    public List<Project> searchByName(String name, User user) {
        return searchByName(name, user, SummaryType.SUMMARY);
    }

    public Map<PaginationData, List<Project>> searchProjectByNamePrefixPaginated(User user, String name, PaginationData pageData) {
        Map<PaginationData, List<Project>> result = Maps.newHashMap();
        Set<String> searchIds = queryForIdsByPrefixPaginated("byname", name, pageData, false);
        result.put(pageData, makeSummaryFromFullDocs(SummaryType.SUMMARY, filterAccessibleProjectsByIds(user, searchIds)));
        return result;
    }

    public Map<PaginationData, List<Project>> searchProjectByExactNamePaginated(User user, String name, PaginationData pageData) {
        Map<PaginationData, List<Project>> result = Maps.newHashMap();
        List<Project> projects = queryViewPaginated("byname", name, pageData, false);
        result.put(pageData, makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, projects, user));
        return result;
    }

    public Map<PaginationData, List<Project>> searchAccessibleProjectByExactValues(
            Map<String, Set<String>> subQueryRestrictions, User user, @NotNull PaginationData pageData
    ) {
        final Map<String, String> sortSelector = getSortSelector(pageData);

        Map<String, Object> finalSelector = getAccessibleProjectSelector(user, subQueryRestrictions);

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(PROJECT_BY_ALL_IDX));

        List<Project> projects = getConnector().getQueryResultPaginated(
                qb, Project.class, pageData, sortSelector
        );

        return Collections.singletonMap(pageData, projects);
    }

    public Set<String> getGroups() {
        return getConnector().getDistinctSortedStringKeys(Project.class, "buprojects");
    }

    @NotNull
    private Set<Project> filterAccessibleProjectsByIds(User user, Set<String> searchIds) {
        final Set<Project> output = new HashSet<>();
        searchIds.stream().filter(CommonUtils::isNotNullEmptyOrWhitespace).forEach(searchId -> {
            Project project = get(searchId);
            if (project != null) {
                if (ProjectPermissions.isVisible(user).test(project)) {
                    output.add(project);
                } else {
                    log.warn("Project with Id - " + searchId + " not visisble to user - " + user.getEmail());
                }
            } else {
                log.warn("Error occured while fetching Project with Id - " + searchId);
            }
        });

        return output;
    }

    private Set<Project> getAccessibleProjectSummary(User user, Set<String> searchIds) {
        Set<Project> accessibleProjects = filterAccessibleProjectsByIds(user, searchIds);
        return new HashSet<>(makeSummaryFromFullDocs(SummaryType.LINKED_PROJECT_ACCESSIBLE, accessibleProjects));
    }

    public int getMyAccessibleProjectsCount(User user) {
        boolean isAdmin = PermissionUtils.isAdmin(user);
        boolean isClearingAdmin = PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user);
        Set<String> BUs = new HashSet<>();
        String primaryOrg = SW360Utils.getBUFromOrganisation(user.getDepartment());
        BUs.add(primaryOrg);

        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = user.getSecondaryDepartmentsAndRoles();
        if (!CommonUtils.isNullOrEmptyMap(secondaryDepartmentsAndRoles)) {
            Set<String> secondaryUGs = secondaryDepartmentsAndRoles.keySet();
            Set<String> secondaryBUs = secondaryUGs.stream().map(SW360Utils::getBUFromOrganisation)
                    .collect(Collectors.toSet());
            BUs.addAll(secondaryBUs);
        }

        String[] keys = new String[BUs.size() + 2];
        int index = 0;
        for (String str : BUs) {
            keys[index++] = str;
        }
        keys[keys.length - 2] = user.getEmail();
        keys[keys.length - 1] = "everyone";
        if (SW360Utils.readConfig(IS_ADMIN_PRIVATE_ACCESS_ENABLED, false) && isAdmin) {
            return getConnector().getDocumentCount(Project.class);
        }
        if (isClearingAdmin) {
            String[] keyss = new String[3];
            keyss[keyss.length - 3] = "bu";
            keyss[keyss.length - 2] = user.getEmail();
            keyss[keyss.length - 1] = "everyone";
            return getConnector().getDocumentCount(Project.class, "myfullprojectscountca", keyss);
        }
        return getConnector().getDocumentCount(Project.class, "myfullprojectscount", keys);
    }

    public ProjectData searchByGroup(String group, User user) {
        Set<String> searchIds = queryForIdsByPrefix("bygroup", group);
        Set<Project> accessibleProjects = filterAccessibleProjectsByIds(user, searchIds);
        return getProjectData(accessibleProjects);
    }

    public ProjectData searchByTag(String tag, User user) {
        Set<String> searchIds = queryForIdsByPrefix("bytag", tag);
        Set<Project> accessibleProjects = filterAccessibleProjectsByIds(user, searchIds);
        return getProjectData(accessibleProjects);
    }

    public ProjectData searchByType(String type, User user) {
        Set<String> searchIds = queryForIdsByPrefix("bytype", type);
        Set<Project> accessibleProjects = filterAccessibleProjectsByIds(user, searchIds);
        return getProjectData(accessibleProjects);
    }

    private ProjectData getProjectData(Set<Project> accessibleProjects) {
        int totalSize = accessibleProjects.size();
        ProjectData projectData = new ProjectData();
        projectData.setTotalNumberOfProjects(totalSize);
        List<Project> first250Projects = new ArrayList<>(totalSize > 250 ? 250 : totalSize);
        List<String> listOfRemainingIds = new LinkedList<>();
        int i = 0;
        Iterator<Project> iterator = accessibleProjects.iterator();
        while (iterator.hasNext()) {
            Project nextPrj = iterator.next();
            if (i < 250) {
                first250Projects.add(nextPrj);
                i++;
            } else {
                listOfRemainingIds.add(nextPrj.getId());
            }
        }
        return projectData.setFirst250Projects(first250Projects).setProjectIdsOfRemainingProject(listOfRemainingIds);
    }

    private Map<String, Object> getAccessibleProjectSelector(User user, Map<String, Set<String>> subQueryRestrictions) {
        final String requestingUserEmail = user.getEmail();
        final String userBU = getBUFromOrganisation(user.getDepartment());

        final Map<String, Object> typeSelector = eq("type", "project");
        final Map<String, Object> private_visibility_Selector = eq("visbility", "PRIVATE");
        final Map<String, Object> createdBySelector = eq("createdBy", requestingUserEmail);
        final Map<String, Object> getAllPrivateProjects = and(List.of(private_visibility_Selector, createdBySelector));
        final Map<String, Object> everyone_visibility_Selector = eq("visbility", "EVERYONE");

        final Map<String, Object> isAProjectResponsible = eq("projectResponsible", requestingUserEmail);
        final Map<String, Object> isALeadArchitect = eq("leadArchitect", requestingUserEmail);
        final Map<String, Object> isAModerator = elemMatch("moderators", requestingUserEmail);
        final Map<String, Object> isAContributor = elemMatch("contributors", requestingUserEmail);
        final Map<String, Object> meAndModerator_visibility_Selector = eq("visbility", "ME_AND_MODERATORS");
        final List<Map<String, Object>> checkUserInProjectFields = List.of(createdBySelector, isAProjectResponsible, isALeadArchitect, isAModerator, isAContributor);
        final Map<String, Object> isUserBelongToMeAndModerator = and(List.of(meAndModerator_visibility_Selector,
                or(checkUserInProjectFields)));

        final Map<String, Object> buAndModerator_visibility_Selector = eq("visbility", "BUISNESSUNIT_AND_MODERATORS");
        final Map<String, Object> userBuSelector = eq("businessUnit", userBU);
        boolean isAdmin = PermissionUtils.isAdmin(user);
        boolean isSecurityUser = PermissionUtils.isSecurityUser(user);
        boolean isClearingAdmin = PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user);
        Map<String, Object> isUserBelongToBuAndModerator;

        List<Map<String, Object>> buSelectors = new ArrayList<>();
        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = user.getSecondaryDepartmentsAndRoles();
        if (!CommonUtils.isNullOrEmptyMap(secondaryDepartmentsAndRoles)) {
            Set<String> secondaryUgs = secondaryDepartmentsAndRoles.keySet();
            Set<String> secondaryBus = secondaryUgs.stream().map(SW360Utils::getBUFromOrganisation)
                    .collect(Collectors.toSet());
            for (String secondaryBU : secondaryBus) {
                buSelectors.add(eq("businessUnit", secondaryBU));
            }
        }
        buSelectors.addAll(checkUserInProjectFields);
        buSelectors.add(userBuSelector);
        isUserBelongToBuAndModerator = and(List.of(buAndModerator_visibility_Selector, or(buSelectors)));

        List<Map<String, Object>> innerRestrictions = new ArrayList<>();
        innerRestrictions.add(getAllPrivateProjects);
        innerRestrictions.add(everyone_visibility_Selector);
        innerRestrictions.add(isUserBelongToMeAndModerator);

        Map<String, Object> finalSelector;
        if ((SW360Utils.readConfig(IS_ADMIN_PRIVATE_ACCESS_ENABLED, false) && isAdmin) || isSecurityUser) {
            if (!subQueryRestrictions.isEmpty()) {
                finalSelector = and(List.of(typeSelector, getQueryFromRestrictions(subQueryRestrictions)));
            } else {
                finalSelector = typeSelector;
            }
        } else {
            if (isClearingAdmin) {
                innerRestrictions.add(buAndModerator_visibility_Selector);
            } else {
                innerRestrictions.add(isUserBelongToBuAndModerator);
            }
            // If there are subQuery, then "and" them other restrictions
            if (!subQueryRestrictions.isEmpty()) {
                finalSelector = and(List.of(
                        typeSelector,
                        getQueryFromRestrictions(subQueryRestrictions),
                        or(innerRestrictions)
                ));
            } else {
                finalSelector = and(List.of(typeSelector, or(innerRestrictions)));
            }
        }
        return finalSelector;
    }

    private static @NotNull Map<String, String> getSortSelector(PaginationData pageData) {
        final boolean ascending = pageData.isAscending();
        return switch (ProjectSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case ProjectSortColumn.BY_DESCRIPTION ->
                    Collections.singletonMap("description", ascending ? "asc" : "desc");
            case ProjectSortColumn.BY_RESPONSIBLE ->
                    Collections.singletonMap("projectResponsible", ascending ? "asc" : "desc");
            case ProjectSortColumn.BY_STATE ->
                    Collections.singletonMap("state", ascending ? "asc" : "desc");
            case ProjectSortColumn.BY_CREATEDON ->
                    Collections.singletonMap("createdOn", ascending ? "asc" : "desc");
            case null, default ->
                    Collections.singletonMap("name", ascending ? "asc" : "desc"); // Default sort by name
        };
    }

    private Map<String, Object> getQueryFromRestrictions(Map<String, Set<String>> subQueryRestrictions) {
        List<Map<String, Object>> andConditions = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : subQueryRestrictions.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (Project._Fields.ADDITIONAL_DATA.getFieldName().equals(entry.getKey())) {
                    andConditions.add(all(entry.getKey(), entry.getValue().stream().toList()));
                } else if (!entry.getValue().stream().findFirst().orElse("").isEmpty()) {
                    andConditions.add(eq(entry.getKey(), entry.getValue().stream().findFirst().get()));
                }
            }
        }
        return and(andConditions);
    }
}
