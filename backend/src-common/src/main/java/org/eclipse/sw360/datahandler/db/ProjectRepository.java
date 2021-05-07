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
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;

import com.cloudant.client.api.model.DesignDocument.MapReduce;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getBUFromOrganisation;

/**
 * CRUD access for the Project class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author thomas.maier@evosoft.com
 * @author ksoranko@verifa.io
 */
public class ProjectRepository extends SummaryAwareRepository<Project> {
    private static final String ALL = "function(doc) { if (doc.type == 'project') emit(null, doc._id) }";

    private static final String MY_PROJECTS_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    if(doc.createdBy)" +
                    "      emit(doc.createdBy, doc._id);" +
                    "    if(doc.leadArchitect)" +
                    "      emit(doc.leadArchitect, doc._id);" +
                    "    for(var i in doc.moderators) {" +
                    "      emit(doc.moderators[i], doc._id);" +
                    "    }" +
                    "    for(var i in doc.contributors) {" +
                    "      emit(doc.contributors[i], doc._id);" +
                    "    }" +
                    "  }" +
                    "}";

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
                    "      emit(i,doc);\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";


    private static final String BU_PROJECTS_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.businessUnit, doc._id);" +
                    "  }" +
                    "}";

    private static final String FULL_BU_PROJECTS_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.businessUnit, doc);" +
                    "  }" +
                    "}";

    private static final String BY_NAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'project') {" +
                    "    emit(doc.name, doc._id);" +
                    "  }" +
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
                    "      emit(i, doc);" +
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

    private static final String FULL_BY_LINKING_PROJECT_ID_VIEW =
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

    public ProjectRepository(DatabaseConnectorCloudant db) {
        super(Project.class, db, new ProjectSummary());
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("byname", createMapReduce(BY_NAME_VIEW, null));
        views.put("byreleaseid", createMapReduce(BY_RELEASE_ID_VIEW, null));
        views.put("fullbyreleaseid", createMapReduce(FULL_BY_RELEASE_ID_VIEW, null));
        views.put("bylinkingprojectid", createMapReduce(BY_LINKING_PROJECT_ID_VIEW, null));
        views.put("fullbylinkingprojectid", createMapReduce(FULL_BY_LINKING_PROJECT_ID_VIEW, null));
        views.put("myprojects", createMapReduce(MY_PROJECTS_VIEW, null));
        views.put("fullmyprojects", createMapReduce(FULL_MY_PROJECTS_VIEW, null));
        views.put("buprojects", createMapReduce(BU_PROJECTS_VIEW, null));
        views.put("fullbuprojects", createMapReduce(FULL_BU_PROJECTS_VIEW, null));
        views.put("byexternalids", createMapReduce(BY_EXTERNAL_IDS, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
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

    public Set<Project> searchByReleaseId(String id) {
        return new HashSet<>(queryView("fullbyreleaseid", id));
    }

    public Set<Project> searchByReleaseId(Set<String> ids) {
        return new HashSet<>(queryByIds("fullbyreleaseid", ids));
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

    private Set<String> getMyProjectsIds(String user) {
        return queryForIds("myprojects", user);
    }

    private Set<Project> getMyProjects(String user) {
        return new HashSet<Project>(queryView("fullmyprojects", user));
    }

    public List<Project> getMyProjectsSummary(String user) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, getMyProjects(user));
    }

    public List<Project> getMyProjectsFull(String user) {
        return new ArrayList<>(getMyProjects(user));
    }

    private Set<String> getBUProjectsIds(String organisation) {
        // Filter BU to first three blocks
        String bu = getBUFromOrganisation(organisation);
        return queryForIdsByPrefix("buprojects", bu);
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


    public List<Project> getAccessibleProjectsSummary(User user) {
        return makeSummaryFromFullDocs(SummaryType.SUMMARY, getAccessibleProjects(user));
    }

    @NotNull
    public Set<Project> getAccessibleProjects(User user) {
        /** This implementation requires multiple DB requests and has its logic distributed in multiple places **/
//        final Set<Project> buProjects = new HashSet<>(getBUProjects(organisation));
//        final Set<Project> myProjects = getMyProjects(user);
//        return Sets.union(buProjects, myProjects);

        /** I have only one day left in the project so I try this, but if there is time this should be reviewed
         *  The ideal solution would be to make a smarter query with a combined key, but I do not know how easy
         *  this is to refactor if say an enum value gets renamed...
         * **/
        final List<Project> all = getAll();
        return all.stream().filter(ProjectPermissions.isVisible(user)::test).collect(Collectors.toSet());
    }

    public List<Project> searchByName(String name, User user) {
        return searchByName(name, user, SummaryType.SUMMARY);
    }

    @NotNull
    private Set<Project> filterAccessibleProjectsByIds(User user, Set<String> searchIds) {
        final Set<Project> accessibleProjects = getAccessibleProjects(user);

        final Set<Project> output = new HashSet<>();
        for (Project accessibleProject : accessibleProjects) {
            if (searchIds.contains(accessibleProject.getId())) output.add(accessibleProject);
        }

        return output;
    }

    private Set<Project> getAccessibleProjectSummary(User user, Set<String> searchIds) {
        Set<Project> accessibleProjects = filterAccessibleProjectsByIds(user, searchIds);
        return new HashSet<>(makeSummaryFromFullDocs(SummaryType.LINKED_PROJECT_ACCESSIBLE, accessibleProjects));
    }
}
