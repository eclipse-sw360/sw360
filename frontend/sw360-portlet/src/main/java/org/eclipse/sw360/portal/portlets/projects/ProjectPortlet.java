/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.projects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.liferay.portal.kernel.json.*;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.datahandler.common.WrappedException.WrappedTException;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.cvesearch.CveSearchService;
import org.eclipse.sw360.datahandler.thrift.cvesearch.VulnerabilityUpdateStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.exporter.ProjectExporter;
import org.eclipse.sw360.exporter.ReleaseExporter;
import org.eclipse.sw360.portal.common.*;
import org.eclipse.sw360.portal.common.datatables.PaginationParser;
import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;
import org.eclipse.sw360.portal.portlets.FossologyAwarePortlet;
import org.eclipse.sw360.portal.users.LifeRayUserSession;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.eclipse.sw360.portal.users.UserUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONArray;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONObject;
import static java.lang.Math.min;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CONTENT_TYPE_OPENXML_SPREADSHEET;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.portal.common.PortalConstants.*;
import static org.eclipse.sw360.portal.portlets.projects.ProjectPortletUtils.isUsageEquivalent;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties",
    },
    property = {
        "javax.portlet.name=" + PROJECT_PORTLET_NAME,

        "javax.portlet.display-name=Projects",
        "javax.portlet.info.short-title=Projects",
        "javax.portlet.info.title=Projects",

        "javax.portlet.init-param.view-template=/html/projects/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ProjectPortlet extends FossologyAwarePortlet {
    private static final Logger log = Logger.getLogger(ProjectPortlet.class);

    private static final String NOT_CHECKED_YET = "Not checked yet.";
    private static final String EMPTY = "<empty>";
    private static final String LICENSE_NAME_WITH_TEXT_KEY = "key";
    private static final String LICENSE_NAME_WITH_TEXT_NAME = "name";
    private static final String LICENSE_NAME_WITH_TEXT_TEXT = "text";
    private static final String LICENSE_NAME_WITH_TEXT_ERROR = "error";
    private static final String LICENSE_NAME_WITH_TEXT_FILE = "file";
    private static final String CYCLIC_LINKED_PROJECT = "Project cannot be created/updated due to cyclic linked project present. Cyclic Hierarchy : ";

    // Project view datatables, index of columns
    private static final int PROJECT_NO_SORT = -1;
    private static final int PROJECT_DT_ROW_NAME = 0;
    private static final int PROJECT_DT_ROW_DESCRIPTION = 1;
    private static final int PROJECT_DT_ROW_RESPONSIBLE = 2;
    private static final int PROJECT_DT_ROW_STATE = 3;
    private static final int PROJECT_DT_ROW_CLEARING_STATE = 4;
    private static final int PROJECT_DT_ROW_ACTION = 5;

    private static final ImmutableList<Project._Fields> projectFilteredFields = ImmutableList.of(
            Project._Fields.BUSINESS_UNIT,
            Project._Fields.VERSION,
            Project._Fields.PROJECT_TYPE,
            Project._Fields.PROJECT_RESPONSIBLE,
            Project._Fields.NAME,
            Project._Fields.STATE,
            Project._Fields.TAG);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TSerializer THRIFT_JSON_SERIALIZER = new TSerializer(new TSimpleJSONProtocol.Factory());

    public static final String LICENSE_STORE_KEY_PREFIX = "license-store-";

    public ProjectPortlet() {
    }

    public ProjectPortlet(ThriftClients clients) {
        super(clients);
    }

    @Override
    protected Set<Attachment> getAttachments(String documentId, String documentType, User user) {

        try {
            ProjectService.Iface client = thriftClients.makeProjectClient();
            Project projectById = client.getProjectById(documentId, user);
            return CommonUtils.nullToEmptySet(projectById.getAttachments());
        } catch (TException e) {
            log.error("Could not get project", e);
        }
        return Collections.emptySet();
    }

    //Helper methods
    private void addProjectBreadcrumb(RenderRequest request, RenderResponse response, Project project) {
        PortletURL url = response.createRenderURL();
        url.setParameter(PAGENAME, PAGENAME_DETAIL);
        url.setParameter(PROJECT_ID, project.getId());

        addBreadcrumbEntry(request, printName(project), url);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.VIEW_LINKED_PROJECTS.equals(action)) {
            serveLinkedProjects(request, response);
        } else if (PortalConstants.LOAD_PROJECT_LIST.equals(action)) {
            serveProjectList(request, response);
        } else if (PortalConstants.REMOVE_PROJECT.equals(action)) {
            serveRemoveProject(request, response);
        } else if (PortalConstants.VIEW_LINKED_RELEASES.equals(action)) {
            serveLinkedReleases(request, response);
        } else if (PortalConstants.UPDATE_VULNERABILITIES_PROJECT.equals(action)) {
            updateVulnerabilitiesProject(request, response);
        } else if (PortalConstants.UPDATE_VULNERABILITY_RATINGS.equals(action)) {
            updateVulnerabilityRating(request, response);
        } else if (PortalConstants.EXPORT_TO_EXCEL.equals(action)) {
            exportExcel(request, response);
        } else if (PortalConstants.EXPORT_CLEARING_TO_EXCEL.equals(action)) {
            exportReleasesSpreadsheet(request, response);
        } else if (PortalConstants.DOWNLOAD_LICENSE_INFO.equals(action)) {
            downloadLicenseInfo(request, response);
        } else if (PortalConstants.DOWNLOAD_SOURCE_CODE_BUNDLE.equals(action)) {
            downloadSourceCodeBundle(request, response);
        } else if (PortalConstants.GET_CLEARING_STATE_SUMMARY.equals(action)) {
            serveGetClearingStateSummaries(request, response);
        } else if (PortalConstants.GET_LICENCES_FROM_ATTACHMENT.equals(action)) {
            serveAttachmentFileLicenses(request, response);
        } else if (PortalConstants.LOAD_LICENSE_INFO_ATTACHMENT_USAGE.equals(action)) {
            serveAttachmentUsages(request, response, UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
        } else if (PortalConstants.LOAD_SOURCE_PACKAGE_ATTACHMENT_USAGE.equals(action)) {
            serveAttachmentUsages(request, response, UsageData.sourcePackage(new SourcePackageUsage()));
        } else if (PortalConstants.LOAD_ATTACHMENT_USAGES_ROWS.equals(action)) {
            serveAttachmentUsagesRows(request, response);
        } else if (PortalConstants.SAVE_ATTACHMENT_USAGES.equals(action)) {
            saveAttachmentUsages(request, response);
        } else if (isGenericAction(action)) {
            dealWithGenericAction(request, response, action);
        } else if (PortalConstants.SAVE_PROJECT_LICENSE_OBLIGATION.equals(action)) {
            updateProjectReleaseObligations(request, response);
        }
    }

    private void updateProjectReleaseObligations(ResourceRequest request, ResourceResponse response) {
        final String projectId = request.getParameter(PROJECT_ID);
        RequestStatus requestStatus = null;
        try {
            final ProjectService.Iface client = thriftClients.makeProjectClient();
            final User user = UserCacheHolder.getUserFromRequest(request);
            final Project project = client.getProjectByIdForEdit(projectId, user);
            JsonNode rootNode = OBJECT_MAPPER.readTree(request.getParameter(OBLIGATION_DATA));
            final Map<String, ObligationStatusInfo> osiInfoMap = project.isSetLinkedObligations() ? project.getLinkedObligations() : Maps.newHashMap();
            rootNode.fieldNames().forEachRemaining(topic -> {
                JsonNode osiNode = rootNode.get(topic);
                ObligationStatusInfo newOsi = OBJECT_MAPPER.convertValue(osiNode, ObligationStatusInfo.class);
                osiInfoMap.computeIfAbsent(topic, e -> newOsi);
                if (newOsi.isSetModifiedOn()) {
                    newOsi.setModifiedBy(user.getEmail());
                    newOsi.setModifiedOn(SW360Utils.getCreatedOn());
                    osiInfoMap.put(topic, newOsi);
                }
            });
            project.setLinkedObligations(osiInfoMap);
            requestStatus = client.updateProject(project, user);
        } catch (TException | IOException exception) {
            log.error("Failed to update obligation status for project: " + projectId, exception);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
        }
        serveRequestStatus(request, response, requestStatus,
                "Failed to update obligation status for project: " + projectId, log);
    }

    private void saveAttachmentUsages(ResourceRequest request, ResourceResponse response) throws IOException {
        final String projectId = request.getParameter(PROJECT_ID);
        AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
        try {
            Project project = getProjectFromRequest(request);
            User user = UserCacheHolder.getUserFromRequest(request);
            if (PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
                List<AttachmentUsage> deselectedUsagesFromRequest = ProjectPortletUtils.deselectedAttachmentUsagesFromRequest(request);
                List<AttachmentUsage> selectedUsagesFromRequest = ProjectPortletUtils.selectedAttachmentUsagesFromRequest(request);
                List<AttachmentUsage> allUsagesByProject = attachmentClient.getUsedAttachments(Source.projectId(projectId), null);
                List<AttachmentUsage> usagesToDelete = allUsagesByProject.stream()
                        .filter(usage -> deselectedUsagesFromRequest.stream()
                                .anyMatch(isUsageEquivalent(usage)))
                        .collect(Collectors.toList());
                List<AttachmentUsage> usagesToCreate = selectedUsagesFromRequest.stream()
                        .filter(usage -> allUsagesByProject.stream()
                                .noneMatch(isUsageEquivalent(usage)))
                        .collect(Collectors.toList());
                setProjectPathToAttachmentUsages(usagesToCreate,project);
                if (!usagesToDelete.isEmpty()) {
                    attachmentClient.deleteAttachmentUsages(usagesToDelete);
                }
                if (!usagesToCreate.isEmpty()) {
                    attachmentClient.makeAttachmentUsages(usagesToCreate);
                }
                writeJSON(request, response, "{}");
            } else {
                response.setProperty(ResourceResponse.HTTP_STATUS_CODE, Integer.toString(HttpServletResponse.SC_FORBIDDEN));
                PortletResponseUtil.write(response, "No write permission for project");
            }
        } catch (TException e) {
            log.error("Saving attachment usages for project " + projectId + " failed", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }

    }

    private void setProjectPathToAttachmentUsages(List<AttachmentUsage> usagesToCreate, Project project) {
        usagesToCreate.forEach(usage -> {
            LicenseInfoUsage licenseInfo= usage.getUsageData().getLicenseInfo();
            if (licenseInfo != null && !licenseInfo.isSetProjectPath())
                licenseInfo.setProjectPath(project.getId());
        });
    }

    private void serveAttachmentUsagesRows(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        prepareLinkedProjects(request);
        String projectId = request.getParameter(PROJECT_ID);
        putAttachmentUsagesInRequest(request, projectId);
        include("/html/projects/includes/attachmentUsagesRows.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    void putAttachmentUsagesInRequest(PortletRequest request, String projectId) throws PortletException {
        try {
            AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

            List<AttachmentUsage> attachmentUsages = wrapTException(() -> attachmentClient.getUsedAttachments(Source.projectId(projectId), null));
            Collector<AttachmentUsage, ?, Map<String, AttachmentUsage>> attachmentUsageMapCollector =
                    Collectors.toMap(AttachmentUsage::getAttachmentContentId, Function.identity(), ProjectPortletUtils::mergeAttachmentUsages);
            BiFunction<List<AttachmentUsage>, UsageData._Fields, Map<String, AttachmentUsage>> filterAttachmentUsages = (attUsages, type) ->
                    attUsages.stream()
                    .filter(attUsage -> attUsage.getUsageData().getSetField().equals(type))
                    .collect(attachmentUsageMapCollector);

            Map<String, AttachmentUsage> licenseInfoUsages = filterAttachmentUsages.apply(attachmentUsages, UsageData._Fields.LICENSE_INFO);
            Map<String, AttachmentUsage> sourcePackageUsages = filterAttachmentUsages.apply(attachmentUsages, UsageData._Fields.SOURCE_PACKAGE);
            Map<String, AttachmentUsage> manualUsages = filterAttachmentUsages.apply(attachmentUsages, UsageData._Fields.MANUALLY_SET);

            request.setAttribute(LICENSE_INFO_ATTACHMENT_USAGES, licenseInfoUsages);
            request.setAttribute(SOURCE_CODE_ATTACHMENT_USAGES, sourcePackageUsages);
            request.setAttribute(MANUAL_ATTACHMENT_USAGES, manualUsages);
        } catch (WrappedTException e) {
            throw new PortletException("Cannot load attachment usages", e);
        }
    }

    private void downloadLicenseInfo(ResourceRequest request, ResourceResponse response) throws IOException {
        final String projectId = request.getParameter(PROJECT_ID);
        String outputGenerator = request.getParameter(PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT);
        String extIdsFromRequest = request.getParameter(PortalConstants.EXTERNAL_ID_SELECTED_KEYS);

        String externalIds = Optional.ofNullable(extIdsFromRequest).orElse(StringUtils.EMPTY);

        Set<String> selectedAttachmentIdsWithPath = Sets
                .newHashSet(request.getParameterValues(PortalConstants.LICENSE_INFO_RELEASE_TO_ATTACHMENT));
        final Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachmentIdWithPath = ProjectPortletUtils
                .getExcludedLicensesPerAttachmentIdFromRequest(selectedAttachmentIdsWithPath, request);

        final Map<String, Set<String>> releaseIdsToSelectedAttachmentIds = new HashMap<>();
        selectedAttachmentIdsWithPath.stream().forEach(selectedAttachmentIdWithPath -> {
            String[] pathParts = selectedAttachmentIdWithPath.split(":");
            String releaseId = pathParts[pathParts.length - 2];
            String attachmentId = pathParts[pathParts.length - 1];
            if (releaseIdsToSelectedAttachmentIds.containsKey(releaseId)) {
                // since we have a set as value, we can just add without getting duplicates
                releaseIdsToSelectedAttachmentIds.get(releaseId).add(attachmentId);
            } else {
                Set<String> attachmentIds = new HashSet<>();
                attachmentIds.add(attachmentId);
                releaseIdsToSelectedAttachmentIds.put(releaseId, attachmentIds);
            }
        });
        final Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachmentId = new HashMap<>();
        excludedLicensesPerAttachmentIdWithPath.entrySet().stream().forEach(entry -> {
            String attachmentId = entry.getKey().substring(entry.getKey().lastIndexOf(":") + 1);
            Set<LicenseNameWithText> excludedLicenses = entry.getValue();
            if (excludedLicensesPerAttachmentId.containsKey(attachmentId)) {
                // this is the important part: if a license is not excluded (read "included") in
                // one attachment occurence, then include (read "not exclude") it in the final
                // result
                excludedLicenses = Sets.intersection(excludedLicensesPerAttachmentId.get(attachmentId),
                        entry.getValue());
            }
            excludedLicensesPerAttachmentId.put(attachmentId, excludedLicenses);
        });

        try {
            final LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();
            final User user = UserCacheHolder.getUserFromRequest(request);
            Project project = thriftClients.makeProjectClient().getProjectById(projectId, user);
            LicenseInfoFile licenseInfoFile = licenseInfoClient.getLicenseInfoFile(project, user, outputGenerator,
                    releaseIdsToSelectedAttachmentIds, excludedLicensesPerAttachmentId, externalIds);
            saveLicenseInfoAttachmentUsages(project, user, selectedAttachmentIdsWithPath,
                    excludedLicensesPerAttachmentIdWithPath);
            sendLicenseInfoResponse(request, response, project, licenseInfoFile);
        } catch (TException e) {
            log.error("Error getting LicenseInfo file for project with id " + projectId + " and generator " + outputGenerator, e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }

    private void sendLicenseInfoResponse(ResourceRequest request, ResourceResponse response, Project project, LicenseInfoFile licenseInfoFile) throws IOException {
        OutputFormatInfo outputFormatInfo = licenseInfoFile.getOutputFormatInfo();
        String documentVariant = licenseInfoFile.getOutputFormatInfo().getVariant() == OutputFormatVariant.DISCLOSURE ? "LicenseInfo" : "ProjectClearingReport";
        String filename = String.format("%s-%s%s-%s.%s", documentVariant, project.getName(),
			StringUtils.isBlank(project.getVersion()) ? "" : "-" + project.getVersion(),
			SW360Utils.getCreatedOnTime().replaceAll("\\s", "_").replace(":", "_"),
			outputFormatInfo.getFileExtension());
    	String mimetype = outputFormatInfo.getMimeType();
    	if (isNullOrEmpty(mimetype)) {
    		mimetype = URLConnection.guessContentTypeFromName(filename);
    	}

        PortletResponseUtil.sendFile(request, response, filename, licenseInfoFile.getGeneratedOutput(), mimetype);
    }

    private void saveLicenseInfoAttachmentUsages(Project project, User user, Set<String> selectedAttachmentIdsWithPath,
            Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachmentIdWithPath) {

        try {
            Function<String, UsageData> usageDataGenerator = attachmentContentId -> {
                Set<String> licenseIds = CommonUtils
                        .nullToEmptySet(excludedLicensesPerAttachmentIdWithPath.get(attachmentContentId)).stream()
                        .filter(LicenseNameWithText::isSetLicenseName)
                        .map(LicenseNameWithText::getLicenseName)
                        .collect(Collectors.toSet());
                LicenseInfoUsage licenseInfoUsage = new LicenseInfoUsage(licenseIds);
                // until second last occurence of ":" (strip releaseId and attachmentId)
                String projectPath = attachmentContentId.substring(0,
                        attachmentContentId.lastIndexOf(":", attachmentContentId.lastIndexOf(":") - 1));
                licenseInfoUsage.setProjectPath(projectPath);
                return UsageData.licenseInfo(licenseInfoUsage);
            };
            List<AttachmentUsage> attachmentUsages = ProjectPortletUtils.makeLicenseInfoAttachmentUsages(project,
                    selectedAttachmentIdsWithPath, usageDataGenerator);
            replaceAttachmentUsages(project, user, attachmentUsages, UsageData.licenseInfo(new LicenseInfoUsage(Collections.emptySet())));
        } catch (TException e) {
            // there's no need to abort the user's desired action just because the ancillary action of storing selection failed
            log.warn("LicenseInfo usage is not stored due to exception: ", e);
        }
    }

    private void saveSourcePackageAttachmentUsages(Project project, User user, Map<String, Set<String>> selectedReleaseAndAttachmentIds) {
        try {
            Function<String, UsageData> usageDataGenerator = attachmentContentId -> UsageData.sourcePackage(new SourcePackageUsage());
            List<AttachmentUsage> attachmentUsages = ProjectPortletUtils.makeAttachmentUsages(project, selectedReleaseAndAttachmentIds,
                    usageDataGenerator);
            replaceAttachmentUsages(project, user, attachmentUsages, UsageData.sourcePackage(new SourcePackageUsage()));
        } catch (TException e) {
            // there's no need to abort the user's desired action just because the ancillary action of storing selection failed
            log.warn("SourcePackage usage is not stored due to exception: ", e);
        }
    }

    private void replaceAttachmentUsages(Project project, User user, List<AttachmentUsage> attachmentUsages, UsageData defaultEmptyUsageData) throws TException {
        if (PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
            AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
            if (attachmentUsages.isEmpty()) {
                attachmentClient.deleteAttachmentUsagesByUsageDataType(Source.projectId(project.getId()),
                        defaultEmptyUsageData);
            } else {
                attachmentClient.replaceAttachmentUsages(Source.projectId(project.getId()), attachmentUsages);
            }
        } else {
            log.info("LicenseInfo usage is not stored since the user has no write permissions for this project.");
        }
    }

    private String getSourceCodeBundleName(Project project) {
        String timestamp = SW360Utils.getCreatedOn();
        return "SourceCodeBundle-" + project.getName() + "-" + timestamp + ".zip";
    }

    private void downloadSourceCodeBundle(ResourceRequest request, ResourceResponse response) {

        Map<String, Set<String>> selectedReleaseAndAttachmentIds = ProjectPortletUtils
                .getSelectedReleaseAndAttachmentIdsFromRequest(request, false);
        Set<String> selectedAttachmentIds = new HashSet<>();
        selectedReleaseAndAttachmentIds.forEach((key, value) -> selectedAttachmentIds.addAll(value));

        try {
            Project project = getProjectFromRequest(request);
            final User user = UserCacheHolder.getUserFromRequest(request);
            saveSourcePackageAttachmentUsages(project, user, selectedReleaseAndAttachmentIds);
            String sourceCodeBundleName = getSourceCodeBundleName(project);
            new AttachmentPortletUtils()
                    .serveAttachmentBundle(selectedAttachmentIds, request, response, Optional.of(sourceCodeBundleName));
        } catch (TException e) {
            log.error("Failed to get project metadata", e);
        }
    }

    private Project getProjectFromRequest(ResourceRequest request) throws TException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        final String projectId = request.getParameter(PROJECT_ID);
        return thriftClients.makeProjectClient().getProjectById(projectId, user);
    }

    private void serveGetClearingStateSummaries(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);
        List<Project> projects;
        String ids[] = request.getParameterValues(Project._Fields.ID.toString()+"[]");
        if (ids == null || ids.length == 0) {
            JSONArray jsonResponse = createJSONArray();
            writeJSON(request, response, jsonResponse);
        } else {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                projects = client.getProjectsById(Arrays.asList(ids), user);
            } catch (TException e) {
                log.error("Could not fetch project summary from backend!", e);
                projects = Collections.emptyList();
            }

            projects = getWithFilledClearingStateSummaryIncludingSubprojects(projects, user);

            JSONArray jsonResponse = createJSONArray();
            ThriftJsonSerializer thriftJsonSerializer = new ThriftJsonSerializer();
            for (Project project : projects) {
                try {
                    JSONObject row = createJSONObject();
                    row.put("id", project.getId());
                    row.put("clearing", JsonHelpers.toJson(project.getReleaseClearingStateSummary(), thriftJsonSerializer));
                    ProjectClearingState clearingState = project.getClearingState();
                    if (clearingState == null) {
                        row.put("clearingstate", "Unknown");
                    } else {
                        row.put("clearingstate", ThriftEnumUtils.enumToString(clearingState));
                    }

                    jsonResponse.put(row);
                } catch (JSONException e) {
                    log.error("cannot serialize json", e);
                }
            }
            writeJSON(request, response, jsonResponse);
        }
    }

    @Override
    protected void dealWithFossologyAction(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException {
        throw new UnsupportedOperationException("cannot call this action on the project portlet");
    }

    private void serveRemoveProject(ResourceRequest request, ResourceResponse response) throws IOException {
        RequestStatus requestStatus = removeProject(request);
        serveRequestStatus(request, response, requestStatus, "Problem removing project", log);
    }

    private void exportExcel(ResourceRequest request, ResourceResponse response) {
        final User user = UserCacheHolder.getUserFromRequest(request);
        final String projectId = request.getParameter(Project._Fields.ID.toString());
        String filename = String.format("projects-%s.xlsx", SW360Utils.getCreatedOn());
        try {
            boolean extendedByReleases = Boolean.valueOf(request.getParameter(PortalConstants.EXTENDED_EXCEL_EXPORT));
            List<Project> projects = getFilteredProjectList(request);
            if (!isNullOrEmpty(projectId)) {
                Project project = projects.stream().filter(p -> p.getId().equals(projectId)).findFirst().get();
                filename = String.format("project-%s-%s-%s.xlsx", project.getName(), project.getVersion(), SW360Utils.getCreatedOn());
            }
            ProjectExporter exporter = new ProjectExporter(
                    thriftClients.makeComponentClient(),
                    thriftClients.makeProjectClient(),
                    user,
                    projects,
                    extendedByReleases);
            PortletResponseUtil.sendFile(request, response, filename, exporter.makeExcelExport(projects), CONTENT_TYPE_OPENXML_SPREADSHEET);
        } catch (IOException | SW360Exception e) {
            log.error("An error occurred while generating the Excel export", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }

    private void exportReleasesSpreadsheet(ResourceRequest request, ResourceResponse response) {
        final User user = UserCacheHolder.getUserFromRequest(request);
        try {
            String id = request.getParameter(PROJECT_ID);
            ProjectService.Iface client = thriftClients.makeProjectClient();
            Project project = null;
            if (!isNullOrEmpty(id)) {
                project = client.getProjectById(id, user);
            }
            if (project != null) {
                List<ReleaseClearingStatusData> releaseStringMap = client.getReleaseClearingStatuses(id, user);
                List<Release> releases = releaseStringMap.stream().map(ReleaseClearingStatusData::getRelease).sorted(Comparator.comparing(SW360Utils::printFullname)).collect(Collectors.toList());
                ReleaseExporter exporter = new ReleaseExporter(thriftClients.makeComponentClient(), releases,
                        user, releaseStringMap);

                PortletResponseUtil.sendFile(request, response,
                        String.format("releases-%s-%s-%s.xlsx", project.getName(), project.getVersion(), SW360Utils.getCreatedOn()),
                        exporter.makeExcelExport(releases), CONTENT_TYPE_OPENXML_SPREADSHEET);
            }
        } catch (IOException | TException e) {
            log.error("An error occurred while generating the Excel export", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }

    private RequestStatus removeProject(PortletRequest request) {
        String projectId = request.getParameter(PortalConstants.PROJECT_ID);
        String encodedDeleteComment = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
        final User user = UserCacheHolder.getUserFromRequest(request);
        if(encodedDeleteComment != null) {
            String deleteComment = new String(Base64.getDecoder().decode(encodedDeleteComment));
            user.setCommentMadeDuringModerationRequest(deleteComment);
        }

        try {
            deleteUnneededAttachments(user.getEmail(), projectId);
            ProjectService.Iface client = thriftClients.makeProjectClient();
            return client.deleteProject(projectId, user);
        } catch (TException e) {
            log.error("Error deleting project from backend", e);
        }

        return RequestStatus.FAILURE;
    }

    private void serveLinkedProjects(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String what = request.getParameter(PortalConstants.WHAT);

        if (PortalConstants.LIST_NEW_LINKED_PROJECTS.equals(what)) {
            String[] where = request.getParameterValues(PortalConstants.WHERE_ARRAY);
            serveNewTableRowLinkedProjects(request, response, where);
        } else if (PortalConstants.PROJECT_SEARCH.equals(what)) {
            String where = request.getParameter(PortalConstants.WHERE);
            serveProjectSearchResults(request, response, where);
        }
    }

    private void serveLinkedReleases(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String what = request.getParameter(PortalConstants.WHAT);

        String projectId = request.getParameter(PROJECT_ID);

        if (PortalConstants.LIST_NEW_LINKED_RELEASES.equals(what)) {
            String[] where = request.getParameterValues(PortalConstants.WHERE_ARRAY);
            serveNewTableRowLinkedRelease(request, response, where);
        } else if (PortalConstants.RELEASE_SEARCH.equals(what)) {
            String where = request.getParameter(PortalConstants.WHERE);
            serveReleaseSearchResults(request, response, where);
        } else if (PortalConstants.RELEASE_LIST_FROM_LINKED_PROJECTS.equals(what)) {
            serveReleasesFromLinkedProjects(request, response, projectId);
        }
    }

    private void serveNewTableRowLinkedProjects(ResourceRequest request, ResourceResponse response, String[] linkedIds) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);

        List<ProjectLink> linkedProjects = new ArrayList<>();
        try {
            ProjectService.Iface client = thriftClients.makeProjectClient();

            for (String linkedId : linkedIds) {
                Project project = client.getProjectById(linkedId, user);
                ProjectLink linkedProject = new ProjectLink(linkedId, project.getName());
                linkedProject.setRelation(ProjectRelationship.CONTAINED);
                linkedProject.setVersion(project.getVersion());
                linkedProjects.add(linkedProject);
            }
        } catch (TException e) {
            log.error("Error getting projects!", e);
            throw new PortletException("cannot get projects " + Arrays.toString(linkedIds), e);
        }

        request.setAttribute(PROJECT_LIST, linkedProjects);

        include("/html/projects/ajax/linkedProjectsAjax.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    @SuppressWarnings("Duplicates")
    private void serveNewTableRowLinkedRelease(ResourceRequest request, ResourceResponse response, String[] linkedIds) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user));

        List<ReleaseLink> linkedReleases = new ArrayList<>();
        try {
            ComponentService.Iface client = thriftClients.makeComponentClient();
            for (Release release : client.getReleasesById(new HashSet<>(Arrays.asList(linkedIds)), user)) {
                final Vendor vendor = release.getVendor();
                final String vendorName = vendor != null ? vendor.getShortname() : "";
                ReleaseLink linkedRelease = new ReleaseLink(release.getId(), vendorName, release.getName(), release.getVersion(),
                        SW360Utils.printFullname(release), !nullToEmptyMap(release.getReleaseIdToRelationship()).isEmpty());
                linkedReleases.add(linkedRelease);
            }
        } catch (TException e) {
            log.error("Error getting releases!", e);
            throw new PortletException("cannot get releases " + Arrays.toString(linkedIds), e);
        }
        request.setAttribute(RELEASE_LIST, linkedReleases);
        include("/html/utils/ajax/linkedReleasesAjax.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }


    private void serveProjectSearchResults(ResourceRequest request, ResourceResponse response, String searchText) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        List<Project> searchResult;

        try {
            ProjectService.Iface client = thriftClients.makeProjectClient();
            if (isNullOrEmpty(searchText)) {
                searchResult = client.getAccessibleProjectsSummary(user);
            } else {
                searchResult = client.search(searchText);
            }
        } catch (TException e) {
            log.error("Error searching projects", e);
            searchResult = Collections.emptyList();
        }

        request.setAttribute(PortalConstants.PROJECT_SEARCH, searchResult);

        include("/html/projects/ajax/searchProjectsAjax.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    private void serveReleaseSearchResults(ResourceRequest request, ResourceResponse response, String searchText) throws IOException, PortletException {
        serveReleaseSearch(request, response, searchText);
    }

    private void serveReleasesFromLinkedProjects(ResourceRequest request, ResourceResponse response, String projectId) throws IOException, PortletException {
        List<Release> searchResult;

        Set<String> releaseIdsFromLinkedProjects = new HashSet<>();

        User user = UserCacheHolder.getUserFromRequest(request);

        try {
            ComponentService.Iface componentClient = thriftClients.makeComponentClient();
            ProjectService.Iface projectClient = thriftClients.makeProjectClient();

            Project project = projectClient.getProjectById(projectId, user);

            Map<String, ProjectRelationship> linkedProjects = CommonUtils.nullToEmptyMap(project.getLinkedProjects());
            for (String linkedProjectId : linkedProjects.keySet()) {
                Project linkedProject = projectClient.getProjectById(linkedProjectId, user);

                if (linkedProject != null) {
                    Map<String, ProjectReleaseRelationship> releaseIdToUsage = CommonUtils.nullToEmptyMap(linkedProject.getReleaseIdToUsage());
                    releaseIdsFromLinkedProjects.addAll(releaseIdToUsage.keySet());
                }
            }

            if (releaseIdsFromLinkedProjects.size() > 0) {
                searchResult = componentClient.getReleasesById(releaseIdsFromLinkedProjects, user);
            } else {
                searchResult = Collections.emptyList();
            }


        } catch (TException e) {
            log.error("Error searching projects", e);
            searchResult = Collections.emptyList();
        }

        request.setAttribute(PortalConstants.RELEASE_SEARCH, searchResult);

        include("/html/utils/ajax/searchReleasesAjax.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    private void serveAttachmentFileLicenses(ResourceRequest request, ResourceResponse response) throws IOException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        final String attachmentContentId = request.getParameter(PortalConstants.ATTACHMENT_ID);
        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
        final LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();

        try {
            Release release = componentClient.getReleaseById(request.getParameter(PortalConstants.RELEASE_ID), user);
            List<LicenseInfoParsingResult> licenseInfos = licenseInfoClient.getLicenseInfoForAttachment(release, attachmentContentId, user);

            // We generate a JSON-serializable list of licenses here.
            // In addition we remember the license information for exclusion later on
            Map<String, LicenseNameWithText> licenseStore = Maps.newHashMap();
            List<Map<String, String>> licenses = Lists.newArrayList();
            licenseInfos.forEach(licenseInfoResult ->
                    addLicenseInfoResultToJsonSerializableLicensesList(licenseInfoResult, licenses, licenseStore::put));
            licenses.sort((l1, l2) ->
                    Strings.nullToEmpty(l1.get(LICENSE_NAME_WITH_TEXT_NAME))
                            .compareTo(l2.get(LICENSE_NAME_WITH_TEXT_NAME)));

            request.getPortletSession()
                    .setAttribute(LICENSE_STORE_KEY_PREFIX + request.getParameter(PortalConstants.PROJECT_PATH) + ":"
                            + release.getId() + ":" + attachmentContentId, licenseStore);
            writeJSON(request, response, OBJECT_MAPPER.writeValueAsString(licenses));
        } catch (TException exception) {
            log.error("Cannot retrieve license information for attachment id " + attachmentContentId + ".", exception);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
        }
    }

    private void addLicenseInfoResultToJsonSerializableLicensesList(LicenseInfoParsingResult licenseInfoResult,
                                                                    List<Map<String, String>> licenses,
                                                                    BiConsumer<String, LicenseNameWithText> storeLicense) {
        switch (licenseInfoResult.getStatus()){
            case SUCCESS:
                Set<LicenseNameWithText> licenseNamesWithTexts = nullToEmptySet(licenseInfoResult.getLicenseInfo().getLicenseNamesWithTexts());
                List<Map<String, String>> licensesAsObject = licenseNamesWithTexts.stream()
                        .filter(licenseNameWithText -> !Strings.isNullOrEmpty(licenseNameWithText.getLicenseName())
                                || !Strings.isNullOrEmpty(licenseNameWithText.getLicenseText())).map(licenseNameWithText -> {
                            // Since the license has no good identifier, we create one and store the license
                            // in the session. If the final report is generated, we use the identifier to
                            // identify the licenses to be excluded
                            // FIXME: this could be changed if we scan the attachments once after uploading
                            // and store them as own entity
                            String key = UUID.randomUUID().toString();
                            storeLicense.accept(key, licenseNameWithText);

                            Map<String, String> data = Maps.newHashMap();
                            data.put(LICENSE_NAME_WITH_TEXT_KEY, key);
                            data.put(LICENSE_NAME_WITH_TEXT_NAME, Strings.isNullOrEmpty(licenseNameWithText.getLicenseName()) ? EMPTY
                                    : licenseNameWithText.getLicenseName());
                            data.put(LICENSE_NAME_WITH_TEXT_TEXT, licenseNameWithText.getLicenseText());
                            return data;
                        }).collect(Collectors.toList());

                licenses.addAll(licensesAsObject);
                break;
            case FAILURE:
            case NO_APPLICABLE_SOURCE:
                LicenseInfo licenseInfo = licenseInfoResult.getLicenseInfo();
                String filename = Optional.ofNullable(licenseInfo)
                        .map(LicenseInfo::getFilenames)
                        .map(CommonUtils.COMMA_JOINER::join)
                        .orElse("<filename unknown>");
                String message = Optional.ofNullable(licenseInfoResult.getMessage())
                        .orElse("<no message>");
                licenses.add(ImmutableMap.of(LICENSE_NAME_WITH_TEXT_ERROR, message,
                        LICENSE_NAME_WITH_TEXT_FILE, filename));
                break;
            default:
                throw new IllegalArgumentException("Unknown LicenseInfoRequestStatus: " + licenseInfoResult.getStatus());
        }
    }

    private void serveAttachmentUsages(ResourceRequest request, ResourceResponse response, UsageData filter) throws IOException {
        final String projectId = request.getParameter(PortalConstants.PROJECT_ID);
        final AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

        try {
            List<AttachmentUsage> usages = attachmentClient.getUsedAttachments(Source.projectId(projectId),
                    filter);
            String serializedUsages = usages.stream()
                    .map(usage -> wrapTException(() -> THRIFT_JSON_SERIALIZER.toString(usage)))
                    .collect(Collectors.joining(",", "[", "]"));

            writeJSON(request, response, serializedUsages);
        } catch (WrappedTException exception) {
            log.error("cannot retrieve information about attachment usages.", exception.getCause());
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
        } catch (TException exception) {
            log.error("cannot retrieve information about attachment usages.", exception);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
        }
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_DETAIL.equals(pageName)) {
            prepareDetailView(request, response);
            include("/html/projects/detail.jsp", request, response);
        } else if (PAGENAME_EDIT.equals(pageName)) {
            prepareProjectEdit(request);
            include("/html/projects/edit.jsp", request, response);
        } else if (PAGENAME_DUPLICATE.equals(pageName)) {
            prepareProjectDuplicate(request);
            include("/html/projects/edit.jsp", request, response);
        } else if (PAGENAME_LICENSE_INFO.equals(pageName)) {
            prepareLicenseInfo(request, response);
            include("/html/projects/licenseInfo.jsp", request, response);
        } else if (PAGENAME_SOURCE_CODE_BUNDLE.equals(pageName)) {
            prepareSourceCodeBundle(request, response);
            include("/html/projects/sourceCodeBundle.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) throws IOException {
        User user = UserCacheHolder.getUserFromRequest(request);
        List<Organization> organizations = UserUtils.getOrganizations(request);
        request.setAttribute(PortalConstants.ORGANIZATIONS, organizations);
        request.setAttribute(IS_USER_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.SW360_ADMIN, user) ? "Yes" : "No");
        for (Project._Fields filteredField : projectFilteredFields) {
            String parameter = request.getParameter(filteredField.toString());
            request.setAttribute(filteredField.getFieldName(), nullToEmpty(parameter));
        }
    }

    private List<Project> getFilteredProjectList(PortletRequest request) throws IOException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        Map<String, Set<String>> filterMap = loadFilterMapFromRequest(request);
        loadAndStoreStickyProjectGroup(request, user, filterMap);
        String id = request.getParameter(Project._Fields.ID.toString());
        return findProjectsByFiltersOrId(filterMap, id, user);
    }

    private void loadAndStoreStickyProjectGroup(PortletRequest request, User user, Map<String, Set<String>> filterMap) {
        String groupFilterValue = request.getParameter(Project._Fields.BUSINESS_UNIT.toString());
        if (null == groupFilterValue) {
            addStickyProjectGroupToFilters(request, user, filterMap);
        } else {
            ProjectPortletUtils.saveStickyProjectGroup(request, user, groupFilterValue);
        }
    }

    private List<Project> findProjectsByFiltersOrId(Map<String, Set<String>> filterMap, String id, User user) {
        ProjectService.Iface projectClient = thriftClients.makeProjectClient();
        List<Project> projectList;
        try {
            if (!isNullOrEmpty(id)){ // the presence of the id signals to load linked projects hierarchy instead of using filters
                final Collection<ProjectLink> projectLinks = SW360Utils.getLinkedProjectsAsFlatList(id, true, thriftClients, log, user);
                List<String> linkedProjectIds = projectLinks.stream().map(ProjectLink::getId).collect(Collectors.toList());
                projectList = projectClient.getProjectsById(linkedProjectIds, user);
            } else {
                if (filterMap.isEmpty()) {
                    projectList = projectClient.getAccessibleProjectsSummary(user);
                } else {
                    projectList = projectClient.refineSearch(null, filterMap, user);
                }
            }
        } catch (TException e) {
            log.error("Could not search projects in backend ", e);
            projectList = Collections.emptyList();
        }
        return projectList;
    }

    @NotNull
    private Map<String, Set<String>> loadFilterMapFromRequest(PortletRequest request) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        for (Project._Fields filteredField : projectFilteredFields) {
            String parameter = request.getParameter(filteredField.toString());
            if (!isNullOrEmpty(parameter) && !((filteredField.equals(Project._Fields.PROJECT_TYPE) || filteredField.equals(Project._Fields.STATE))
                    && parameter.equals(PortalConstants.NO_FILTER))) {
                Set<String> values = CommonUtils.splitToSet(parameter);
                if (filteredField.equals(Project._Fields.NAME) || filteredField.equals(Project._Fields.VERSION)) {
                    values = values.stream().map(LuceneAwareDatabaseConnector::prepareWildcardQuery).collect(Collectors.toSet());
                }
                filterMap.put(filteredField.getFieldName(), values);
            }
            request.setAttribute(filteredField.getFieldName(), nullToEmpty(parameter));
        }
        return filterMap;
    }

    private void addStickyProjectGroupToFilters(PortletRequest request, User user, Map<String, Set<String>> filterMap){
        String stickyGroupFilter = ProjectPortletUtils.loadStickyProjectGroup(request, user);
        if (!isNullOrEmpty(stickyGroupFilter)) {
            String groupFieldName = Project._Fields.BUSINESS_UNIT.getFieldName();
            filterMap.put(groupFieldName, Sets.newHashSet(stickyGroupFilter));
            request.setAttribute(groupFieldName, stickyGroupFilter);
        }
    }

    private void prepareDetailView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);
        String id = request.getParameter(PROJECT_ID);
        setDefaultRequestAttributes(request);
        request.setAttribute(DOCUMENT_ID, id);
        if (id != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                Project project = client.getProjectById(id, user);
                project = getWithFilledClearingStateSummary(project, user);
                request.setAttribute(PROJECT, project);
                setAttachmentsInRequest(request, project);
                List<ProjectLink> mappedProjectLinks = createLinkedProjects(project, user);
                request.setAttribute(PROJECT_LIST, mappedProjectLinks);
                putDirectlyLinkedReleasesInRequest(request, project);
                Set<Project> usingProjects = client.searchLinkingProjects(id, user);
                request.setAttribute(USING_PROJECTS, usingProjects);
                int allUsingProjectCount = client.getCountByProjectId(id);
                request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectCount);
                putReleasesAndProjectIntoRequest(request, id, user);
                putVulnerabilitiesInRequest(request, id, user);
                putAttachmentUsagesInRequest(request, id);
                request.setAttribute(
                        WRITE_ACCESS_USER,
                        PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE));

                addProjectBreadcrumb(request, response, project);
                request.setAttribute(PROJECT_OBLIGATIONS, SW360Utils.getProjectObligations(project));
                request.setAttribute(IS_USER_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.SW360_ADMIN, user) ? "Yes" : "No");
                if (project.getReleaseIdToUsageSize() > 0) {
                    putLinkedObligations(request, project);
                }
            } catch (TException e) {
                log.error("Error fetching project from backend!", e);
                setSW360SessionError(request, ErrorMessages.ERROR_GETTING_PROJECT);
            }
        }
    }

    private void prepareLicenseInfo(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);
        String id = request.getParameter(PROJECT_ID);

        request.setAttribute(PortalConstants.SW360_USER, user);
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);
        request.setAttribute(PROJECT_LINK_TABLE_MODE, PROJECT_LINK_TABLE_MODE_LICENSE_INFO);

        if (id != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                Project project = client.getProjectById(id, user);
                request.setAttribute(PROJECT, project);
                request.setAttribute(DOCUMENT_ID, id);

                Map<String,String> extIdMap = project.getExternalIds();
                if (extIdMap != null) {
                    request.setAttribute("externalIds", extIdMap.keySet());
                }

                LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();
                List<OutputFormatInfo> outputFormats = licenseInfoClient.getPossibleOutputFormats();
                request.setAttribute(PortalConstants.LICENSE_INFO_OUTPUT_FORMATS, outputFormats);

                List<ProjectLink> mappedProjectLinks = createLinkedProjects(project,
                        filterAndSortAttachments(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES), true,
                        user);
                request.setAttribute(PROJECT_LIST, mappedProjectLinks);
                addProjectBreadcrumb(request, response, project);

                storePathsMapInRequest(request, mappedProjectLinks);
                storeAttachmentUsageCountInRequest(request, mappedProjectLinks, UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
            } catch (TException e) {
                log.error("Error fetching project from backend!", e);
                setSW360SessionError(request, ErrorMessages.ERROR_GETTING_PROJECT);
            }
        }
    }

    /**
     * Method generates a map with nodeIds of given {@link ProjectLink}s as keys.
     * The value is the corresponding project path as a {@link String}. A project
     * path denotes the concatenations of projectids from the root project of the
     * given link list to the current project, separated with ":". This map will be
     * put in the given {@link RenderRequest} as attribute value of
     *
     * @param request            the request to store the paths map into
     * @param mappedProjectLinks the list of projectlinks which describe the project
     *                           tree whose paths map should be generated
     */
    private void storePathsMapInRequest(RenderRequest request, List<ProjectLink> mappedProjectLinks) {
        Map<String, String> paths = new HashMap<>();

        for (ProjectLink link : mappedProjectLinks) {
            if (link.getTreeLevel() == 0) {
                paths.put(link.getId(), "");
                continue;
            }

            String path = "";
            ProjectLink current = link;
            while (current.getParentNodeId() != null) {
                final String parentNodeId = current.getParentNodeId();
                path = current.getId() + (path.length() > 0 ? ":" + path : "");
                Optional<ProjectLink> parent = mappedProjectLinks.stream()
                        .filter(l -> l.getNodeId().equals(parentNodeId)).findFirst();
                if (parent.isPresent()) {
                    current = parent.get();
                } else {
                    break;
                }
            }
            path = current.getId() + (path.length() > 0 ? ":" + path : "");
            paths.put(link.getNodeId(), path);
        }

        request.setAttribute(PortalConstants.PROJECT_PATHS, paths);
    }

    private void storeAttachmentUsageCountInRequest(RenderRequest request, List<ProjectLink> mappedProjectLinks, UsageData filter) throws TException {
        AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
        Map<Source, Set<String>> containedAttachments = ProjectPortletUtils
                .extractContainedAttachments(mappedProjectLinks);
        Map<Map<Source, String>, Integer> attachmentUsages = attachmentClient.getAttachmentUsageCount(containedAttachments,
                filter);
        Map<String, Integer> countMap = attachmentUsages.entrySet().stream().collect(Collectors.toMap(entry -> {
            Entry<Source, String> key = entry.getKey().entrySet().iterator().next();
            return key.getKey().getFieldValue() + "_" + key.getValue();
        }, Entry::getValue));
        request.setAttribute(ATTACHMENT_USAGE_COUNT_MAP, countMap);
    }

    private void prepareSourceCodeBundle(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);
        String id = request.getParameter(PROJECT_ID);

        request.setAttribute(PortalConstants.SW360_USER, user);
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);
        request.setAttribute(PROJECT_LINK_TABLE_MODE, PROJECT_LINK_TABLE_MODE_SOURCE_BUNDLE);

        if (id != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                Project project = client.getProjectById(id, user);
                request.setAttribute(PROJECT, project);
                request.setAttribute(DOCUMENT_ID, id);

                List<ProjectLink> mappedProjectLinks = createLinkedProjects(project,
                        filterAndSortAttachments(SW360Constants.SOURCE_CODE_ATTACHMENT_TYPES), true, user);
                request.setAttribute(PROJECT_LIST, mappedProjectLinks);
                addProjectBreadcrumb(request, response, project);
                storeAttachmentUsageCountInRequest(request, mappedProjectLinks, UsageData.sourcePackage(new SourcePackageUsage()));
            } catch (TException e) {
                log.error("Error fetching project from backend!", e);
                setSW360SessionError(request, ErrorMessages.ERROR_GETTING_PROJECT);
            }
        }
    }

    private Function<ProjectLink, ProjectLink> filterAndSortAttachments(Collection<AttachmentType> attachmentTypes) {
        Predicate<Attachment> filter = att -> attachmentTypes.contains(att.getAttachmentType());
        return createProjectLinkMapper(rl -> rl.setAttachments(nullToEmptyList(rl.getAttachments())
                .stream()
                .filter(filter)
                .sorted(Comparator
                        .comparing((Attachment a) -> nullToEmpty(a.getCreatedTeam()))
                        .thenComparing(Comparator.comparing((Attachment a) -> nullToEmpty(a.getCreatedOn())).reversed()))
                .collect(Collectors.toList())));
    }

    private String formatedMessageForVul(List<VulnerabilityCheckStatus> statusHistory) {
        return CommonVulnerabilityPortletUtils.formatedMessageForVul(statusHistory,
                e -> e.getVulnerabilityRating().name(),
                e -> e.getCheckedOn(),
                e -> e.getCheckedBy(),
                e -> e.getComment());
    }

    private boolean addToVulnerabilityRatings(Map<String, Map<String, VulnerabilityRatingForProject>> vulnerabilityRatings,
                                              Map<String, Map<String, String>> vulnerabilityTooltips,
                                              Map<String, Map<String, List<VulnerabilityCheckStatus>>> vulnerabilityIdToReleaseIdToStatus,
                                              VulnerabilityDTO vulnerability) {

        String vulnerabilityId = vulnerability.getExternalId();
        String releaseId = vulnerability.getIntReleaseId();
        if (!vulnerabilityTooltips.containsKey(vulnerabilityId)) {
            vulnerabilityTooltips.put(vulnerabilityId, new HashMap<>());
        }
        if (!vulnerabilityRatings.containsKey(vulnerabilityId)) {
            vulnerabilityRatings.put(vulnerabilityId, new HashMap<>());
        }
        List<VulnerabilityCheckStatus> vulnerabilityCheckStatusHistory = null;
        if(vulnerabilityIdToReleaseIdToStatus.containsKey(vulnerabilityId) && vulnerabilityIdToReleaseIdToStatus.get(vulnerabilityId).containsKey(releaseId)) {
            vulnerabilityCheckStatusHistory = vulnerabilityIdToReleaseIdToStatus.get(vulnerabilityId).get(releaseId);
        }
        if (vulnerabilityCheckStatusHistory != null && vulnerabilityCheckStatusHistory.size() > 0) {
            vulnerabilityTooltips.get(vulnerabilityId).put(releaseId, formatedMessageForVul(vulnerabilityCheckStatusHistory));

            VulnerabilityCheckStatus vulnerabilityCheckStatus = vulnerabilityCheckStatusHistory.get(vulnerabilityCheckStatusHistory.size() - 1);
            VulnerabilityRatingForProject rating = vulnerabilityCheckStatus.getVulnerabilityRating();
            vulnerabilityRatings.get(vulnerabilityId).put(releaseId, rating);
            if (rating != VulnerabilityRatingForProject.NOT_CHECKED) {
                return true;
            }
        } else {
            vulnerabilityTooltips.get(vulnerabilityId).put(releaseId, NOT_CHECKED_YET);
            vulnerabilityRatings.get(vulnerabilityId).put(releaseId, VulnerabilityRatingForProject.NOT_CHECKED);
        }
        return false;
    }

    private void putVulnerabilitiesInRequest(RenderRequest request, String id, User user) throws TException {
        VulnerabilityService.Iface vulClient = thriftClients.makeVulnerabilityClient();
        List<VulnerabilityDTO> vuls = vulClient.getVulnerabilitiesByProjectIdWithoutIncorrect(id, user);

        Optional<ProjectVulnerabilityRating> projectVulnerabilityRating = wrapThriftOptionalReplacement(vulClient.getProjectVulnerabilityRatingByProjectId(id, user));

        CommonVulnerabilityPortletUtils.putLatestVulnerabilitiesInRequest(request, vuls, user);
        CommonVulnerabilityPortletUtils.putMatchedByHistogramInRequest(request, vuls);
        putVulnerabilitiesMetadatasInRequest(request, vuls, projectVulnerabilityRating);
    }

    private void putVulnerabilitiesMetadatasInRequest(RenderRequest request, List<VulnerabilityDTO> vuls, Optional<ProjectVulnerabilityRating> projectVulnerabilityRating) {
        Map<String, Map<String, List<VulnerabilityCheckStatus>>> vulnerabilityIdToStatusHistory = projectVulnerabilityRating
                .map(ProjectVulnerabilityRating::getVulnerabilityIdToReleaseIdToStatus)
                .orElseGet(HashMap::new);

        int numberOfVulnerabilities = 0;
        int numberOfCheckedVulnerabilities = 0;
        Map<String, Map<String, String>> vulnerabilityTooltips = new HashMap<>();
        Map<String, Map<String, VulnerabilityRatingForProject>> vulnerabilityRatings = new HashMap<>();

        for (VulnerabilityDTO vul : vuls) {
            numberOfVulnerabilities++;
            boolean wasAddedVulChecked = addToVulnerabilityRatings(vulnerabilityRatings, vulnerabilityTooltips, vulnerabilityIdToStatusHistory, vul);
            if (wasAddedVulChecked) {
                numberOfCheckedVulnerabilities++;
            }
        }

        int numberOfUncheckedVulnerabilities = numberOfVulnerabilities - numberOfCheckedVulnerabilities;

        request.setAttribute(PortalConstants.VULNERABILITY_RATINGS, vulnerabilityRatings);
        request.setAttribute(PortalConstants.VULNERABILITY_CHECKSTATUS_TOOLTIPS, vulnerabilityTooltips);
        request.setAttribute(PortalConstants.NUMBER_OF_UNCHECKED_VULNERABILITIES, numberOfUncheckedVulnerabilities);
    }

    private Project getWithFilledClearingStateSummary(Project project, User user) {
        ProjectService.Iface projectClient = thriftClients.makeProjectClient();

        try {
            return projectClient.fillClearingStateSummary(Arrays.asList(project), user).get(0);
        } catch (TException e) {
            log.error("Could not get summary of release clearing states for projects!", e);
            return project;
        }
    }

    private List<Project> getWithFilledClearingStateSummaryIncludingSubprojects(List<Project> projects, User user) {
        ProjectService.Iface projectClient = thriftClients.makeProjectClient();

        try {
            return projectClient.fillClearingStateSummaryIncludingSubprojects(projects, user);
        } catch (TException e) {
            log.error("Could not get summary of release clearing states for projects and their subprojects!", e);
            return projects;
        }
    }

    private void prepareProjectEdit(RenderRequest request) {
        User user = UserCacheHolder.getUserFromRequest(request);
        String id = request.getParameter(PROJECT_ID);
        setDefaultRequestAttributes(request);
        Project project;
        Set<Project> usingProjects;
        int allUsingProjectCount = 0;
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user));

        if (id != null) {

            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                project = client.getProjectByIdForEdit(id, user);
                usingProjects = client.searchLinkingProjects(id, user);
                allUsingProjectCount = client.getCountByProjectId(id);
            } catch (TException e) {
                log.error("Something went wrong with fetching the project", e);
                setSW360SessionError(request, ErrorMessages.ERROR_GETTING_PROJECT);
                return;
            }

            request.setAttribute(PROJECT, project);
            request.setAttribute(DOCUMENT_ID, id);
            request.setAttribute(PROJECT_OBLIGATIONS, SW360Utils.getProjectObligations(project));

            setAttachmentsInRequest(request, project);
            try {
                putDirectlyLinkedProjectsInRequest(request, project, user);
                putDirectlyLinkedReleasesInRequest(request, project);
            } catch (TException e) {
                log.error("Could not fetch linked projects or linked releases in projects view.", e);
                return;
            }

            if (project.getReleaseIdToUsageSize() > 0 && PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)
                    && PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
                putLinkedObligations(request, project);
            }
            request.setAttribute(USING_PROJECTS, usingProjects);
            request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectCount);
            Map<RequestedAction, Boolean> permissions = project.getPermissions();
            DocumentState documentState = project.getDocumentState();

            addEditDocumentMessage(request, permissions, documentState);
        } else {
            if(request.getAttribute(PROJECT) == null) {
                project = new Project();
                project.setBusinessUnit(user.getDepartment());
                request.setAttribute(PROJECT, project);
                setAttachmentsInRequest(request, project);
                try {
                    putDirectlyLinkedProjectsInRequest(request, project, user);
                    putDirectlyLinkedReleasesInRequest(request, project);
                } catch(TException e) {
                    log.error("Could not put empty linked projects or linked releases in projects view.", e);
                }
                request.setAttribute(USING_PROJECTS, Collections.emptySet());
                request.setAttribute(ALL_USING_PROJECTS_COUNT, 0);
                request.setAttribute(PROJECT_OBLIGATIONS, SW360Utils.getProjectObligations(project));

                SessionMessages.add(request, "request_processed", "New Project");
            }
        }

    }

    private void prepareProjectDuplicate(RenderRequest request) {
        User user = UserCacheHolder.getUserFromRequest(request);
        String id = request.getParameter(PROJECT_ID);
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user));
        setDefaultRequestAttributes(request);
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user));

        try {
            if (id != null) {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                String emailFromRequest = LifeRayUserSession.getEmailFromRequest(request);
                String department = user.getDepartment();

                Project newProject = PortletUtils.cloneProject(emailFromRequest, department, client.getProjectById(id, user));
                setAttachmentsInRequest(request, newProject);
                request.setAttribute(PROJECT, newProject);
                putDirectlyLinkedProjectsInRequest(request, newProject, user);
                putDirectlyLinkedReleasesInRequest(request, newProject);
                request.setAttribute(USING_PROJECTS, Collections.emptySet());
                request.setAttribute(ALL_USING_PROJECTS_COUNT, 0);
                request.setAttribute(SOURCE_PROJECT_ID, id);
            } else {
                Project project = new Project();
                project.setBusinessUnit(user.getDepartment());
                setAttachmentsInRequest(request, project);

                request.setAttribute(PROJECT, project);
                putDirectlyLinkedProjectsInRequest(request, project, user);
                putDirectlyLinkedReleasesInRequest(request, project);

                request.setAttribute(USING_PROJECTS, Collections.emptySet());
                request.setAttribute(ALL_USING_PROJECTS_COUNT, 0);
            }
        } catch (TException e) {
            log.error("Error fetching project from backend!", e);
        }

    }

    //! Actions
    @UsedAsLiferayAction
    public void delete(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        RequestStatus requestStatus = removeProject(request);
        setSessionMessage(request, requestStatus, "Project", "remove");
    }

    @UsedAsLiferayAction
    public void update(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String id = request.getParameter(PROJECT_ID);
        User user = UserCacheHolder.getUserFromRequest(request);
        RequestStatus requestStatus;
        try {
            ProjectService.Iface client = thriftClients.makeProjectClient();
            if (id != null) {
                Project project = client.getProjectByIdForEdit(id, user);
                ProjectPortletUtils.updateProjectFromRequest(request, project);
                String ModerationRequestCommentMsg = request.getParameter(MODERATION_REQUEST_COMMENT);
                user.setCommentMadeDuringModerationRequest(ModerationRequestCommentMsg);

                String cyclicLinkedProjectPath = client.getCyclicLinkedProjectPath(project, user);
                if (!isNullEmptyOrWhitespace(cyclicLinkedProjectPath)) {
                    addErrorMessages(cyclicLinkedProjectPath, request, response);
                    response.setRenderParameter(PROJECT_ID, id);
                    return;
                }

                requestStatus = client.updateProject(project, user);
                setSessionMessage(request, requestStatus, "Project", "update", printName(project));
                if (RequestStatus.DUPLICATE.equals(requestStatus) || RequestStatus.DUPLICATE_ATTACHMENT.equals(requestStatus)) {
                    if(RequestStatus.DUPLICATE.equals(requestStatus))
                        setSW360SessionError(request, ErrorMessages.PROJECT_DUPLICATE);
                    else
                        setSW360SessionError(request, ErrorMessages.DUPLICATE_ATTACHMENT);
                    response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
                    request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);
                    request.setAttribute(DOCUMENT_ID, id);
                    prepareRequestForEditAfterDuplicateError(request, project, user);
                } else {
                    cleanUploadHistory(user.getEmail(), id);
                    response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
                    response.setRenderParameter(PROJECT_ID, request.getParameter(PROJECT_ID));
                }
            } else {
                // Add project
                Project project = new Project();
                ProjectPortletUtils.updateProjectFromRequest(request, project);

                String cyclicLinkedProjectPath = client.getCyclicLinkedProjectPath(project, user);
                if (!isNullEmptyOrWhitespace(cyclicLinkedProjectPath)) {
                    addErrorMessages(cyclicLinkedProjectPath, request, response);
                    prepareRequestForEditAfterDuplicateError(request, project, user);
                    return;
                }

                AddDocumentRequestSummary summary = client.addProject(project, user);
                String  newProjectId= summary.getId();
                String sourceProjectId = request.getParameter(SOURCE_PROJECT_ID);

                if (sourceProjectId != null) {
                    copyAttachmentUsagesForClonedProject(request, sourceProjectId, newProjectId);
                }

                AddDocumentRequestStatus status = summary.getRequestStatus();
                switch(status) {
                    case SUCCESS:
                        String successMsg = "Project " + printName(project) + " added successfully";
                        SessionMessages.add(request, "request_processed", successMsg);
                        response.setRenderParameter(PROJECT_ID, summary.getId());
                        response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
                        break;
                    case DUPLICATE:
                        setSW360SessionError(request, ErrorMessages.PROJECT_DUPLICATE);
                        response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
                        prepareRequestForEditAfterDuplicateError(request, project, user);
                        break;
                    default:
                        setSW360SessionError(request, ErrorMessages.PROJECT_NOT_ADDED);
                        response.setRenderParameter(PAGENAME, PAGENAME_VIEW);
                }

            }
        } catch (TException e) {
            log.error("Error updating project in backend!", e);
            setSW360SessionError(request, ErrorMessages.DEFAULT_ERROR_MESSAGE);
        }
    }

    private void copyAttachmentUsagesForClonedProject(ActionRequest request, String sourceProjectId, String newProjectId)
            throws TException, PortletException {
        try {
            AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

            List<AttachmentUsage> attachmentUsages = wrapTException(
                    () -> attachmentClient.getUsedAttachments(Source.projectId(sourceProjectId), null));
            attachmentUsages.forEach(attachmentUsage -> {
                attachmentUsage.unsetId();
                attachmentUsage.setUsedBy(Source.projectId(newProjectId));
                if (attachmentUsage.isSetUsageData()
                        && attachmentUsage.getUsageData().getSetField().equals(UsageData._Fields.LICENSE_INFO)
                        && attachmentUsage.getUsageData().getLicenseInfo().isSetProjectPath()) {
                    LicenseInfoUsage licenseInfoUsage = attachmentUsage.getUsageData().getLicenseInfo();
                    String projectPath = licenseInfoUsage.getProjectPath();
                    licenseInfoUsage.setProjectPath(projectPath.replace(sourceProjectId, newProjectId));
                }
            });
            attachmentClient.makeAttachmentUsages(attachmentUsages);
        } catch (WrappedTException e) {
            throw new PortletException("Cannot clone attachment usages", e);
        }
    }

    private void prepareRequestForEditAfterDuplicateError(ActionRequest request, Project project, User user) throws TException {
        request.setAttribute(PROJECT, project);
        setAttachmentsInRequest(request, project);
        request.setAttribute(USING_PROJECTS, Collections.emptySet());
        request.setAttribute(ALL_USING_PROJECTS_COUNT, 0);
        putDirectlyLinkedProjectsInRequest(request, project, user);
        putDirectlyLinkedReleasesInRequest(request, project);
    }

    @UsedAsLiferayAction
    public void applyFilters(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        for (Project._Fields projectFilteredField : projectFilteredFields) {
            response.setRenderParameter(projectFilteredField.toString(), nullToEmpty(request.getParameter(projectFilteredField.toString())));
        }
    }

    private void updateVulnerabilitiesProject(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        String projectId = request.getParameter(PortalConstants.PROJECT_ID);
        CveSearchService.Iface cveClient = thriftClients.makeCvesearchClient();
        try {
            VulnerabilityUpdateStatus importStatus = cveClient.updateForProject(projectId);
            JSONObject responseData = PortletUtils.importStatusToJSON(importStatus);
            PrintWriter writer = response.getWriter();
            writer.write(responseData.toString());
        } catch (TException e) {
            log.error("Error updating CVEs for project in backend.", e);
        }
    }

    private void updateVulnerabilityRating(ResourceRequest request, ResourceResponse response) throws IOException {
        String projectId = request.getParameter(PortalConstants.PROJECT_ID);
        User user = UserCacheHolder.getUserFromRequest(request);

        VulnerabilityService.Iface vulClient = thriftClients.makeVulnerabilityClient();

        RequestStatus requestStatus = RequestStatus.FAILURE;
        try {
            Optional<ProjectVulnerabilityRating> projectVulnerabilityRatings = wrapThriftOptionalReplacement(vulClient.getProjectVulnerabilityRatingByProjectId(projectId, user));
            ProjectVulnerabilityRating link = ProjectPortletUtils.updateProjectVulnerabilityRatingFromRequest(projectVulnerabilityRatings, request);
            requestStatus = vulClient.updateProjectVulnerabilityRating(link, user);
        } catch (TException e) {
            log.error("Error updating vulnerability ratings for project in backend.", e);
        }

        JSONObject responseData = JSONFactoryUtil.createJSONObject();
        responseData.put(PortalConstants.REQUEST_STATUS, requestStatus.toString());
        PrintWriter writer = response.getWriter();
        writer.write(responseData.toString());
    }

    private String getProjectDefaultLicenseInfoHeaderText() {
        final LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();
        try {
            String defaultLicenseInfoHeaderText = licenseInfoClient.getDefaultLicenseInfoHeaderText();
            return defaultLicenseInfoHeaderText;
        } catch (TException e) {
            log.error("Could not load default license info header text from backend.", e);
            return "";
        }
    }

    private String getProjectDefaultObligationsText() {
        final LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();
        try {
            String defaultObligationsText = licenseInfoClient.getDefaultObligationsText();
            return defaultObligationsText;
        } catch (TException e) {
            log.error("Could not load default license info header text from backend.", e);
            return "";
        }
    }

    private void putLinkedObligations(RenderRequest request, Project project) {
        final User user = UserCacheHolder.getUserFromRequest(request);
        List<Release> releases;
        try {
            final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
            releases = componentClient.getFullReleasesById(project.getReleaseIdToUsage().keySet(), user);
            final List<LicenseInfoParsingResult> licenseInfos = addLicenseInfos(request, project, releases);
            sortLicenseInfo(licenseInfos);
            request.setAttribute(PROJECT_RELEASE_LICENSE_INFO, licenseInfos);
        } catch (TException exception) {
            log.error("Could not fetch Release Information: ", exception);
        }
    }

    private List<LicenseInfoParsingResult> addLicenseInfos(RenderRequest request, Project project, List<Release> releases) {
        final User user = UserCacheHolder.getUserFromRequest(request);
        final LicenseInfoService.Iface licenseClient = thriftClients.makeLicenseInfoClient();
        final List<LicenseInfoParsingResult> licenseInfos = new ArrayList<LicenseInfoParsingResult>();
        releases.forEach(release -> {
            Attachment filteredAttachment = SW360Utils.getApprovedClxAttachmentForRelease(release);

            if (filteredAttachment.isSetAttachmentContentId()) {
                release.unsetAttachments();
                release.setAttachments(new HashSet<Attachment>(Arrays.asList(filteredAttachment)));

                try {
                    List<LicenseInfoParsingResult> licenseResults = licenseClient.getLicenseInfoForAttachment(release,
                            filteredAttachment.getAttachmentContentId(), user);

                    List<ObligationParsingResult> obligationResults = licenseClient.getObligationsForAttachment(release,
                            filteredAttachment.getAttachmentContentId(), user);

                    if (licenseResults.size() > 0 && obligationResults.size() > 0) {
                        licenseInfos.add(licenseClient.createLicenseToObligationMapping(licenseResults.get(0), obligationResults.get(0)));
                    }
                } catch (TException exception) {
                    log.error(String.format("Could not fetch license Information for attachment: %s in release: %s",
                            filteredAttachment.getFilename(), release.getId()), exception);
                }
            }
        });
        setLinkedObligations(project, licenseInfos);
        request.setAttribute(APPROVED_OBLIGATIONS_COUNT, getFulfilledObligationsCount(project));
        return licenseInfos;
    }

    private void setLinkedObligations(Project project, List<LicenseInfoParsingResult> licenseResults) {
        final LicenseInfoService.Iface licenseClient = thriftClients.makeLicenseInfoClient();
        Map<Project, List<LicenseInfoParsingResult>> projectObligationMap = Maps.newHashMap();
        try {
            projectObligationMap = licenseClient.setProjectObligationStatus(project, licenseResults);
            project.unsetLinkedObligations();
            licenseResults.clear();
            licenseResults.addAll(projectObligationMap.values().iterator().next());
            project.setLinkedObligations(projectObligationMap.keySet().iterator().next().getLinkedObligations());
        } catch (TException exception) {
            log.error(String.format("Failed to set linked obligations for project: %s ", SW360Utils.printName(project)),
                    exception);
        }
    }

    private void sortLicenseInfo(List<LicenseInfoParsingResult> licenseInfos) {
        licenseInfos.stream()
                .sorted(Comparator.comparing(LicenseInfoParsingResult::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(e -> {
                    e.getLicenseInfo().setLicenseNamesWithTexts(e.getLicenseInfo().getLicenseNamesWithTexts().stream()
                            .sorted(Comparator.comparing(LicenseNameWithText::getType, String.CASE_INSENSITIVE_ORDER)
                                    .thenComparing(LicenseNameWithText::getLicenseSpdxId, String.CASE_INSENSITIVE_ORDER))
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
                });
    }

    private int getFulfilledObligationsCount(Project project) {
        if (project.isSetLinkedObligations() && project.getLinkedObligationsSize() > 0) {
            return Math.toIntExact(project.getLinkedObligations().values().stream()
                    .filter(obligation -> ProjectObligationStatus.FULFILLED.equals(obligation.getStatus())).count());
        }
        return 0;
    }

    private void serveProjectList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        HttpServletRequest originalServletRequest = PortalUtil.getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
        PaginationParameters paginationParameters = PaginationParser.parametersFrom(originalServletRequest);
        handlePaginationSortOrder(request, paginationParameters);
        List<Project> projectList = getFilteredProjectList(request);

        JSONArray jsonProjects = getProjectData(projectList, paginationParameters);
        JSONObject jsonResult = createJSONObject();
        jsonResult.put(DATATABLE_RECORDS_TOTAL, projectList.size());
        jsonResult.put(DATATABLE_RECORDS_FILTERED, projectList.size());
        jsonResult.put(DATATABLE_DISPLAY_DATA, jsonProjects);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem rendering RequestStatus", e);
        }
    }

    private void handlePaginationSortOrder(ResourceRequest request, PaginationParameters paginationParameters) {
        if (!paginationParameters.getSortingColumn().isPresent()) {
            for (Project._Fields filteredField : projectFilteredFields) {
                if (!isNullOrEmpty(request.getParameter(filteredField.toString()))) {
                    paginationParameters.setSortingColumn(Optional.of(PROJECT_NO_SORT));
                    break;
                }
            }
        }
    }

    public JSONArray getProjectData(List<Project> projectList, PaginationParameters projectParameters) {
        List<Project> sortedProjects = sortProjectList(projectList, projectParameters);
        int count = getProjectDataCount(projectParameters, projectList.size());

        JSONArray projectData = createJSONArray();
        for (int i = projectParameters.getDisplayStart(); i < count; i++) {
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
            Project project = sortedProjects.get(i);
            jsonObject.put("id", project.getId());
            jsonObject.put("DT_RowId", project.getId());
            jsonObject.put("name", SW360Utils.printName(project));
            jsonObject.put("desc", nullToEmptyString(project.getDescription()));
            jsonObject.put("state", nullToEmptyString(project.getState()));
            jsonObject.put("cState", nullToEmptyString(project.getClearingState()));
            jsonObject.put("clearing", "Not loaded yet");
            jsonObject.put("resp", nullToEmptyString(project.getProjectResponsible()));
            jsonObject.put("lProjSize", String.valueOf(project.getLinkedProjectsSize()));
            jsonObject.put("lRelsSize", String.valueOf(project.getReleaseIdToUsageSize()));
            jsonObject.put("attsSize", String.valueOf(project.getAttachmentsSize()));
            projectData.put(jsonObject);
        }

        return projectData;
    }

    private int getProjectDataCount(PaginationParameters projectParameters, int maxSize) {
        if (projectParameters.getDisplayLength() == -1) {
            return maxSize;
        } else {
            return min(projectParameters.getDisplayStart() + projectParameters.getDisplayLength(), maxSize);
        }
    }

    private void setDefaultRequestAttributes(RenderRequest request) {
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);
        request.setAttribute(DEFAULT_LICENSE_INFO_HEADER_TEXT, getProjectDefaultLicenseInfoHeaderText());
        request.setAttribute(DEFAULT_OBLIGATIONS_TEXT, getProjectDefaultObligationsText());
    }

    private List<Project> sortProjectList(List<Project> projectList, PaginationParameters projectParameters) {
        boolean isAsc = projectParameters.isAscending().orElse(true);

        switch (projectParameters.getSortingColumn().orElse(PROJECT_DT_ROW_NAME)) {
            case PROJECT_DT_ROW_NAME:
                Collections.sort(projectList, compareByName(isAsc));
                break;
            case PROJECT_DT_ROW_DESCRIPTION:
                Collections.sort(projectList, compareByDescription(isAsc));
                break;
            case PROJECT_DT_ROW_RESPONSIBLE:
                Collections.sort(projectList, compareByResponsible(isAsc));
                break;
            case PROJECT_DT_ROW_STATE:
                Collections.sort(projectList, compareByState(isAsc));
                break;
            case PROJECT_DT_ROW_CLEARING_STATE:
                break;
            case PROJECT_DT_ROW_ACTION:
                break;
            default:
                break;
        }

        return projectList;
    }

    private Comparator<Project> compareByName(boolean isAscending) {
        Comparator<Project> comparator = Comparator.comparing(
                p -> SW360Utils.printName(p).toLowerCase());
        return isAscending ? comparator : comparator.reversed();
    }

    private Comparator<Project> compareByDescription(boolean isAscending) {
        Comparator<Project> comparator = Comparator.comparing(
                p -> nullToEmptyString(p.getDescription()));
        return isAscending ? comparator : comparator.reversed();
    }

    private Comparator<Project> compareByResponsible(boolean isAscending) {
        Comparator<Project> comparator = Comparator.comparing(
                p -> nullToEmptyString(p.getProjectResponsible()));
        return isAscending ? comparator : comparator.reversed();
    }

    private Comparator<Project> compareByState(boolean isAscending) {
        Comparator<Project> comparator = Comparator.comparing(
                p -> nullToEmptyString(p.getState()));
        return isAscending ? comparator : comparator.reversed();
    }

    private void addErrorMessages(String cyclicHierarchy, ActionRequest request, ActionResponse response) {
        SessionErrors.add(request, "custom_error");
        request.setAttribute("cyclicError", CYCLIC_LINKED_PROJECT + cyclicHierarchy);
        SessionMessages.add(request,
                PortalUtil.getPortletId(request) + SessionMessages.KEY_SUFFIX_HIDE_DEFAULT_ERROR_MESSAGE);
        SessionMessages.add(request,
                PortalUtil.getPortletId(request) + SessionMessages.KEY_SUFFIX_HIDE_DEFAULT_SUCCESS_MESSAGE);
        response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
    }
}
