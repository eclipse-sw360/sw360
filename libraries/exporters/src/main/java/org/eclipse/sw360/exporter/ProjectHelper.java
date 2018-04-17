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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.putProjectNamesInMap;
import static org.eclipse.sw360.datahandler.common.SW360Utils.putReleaseNamesInMap;
import static org.eclipse.sw360.exporter.ProjectExporter.HEADERS;
import static org.eclipse.sw360.exporter.ProjectExporter.HEADERS_EXTENDED_BY_RELEASES;
import static org.eclipse.sw360.exporter.ProjectExporter.PROJECT_RENDERED_FIELDS;

class ProjectHelper implements ExporterHelper<Project> {

    private final ProjectService.Iface projectClient;
    private final User user;
    private boolean extendedByReleases;
    private ReleaseHelper releaseHelper;
    private Map<String, Project> preloadedLinkedProjects;

    ProjectHelper(ProjectService.Iface projectClient, User user, boolean extendedByReleases, ReleaseHelper releaseHelper) {
        this.projectClient = projectClient;
        this.user = user;
        this.extendedByReleases = extendedByReleases;
        this.releaseHelper = releaseHelper;
    }

    @Override
    public int getColumns() {
        return getHeaders().size();
    }

    @Override
    public List<String> getHeaders() {
        return extendedByReleases ? HEADERS_EXTENDED_BY_RELEASES : HEADERS;
    }

    @Override
    public SubTable makeRows(Project project) throws SW360Exception {
        return extendedByReleases ? makeRowsWithReleases(project) : makeRowForProjectOnly(project);
    }

    private SubTable makeRowsWithReleases(Project project) throws SW360Exception {
        List<Release> releases = getReleases(project);
        SubTable table = new SubTable();

        if (releases.size() > 0) {
            for (Release release : releases) {
                List<String> currentRow = makeRowForProject(project);
                currentRow.addAll(releaseHelper.makeRows(release).elements.get(0));
                table.addRow(currentRow);
            }
        } else {
            List<String> projectRowWithEmptyReleaseFields = makeRowForProject(project);
            for (int i = 0; i < releaseHelper.getColumns(); i++) {
                projectRowWithEmptyReleaseFields.add("");
            }
            table.addRow(projectRowWithEmptyReleaseFields);
        }
        return table;
    }

    private List<String> makeRowForProject(Project project) throws SW360Exception {
        if (!project.isSetAttachments()) {
            project.setAttachments(Collections.emptySet());
        }
        List<String> row = new ArrayList<>(getColumns());
        for (Project._Fields renderedField : PROJECT_RENDERED_FIELDS) {
            addFieldValueToRow(row, renderedField, project);
        }

        return row;
    }

    private void addFieldValueToRow(List<String> row, Project._Fields field, Project project) throws SW360Exception {
        if (project.isSet(field)) {
            Object fieldValue = project.getFieldValue(field);
            switch (field) {
                case RELEASE_ID_TO_USAGE:
                    row.add(fieldValueAsString(putReleaseNamesInMap(project.releaseIdToUsage, getReleases(project))));
                    break;
                case LINKED_PROJECTS:
                    row.add(fieldValueAsString(putProjectNamesInMap(project.getLinkedProjects(), getProjects(project
                            .getLinkedProjects()
                            .keySet(), user))));
                    break;
                case ATTACHMENTS:
                    row.add(project.attachments.size() + "");
                    break;
                default:
                    row.add(fieldValueAsString(fieldValue));
            }
        } else {
            row.add("");
        }
    }

    private SubTable makeRowForProjectOnly(Project project) throws SW360Exception {
        return new SubTable(makeRowForProject(project));
    }

    public void setPreloadedLinkedReleases(Map<String, Release> preloadedLinkedReleases, boolean componentsNeeded)
            throws SW360Exception {
        releaseHelper.setPreloadedLinkedReleases(preloadedLinkedReleases, componentsNeeded);
    }

    List<Release> getReleases(Project project) throws SW360Exception {
        return getReleases(nullToEmptyMap(project.getReleaseIdToUsage()).keySet());
    }
    List<Release> getReleases(Set<String> ids) throws SW360Exception {
        return releaseHelper.getReleases(ids);
    }

    List<Project> getProjects(Set<String> ids, User user) throws SW360Exception {
        if (preloadedLinkedProjects != null) {
            return getPreloadedProjects(ids);
        }
        List<Project> projects;
        try {
            projects = projectClient.getProjectsById(new ArrayList<>(ids), user);
        } catch (TException e) {
            throw new SW360Exception("Error fetching linked projects");
        }
        return projects;
    }

    private List<Project> getPreloadedProjects(Set<String> ids) {
        return ids.stream().map(preloadedLinkedProjects::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    void setPreloadedLinkedProjects(Map<String, Project> preloadedLinkedProjects) {
        this.preloadedLinkedProjects = preloadedLinkedProjects;
    }
}
