/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.model.Organization;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.datahandler.common.WrappedException.WrappedTException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.cvesearch.CveSearchService;
import org.eclipse.sw360.datahandler.thrift.cvesearch.VulnerabilityUpdateStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.exporter.ProjectExporter;
import org.eclipse.sw360.exporter.ReleaseExporter;
import org.eclipse.sw360.portal.common.*;
import org.eclipse.sw360.portal.portlets.FossologyAwarePortlet;
import org.eclipse.sw360.portal.users.LifeRayUserSession;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.eclipse.sw360.portal.users.UserUtils;
import org.jetbrains.annotations.NotNull;

import javax.portlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONArray;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONObject;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CONTENT_TYPE_OPENXML_SPREADSHEET;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

/**
 * Project portlet implementation
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ProjectPortlet extends FossologyAwarePortlet {
    private static final Logger log = Logger.getLogger(ProjectPortlet.class);

    private static final String NOT_CHECKED_YET = "Not checked yet.";
    private static final String EMPTY = "<empty>";
    private static final String LICENSE_NAME_WITH_TEXT_KEY = "key";
    private static final String LICENSE_NAME_WITH_TEXT_NAME = "name";
    private static final String LICENSE_NAME_WITH_TEXT_TEXT = "text";
    private static final String LICENSE_NAME_WITH_TEXT_ERROR = "error";
    private static final String LICENSE_NAME_WITH_TEXT_FILE = "file";

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

    @SuppressWarnings("Duplicates")
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.VIEW_LINKED_PROJECTS.equals(action)) {
            serveLinkedProjects(request, response);
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
            serveLicenseInfoAttachmentUsage(request, response);
        } else if (isGenericAction(action)) {
            dealWithGenericAction(request, response, action);
        }
    }

    private void downloadLicenseInfo(ResourceRequest request, ResourceResponse response) throws IOException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        final String projectId = request.getParameter(PROJECT_ID);
        final String outputGenerator = request.getParameter(PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT);
        final Map<String, Set<String>> selectedReleaseAndAttachmentIds = ProjectPortletUtils
                .getSelectedReleaseAndAttachmentIdsFromRequest(request);
        final Set<String> attachmentIds = selectedReleaseAndAttachmentIds.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toSet());
        final Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachmentId = ProjectPortletUtils
                .getExcludedLicensesPerAttachmentIdFromRequest(attachmentIds, request);

        try {
            final LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();
            final ProjectService.Iface projectClient = thriftClients.makeProjectClient();

            Project project = projectClient.getProjectById(projectId, user);
            LicenseInfoFile licenseInfoFile = licenseInfoClient.getLicenseInfoFile(project, user, outputGenerator,
                    selectedReleaseAndAttachmentIds, excludedLicensesPerAttachmentId);
            try {
                replaceAttachmentUsages(user, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachmentId, project);
            } catch (TException e) {
                // there's no need to abort the user's desired action just because the ancillary action of storing selection failed
                log.warn("LicenseInfo usage is not stored due to exception: ", e);
            }

            OutputFormatInfo outputFormatInfo = licenseInfoFile.getOutputFormatInfo();
            String filename = String.format("LicenseInfo-%s-%s.%s", project.getName(), SW360Utils.getCreatedOn(),
                    outputFormatInfo.getFileExtension());
            String mimetype = outputFormatInfo.getMimeType();
            if (isNullOrEmpty(mimetype)) {
                mimetype = URLConnection.guessContentTypeFromName(filename);
            }

            PortletResponseUtil.sendFile(request, response, filename, licenseInfoFile.getGeneratedOutput(), mimetype);
        } catch (TException e) {
            log.error("Error getting LicenseInfo file for project with id " + projectId + " and generator " + outputGenerator, e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }

    private void replaceAttachmentUsages(User user, Map<String, Set<String>> selectedReleaseAndAttachmentIds, Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachmentId, Project project) throws TException {
        List<AttachmentUsage> attachmentUsages = ProjectPortletUtils.makeAttachmentUsages(project, selectedReleaseAndAttachmentIds,
                excludedLicensesPerAttachmentId);
        if (PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
            AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
            if (attachmentUsages.isEmpty()) {
                attachmentClient.deleteAttachmentUsagesByUsageDataType(Source.projectId(project.getId()),
                        UsageData.licenseInfo(new LicenseInfoUsage(Collections.emptySet())));
            } else {
                attachmentClient.replaceAttachmentUsages(Source.projectId(project.getId()), attachmentUsages);
            }
        } else {
            log.info("LicenseInfo usage is not stored since the user has no write permissions for this project.");
        }
    }

    private String getSourceCodeBundleName(ResourceRequest request) throws TException {
        User user = UserCacheHolder.getUserFromRequest(request);
        ProjectService.Iface projectClient = thriftClients.makeProjectClient();
        String projectId = request.getParameter(PROJECT_ID);
        Project project = projectClient.getProjectById(projectId, user);
        String timestamp = SW360Utils.getCreatedOn();
        return "SourceCodeBundle-" + project.getName() + "-" + timestamp + ".zip";
    }

    private void downloadSourceCodeBundle(ResourceRequest request, ResourceResponse response) {

        Map<String, Set<String>> selectedReleaseAndAttachmentIds = ProjectPortletUtils.getSelectedReleaseAndAttachmentIdsFromRequest(request);
        Set<String> selectedAttachmentIds = new HashSet<>();
        selectedReleaseAndAttachmentIds.entrySet()
                .forEach(e -> selectedAttachmentIds.addAll(e.getValue()));

        try {
            String sourceCodeBundleName = getSourceCodeBundleName(request);

            new AttachmentPortletUtils()
                    .serveAttachmentBundle(selectedAttachmentIds,request,response, Optional.of(sourceCodeBundleName));
        } catch (TException e) {
            log.error("Failed to get project metadata", e);
        }
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
        if (PortalConstants.FOSSOLOGY_SEND.equals(action)) {
            serveProjectSendToFossology(request, response);
        } else if (PortalConstants.FOSSOLOGY_GET_SENDABLE.equals(action)) {
            serveGetSendableReleases(request, response);
        } else if (PortalConstants.FOSSOLOGY_GET_STATUS.equals(action)) {
            serveFossologyStatus(request, response);
        }
    }

    private void serveRemoveProject(ResourceRequest request, ResourceResponse response) throws IOException {
        RequestStatus requestStatus = removeProject(request);
        serveRequestStatus(request, response, requestStatus, "Problem removing project", log);
    }

    private void exportExcel(ResourceRequest request, ResourceResponse response) {
        final User user = UserCacheHolder.getUserFromRequest(request);
        try {
            boolean extendedByReleases = Boolean.valueOf(request.getParameter(PortalConstants.EXTENDED_EXCEL_EXPORT));
            List<Project> projects = getFilteredProjectList(request);
            ProjectExporter exporter = new ProjectExporter(
                    thriftClients.makeComponentClient(),
                    thriftClients.makeProjectClient(),
                    user,
                    projects,
                    extendedByReleases);
            PortletResponseUtil.sendFile(request, response, "Projects.xlsx", exporter.makeExcelExport(projects), CONTENT_TYPE_OPENXML_SPREADSHEET);
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
                searchResult = client.searchByName(searchText, user);
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

    @SuppressWarnings("Duplicates")
    private void serveReleaseSearch(ResourceRequest request, ResourceResponse response, String searchText) throws IOException, PortletException {
        List<Release> searchResult;

        try {

            ComponentService.Iface componentClient = thriftClients.makeComponentClient();

            // First: Search by name:
            searchResult = componentClient.searchReleaseByNamePrefix(searchText);

            // If one is searching for everything, then it makes no sense to search also by vendors, as 'searchResult' already contains all releases.
            if(searchText != "") {
                // Second: Search by vendor and append:
                //  Note: The search-by-vendor by definition does not give results if the search string is empty.
                //        Therefore, an empty-string search does not introduce duplicated results, which is nice.
                final VendorService.Iface vendorClient = thriftClients.makeVendorClient();
                final Set<String> vendorIds = vendorClient.searchVendorIds(searchText);
                if (vendorIds != null && vendorIds.size() > 0) {
                    searchResult.addAll(componentClient.getReleasesFromVendorIds(vendorIds));
                }
            }
        } catch (TException e) {
            log.error("Error searching projects", e);
            searchResult = Collections.emptyList();
        }

        request.setAttribute(PortalConstants.RELEASE_SEARCH, searchResult);

        include("/html/utils/ajax/searchReleasesAjax.jsp", request, response, PortletRequest.RESOURCE_PHASE);
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

            request.getPortletSession().setAttribute(LICENSE_STORE_KEY_PREFIX + attachmentContentId, licenseStore);
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

    private void serveLicenseInfoAttachmentUsage(ResourceRequest request, ResourceResponse response) throws IOException {
        final String projectId = request.getParameter(PortalConstants.PROJECT_ID);
        final AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

        try {
            List<AttachmentUsage> usages = attachmentClient.getUsedAttachments(Source.projectId(projectId),
                    UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
            String serializedUsages = usages.stream()
                    .map(usage -> WrappedException.wrapTException(() -> THRIFT_JSON_SERIALIZER.toString(usage)))
                    .collect(Collectors.joining(",", "[", "]"));

            writeJSON(request, response, serializedUsages);
        } catch (WrappedTException exception) {
            log.error("cannot retrieve information about used license info files and exclusions.", exception.getCause());
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "500");
        } catch (TException exception) {
            log.error("cannot retrieve information about used license info files and exclusions.", exception);
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
        List<Project> projectList = getFilteredProjectList(request);

        // The data should be sorted like it will be initially sorted on the client
        // side. That way additional data can be loaded in order the data is visible to
        // the user.
        // As we currently use no defined sorting on the client, jquery dataTables
        // default sorting is used. This is for the first column and case insensitive.
        // So we duplicate that sorting here.
        List<Project> sortedProjectList = projectList.stream()
                .sorted(Comparator.comparing(p -> SW360Utils.printName(p).toLowerCase())).collect(Collectors.toList());
        request.setAttribute(PROJECT_LIST, sortedProjectList);

        List<Organization> organizations = UserUtils.getOrganizations(request);
        request.setAttribute(PortalConstants.ORGANIZATIONS, organizations);
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
            if (!isNullOrEmpty(parameter)) {
                Set<String> values = CommonUtils.splitToSet(parameter);
                if (filteredField.equals(Project._Fields.NAME)) {
                    values = values.stream().map(v -> v + "*").collect(Collectors.toSet());
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
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);
        request.setAttribute(DOCUMENT_ID, id);
        request.setAttribute(DEFAULT_LICENSE_INFO_HEADER_TEXT, getProjectDefaultLicenseInfoHeaderText());
        if (id != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                Project project = client.getProjectById(id, user);
                project = getWithFilledClearingStateSummary(project, user);
                request.setAttribute(PROJECT, project);
                setAttachmentsInRequest(request, project.getAttachments());
                List<ProjectLink> mappedProjectLinks = createLinkedProjects(project, user);
                request.setAttribute(PROJECT_LIST, mappedProjectLinks);
                putDirectlyLinkedReleasesInRequest(request, project);
                Set<Project> usingProjects = client.searchLinkingProjects(id, user);
                request.setAttribute(USING_PROJECTS, usingProjects);
                int allUsingProjectCount = client.getCountByProjectId(id);
                request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectCount);
                putReleasesAndProjectIntoRequest(request, id, user);
                putVulnerabilitiesInRequest(request, id, user);
                request.setAttribute(
                        VULNERABILITY_RATINGS_EDITABLE,
                        PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE));

                addProjectBreadcrumb(request, response, project);

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

                LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();
                List<OutputFormatInfo> outputFormats = licenseInfoClient.getPossibleOutputFormats();
                request.setAttribute(PortalConstants.LICENSE_INFO_OUTPUT_FORMATS, outputFormats);

                List<ProjectLink> mappedProjectLinks = createLinkedProjects(project,
                        filterAndSortAttachments(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES), true,
                        user);
                request.setAttribute(PROJECT_LIST, mappedProjectLinks);
                addProjectBreadcrumb(request, response, project);

                AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
                Map<Source, Set<String>> containedAttachments = ProjectPortletUtils
                        .extractContainedAttachments(mappedProjectLinks);
                Map<Map<Source, String>, Integer> attachmentUsages = attachmentClient.getAttachmentUsageCount(containedAttachments,
                        UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
                Map<String, Integer> countMap = attachmentUsages.entrySet().stream().collect(Collectors.toMap(entry -> {
                    Entry<Source, String> key = entry.getKey().entrySet().iterator().next();
                    return key.getKey().getFieldValue() + "_" + key.getValue();
                }, entry -> entry.getValue()));
                request.setAttribute(ATTACHMENT_USAGE_COUNT_MAP, countMap);
            } catch (TException e) {
                log.error("Error fetching project from backend!", e);
                setSW360SessionError(request, ErrorMessages.ERROR_GETTING_PROJECT);
            }
        }
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
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);
        Project project;
        Set<Project> usingProjects;
        int allUsingProjectCount = 0;
        request.setAttribute(DEFAULT_LICENSE_INFO_HEADER_TEXT, getProjectDefaultLicenseInfoHeaderText());

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

            setAttachmentsInRequest(request, project.getAttachments());
            try {
                putDirectlyLinkedProjectsInRequest(request, project, user);
                putDirectlyLinkedReleasesInRequest(request, project);
            } catch (TException e) {
                log.error("Could not fetch linked projects or linked releases in projects view.", e);
                return;
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
                setAttachmentsInRequest(request, project.getAttachments());
                try {
                    putDirectlyLinkedProjectsInRequest(request, project, user);
                    putDirectlyLinkedReleasesInRequest(request, project);
                } catch(TException e) {
                    log.error("Could not put empty linked projects or linked releases in projects view.", e);
                }
                request.setAttribute(USING_PROJECTS, Collections.emptySet());
                request.setAttribute(ALL_USING_PROJECTS_COUNT, 0);

                SessionMessages.add(request, "request_processed", "New Project");
            }
        }

    }

    private void prepareProjectDuplicate(RenderRequest request) {
        User user = UserCacheHolder.getUserFromRequest(request);
        String id = request.getParameter(PROJECT_ID);
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);

        try {
            if (id != null) {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                String emailFromRequest = LifeRayUserSession.getEmailFromRequest(request);
                String department = user.getDepartment();

                Project newProject = PortletUtils.cloneProject(emailFromRequest, department, client.getProjectById(id, user));
                setAttachmentsInRequest(request, newProject.getAttachments());
                request.setAttribute(PROJECT, newProject);
                putDirectlyLinkedProjectsInRequest(request, newProject, user);
                putDirectlyLinkedReleasesInRequest(request, newProject);
                request.setAttribute(USING_PROJECTS, Collections.emptySet());
                request.setAttribute(ALL_USING_PROJECTS_COUNT, 0);
            } else {
                Project project = new Project();
                project.setBusinessUnit(user.getDepartment());
                setAttachmentsInRequest(request, project.getAttachments());

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
                requestStatus = client.updateProject(project, user);
                setSessionMessage(request, requestStatus, "Project", "update", printName(project));
                cleanUploadHistory(user.getEmail(), id);
                response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
                response.setRenderParameter(PROJECT_ID, request.getParameter(PROJECT_ID));
            } else {
                // Add project
                Project project = new Project();
                ProjectPortletUtils.updateProjectFromRequest(request, project);
                AddDocumentRequestSummary summary = client.addProject(project, user);

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

    private void prepareRequestForEditAfterDuplicateError(ActionRequest request, Project project, User user) throws TException {
        request.setAttribute(PROJECT, project);
        setAttachmentsInRequest(request, project.getAttachments());
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
}
