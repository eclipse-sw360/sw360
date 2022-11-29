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

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.helper.ProjectHelper;
import org.eclipse.sw360.exporter.helper.ReleaseHelper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.thrift.projects.Project._Fields.*;

public class ProjectExporter extends ExcelExporter<Project, ProjectHelper> {

    private static final Map<String, String> nameToDisplayName;

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put(ID.getFieldName(), "project ID");
        nameToDisplayName.put(NAME.getFieldName(), "project name");
        nameToDisplayName.put(STATE.getFieldName(), "project state");
        nameToDisplayName.put(CREATED_BY.getFieldName(), "created by");
        nameToDisplayName.put(CREATED_ON.getFieldName(), "creation date");
        nameToDisplayName.put(PROJECT_RESPONSIBLE.getFieldName(), "project responsible");
        nameToDisplayName.put(LEAD_ARCHITECT.getFieldName(), "project lead architect");
        nameToDisplayName.put(TAG.getFieldName(), "project tag");
        nameToDisplayName.put(BUSINESS_UNIT.getFieldName(), "group");
        nameToDisplayName.put(RELEASE_CLEARING_STATE_SUMMARY.getFieldName(), "release clearing state summary");
        nameToDisplayName.put(EXTERNAL_IDS.getFieldName(), "external IDs");
        nameToDisplayName.put(VISBILITY.getFieldName(), "visibility");
        nameToDisplayName.put(PROJECT_TYPE.getFieldName(), "project type");
        nameToDisplayName.put(LINKED_PROJECTS.getFieldName(), "linked projects with relationship");
        nameToDisplayName.put(RELEASE_ID_TO_USAGE.getFieldName(), "releases with usage");
        nameToDisplayName.put(CLEARING_TEAM.getFieldName(), "clearing team");
        nameToDisplayName.put(PREEVALUATION_DEADLINE.getFieldName(), "pre-evaluation deadline");
        nameToDisplayName.put(SYSTEM_TEST_START.getFieldName(), "system test start");
        nameToDisplayName.put(SYSTEM_TEST_END.getFieldName(), "system test end");
        nameToDisplayName.put(DELIVERY_START.getFieldName(), "delivery start");
        nameToDisplayName.put(PHASE_OUT_SINCE.getFieldName(), "phase out since");
        nameToDisplayName.put(PROJECT_OWNER.getFieldName(), "project owner");
        nameToDisplayName.put(OWNER_ACCOUNTING_UNIT.getFieldName(), "owner accounting unit");
        nameToDisplayName.put(OWNER_GROUP.getFieldName(), "owner group");
        nameToDisplayName.put(OWNER_COUNTRY.getFieldName(), "owner country");
        nameToDisplayName.put(VENDOR_ID.getFieldName(), "vendor id");
        nameToDisplayName.put(SECURITY_RESPONSIBLES.getFieldName(), "security responsibles");
        nameToDisplayName.put(ENABLE_VULNERABILITIES_DISPLAY.getFieldName(), "enable vulnerabilities display");
    }

    private static final List<Project._Fields> PROJECT_REQUIRED_FIELDS = ImmutableList.<Project._Fields>builder()
            .add(NAME)
            .add(VERSION)
            .add(BUSINESS_UNIT)
            .add(PROJECT_TYPE)
            .add(TAG)
            .add(CLEARING_STATE)
            .add(PROJECT_RESPONSIBLE)
            .add(PROJECT_OWNER)
            .add(SECURITY_RESPONSIBLES)
            .add(ENABLE_VULNERABILITIES_DISPLAY)
            .build();

    public static final List<Project._Fields> PROJECT_RENDERED_FIELDS = Project.metaDataMap.keySet()
            .stream()
            .filter(k -> PROJECT_REQUIRED_FIELDS.contains(k))
            .collect(Collectors.toList());

    public static List<String> HEADERS = PROJECT_RENDERED_FIELDS
            .stream()
            .map(Project._Fields::getFieldName)
            .map(n -> SW360Utils.displayNameFor(n, nameToDisplayName))
            .collect(Collectors.toList());

    public static List<String> HEADERS_EXTENDED_BY_RELEASES = ExporterHelper.addSubheadersWithPrefixesAsNeeded(HEADERS, ReleaseExporter.RELEASE_HEADERS_PROJECT_EXPORT, "release: ");

    public ProjectExporter(ComponentService.Iface componentClient, ProjectService.Iface projectClient, User user, List<Project> projects, boolean extendedByReleases) throws SW360Exception {
        super(new ProjectHelper(projectClient, user, extendedByReleases, new ReleaseHelper(componentClient, user)));
        preloadRelatedDataFor(projects, extendedByReleases, user);
    }

    public ProjectExporter(ComponentService.Iface componentClient, ProjectService.Iface projectClient, User user,
            boolean extendedByReleases) throws SW360Exception {
        super(new ProjectHelper(projectClient, user, extendedByReleases, new ReleaseHelper(componentClient, user)));
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
