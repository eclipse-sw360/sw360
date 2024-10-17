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

import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.REPORT_FILENAME_MAPPING;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import com.google.common.base.Strings;

import lombok.NonNull;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SW360ReportService {

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360LicenseService licenseService;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final Sw360LicenseInfoService licenseInfoService;

    ThriftClients thriftClients = new ThriftClients();
    ProjectService.Iface projectclient = thriftClients.makeProjectClient();
    ComponentService.Iface componentclient = thriftClients.makeComponentClient();
    LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

    public ByteBuffer getProjectBuffer(User user, boolean extendedByReleases, String projectId) throws TException {
        if (projectId != null && validateProject(projectId, user)) {
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

    public String getDocumentName(User user, String projectId) throws TException {
        if (projectId != null && !projectId.equalsIgnoreCase("null")) {
            Project project = projectclient.getProjectById(projectId, user);
            return String.format("project-%s-%s-%s.xlsx", project.getName(), project.getVersion(), SW360Utils.getCreatedOn());
        }
        return String.format("projects-%s.xlsx", SW360Utils.getCreatedOn());
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

    public ByteBuffer getLicenseInfoBuffer(User sw360User, String id, String generatorClassName,
                                           String variant, String template, String externalIds,
                                           boolean excludeReleaseVersion) throws TException {
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        List<ProjectLink> mappedProjectLinks = projectService.createLinkedProjects(sw360Project,
                projectService.filterAndSortAttachments(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES), true, sw360User);

        List<AttachmentUsage> attchmntUsg = attachmentService.getAttachemntUsages(id);

        Map<Source, Set<String>> releaseIdToExcludedLicenses = attchmntUsg.stream()
                .collect(Collectors.toMap(AttachmentUsage::getOwner,
                        x -> x.getUsageData().getLicenseInfo().getExcludedLicenseIds(), (li1, li2) -> li1));

        Map<String, Boolean> usedAttachmentContentIds = attchmntUsg.stream()
                .collect(Collectors.toMap(AttachmentUsage::getAttachmentContentId, attUsage -> {
                    if (attUsage.isSetUsageData()
                            && attUsage.getUsageData().getSetField().equals(UsageData._Fields.LICENSE_INFO)) {
                        return Boolean.valueOf(attUsage.getUsageData().getLicenseInfo().isIncludeConcludedLicense());
                    }
                    return Boolean.FALSE;
                }, (li1, li2) -> li1));

        final Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = new HashMap<>();
        final Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachments = new HashMap<>();

        getSelectedAttchIdsAndExcludedLicInfo(sw360User, mappedProjectLinks, releaseIdToExcludedLicenses,
                usedAttachmentContentIds, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachments);

        String outputGeneratorClassNameWithVariant = generatorClassName + "::" + variant;
        String fileName = "";
        if (CommonUtils.isNotNullEmptyOrWhitespace(template)
                && CommonUtils.isNotNullEmptyOrWhitespace(REPORT_FILENAME_MAPPING)) {
            Map<String, String> orgToTemplate = Arrays.stream(REPORT_FILENAME_MAPPING.split(","))
                    .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.split(":")[1]));
            fileName = orgToTemplate.get(template);
        }
        final LicenseInfoFile licenseInfoFile = licenseInfoService.getLicenseInfoFile(sw360Project, sw360User,
                outputGeneratorClassNameWithVariant, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachments,
                externalIds, fileName, excludeReleaseVersion);
        return licenseInfoFile.bufferForGeneratedOutput();
    }

    private void getSelectedAttchIdsAndExcludedLicInfo(User sw360User, List<ProjectLink> mappedProjectLinks,
                                                       Map<Source, Set<String>> releaseIdToExcludedLicenses, Map<String, Boolean> usedAttachmentContentIds,
                                                       final Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds,
                                                       final Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachments) {
        mappedProjectLinks.forEach(projectLink -> wrapTException(() -> projectLink.getLinkedReleases().stream()
                .filter(ReleaseLink::isSetAttachments).forEach(releaseLink -> {
                    String releaseLinkId = releaseLink.getId();
                    Set<String> excludedLicenseIds = releaseIdToExcludedLicenses.get(Source.releaseId(releaseLinkId));

                    if (!selectedReleaseAndAttachmentIds.containsKey(releaseLinkId)) {
                        selectedReleaseAndAttachmentIds.put(releaseLinkId, new HashMap<>());
                    }
                    final List<Attachment> attachments = releaseLink.getAttachments();
                    Release release = componentService.getReleaseById(releaseLinkId, sw360User);
                    for (final Attachment attachment : attachments) {
                        String attachemntContentId = attachment.getAttachmentContentId();
                        if (usedAttachmentContentIds.containsKey(attachemntContentId)) {
                            boolean includeConcludedLicense = usedAttachmentContentIds.get(attachemntContentId);
                            List<LicenseInfoParsingResult> licenseInfoParsingResult = licenseInfoService
                                    .getLicenseInfoForAttachment(release, sw360User, attachemntContentId,
                                            includeConcludedLicense);
                            excludedLicensesPerAttachments.put(attachemntContentId,
                                    getExcludedLicenses(excludedLicenseIds, licenseInfoParsingResult));
                            selectedReleaseAndAttachmentIds.get(releaseLinkId).put(attachemntContentId,
                                    includeConcludedLicense);
                        }
                    }
                })));
    }

    public String getGenericLicInfoFileName(HttpServletRequest request, User sw360User) throws TException {
        final String variant = request.getParameter("variant");
        final Project sw360Project = projectService.getProjectForUserById(request.getParameter("projectId"), sw360User);
        final String generatorClassName = request.getParameter("generatorClassName");
        final String timestamp = SW360Utils.getCreatedOnTime().replaceAll("\\s", "_").replace(":", "_");
        final OutputFormatInfo outputFormatInfo = licenseInfoService
                .getOutputFormatInfoForGeneratorClass(generatorClassName);
        return String.format("%s-%s%s-%s.%s",
                Strings.nullToEmpty(variant).equals("DISCLOSURE") ? "LicenseInfo" : "ProjectClearingReport",
                sw360Project.getName(),
                StringUtils.isBlank(sw360Project.getVersion()) ? "" : "-" + sw360Project.getVersion(), timestamp,
                outputFormatInfo.getFileExtension());
    }

    private Set<LicenseNameWithText> getExcludedLicenses(Set<String> excludedLicenseIds,
                                                         List<LicenseInfoParsingResult> licenseInfoParsingResult) {
        Predicate<LicenseNameWithText> filteredLicense = licenseNameWithText -> excludedLicenseIds
                .contains(licenseNameWithText.getLicenseName());
        Function<LicenseInfo, Stream<LicenseNameWithText>> streamLicenseNameWithTexts = licenseInfo -> licenseInfo
                .getLicenseNamesWithTexts().stream();
        return licenseInfoParsingResult.stream().map(LicenseInfoParsingResult::getLicenseInfo)
                .flatMap(streamLicenseNameWithTexts).filter(filteredLicense).collect(Collectors.toSet());
    }
}