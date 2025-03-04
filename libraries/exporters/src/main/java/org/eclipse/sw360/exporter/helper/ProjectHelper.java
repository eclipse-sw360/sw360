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
package org.eclipse.sw360.exporter.helper;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.utils.SubTable;

import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.putProjectNamesInMap;
import static org.eclipse.sw360.datahandler.common.SW360Utils.putAccessibleReleaseNamesInMap;
import static org.eclipse.sw360.exporter.ProjectExporter.HEADERS;
import static org.eclipse.sw360.exporter.ProjectExporter.HEADERS_EXTENDED_BY_RELEASES;
import static org.eclipse.sw360.exporter.ProjectExporter.HEADERS_EXTENDED_BY_PACKAGES;
import static org.eclipse.sw360.exporter.ProjectExporter.PROJECT_RENDERED_FIELDS;
import static org.eclipse.sw360.exporter.helper.PackageHelper.RELEASES_LINKED_TO_PACKAGES;

public class ProjectHelper implements ExporterHelper<Project> {

    private static final String RELEASE_NOT_AVAILABLE = "Release not available";
    private static final String PACKAGE_NOT_AVAILABLE = "Package not available";
    private final ProjectService.Iface projectClient;
    private final User user;
    private boolean extendedByReleases;
    private boolean extendedByPackages;
    private ReleaseHelper releaseHelper;
    private PackageHelper packageHelper;
    private Map<String, Project> preloadedLinkedProjects;

    public ProjectHelper(ProjectService.Iface projectClient, User user, boolean extendedByReleases, ReleaseHelper releaseHelper) {
        this.projectClient = projectClient;
        this.user = user;
        this.extendedByReleases = extendedByReleases;
        this.releaseHelper = releaseHelper;
    }

    public ProjectHelper(ProjectService.Iface projectClient, User user, boolean extendedByPackages, PackageHelper packageHelper, ReleaseHelper releaseHelper) {
        this.projectClient = projectClient;
        this.user = user;
        this.extendedByPackages = extendedByPackages;
        this.packageHelper = packageHelper;
        this.releaseHelper = releaseHelper;
    }


    @Override
    public int getColumns() {
        return getHeaders().size();
    }

    @Override
    public List<String> getHeaders() {
        if(extendedByPackages) return HEADERS_EXTENDED_BY_PACKAGES;
        return extendedByReleases ? HEADERS_EXTENDED_BY_RELEASES : HEADERS;
    }

    @Override
    public SubTable makeRows(Project project) throws SW360Exception {
        if(extendedByPackages) return makeRowsWithPackages(project);
        return extendedByReleases ? makeRowsWithReleases(project) : makeRowForProjectOnly(project);
    }

    private SubTable makeRowsWithReleases(Project project) throws SW360Exception {
        List<Release> releases = getReleases(project);
        SubTable table = new SubTable();
        Set<String> releaseIdsNotAvaialbleInDB = Sets.difference(nullToEmptyMap(project.getReleaseIdToUsage()).keySet(),
                releases.stream().map(Release::getId).collect(Collectors.toSet()));

        if (releases.size() > 0) {
            for (Release release : releases) {
                List<String> currentRow = makeRowForProject(project);
                currentRow.addAll(releaseHelper.makeCustomRowsForProjectExport(release).getRow(0));
                table.addRow(currentRow);
            }
            for (String releaseId : releaseIdsNotAvaialbleInDB) {
                List<String> projRowWithNotAvailableReleaseFields = makeRowForProject(project);
                for (int i = 0; i < releaseHelper.getColumnsProjExport(); i++) {
                    if (i == 0) {
                        projRowWithNotAvailableReleaseFields.add(releaseId);
                        continue;
                    }
                    projRowWithNotAvailableReleaseFields.add(RELEASE_NOT_AVAILABLE);
                }
                table.addRow(projRowWithNotAvailableReleaseFields);
            }
        } else {
            List<String> projectRowWithEmptyReleaseFields = makeRowForProject(project);
            for (int i = 0; i < releaseHelper.getColumnsProjExport(); i++) {
                projectRowWithEmptyReleaseFields.add("");
            }
            table.addRow(projectRowWithEmptyReleaseFields);
        }
        return table;
    }

    private SubTable makeRowsWithPackages(Project project) throws SW360Exception{
        List<Package> packages = getPackages(project);
        SubTable table = new SubTable();
        Set<String> packageIdsNotAvailableInDB = Sets.difference(nullToEmptySet(project.getPackageIds()), packages.stream().map(Package::getId).collect(Collectors.toSet()));
        if(packages.size() > 0){
            for(Package pack : packages){
                List<String> currentRow = makeRowForProject(project);
                currentRow.addAll(packageHelper.makeCustomRowsForProjectExport(pack).getRow(0));
                table.addRow(currentRow);
            }

            addReleaseInformation(project, table);

            for(String packageId : packageIdsNotAvailableInDB){
                List<String> projRowWithNotAvailablePackageFields = makeRowForProject(project);
                for(int i = 0; i < packageHelper.getColumns(); i++){
                    if(i == 0){
                        projRowWithNotAvailablePackageFields.add(packageId);
                        continue;
                    }
                    projRowWithNotAvailablePackageFields.add(PACKAGE_NOT_AVAILABLE);
                }
                table.addRow(projRowWithNotAvailablePackageFields);
            }
        } else {
            List<String> projectRowWithEmptyPackageFields = makeRowForProject(project);
            for(int i = 0; i < packageHelper.getColumnsProjExport(); i++){
                projectRowWithEmptyPackageFields.add("");
            }
            table.addRow(projectRowWithEmptyPackageFields);

            addReleaseInformation(project, table);
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
                    ProjectReleaseRelationship inaccessibleRelationship = new ProjectReleaseRelationship();
                    row.add(fieldValueAsString(putAccessibleReleaseNamesInMap(project.releaseIdToUsage, getReleases(project), user, inaccessibleRelationship)));
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

    public List<Release> getReleases(Project project) throws SW360Exception {
        return getReleases(nullToEmptyMap(project.getReleaseIdToUsage()).keySet());
    }
    public List<Release> getReleases(Set<String> ids) throws SW360Exception {
        return releaseHelper.getReleases(ids);
    }

    public List<Package> getPackages(Project project) throws SW360Exception {
        return getPackages(project.getPackageIds());
    }
    public List<Package> getPackages(Set<String> ids) throws SW360Exception {
        return packageHelper.getPackages(ids);
    }


    public List<Project> getProjects(Set<String> ids, User user) throws SW360Exception {
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

    public void setPreloadedLinkedProjects(Map<String, Project> preloadedLinkedProjects) {
        this.preloadedLinkedProjects = preloadedLinkedProjects;
    }

    private void addReleaseInformation(Project project, SubTable table) throws SW360Exception {
        List<Release> leftoverReleases = getReleases(project).stream()
                .filter(release -> !RELEASES_LINKED_TO_PACKAGES.contains(release.getId()))
                .toList();

        for(Release release : leftoverReleases) {
            List<String> projRow = makeRowForProject(project);
            for(int i = 0; i < 5; i++) {
                projRow.add("");
            }
            projRow.addAll(releaseHelper.makeCustomRowsForProjectExport(release).getRow(0));
            table.addRow(projRow);
        }
    }
}
