/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.helper.ProjectHelper;
import org.eclipse.sw360.exporter.helper.ProjectHelper.ProjectByIdsLoader;
import org.eclipse.sw360.exporter.helper.ReleaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectExporter extends ExcelExporter<Project, ProjectHelper> {

    private static final Map<String, String> nameToDisplayName;

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put("id", "project ID");
        nameToDisplayName.put("name", "project name");
        nameToDisplayName.put("state", "project state");
        nameToDisplayName.put("createdBy", "created by");
        nameToDisplayName.put("createdOn", "creation date");
        nameToDisplayName.put("projectResponsible", "project responsible");
        nameToDisplayName.put("leadArchitect", "project lead architect");
        nameToDisplayName.put("tag", "project tag");
        nameToDisplayName.put("businessUnit", "group");
        nameToDisplayName.put("releaseClearingStateSummary", "release clearing state summary");
        nameToDisplayName.put("visbility", "visibility");
        nameToDisplayName.put("projectType", "project type");
        nameToDisplayName.put("linkedProjects", "linked projects with relationship");
        nameToDisplayName.put("releaseIdToUsage", "releases with usage");
        nameToDisplayName.put("clearingTeam", "clearing team");
        nameToDisplayName.put("preevaluationDeadline", "pre-evaluation deadline");
        nameToDisplayName.put("systemTestStart", "system test start");
        nameToDisplayName.put("systemTestEnd", "system test end");
        nameToDisplayName.put("deliveryStart", "delivery start");
        nameToDisplayName.put("phaseOutSince", "phase out since");
        nameToDisplayName.put("projectOwner", "project owner");
        nameToDisplayName.put("ownerAccountingUnit", "owner accounting unit");
        nameToDisplayName.put("ownerGroup", "owner group");
        nameToDisplayName.put("ownerCountry", "owner country");
        nameToDisplayName.put("vendorId", "vendor id");
        nameToDisplayName.put("securityResponsibles", "security responsibles");
        nameToDisplayName.put("createdOn", "created on");
        nameToDisplayName.put("enableSvm", "enable svm");
        nameToDisplayName.put("externalIds", "external IDs");
        nameToDisplayName.put("additionalData", "additional data");
        nameToDisplayName.put("visbility", "Project visibility");
    }

    public static final List<FieldAccessor<Project>> PROJECT_RENDERED_FIELDS = ExporterFields.only(
            Project.class,
            "name",
            "id",
            "version",
            "businessUnit",
            "projectType",
            "tag",
            "clearingState",
            "projectResponsible",
            "projectOwner",
            "securityResponsibles",
            "createdOn",
            "enableSvm",
            "externalIds",
            "additionalData",
            "visbility");

    public static List<String> HEADERS = PROJECT_RENDERED_FIELDS
            .stream()
            .map(FieldAccessor::name)
            .map(n -> SW360Utils.displayNameFor(n, nameToDisplayName))
            .collect(Collectors.toList());

    public static List<String> HEADERS_EXTENDED_BY_RELEASES = ExporterHelper.addSubheadersWithPrefixesAsNeeded(HEADERS, ReleaseExporter.RELEASE_HEADERS_PROJECT_EXPORT, "release: ");

    public ProjectExporter(ComponentExportClient componentClient, ProjectByIdsLoader projectLoader, User user,
            List<Project> projects, boolean extendedByReleases) throws SW360Exception {
        super(new ProjectHelper(projectLoader, user, extendedByReleases, new ReleaseHelper(componentClient, user)));
        preloadRelatedDataFor(projects, extendedByReleases, user);
    }

    public ProjectExporter(ComponentExportClient componentClient, ProjectByIdsLoader projectLoader, User user,
            boolean extendedByReleases) throws SW360Exception {
        super(new ProjectHelper(projectLoader, user, extendedByReleases, new ReleaseHelper(componentClient, user)));
    }

    private void preloadRelatedDataFor(List<Project> projects, boolean withLinkedOfLinked, User user) throws SW360Exception {
        Function<Function<Project, Map<String, ?>>, Set<String>> extractIds = mapExtractor -> projects
                .stream()
                .map(mapExtractor)
                .filter(Objects::nonNull)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Set<String> linkedProjectIds = extractIds.apply(Project::getLinkedProjects);
        Map<String, Project> projectsById = ThriftUtils.getIdMap(helper.getProjects(linkedProjectIds, user));
        helper.setPreloadedLinkedProjects(projectsById);

        Set<String> linkedReleaseIds = extractIds.apply(Project::getReleaseIdToUsage);
        preloadLinkedReleases(linkedReleaseIds, withLinkedOfLinked);
    }

    private void preloadLinkedReleases(Set<String> linkedReleaseIds, boolean withLinkedOfLinked) throws SW360Exception {
        Map<String, Release> releasesById = ThriftUtils.getIdMap(helper.getReleases(linkedReleaseIds));
        if (withLinkedOfLinked) {
            Set<String> linkedOfLinkedReleaseIds = releasesById
                    .values()
                    .stream()
                    .map(Release::getReleaseIdToRelationship)
                    .filter(Objects::nonNull)
                    .map(Map::keySet)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            Map<String, Release> joinedMap = new HashMap<>();
            Map<String, Release> linkedOfLinkedReleasesById = ThriftUtils.getIdMap(helper.getReleases(linkedOfLinkedReleaseIds));
            joinedMap.putAll(releasesById);
            joinedMap.putAll(linkedOfLinkedReleasesById);
            releasesById = joinedMap;
        }
        helper.setPreloadedLinkedReleases(releasesById, withLinkedOfLinked);
    }

}
