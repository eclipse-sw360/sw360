/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.report;

import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.SourcePackageUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cloudant.client.api.model.Attachment;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SW360ReportService {

    ThriftClients thriftClients = new ThriftClients();
    ProjectService.Iface projectclient = thriftClients.makeProjectClient();
    ComponentService.Iface componentclient = thriftClients.makeComponentClient();
    LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
    AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

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

    public ByteBuffer getLicenseResourceBundleBuffer() throws TException {
        return licenseClient.getLicenseReportDataStream();
    }

    public ByteBuffer downloadSourceCodeBundle(String projectId, HttpServletRequest request, User sw360User)
            throws TException {
        Map<String, Set<String>> selectedReleaseAndAttachmentIds = getSelectedReleaseAndAttachmentIdsFromRequest(
                request, false);
        Set<String> selectedAttachmentIds = new HashSet<>();
        selectedReleaseAndAttachmentIds.forEach((key, value) -> selectedAttachmentIds.addAll(value));

        try {
            Project project = projectclient.getProjectById(projectId, sw360User);
            saveSourcePackageAttachmentUsages(project, sw360User, selectedReleaseAndAttachmentIds);
            List<AttachmentContent> attachments = new ArrayList<>();
            for (String id : selectedAttachmentIds) {
                attachments.add(attachmentClient.getAttachmentContent(id));
            }
            return serveAttachmentBundle(attachments, request, project, sw360User);
        } catch (TException e) {
            throw new TException(e.getMessage());
        }
    }

    public static Map<String, Set<String>> getSelectedReleaseAndAttachmentIdsFromRequest(HttpServletRequest request,
            boolean withPath) {
        Map<String, Set<String>> releaseIdToAttachmentIds = new HashMap<>();
        String[] checkboxes = request.getParameterValues("licenseInfoAttachmentSelected");
        if (checkboxes == null) {
            return ImmutableMap.of();
        }
        Arrays.stream(checkboxes).forEach(s -> {
            String[] split = s.split(":");
            if (split.length >= 2) {
                String attachmentId = split[split.length - 1];
                String releaseIdMaybeWithPath;
                if (withPath) {
                    releaseIdMaybeWithPath = Arrays.stream(Arrays.copyOf(split, split.length - 1))
                            .collect(Collectors.joining(":"));
                } else {
                    releaseIdMaybeWithPath = split[split.length - 2];
                }
                if (!releaseIdToAttachmentIds.containsKey(releaseIdMaybeWithPath)) {
                    releaseIdToAttachmentIds.put(releaseIdMaybeWithPath, new HashSet<>());
                }
                releaseIdToAttachmentIds.get(releaseIdMaybeWithPath).add(attachmentId);
            }
        });
        return releaseIdToAttachmentIds;
    }

    private void saveSourcePackageAttachmentUsages(Project project, User user,
            Map<String, Set<String>> selectedReleaseAndAttachmentIds) throws TException {
        try {
            Function<String, UsageData> usageDataGenerator = attachmentContentId -> UsageData
                    .sourcePackage(new SourcePackageUsage());
            List<AttachmentUsage> attachmentUsages = makeAttachmentUsages(project, selectedReleaseAndAttachmentIds,
                    usageDataGenerator);
            replaceAttachmentUsages(project, user, attachmentUsages, UsageData.sourcePackage(new SourcePackageUsage()));
        } catch (TException e) {
            throw new TException(e.getMessage());
        }
    }

    public String getSourceCodeBundleName(String projectId, User sw360User) throws TException {
        Project project = projectclient.getProjectById(projectId, sw360User);
        String timestamp = SW360Utils.getCreatedOn();
        return "SourceCodeBundle-" + project.getName() + "-" + timestamp + ".zip";
    }

    private ByteBuffer serveAttachmentBundle(List<AttachmentContent> attachments, HttpServletRequest request,
            Project project, User sw360User) throws TException {
        try {
            final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
            final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
            return getAttachmentBundleByteBuffer(attachmentStreamConnector, attachments, request, project, sw360User);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private ByteBuffer getAttachmentBundleByteBuffer(AttachmentStreamConnector attachmentStreamConnector,
            List<AttachmentContent> attachments, HttpServletRequest request, Project project, User sw360User)
            throws TException, IOException {
        String isAllAttachment = request.getParameter("isAllAttachmentSelected");
        InputStream stream = null;
        try {
            Optional<Object> context = getContextFromRequest(project);

            if (context.isPresent()) {
                if (StringUtils.isNotEmpty(isAllAttachment) && isAllAttachment.equalsIgnoreCase("true")) {
                    stream = getStreamToServeBundle(attachmentStreamConnector, attachments, sw360User, context);
                } else {
                    stream = getStreamToServeAFile(attachmentStreamConnector, attachments, sw360User, context);
                }
            }
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
        return ByteBuffer.wrap(IOUtils.toByteArray(stream));
    }

    private Optional<Object> getContextFromRequest(Project project) {
        return Optional.ofNullable(project);
    }

    private void replaceAttachmentUsages(Project project, User user, List<AttachmentUsage> attachmentUsages,
            UsageData defaultEmptyUsageData) throws TException {
        if (PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
            AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
            if (attachmentUsages.isEmpty()) {
                attachmentClient.deleteAttachmentUsagesByUsageDataType(Source.projectId(project.getId()),
                        defaultEmptyUsageData);
            } else {
                attachmentClient.replaceAttachmentUsages(Source.projectId(project.getId()), attachmentUsages);
            }
        } else {
            throw new TException(
                    "LicenseInfo usage is not stored since the user has no write permissions for this project.");
        }
    }

    public static List<AttachmentUsage> makeAttachmentUsages(Project project,
            Map<String, Set<String>> selectedReleaseAndAttachmentIds, Function<String, UsageData> usageDataGenerator) {
        List<AttachmentUsage> attachmentUsages = Lists.newArrayList();

        for (String releaseId : selectedReleaseAndAttachmentIds.keySet()) {
            for (String attachmentContentId : selectedReleaseAndAttachmentIds.get(releaseId)) {
                AttachmentUsage usage = new AttachmentUsage();
                usage.setUsedBy(Source.projectId(project.getId()));
                usage.setOwner(Source.releaseId(releaseId));
                usage.setAttachmentContentId(attachmentContentId);

                UsageData usageData = usageDataGenerator.apply(attachmentContentId);
                usage.setUsageData(usageData);

                attachmentUsages.add(usage);
            }
        }
        return attachmentUsages;
    }

    private InputStream getStreamToServeBundle(AttachmentStreamConnector attachmentStreamConnector,
            List<AttachmentContent> attachments, User sw360User, Optional<Object> context)
            throws IOException, TException {
        return attachmentStreamConnector.getAttachmentBundleStream(new HashSet<>(attachments), sw360User, context);
    }

    private InputStream getStreamToServeAFile(AttachmentStreamConnector attachmentStreamConnector,
            List<AttachmentContent> attachments, User sw360User, Optional<Object> context)
            throws IOException, TException {
        if (attachments == null) {
            throw new TException("Tried to download empty set of Attachments");
        } else if (attachments.isEmpty()) {
            return attachmentStreamConnector.getAttachmentBundleStream(new HashSet<>(), sw360User, context);
        } else if (attachments.size() == 1) {
            // Temporary solutions, permission check needs to be implemented
            // (getAttachmentStream)
            return attachmentStreamConnector.unsafeGetAttachmentStream(attachments.iterator().next());
        } else {
            return attachmentStreamConnector.getAttachmentBundleStream(new HashSet<>(attachments), sw360User, context);
        }
    }
}