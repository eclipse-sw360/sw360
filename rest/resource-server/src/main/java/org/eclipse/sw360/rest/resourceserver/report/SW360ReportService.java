/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.report;

import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.ReleaseExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SW360ReportService {

    ThriftClients thriftClients = new ThriftClients();
    ProjectService.Iface projectclient = thriftClients.makeProjectClient();
    ComponentService.Iface componentclient = thriftClients.makeComponentClient();
    LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

    public ByteBuffer getProjectBuffer(User user, boolean extendedByReleases, String projectId) throws TException {
        if (projectId != null && !validateProject(projectId, user)) {
            throw new TException("No project record found for the project Id : " + projectId);
        }
        return projectclient.getReportDataStream(user, extendedByReleases, projectId);
    }

    private boolean validateProject(String projectId, User user) throws TException {
        boolean validProject = true;
        try {
            Project project = projectclient.getProjectById(projectId, user);
            if (project == null) {
                return false;
            }
        } catch (Exception e) {
            validProject = false;
        }
        return validProject;
    }

    public String getDocumentName(User user, String projectId, String module) throws TException {
        String documentName = String.format("projects-%s.xlsx", SW360Utils.getCreatedOn());
        switch (module) {
        case SW360Constants.PROJECTS:
            if (projectId != null && !projectId.equalsIgnoreCase("null")) {
                Project project = projectclient.getProjectById(projectId, user);
                documentName = String.format("project-%s-%s-%s.xlsx", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOn());
            }
            break;
        case SW360Constants.LICENSES:
            documentName = String.format("licenses-%s.xlsx", SW360Utils.getCreatedOn());
            break;
        case SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO:
            if (projectId != null && !projectId.equalsIgnoreCase("null")) {
                Project project = projectclient.getProjectById(projectId, user);
                documentName = String.format("releases-%s-%s-%s.xlsx", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOn());
            }
            break;
        default:
            break;
        }
        return documentName;
    }

    public void getUploadedProjectPath(User user, boolean withLinkedReleases, String base, String projectId)
            throws TException {
        if (projectId!=null && !validateProject(projectId, user)) {
            throw new TException("No project record found for the project Id : " + projectId);
        }
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                String projectPath = projectclient.getReportInEmail(user, withLinkedReleases, projectId);
                String backendURL = base + "api/reports/download?user=" + user.getEmail() + "&module=projects"
                        + "&extendedByReleases=" + withLinkedReleases + "&projectId=" + projectId + "&token=";
                URL emailURL = new URL(backendURL + projectPath);
                if (!CommonUtils.isNullEmptyOrWhitespace(projectPath)) {
                    sendExportSpreadsheetSuccessMail(emailURL.toString(), user.getEmail());
                }
            } catch (Exception exp) {
                throw new TException(exp.getMessage());
            }
        });
        Thread asyncThread = new Thread(asyncRunnable);
        asyncThread.start();
    }

    public ByteBuffer getReportStreamFromURl(User user, boolean extendedByReleases, String token) throws TException {
        return projectclient.downloadExcel(user, extendedByReleases, token);
    }

    public void sendExportSpreadsheetSuccessMail(String emailURL, String email) throws TException {
        projectclient.sendExportSpreadsheetSuccessMail(emailURL, email);
    }

    public void getUploadedComponentPath(User sw360User, boolean withLinkedReleases, String base) {
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                String componentPath = componentclient.getComponentReportInEmail(sw360User, withLinkedReleases);
                String backendURL = base + "api/reports/download?user=" + sw360User.getEmail() + "&module=components"
                        + "&extendedByReleases=" + withLinkedReleases + "&token=";
                URL emailURL = new URL(backendURL + componentPath);
                if (!CommonUtils.isNullEmptyOrWhitespace(componentPath)) {
                    sendComponentExportSpreadsheetSuccessMail(emailURL.toString(), sw360User.getEmail());
                }
            } catch (Exception exp) {
                throw new TException(exp.getMessage());
            }
        });
        Thread asyncThread = new Thread(asyncRunnable);
        asyncThread.start();
    }

    public ByteBuffer getComponentBuffer(User sw360User, boolean withLinkedReleases) throws TException {
        return componentclient.getComponentReportDataStream(sw360User, withLinkedReleases);
    }

    public ByteBuffer getLicenseBuffer() throws TException {
        return licenseClient.getLicenseReportDataStream();
    }

    public ByteBuffer getComponentReportStreamFromURl(User user, boolean extendedByReleases, String token)
            throws TException {
        return componentclient.downloadExcel(user, extendedByReleases, token);
    }

    public ByteBuffer getLicenseReportStreamFromURl(String token)
            throws TException {
        return licenseClient.downloadExcel(token);
    }

    public void sendComponentExportSpreadsheetSuccessMail(String emailURL, String email) throws TException {
        componentclient.sendExportSpreadsheetSuccessMail(emailURL, email);
    }

    public ByteBuffer getProjectReleaseSpreadSheetWithEcc(User user, String projectId) throws TException, IOException {
        if (projectId == null || projectId.isEmpty() || !validateProject(projectId, user)) {
            throw new TException("No project record found for the project Id : " + projectId);
        }
        ReleaseExporter exporter = null;
        List<Release> releases = null;
        try {
            List<ReleaseClearingStatusData> releaseStringMap = projectclient
                    .getReleaseClearingStatusesWithAccessibility(projectId, user);
            releases = releaseStringMap.stream().map(ReleaseClearingStatusData::getRelease)
                    .sorted(Comparator.comparing(SW360Utils::printFullname)).collect(Collectors.toList());
            exporter = new ReleaseExporter(componentclient, releases, user, releaseStringMap);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
        return ByteBuffer.wrap(IOUtils.toByteArray(exporter.makeExcelExport(releases)));
    }
}