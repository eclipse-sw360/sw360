/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.report;

import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;

import java.net.URL;
import java.nio.ByteBuffer;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
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
        return projectclient.getReportDataStream(user, extendedByReleases, projectId);
    }

    public String getDocumentName(User user, String projectId) throws TException {
        if (projectId != null) {
            Project project = projectclient.getProjectById(projectId, user);
            return String.format("project-%s-%s-%s.xlsx", project.getName(), project.getVersion(), SW360Utils.getCreatedOn());
        }
        return String.format("projects-%s.xlsx", SW360Utils.getCreatedOn());
    }

    public void getUploadedProjectPath(User user, boolean withLinkedReleases, String base, String projectId){
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                String projectPath = projectclient.getReportInEmail(user, withLinkedReleases, projectId);
                String backendURL = base + "api/reports/download?user=" + user.getEmail() + "&module=projects"
                        + "&extendedByReleases=" + withLinkedReleases + "&token=";
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
}