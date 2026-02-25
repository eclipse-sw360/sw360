/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.report;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.SourcePackageUsage;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.exporter.ReleaseExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import lombok.RequiredArgsConstructor;

import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.REPORT_FILENAME_MAPPING;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
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

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;


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

    private static final Logger log = LogManager.getLogger(SW360ReportService.class);
    ThriftClients thriftClients = new ThriftClients();
    ProjectService.Iface projectclient = thriftClients.makeProjectClient();
    ComponentService.Iface componentclient = thriftClients.makeComponentClient();
    LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
    AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

    public ByteBuffer getProjectBuffer(User user, boolean extendedByReleases, String projectId) throws TException {
        /*
            * If projectId is not null, then validate the project record for the given projectId
            * If the projectId is null, then fetch the project details which are assigned with user
         */
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
        case SW360Constants.COMPONENTS:
            documentName = String.format("components-%s.xlsx", SW360Utils.getCreatedOn());
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
            throw new SW360Exception("No project record found for the project Id : " + projectId);
        }
        Runnable asyncRunnable = () -> wrapTException(() -> {
            try {
                String projectPath = projectclient.getReportInEmail(user, withLinkedReleases, projectId);
                String backendURL = base + "api/reports/download?user=" + user.getEmail() + "&module=projects"
                        + "&extendedByReleases=" + withLinkedReleases + "&projectId=" + projectId + "&token=";
                URL emailURL = new URI(backendURL + URLEncoder.encode(projectPath, StandardCharsets.UTF_8)).toURL();
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
                URL emailURL = new URI(backendURL + URLEncoder.encode(componentPath, StandardCharsets.UTF_8)).toURL();
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
                projectService.filterAndSortAttachments(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES), true, true, sw360User);

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
            final Collection<ProjectLink> linkedProjects = SW360Utils.getLinkedProjectsAsFlatList(sw360Project, true, thriftClients, log, sw360User);
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
