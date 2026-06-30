/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.report;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.REPORT_FILENAME_MAPPING;

import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.sw360.rest.resourceserver.attachment.SW360AttachmentBackendService;
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
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.exporter.CSVExport;
import org.eclipse.sw360.exporter.JsonExport;
import org.eclipse.sw360.exporter.LicenseInfoExporter;
import org.eclipse.sw360.exporter.ProjectExporter;
import org.eclipse.sw360.exporter.ReleaseExporter;
import org.eclipse.sw360.exporter.XmlExport;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
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
    private final Sw360LicenseService licenseService;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final Sw360LicenseInfoService licenseInfoService;

    @NonNull
    private final SW360AttachmentBackendService attachmentBackendService;

    @org.springframework.beans.factory.annotation.Value("${sw360.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final Logger log = LogManager.getLogger(SW360ReportService.class);
    private static final Set<String> SUPPORTED_FORMATS = Set.of("xlsx", "csv", "json", "xml");
    private final LicenseInfoExporter licenseInfoExporter = new LicenseInfoExporter();
    ProjectService.Iface projectclient = ThriftClients.makeProjectClient();
    ComponentService.Iface componentclient = ThriftClients.makeComponentClient();
    LicenseService.Iface licenseClient = ThriftClients.makeLicenseClient();

    public ByteBuffer getProjectBuffer(User user, boolean extendedByReleases, String projectId, String format) throws TException {
        return getProjectBuffer(user, extendedByReleases, projectId, format, null);
    }

    public ByteBuffer getProjectBuffer(User user, boolean extendedByReleases, String projectId, String format,
                                       SW360ReportBean reportBean) throws TException {
        String fmt = (format == null) ? "xlsx" : format.trim().toLowerCase();
        validateFormat(fmt);
        try {
            List<Project> projects;
            if (projectId != null) {
                if (!validateProject(projectId, user)) {
                    throw new ResourceNotFoundException("No project record found for the project Id : " + projectId);
                }
                // For a single specific project, delegate to backend for xlsx
                // but use ProjectExporter for other formats
                if ("xlsx".equals(fmt)) {
                    return projectclient.getReportDataStream(user, extendedByReleases, projectId);
                }
                Project project = projectclient.getProjectById(projectId, user);
                if (project == null) {
                    throw new ResourceNotFoundException("No project record found for the project Id : " + projectId);
                }
                projects = List.of(project);
            } else {
                // No specific projectId: export projects matching the given filters.
                projects = getFilteredProjects(user, reportBean);
                if ("xlsx".equals(fmt)) {
                    // Build xlsx from the filtered project list via ProjectExporter
                    ProjectExporter exporter = new ProjectExporter(componentclient, projectclient, user, projects, extendedByReleases);
                    return ByteBuffer.wrap(IOUtils.toByteArray(exporter.makeExcelExport(projects)));
                }
            }
            ProjectExporter exporter = new ProjectExporter(componentclient, projectclient, user, projects, extendedByReleases);
            List<Map<String, String>> records = exporter.makeRecords(projects);
            List<String> headers = extendedByReleases ? ProjectExporter.HEADERS_EXTENDED_BY_RELEASES : ProjectExporter.HEADERS;
            return convertToFormat(records, headers, fmt);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            throw new TException("Failed to export projects in format " + fmt + ": " + e.getMessage(), e);
        }
    }

    /** Retrieves all projects matching the filters in {@code reportBean} for export. */
    private List<Project> getFilteredProjects(User user, SW360ReportBean reportBean) throws TException {
        Map<String, Set<String>> filterMap;
        if (reportBean == null) {
            filterMap = new HashMap<>();
        } else {
            filterMap = RestControllerHelper.getFilterMapForProject(
                    reportBean.getTag(), reportBean.getType(), reportBean.getGroup(), reportBean.getVersion(),
                    reportBean.getProjectResponsible(), reportBean.getProjectState(), reportBean.getProjectClearingState(),
                    reportBean.getAdditionalData(), null
            );
            if (CommonUtils.isNotNullEmptyOrWhitespace(reportBean.getName())) {
                filterMap.put(Project._Fields.NAME.getFieldName(), CommonUtils.splitToSet(reportBean.getName()));
            }
        }
        return projectService.getFilteredProjectsForExport(filterMap, user,
                reportBean != null && reportBean.isLuceneSearch());
    }

    private ByteBuffer convertToFormat(List<Map<String, String>> records, List<String> headers, String format) throws IOException {
        switch (format) {
            case "csv":
                List<List<String>> rows = new ArrayList<>();
                for (Map<String, String> record : records) {
                    List<String> row = new ArrayList<>();
                    for (String header : headers) {
                        row.add(record.getOrDefault(header, ""));
                    }
                    rows.add(row);
                }
                Iterable<Iterable<String>> iterableRows = rows.stream().map(row -> (Iterable<String>) row).collect(Collectors.toList());
                return ByteBuffer.wrap(IOUtils.toByteArray(CSVExport.createCSV(headers, iterableRows)));
            case "json":
                return ByteBuffer.wrap(IOUtils.toByteArray(JsonExport.toJson(records)));
            case "xml":
                return ByteBuffer.wrap(IOUtils.toByteArray(XmlExport.toXml(records)));
            default:
                throw new BadRequestClientException("Unsupported format: " + format);
        }
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
        return getDocumentName(user, projectId, module, "xlsx");
    }

    public String getDocumentName(User user, String projectId, String module, String format) throws TException {
        String fmt = (format == null) ? "xlsx" : format.trim().toLowerCase();
        validateFormat(fmt);
        String extension = getFileExtension(fmt);
        String documentName = String.format("projects-%s.%s", SW360Utils.getCreatedOn(), extension);
        if (SW360Constants.PROJECTS.equalsIgnoreCase(module)) {
            if (projectId != null && !projectId.equalsIgnoreCase("null")) {
                Project project = projectclient.getProjectById(projectId, user);
                documentName = String.format("project-%s-%s-%s.%s", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOn(), extension);
            }
        } else if (SW360Constants.COMPONENTS.equalsIgnoreCase(module)) {
            documentName = String.format("components-%s.%s", SW360Utils.getCreatedOn(), extension);
        } else if (SW360Constants.LICENSES.equalsIgnoreCase(module)) {
            documentName = String.format("licenses-%s.%s", SW360Utils.getCreatedOn(), extension);
        } else if (SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO.equals(module)) {
            if (projectId != null && !projectId.equalsIgnoreCase("null")) {
                Project project = projectclient.getProjectById(projectId, user);
                documentName = String.format("releases-%s-%s-%s.%s", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOn(), extension);
            }
        }
        return documentName;
    }

    private void validateFormat(String format) {
        if (!SUPPORTED_FORMATS.contains(format)) {
            throw new IllegalArgumentException("Unsupported export format: " + format + ". Supported formats: " + SUPPORTED_FORMATS);
        }
    }

    private String getFileExtension(String format) {
        switch (format) {
            case "csv":
                return "csv";
            case "json":
                return "json";
            case "xml":
                return "xml";
            case "xlsx":
            default:
                return "xlsx";
        }
    }

    public void getUploadedLicenseInfoPath(User user, boolean withSubProject, String base, String projectId,
                                           SW360ReportBean reportBean) throws TException {
        if (projectId != null && !validateProject(projectId, user)) {
            throw new SW360Exception("No project record found for the project Id : " + projectId);
        }
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                String licenseInfoPath = generateLicenseInfoReport(user, projectId, reportBean);
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
            } catch (ResourceNotFoundException exp) {
                throw exp;
            } catch (AccessDeniedException exp) {
                throw exp;
            } catch (Exception exp) {
                throw new TException(exp.getMessage());
            }
        });
        Thread asyncThread = new Thread(asyncRunnable);
        asyncThread.start();
    }

    private String generateLicenseInfoReport(User user, String projectId, SW360ReportBean reportBean)
            throws TException, IOException {
        ByteBuffer buffer = getLicenseInfoBuffer(user, projectId, reportBean);
        return licenseInfoExporter.saveReportToFile(buffer, user);
    }

    public ByteBuffer getLicenseInfoReportStreamFromUrl(String token) throws TException {
        try {
            return licenseInfoExporter.downloadReport(token);
        } catch (Exception e) {
            throw new TException("Failed to read license info report: " + e.getMessage(), e);
        }
    }

    public void getUploadedProjectPath(User user, boolean withLinkedReleases, String base, String projectId)
            throws TException {
        if (projectId!=null && !validateProject(projectId, user)) {
            throw new SW360Exception("No project record found for the project Id : " + projectId);
        }
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                String projectPath = projectclient.getReportInEmail(user, withLinkedReleases, projectId);
                String downloadUrl = frontendUrl + "/reports/download?module=projects"
                        + "&extendedByReleases=" + withLinkedReleases + "&projectId=" + projectId + "&token="
                        + URLEncoder.encode(projectPath, StandardCharsets.UTF_8);
                URL emailURL = new URI(downloadUrl).toURL();
                log.debug("Report download link for user {}: {}", user.getEmail(), emailURL);
                if (!CommonUtils.isNullEmptyOrWhitespace(projectPath)) {
                    sendExportSpreadsheetSuccessMail(emailURL.toString(), user.getEmail());
                }
            } catch (ResourceNotFoundException exp) {
                throw exp;
            } catch (AccessDeniedException exp) {
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

    public void getUploadedComponentPath(User sw360User, boolean withLinkedReleases, String base) {
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                String componentPath = componentclient.getComponentReportInEmail(sw360User, withLinkedReleases);
                String downloadUrl = frontendUrl + "/reports/download?module=components"
                        + "&extendedByReleases=" + withLinkedReleases + "&token="
                        + URLEncoder.encode(componentPath, StandardCharsets.UTF_8);
                URL emailURL = new URI(downloadUrl).toURL();
                log.debug("Report download link for user {}: {}", sw360User.getEmail(), emailURL);
                if (!CommonUtils.isNullEmptyOrWhitespace(componentPath)) {
                    sendComponentExportSpreadsheetSuccessMail(emailURL.toString(), sw360User.getEmail());
                }
            } catch (ResourceNotFoundException exp) {
                throw exp;
            } catch (AccessDeniedException exp) {
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
        String fileName = "";
        if (CommonUtils.isNotNullEmptyOrWhitespace(reportBean.getTemplate())
                && CommonUtils.isNotNullEmptyOrWhitespace(REPORT_FILENAME_MAPPING)) {
            Map<String, String> orgToTemplate = Arrays.stream(REPORT_FILENAME_MAPPING.split(","))
                    .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.split(":")[1]));
            fileName = orgToTemplate.get(reportBean.getTemplate());
        }
        final LicenseInfoFile licenseInfoFile = licenseInfoService.getLicenseInfoFile(sw360Project, sw360User,
                outputGeneratorClassNameWithVariant, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachments,
                reportBean.getExternalIds(), fileName, reportBean.isExcludeReleaseVersion());
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
        Function<LicenseInfo, Stream<LicenseNameWithText>> streamLicenseNameWithTexts = licenseInfo -> licenseInfo
                .getLicenseNamesWithTexts().stream();
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
            attachments.add(attachmentBackendService.getAttachmentContent(id));
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
                List<AttachmentUsage> attachmentSourceUsages = attachmentBackendService.getUsedAttachments(Source.projectId(project.getId()),
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
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
        return ByteBuffer.wrap(IOUtils.toByteArray(exporter.makeExcelExport(releases)));
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
                    bomString = status.name();
                    throw new SW360Exception(bomString);
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
            documentName = String.format("project_%s(%s)_%s.xml", project.getName(), project.getVersion(),
                    SW360Utils.getCreatedOnTime(), "_SBOM");
            if(SW360Constants.JSON_FILE_EXTENSION.equalsIgnoreCase(bomType)){
                documentName = String.format("project_%s(%s)_%s.json", project.getName(), project.getVersion(),
                        SW360Utils.getCreatedOnTime(), "_SBOM");
            }
        }
        return documentName;
    }
}
