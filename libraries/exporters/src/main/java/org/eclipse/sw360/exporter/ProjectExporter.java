/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.exporter;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.thrift.projects.Project._Fields.DOCUMENT_STATE;
import static org.eclipse.sw360.datahandler.thrift.projects.Project._Fields.PERMISSIONS;
import static org.eclipse.sw360.datahandler.thrift.projects.Project._Fields.REVISION;

public class ProjectExporter extends ExcelExporter<Project, ProjectHelper> {

    private static final Map<String, String> nameToDisplayName;

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put(Project._Fields.ID.getFieldName(), "project ID");
        nameToDisplayName.put(Project._Fields.NAME.getFieldName(), "project name");
        nameToDisplayName.put(Project._Fields.STATE.getFieldName(), "project state");
        nameToDisplayName.put(Project._Fields.CREATED_BY.getFieldName(), "created by");
        nameToDisplayName.put(Project._Fields.CREATED_ON.getFieldName(), "creation date");
        nameToDisplayName.put(Project._Fields.PROJECT_RESPONSIBLE.getFieldName(), "project responsible");
        nameToDisplayName.put(Project._Fields.LEAD_ARCHITECT.getFieldName(), "project lead architect");
        nameToDisplayName.put(Project._Fields.TAG.getFieldName(), "project tag");
        nameToDisplayName.put(Project._Fields.BUSINESS_UNIT.getFieldName(), "group");
        nameToDisplayName.put(Project._Fields.RELEASE_CLEARING_STATE_SUMMARY.getFieldName(), "release clearing state summary");
        nameToDisplayName.put(Project._Fields.EXTERNAL_IDS.getFieldName(), "external IDs");
        nameToDisplayName.put(Project._Fields.VISBILITY.getFieldName(), "visibility");
        nameToDisplayName.put(Project._Fields.PROJECT_TYPE.getFieldName(), "project type");
        nameToDisplayName.put(Project._Fields.LINKED_PROJECTS.getFieldName(), "linked projects with relationship");
        nameToDisplayName.put(Project._Fields.RELEASE_ID_TO_USAGE.getFieldName(), "releases with usage");
        nameToDisplayName.put(Project._Fields.CLEARING_TEAM.getFieldName(), "clearing team");
        nameToDisplayName.put(Project._Fields.PREEVALUATION_DEADLINE.getFieldName(), "pre-evaluation deadline");
        nameToDisplayName.put(Project._Fields.SYSTEM_TEST_START.getFieldName(), "system test start");
        nameToDisplayName.put(Project._Fields.SYSTEM_TEST_END.getFieldName(), "system test end");
        nameToDisplayName.put(Project._Fields.DELIVERY_START.getFieldName(), "delivery start");
        nameToDisplayName.put(Project._Fields.PHASE_OUT_SINCE.getFieldName(), "phase out since");
        nameToDisplayName.put(Project._Fields.PROJECT_OWNER.getFieldName(), "project owner");
        nameToDisplayName.put(Project._Fields.OWNER_ACCOUNTING_UNIT.getFieldName(), "owner accounting unit");
        nameToDisplayName.put(Project._Fields.OWNER_GROUP.getFieldName(), "owner group");
        nameToDisplayName.put(Project._Fields.OWNER_COUNTRY.getFieldName(), "owner country");
    }

    private static final List<Project._Fields> PROJECT_IGNORED_FIELDS = ImmutableList.<Project._Fields>builder()
            .add(REVISION)
            .add(DOCUMENT_STATE)
            .add(PERMISSIONS)
            .build();

    public static final List<Project._Fields> PROJECT_RENDERED_FIELDS = Project.metaDataMap.keySet()
            .stream()
            .filter(k -> ! PROJECT_IGNORED_FIELDS.contains(k))
            .collect(Collectors.toList());

    static List<String> HEADERS = PROJECT_RENDERED_FIELDS
            .stream()
            .map(Project._Fields::getFieldName)
            .map(n -> SW360Utils.displayNameFor(n, nameToDisplayName))
            .collect(Collectors.toList());

    static List<String> HEADERS_EXTENDED_BY_RELEASES = ExporterHelper.addSubheadersWithPrefixesAsNeeded(HEADERS, ReleaseExporter.HEADERS, "release: ");

    public ProjectExporter(ComponentService.Iface componentClient, ProjectService.Iface projectClient, User user, List<Project> projects, boolean extendedByReleases) throws SW360Exception {
        super(new ProjectHelper(projectClient, user, extendedByReleases, new ReleaseHelper(componentClient, user)));
        preloadRelatedDataFor(projects, extendedByReleases, user);
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
