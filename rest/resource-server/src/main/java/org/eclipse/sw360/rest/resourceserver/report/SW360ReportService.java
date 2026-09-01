/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.report;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CSV_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.common.SW360Constants.EXCEL_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.common.SW360Constants.JSON_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.common.SW360Constants.XML_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.exporter.ExcelExporter.SLASH;
import static org.eclipse.sw360.exporter.ExcelExporter.TMP_EXPORTEDFILES;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.REPORT_FILENAME_MAPPING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.SourcePackageUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projects.SW360ReportBean;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.exporter.LicenseInfoExporter;
import org.eclipse.sw360.exporter.ReleaseExporter;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.jetbrains.annotations.Contract;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360ReportService {

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final Sw360LicenseInfoService licenseInfoService;

    @org.springframework.beans.factory.annotation.Value("${sw360.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final Logger log = LogManager.getLogger(SW360ReportService.class);
    private final LicenseInfoExporter licenseInfoExporter = new LicenseInfoExporter();
    ProjectService.Iface projectclient = ThriftClients.makeProjectClient();
    ComponentService.Iface componentclient = ThriftClients.makeComponentClient();
    LicenseService.Iface licenseClient = ThriftClients.makeLicenseClient();
    AttachmentService.Iface attachmentClient = ThriftClients.makeAttachmentClient();

    public ByteBuffer getProjectReportBuffer(User user, String projectId, SW360ReportBean reportBean) throws TException {
        return projectclient.getProjectReportBuffer(user, projectId, reportBean);
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
        return getDocumentName(user, projectId, module, ReportFormat.EXCEL);
    }

    public String getDocumentName(User user, String projectId, String module, ReportFormat format) throws TException {
        String extension = getFileExtension(format);
        String documentName = String.format("projects-%s.%s", SW360Utils.getCreatedOn(), extension);
        if (SW360Constants.PROJECTS.equalsIgnoreCase(module)) {
            if (CommonUtils.isNotNullEmptyOrWhitespace(projectId) && !projectId.equalsIgnoreCase("null")) {
                Project project = projectclient.getProjectById(projectId, user);
                documentName = String.format("project-%s-%s-%s.%s", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOn(), extension);
            }
        } else if (SW360Constants.COMPONENTS.equalsIgnoreCase(module)) {
            documentName = String.format("components-%s.%s", SW360Utils.getCreatedOn(), extension);
        } else if (SW360Constants.LICENSES.equalsIgnoreCase(module)) {
            documentName = String.format("licenses-%s.%s", SW360Utils.getCreatedOn(), extension);
        } else if (SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO.equals(module)) {
            if (CommonUtils.isNotNullEmptyOrWhitespace(projectId) && !projectId.equalsIgnoreCase("null")) {
                Project project = projectclient.getProjectById(projectId, user);
                documentName = String.format("releases-%s-%s-%s.%s", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOn(), extension);
            }
        }
        return documentName;
    }

    @Contract(pure = true)
    private @NonNull String getFileExtension(@NonNull ReportFormat format) {
        return switch (format) {
            case CSV -> CSV_FILE_EXTENSION;
            case JSON -> JSON_FILE_EXTENSION;
            case XML -> XML_FILE_EXTENSION;
            default -> EXCEL_FILE_EXTENSION;
        };
    }

    public void getUploadedLicenseInfoPath(User user, boolean withSubProject, String base, String projectId,
                                           SW360ReportBean reportBean) throws TException {
        if (projectId != null && !validateProject(projectId, user)) {
            throw new SW360Exception("No project record found for the project Id : " + projectId);
        }
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                ByteBuffer buff = getLicenseInfoBuffer(user, projectId, reportBean);
                String licenseInfoPath = writeToTempFile(buff, user);
                String downloadUrl = frontendUrl + "/reports/download?module=licenseInfo"
                        + "&withSubProject=" + withSubProject
                        + "&projectId=" + projectId
                        + "&generatorClassName=" + URLEncoder.encode(
                                reportBean.getGeneratorClassName() != null ? reportBean.getGeneratorClassName() : "",
                                StandardCharsets.UTF_8)
                        + "&variant=" + URLEncoder.encode(
                                reportBean.getVariant() != null ? reportBean.getVariant() : "",
                                StandardCharsets.UTF_8)
                        + "&token=" + URLEncoder.encode(licenseInfoPath, StandardCharsets.UTF_8);
                URL emailURL = new URI(downloadUrl).toURL();
                log.debug("License info report download link for user {}: {}", user.getEmail(), emailURL);
                if (!CommonUtils.isNullEmptyOrWhitespace(licenseInfoPath)) {
                    sendExportSpreadsheetSuccessMail(emailURL.toString(), user.getEmail());
                }
            } catch (ResourceNotFoundException | AccessDeniedException exp) {
                throw exp;
            } catch (Exception exp) {
                throw new TException(exp.getMessage());
            }
        });
        Thread asyncThread = new Thread(asyncRunnable);
        asyncThread.start();
    }

    public ByteBuffer getLicenseInfoReportStreamFromUrl(String token) throws TException {
        try {
            return licenseInfoExporter.downloadReport(token);
        } catch (Exception e) {
            throw new TException("Failed to read license info report: " + e.getMessage(), e);
        }
    }

    public void getUploadedProjectPath(
            User user, SW360ReportBean reportBean, String projectId
    ) throws TException {
        if (CommonUtils.isNotNullEmptyOrWhitespace(projectId) && !validateProject(projectId, user)) {
            throw new SW360Exception("No project record found for the project Id : " + projectId);
        }
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                ByteBuffer buff = getProjectReportBuffer(user, projectId, reportBean);
                String projectPath = writeToTempFile(buff, user);
                String downloadUrl = frontendUrl + "/reports/download?module=projects"
                        + "&extendedByReleases=" + reportBean.isWithLinkedReleases() + "&projectId=" + projectId + "&token="
                        + URLEncoder.encode(projectPath, StandardCharsets.UTF_8);
                URL emailURL = new URI(downloadUrl).toURL();
                log.debug("Report download link for user {}: {}", user.getEmail(), emailURL);
                if (!CommonUtils.isNullEmptyOrWhitespace(projectPath)) {
                    sendExportSpreadsheetSuccessMail(emailURL.toString(), user.getEmail());
                }
            } catch (SW360Exception exp) {
                throw exp;
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

    public void getUploadedComponentPath(User sw360User, SW360ReportBean reportBean) {
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                ByteBuffer buff = getComponentBuffer(sw360User, reportBean.isWithLinkedReleases());
                String componentPath = writeToTempFile(buff, sw360User);
                String downloadUrl = frontendUrl + "/reports/download?module=components"
                        + "&extendedByReleases=" + reportBean.isWithLinkedReleases() + "&token="
                        + URLEncoder.encode(componentPath, StandardCharsets.UTF_8);
                URL emailURL = new URI(downloadUrl).toURL();
                log.debug("Report download link for user {}: {}", sw360User.getEmail(), emailURL);
                if (!CommonUtils.isNullEmptyOrWhitespace(componentPath)) {
                    sendComponentExportSpreadsheetSuccessMail(emailURL.toString(), sw360User.getEmail());
                }
            } catch (ResourceNotFoundException | AccessDeniedException exp) {
                throw exp;
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

    public ByteBuffer getLicenseInfoBuffer(User sw360User, String id, SW360ReportBean reportBean) throws TException {
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        List<String> selectedReleaseRelationships = getSelectedReleaseRelationships(reportBean.getSelectedRelRelationship());

        final Set<ReleaseRelationship> listOfSelectedRelationships = (selectedReleaseRelationships != null)
                ? selectedReleaseRelationships.stream()
                .map(rel -> ThriftEnumUtils.stringToEnum(rel, ReleaseRelationship.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                : null;

        final Set<String> listOfSelectedRelationshipsInString = (listOfSelectedRelationships != null)
                ? listOfSelectedRelationships.stream()
                .map(ReleaseRelationship::name)
                .collect(Collectors.toSet())
                : null;

        List<ProjectLink> mappedProjectLinks = projectService.createLinkedProjects(sw360Project,
                projectService.filterAndSortAttachments(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES), true,
                reportBean.isWithSubProject(), sw360User);

        List<AttachmentUsage> attchmntUsg = new ArrayList<>(attachmentService.getAttachmentUsages(id));
        if (reportBean.isWithSubProject()) {
            mappedProjectLinks.stream()
                    .map(ProjectLink::getId)
                    .filter(projectLinkId -> !id.equals(projectLinkId))
                    .distinct()
                    .forEach(subProjectId -> wrapTException(() -> {
                        attchmntUsg.addAll(attachmentService.getAttachmentUsages(subProjectId));
                    }));
        }

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
                usedAttachmentContentIds, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachments, listOfSelectedRelationshipsInString);

        String outputGeneratorClassNameWithVariant = reportBean.getGeneratorClassName() + "::" + reportBean.getVariant();
        String templateFileName = "";
        if (CommonUtils.isNotNullEmptyOrWhitespace(reportBean.getTemplate())
                && CommonUtils.isNotNullEmptyOrWhitespace(REPORT_FILENAME_MAPPING)) {
            Map<String, String> orgToTemplate = Arrays.stream(REPORT_FILENAME_MAPPING.split(","))
                    .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.split(":")[1]));
            templateFileName = orgToTemplate.get(reportBean.getTemplate());
        }
        final LicenseInfoFile licenseInfoFile = licenseInfoService.getLicenseInfoFile(sw360Project, sw360User,
                outputGeneratorClassNameWithVariant, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachments,
                reportBean.getExternalIds(), templateFileName, reportBean.isExcludeReleaseVersion());
        return licenseInfoFile.bufferForGeneratedOutput();
    }

    private List<String> getSelectedReleaseRelationships(List<ReleaseRelationship> selectedRelRelationship) {
        List<String> selectedReleaseRelationships = null;
//        if (!CommonUtils.isNullEmptyOrWhitespace(selectedRelRelationship)) {
//            selectedReleaseRelationships = Arrays.asList(selectedRelRelationship.split(","));
//        }
        if (selectedRelRelationship != null && !selectedRelRelationship.isEmpty()) {
            selectedReleaseRelationships = selectedRelRelationship.stream()
                    .map(ReleaseRelationship::name).collect(Collectors.toList());
        }
        return selectedReleaseRelationships;
    }

    private void getSelectedAttchIdsAndExcludedLicInfo(User sw360User, List<ProjectLink> mappedProjectLinks,
                                                       Map<Source, Set<String>> releaseIdToExcludedLicenses, Map<String, Boolean> usedAttachmentContentIds,
                                                       final Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds,
                                                       final Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachments,
                                                       Set<String> listOfSelectedRelationshipsInString) {
        mappedProjectLinks.forEach(projectLink -> wrapTException(() -> projectLink.getLinkedReleases().stream()
                .filter(ReleaseLink::isSetAttachments).forEach(releaseLink -> {
                    String releaseLinkId = releaseLink.getId();
                    Set<String> excludedLicenseIds = releaseIdToExcludedLicenses.get(Source.releaseId(releaseLinkId));

                    if(null!=listOfSelectedRelationshipsInString && !listOfSelectedRelationshipsInString.contains(releaseLink.getReleaseRelationship().name())){
                        return;
                    }
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

    public String getGenericLicInfoFileName(User sw360User, String projectId, String generatorClassName, String variant) throws TException {
        final Project sw360Project = projectService.getProjectForUserById(projectId, sw360User);
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
        Function<LicenseInfo, Stream<LicenseNameWithText>> streamLicenseNameWithTexts = licenseInfo ->
                (licenseInfo != null && licenseInfo.getLicenseNamesWithTexts() != null)
                        ? licenseInfo.getLicenseNamesWithTexts().stream()
                        : Stream.empty();
        return licenseInfoParsingResult.stream().map(LicenseInfoParsingResult::getLicenseInfo)
                .flatMap(streamLicenseNameWithTexts).filter(filteredLicense).collect(Collectors.toSet());
    }

    public ByteBuffer getLicenseResourceBundleBuffer() throws TException {
        return licenseClient.getLicenseReportDataStream();
    }

    public ByteBuffer downloadSourceCodeBundle(String projectId, User sw360User, boolean withSubProject)
            throws IOException, TException {
        if (projectId == null || !validateProject(projectId, sw360User)) {
            throw new TException("No project record found for the project Id : " + projectId);
        }
        Project project = projectclient.getProjectById(projectId, sw360User);
        List<AttachmentContent> attachments = new ArrayList<>();
        for (String id : getAttachmentIdFromAttachmentUsages(project, sw360User, withSubProject)) {
            attachments.add(attachmentClient.getAttachmentContent(id));
        }
        return serveAttachmentBundle(attachments, project, sw360User);
    }

    public String getSourceCodeBundleName(String projectId, User sw360User) throws TException {
        Project project = projectclient.getProjectById(projectId, sw360User);
        String timestamp = SW360Utils.getCreatedOn();
        return "SourceCodeBundle-" + project.getName() + "-" + timestamp + ".zip";
    }

    private ByteBuffer serveAttachmentBundle(List<AttachmentContent> attachments,
                                             Project project, User sw360User) throws IOException, TException {
        final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
        final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
        return getAttachmentBundleByteBuffer(attachmentStreamConnector, attachments, project, sw360User);
    }

    private ByteBuffer getAttachmentBundleByteBuffer(AttachmentStreamConnector attachmentStreamConnector,
            List<AttachmentContent> attachments, Project project, User sw360User)
            throws TException, IOException {
        InputStream stream = null;
        Optional<Object> context = getContextFromRequest(project);
        if (context.isPresent()) {
            stream = getStreamToServeAFile(attachmentStreamConnector, attachments, sw360User, context);
        }
        return ByteBuffer.wrap(IOUtils.toByteArray(stream));
    }

    private Optional<Object> getContextFromRequest(Project project) {
        return Optional.ofNullable(project);
    }

    public List<String> getAttachmentIdFromAttachmentUsages(Project sw360Project, User sw360User, boolean withSubProject) {
        final Set<String> attachmentIds = new HashSet<>();
        final Set<Project> projects = new HashSet<>(List.of(sw360Project));
        if (withSubProject) {
            final Collection<ProjectLink> linkedProjects = SW360Utils.getLinkedProjectsAsFlatList(sw360Project, true, log, sw360User);
            projects.addAll(linkedProjects.stream().map(link -> wrapTException(() -> projectService.getProjectForUserById(link.getId(), sw360User))).toList());
        }
        for (Project project : projects) {
            try {
                List<AttachmentUsage> attachmentSourceUsages = attachmentClient.getUsedAttachments(Source.projectId(project.getId()),
                        UsageData.sourcePackage(new SourcePackageUsage()));
                List<String> currentProjAttachments = attachmentSourceUsages.stream().map(AttachmentUsage::getAttachmentContentId).toList();
                if (! currentProjAttachments.isEmpty()) {
                    attachmentIds.addAll(currentProjAttachments);
                    continue;
                }
                Map<String, ProjectReleaseRelationship> releaseUsage = project.getReleaseIdToUsage();
                try {
                    List<Release> releases = componentclient.getFullReleasesById(releaseUsage.keySet(), sw360User);
                    releases.forEach(release -> {
                        Set<Attachment> attachments = release.getAttachments();
                        if (attachments != null) {
                            attachments.forEach(attachment -> {
                                if (attachment.getAttachmentType() == AttachmentType.SOURCE || attachment.getAttachmentType() == AttachmentType.SOURCE_SELF) {
                                    attachmentIds.add(attachment.getAttachmentContentId());
                                }
                            });
                        }
                    });
                } catch (TException ignored) {
                }
            } catch (TException ignored) {
            }
        }

        return attachmentIds.stream().toList();
    }

    private InputStream getStreamToServeAFile(AttachmentStreamConnector attachmentStreamConnector,
            List<AttachmentContent> attachments, User sw360User, Optional<Object> context)
            throws IOException, TException {
        if (attachments == null) {
            throw new TException("Tried to download empty set of Attachments");
        } else if (attachments.isEmpty()) {
            return attachmentStreamConnector.getAttachmentBundleStream(new HashSet<>(), sw360User, context);
        } else if (attachments.size() == 1) {
            return attachmentStreamConnector.unsafeGetAttachmentStream(attachments.iterator().next());
        } else {
            return attachmentStreamConnector.getAttachmentBundleStream(new HashSet<>(attachments), sw360User, context);
        }
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
        } catch (ResourceNotFoundException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
        return exporter.toByteBuffer(releases);
    }

    public String getProjectSBOMBuffer(User user, String projectId, String bomType, boolean withSubProject) throws TException {
        String bomString = "";
            if (CommonUtils.isNotNullEmptyOrWhitespace(projectId)) {
                if (CommonUtils.isNullEmptyOrWhitespace(bomType)) {
                    throw new SW360Exception("Bom type cannot be empty");
                }
                RequestSummary summary = projectclient.exportCycloneDxSbom(projectId, bomType, withSubProject, user);
                RequestStatus status = summary.getRequestStatus();
                if (RequestStatus.FAILED_SANITY_CHECK.equals(status)) {
                    String msg = CommonUtils.isNotNullEmptyOrWhitespace(summary.getMessage()) ?
                            summary.getMessage() : "Cannot export SBOM: The project does not contain any linked releases or packages.";
                    throw new BadRequestClientException(msg);
                } else if (RequestStatus.ACCESS_DENIED.equals(status)) {
                    bomString = status.name() + ", only user with role " + SW360Utils.readConfig(SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE, UserGroup.USER).name() + " can access.";
                    throw new AccessDeniedException(bomString);
                } else if (RequestStatus.FAILURE.equals(status)) {
                    bomString = status.name() + "-" + summary.getMessage() ;
                    throw new SW360Exception(bomString);
                } else {
                    bomString = summary.getMessage();
                }
            }
            else{
                throw new SW360Exception("Project Id cannot be empty");
            }
        return bomString;
    }

    public String getSBOMFileName(User user, String projectId, String module, String bomType) throws TException {
        String documentName = "";
        if(projectId != null && !projectId.equalsIgnoreCase("null")) {
            Project project = projectclient.getProjectById(projectId, user);
            documentName = String.format("project_%s(%s)_%s%s.xml", project.getName(), project.getVersion(),
                    SW360Utils.getCreatedOnTime(), "_SBOM");
            if(JSON_FILE_EXTENSION.equalsIgnoreCase(bomType)){
                documentName = String.format("project_%s(%s)_%s%s.json", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOnTime(), "_SBOM");
            }
        }
        return documentName;
    }

    @NonNull
    private static String writeToTempFile(@NonNull ByteBuffer buffer, @NonNull User user) throws IOException {
        String token = UUID.randomUUID().toString();
        String filePath = TMP_EXPORTEDFILES + user.getEmail() + SLASH + "file" + SLASH;
        String relativePath;
        File dir = new File(filePath);
        if (!dir.mkdirs() && !dir.exists()) {
            log.error("Failed to create export directory: {}", dir.getAbsolutePath());
            throw new IOException("Failed to create export directory: " + dir.getAbsolutePath());
        }
        File file = new File(dir.getPath() + SLASH + SW360Utils.getCreatedOn() + "_" + token);
        if (!file.createNewFile()) {
            log.error("Failed to create export file: {}", file.getAbsolutePath());
            throw new IOException("Failed to create export file: " + file.getAbsolutePath());
        }
        relativePath = user.getEmail() + SLASH + "file" + SLASH + file.getName();

        buffer.rewind();
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
        return relativePath;
    }
}
