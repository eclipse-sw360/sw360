/*
 * Copyright (c) Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.thrift;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.wsimport.utility.TranslationConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus.DUPLICATE;
import static org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus.SUCCESS;

/**
 * @author: ksoranko@verifa.io
 */
public class ThriftExchange {

    private static final Logger LOGGER = LogManager.getLogger(ThriftExchange.class);
    private static ThriftClients thriftClients = new ThriftClients();

    //public ThriftExchange(ThriftClients thriftClients) {
    //    this.thriftClients = thriftClients;
    //}

    /**
     * Add the Project to DB. Required fields are: name.
     *
     * @param project Project to be added
     * @param user
     * @return projectId-String from DB
     */
    public String addProject(Project project, User user) {
        String projectId = null;
        try {
            AddDocumentRequestSummary summary = thriftClients.makeProjectClient().addProject(project, user);
            if (SUCCESS.equals(summary.getRequestStatus())) {
                projectId = summary.getId();
            } else {
                logFailedAddDocument(summary.getRequestStatus(), "project");
            }
        } catch (TException e) {
            LOGGER.error("Could not add Project for user with email=[" + user.getEmail() + "]:" + e);
        }
        return projectId;
    }

    /**
     * Add the Component to DB. Required fields are: name.
     *
     * @param component Component to be added
     * @param user
     * @return ComponentId-String from DB.
     */
    public String addComponent(Component component, User user) {
        String componentId = null;
        try {
            AddDocumentRequestSummary summary = thriftClients.makeComponentClient().addComponent(component, user);
            if (SUCCESS.equals(summary.getRequestStatus())) {
                componentId = summary.getId();
            } else {
                logFailedAddDocument(summary.getRequestStatus(), "component");
            }
        } catch (TException e) {
            LOGGER.error("Could not add Component for user with email=[" + user.getEmail() + "]:" + e);
        }
        return componentId;
    }

    /**
     * Add the Release to DB. Required fields are: name, version, componentId.
     *
     * @param release Release to be added
     * @param user
     * @return releaseId-String from DB.
     */
    public String addRelease(Release release, User user) {
        String releaseId = null;
        try {
            AddDocumentRequestSummary summary = thriftClients.makeComponentClient().addRelease(release, user);
            if (SUCCESS.equals(summary.getRequestStatus())) {
                releaseId = summary.getId();
            } else {
                logFailedAddDocument(summary.getRequestStatus(), "release");
            }
        } catch (TException e) {
            LOGGER.error("Could not add Release for user with email=[" + user.getEmail() + "]:" + e);
        }
        return releaseId;
    }

    /**
     * Add the License to DB.
     *
     * @param license
     * @param user
     * @return license-String from DB
     */
    public String addLicense(License license, User user) {
        LicenseService.Iface  client = thriftClients.makeLicenseClient();
        List<License> licenses = null;
        try {
            licenses = client.addLicenses(Collections.singletonList(license), user);
        } catch (TException e) {
            LOGGER.error("Could not add License for user with email=[" + user.getEmail() + "]:" + e);
        }
        if (licenses != null && licenses.get(0) != null) {
            return licenses.get(0).getId();
        } else {
            return null;
        }
    }

    public Optional<List<Release>> searchReleaseByNameAndVersion(String name, String version) {
        List<Release> releases = null;
        try {
            releases = thriftClients.makeComponentClient().searchReleaseByNamePrefix(name);
        } catch (TException e) {
            LOGGER.error("Could not fetch Release list for name=[" + name + "], version=[" + version + "]:" + e);
        }

        if (releases != null) {
            return Optional.of(releases.stream()
                    .filter(r -> r.getVersion().equals(version))
                    .collect(Collectors.toList()));
        } else {
            return Optional.empty();
        }
    }

    public Optional<List<Component>> searchComponentByName(String name) {
        try {
            return Optional.of(thriftClients.makeComponentClient().searchComponentForExport(name));
        } catch (TException e) {
            LOGGER.error("Could not fetch Component list for name=[" + name + "]:" + e);
            return Optional.empty();
        }
    }

    public Optional<List<License>> searchLicenseByWsName(String wsName) {
        return getFilteredLicenseList(license ->
                        license.isSetExternalIds() && CommonUtils.nullToEmptyString(license.getExternalIds().get(TranslationConstants.WS_ID)).equals(wsName),
                "wsId=[" + wsName + "]:"
        );
    }

    boolean projectExists(int wsProjectId, String wsProjectName, User user) throws TException {
        List<Project> accessibleProjects = getAccessibleProjectsSummary(user);
        if (hasAccessibleProjectWithWsToken(wsProjectId, accessibleProjects)) {
            LOGGER.info("Project to import was already imported with wsId: " + wsProjectId);
            return true;
        }
        if (hasAccessibleProjectWithWsName(wsProjectName, accessibleProjects)) {
            LOGGER.info("Project to import already exists in the DB with name: " + wsProjectName);
            return true;
        }
        return false;
    }

    private void logFailedAddDocument(AddDocumentRequestStatus status, String documentTypeString) {
        if (DUPLICATE.equals(status)) {
            LOGGER.error("Could not add duplicate " + documentTypeString + ".");
        } else {
            LOGGER.error("Adding the " + documentTypeString + "failed.");
        }
    }

    private Optional<List<License>> getFilteredLicenseList(Predicate<License> filter, String selector) {
        try {
            return Optional.of(thriftClients.makeLicenseClient()
                    .getLicenses()
                    .stream()
                    .filter(filter)
                    .collect(Collectors.toList()));
        } catch (TException e) {
            LOGGER.error("Could not fetch License list for " + selector + ": " + e);
            return Optional.empty();
        }
    }

    private List<Project> getAccessibleProjectsSummary(User user) {
        List<Project> accessibleProjectsSummary = null;
        try {
            accessibleProjectsSummary = thriftClients.makeProjectClient().getAccessibleProjectsSummary(user);
        } catch (TException e) {
            LOGGER.error("Could not fetch Project list for user with email=[" + user.getEmail() + "]:" + e);
        }
        return nullToEmptyList(accessibleProjectsSummary);
    }

    private boolean hasAccessibleProjectWithWsToken(int wsProjectId, List<Project> accessibleProjects) {
        return accessibleProjects.stream()
                .filter(Project::isSetExternalIds)
                .anyMatch(project -> Integer.toString(wsProjectId).equals(project.getExternalIds().get(TranslationConstants.WS_ID)));
    }

    private boolean hasAccessibleProjectWithWsName(String wsProjectName, List<Project> accessibleProjects) {
        return accessibleProjects.stream()
                .anyMatch(project -> wsProjectName.equals(project.getName()));
    }
}
