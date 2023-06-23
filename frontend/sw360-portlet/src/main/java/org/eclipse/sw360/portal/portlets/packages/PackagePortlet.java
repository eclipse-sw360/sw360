/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License 2.0 
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.packages;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONArray;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONObject;
import static java.lang.Math.min;
import static org.eclipse.sw360.datahandler.common.CommonUtils.isNotNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.portal.common.PortalConstants.ALL_USING_PROJECTS_COUNT;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_DISPLAY_DATA;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_FILTERED;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_TOTAL;
import static org.eclipse.sw360.portal.common.PortalConstants.DOCUMENT_ID;
import static org.eclipse.sw360.portal.common.PortalConstants.IS_ERROR_IN_UPDATE_OR_CREATE;
import static org.eclipse.sw360.portal.common.PortalConstants.PACKAGE;
import static org.eclipse.sw360.portal.common.PortalConstants.PACKAGES_PORTLET_NAME;
import static org.eclipse.sw360.portal.common.PortalConstants.PACKAGE_ID;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME_DETAIL;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME_EDIT;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME_VIEW;
import static org.eclipse.sw360.portal.common.PortalConstants.PKG;
import static org.eclipse.sw360.portal.common.PortalConstants.RELEASE;
import static org.eclipse.sw360.portal.common.PortalConstants.USING_PROJECTS;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.portlet.ActionParameters;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.MutableRenderParameters;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.RenderParameters;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceParameters;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.commonIO.SampleOptions;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.DateRange;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.ChangeLogsPortletUtils;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.PortletUtils;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.common.datatables.PaginationParser;
import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.projects.ProjectPortlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.PortalUtil;

@org.osgi.service.component.annotations.Component(
        immediate = true, 
        properties = {
                "/org/eclipse/sw360/portal/portlets/base.properties",
                "/org/eclipse/sw360/portal/portlets/default.properties" 
        }, property = {
                "javax.portlet.name=" + PACKAGES_PORTLET_NAME,
                "javax.portlet.display-name=Packages",
                "javax.portlet.info.short-title=Packages",
                "javax.portlet.info.title=Packages",
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.init-param.view-template=/html/packages/view.jsp",
        },
        service = Portlet.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class PackagePortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(ProjectPortlet.class);

    private static final ImmutableList<Package._Fields> packageFilteredFields = ImmutableList.of(
            Package._Fields.NAME,
            Package._Fields.VERSION,
            Package._Fields.PACKAGE_MANAGER,
            Package._Fields.LICENSE_IDS,
            Package._Fields.CREATED_BY,
            Package._Fields.CREATED_ON);

    // Package view data tables, index of columns
    private static final int PACKAGE_NO_SORT = -1;
    private static final int PACKAGE_DT_ROW_NAME = 0;
    private static final int PACKAGE_DT_ROW_LICENSES = 3;
    private static final int PACKAGE_DT_ROW_PACKAGE_MANAGER = 4;

    // Helper methods
    private void addPackageBreadcrumb(RenderRequest request, RenderResponse response, Package pkg) {
        PortletURL url = response.createRenderURL();
        MutableRenderParameters parameters = url.getRenderParameters();
        parameters.setValue(PAGENAME, PAGENAME_DETAIL);
        parameters.setValue(PACKAGE_ID, pkg.getId());
        addBreadcrumbEntry(request, pkg.getName(), url);
    }

    // ! Serve resource and helpers
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {

        final var action = request.getResourceParameters().getValue(PortalConstants.ACTION);
        if (PortalConstants.LOAD_PACKAGE_LIST.equals(action)) {
            servePackageList(request, response);
        } else if (isGenericAction(action)) {
            dealWithGenericAction(request, response, action);
        } else if (PortalConstants.VIEW_LINKED_RELEASES.equals(action)) {
            serveLinkedReleases(request, response);
        } else if (PortalConstants.LOAD_CHANGE_LOGS.equals(action) || PortalConstants.VIEW_CHANGE_LOGS.equals(action)) {
            ChangeLogsPortletUtils changeLogsPortletUtilsPortletUtils = PortletUtils.getChangeLogsPortletUtils(thriftClients);
            JSONObject dataForChangeLogs = changeLogsPortletUtilsPortletUtils.serveResourceForChangeLogs(request, response, action);
            writeJSON(request, response, dataForChangeLogs);
        } else if (PortalConstants.DELETE_PACKAGE.equals(action)) {
            serveDeletePackage(request, response);
        }
    }

    // ! VIEW and helpers
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {

        final var pageName = request.getRenderParameters().getValue(PAGENAME);
        if (PAGENAME_DETAIL.equals(pageName)) {
            prepareDetailView(request, response);
            include("/html/packages/detailPackage.jsp", request, response);
        } else if (PAGENAME_EDIT.equals(pageName)) {
            preparePackageEdit(request);
            include("/html/packages/editPackage.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void serveLinkedReleases(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        ResourceParameters parameters = request.getResourceParameters();
        String what = parameters.getValue(PortalConstants.WHAT);
        if (PortalConstants.RELEASE_SEARCH.equals(what)) {
            String where = parameters.getValue(PortalConstants.WHERE);
            serveReleaseSearchResults(request, response, where);
        }
    }

    private void serveReleaseSearchResults(ResourceRequest request, ResourceResponse response, String searchText) throws IOException, PortletException {
        request.setAttribute("isSingleRelease", "true");
        serveReleaseSearch(request, response, searchText);
    }

    private void prepareStandardView(RenderRequest request) {
        RenderParameters parameters = request.getRenderParameters();
        for (Package._Fields filteredField : packageFilteredFields) {
            String parameter = parameters.getValue(filteredField.toString());
            request.setAttribute(filteredField.getFieldName(), nullToEmpty(parameter));
        }
        request.setAttribute(PortalConstants.EXACT_MATCH_CHECKBOX, nullToEmpty(parameters.getValue(PortalConstants.EXACT_MATCH_CHECKBOX)));
        request.setAttribute(PortalConstants.ORPHAN_PACKAGE_CHECKBOX, nullToEmpty(parameters.getValue(PortalConstants.ORPHAN_PACKAGE_CHECKBOX)));
        request.setAttribute(PortalConstants.DATE_RANGE, nullToEmpty(parameters.getValue(PortalConstants.DATE_RANGE)));
        request.setAttribute(PortalConstants.END_DATE, nullToEmpty(parameters.getValue(PortalConstants.END_DATE)));
    }

    private void servePackageList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        HttpServletRequest originalServletRequest = PortalUtil.getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
        PaginationParameters paginationParameters = PaginationParser.parametersFrom(originalServletRequest);
        handlePaginationSortOrder(request, paginationParameters);
        PaginationData pageData = new PaginationData();
        pageData.setRowsPerPage(paginationParameters.getDisplayLength());
        pageData.setDisplayStart(paginationParameters.getDisplayStart());
        pageData.setAscending(paginationParameters.isAscending().get());
        int sortParam = -1;
        if (paginationParameters.getSortingColumn().isPresent()) {
            sortParam = paginationParameters.getSortingColumn().get();
        }

        pageData.setSortColumnNumber(sortParam);
        Map<String, Set<String>> filterMap = getPackageFilterMap(request);
        Map<PaginationData, List<Package>> pageDataPackageList = getFilteredPackageList(request, pageData, filterMap);
        JSONArray jsonPackages = getPackageData(request, pageDataPackageList.values().iterator().next(), paginationParameters, filterMap);
        JSONObject jsonResult = createJSONObject();
        jsonResult.put(DATATABLE_RECORDS_TOTAL, pageDataPackageList.keySet().iterator().next().getTotalRowCount());
        jsonResult.put(DATATABLE_RECORDS_FILTERED, pageDataPackageList.keySet().iterator().next().getTotalRowCount());
        jsonResult.put(DATATABLE_DISPLAY_DATA, jsonPackages);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("An error ocured while writing Package list to JSON: ", e);
        }
    }

    private void handlePaginationSortOrder(ResourceRequest request, PaginationParameters paginationParameters) {
        if (!paginationParameters.getSortingColumn().isPresent()) {
            RenderParameters parameters = request.getRenderParameters();
            for (Package._Fields filteredField : packageFilteredFields) {
                if (!isNullOrEmpty(parameters.getValue(filteredField.toString()))) {
                    paginationParameters.setSortingColumn(Optional.of(PACKAGE_NO_SORT));
                    break;
                }
            }
        }
    }

    private Map<PaginationData, List<Package>> getFilteredPackageList(PortletRequest request, PaginationData pageData, Map<String, Set<String>> filterMap) {
        List<Package> packageList;
        Map<PaginationData, List<Package>> pageDataPackages = Maps.newHashMap();

        try {
            PackageService.Iface packageClient = thriftClients.makePackageClient();
            if (filterMap.isEmpty()) {
                pageDataPackages = packageClient.getPackagesWithPagination(pageData);
            } else {
                packageList = packageClient.searchPackagesWithFilter(null, filterMap);
                pageDataPackages.put(pageData.setTotalRowCount(packageList.size()), packageList);
            }
        } catch (TException e) {
            log.error("Could not search packages in backend ", e);
            pageDataPackages = Collections.emptyMap();
        }

        return pageDataPackages;
    }

    private Map<String, Set<String>> getPackageFilterMap(ResourceRequest request) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        RenderParameters parameters = request.getRenderParameters();
        String exactMatch = parameters.getValue(PortalConstants.EXACT_MATCH_CHECKBOX);
        String orphanSearch = parameters.getValue(PortalConstants.ORPHAN_PACKAGE_CHECKBOX);
        for (Package._Fields filteredField : packageFilteredFields) {
            String parameter = parameters.getValue(filteredField.toString());
            if (!isNullOrEmpty(parameter) && !(filteredField.equals(Package._Fields.PACKAGE_MANAGER) && parameter.equals(PortalConstants.NO_FILTER))) {
                if (filteredField.equals(Package._Fields.CREATED_ON) && isNotNullEmptyOrWhitespace(parameters.getValue(PortalConstants.DATE_RANGE))) {
                    Date date = new Date();
                    String upperLimit = new SimpleDateFormat(SampleOptions.DATE_OPTION).format(date);
                    String dateRange = parameters.getValue(PortalConstants.DATE_RANGE);
                    String query = new StringBuilder("[%s ").append(PortalConstants.TO).append(" %s]").toString();
                    DateRange range = ThriftEnumUtils.stringToEnum(dateRange, DateRange.class);
                    switch (range) {
                        case EQUAL:
                            break;
                        case LESS_THAN_OR_EQUAL_TO:
                            parameter = String.format(query, PortalConstants.EPOCH_DATE, parameter);
                            break;
                        case GREATER_THAN_OR_EQUAL_TO:
                            parameter = String.format(query, parameter, upperLimit);
                            break;
                        case BETWEEN:
                            String endDate = parameters.getValue(PortalConstants.END_DATE);
                            if (isNullEmptyOrWhitespace(endDate)) {
                                endDate = upperLimit;
                            }
                            parameter = String.format(query, parameter, endDate);
                            break;
                    }
                }
                Set<String> values = CommonUtils.splitToSet(parameter);
                if (filteredField.equals(Package._Fields.NAME) || filteredField.equals(Package._Fields.VERSION)) {
                    if (!exactMatch.isEmpty() && !(parameter.startsWith("\"") && parameter.endsWith("\""))) {
                        values = values.stream().map(s -> "\"" + s + "\"").map(LuceneAwareDatabaseConnector::prepareWildcardQuery).collect(Collectors.toSet());
                    }
                    else {
                        values = values.stream().map(LuceneAwareDatabaseConnector::prepareWildcardQuery).collect(Collectors.toSet());
                    }
                }
                filterMap.put(filteredField.getFieldName(), values);
            }
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(orphanSearch)) {
            filterMap.put(PortalConstants.ORPHAN_PACKAGE_CHECKBOX, Collections.emptySet());
        }
        return filterMap;
    }

    public JSONArray getPackageData(ResourceRequest request, List<Package> packageList, PaginationParameters packageParameters, Map<String, Set<String>> filterMap) {
        List<Package> sortedPackages = sortPackageList(packageList, packageParameters);
        int count = getPackageDataCount(packageParameters, packageList.size());
        ComponentService.Iface compClient = thriftClients.makeComponentClient();
        final int start = filterMap.isEmpty() ? 0 : packageParameters.getDisplayStart();
        final User user = UserCacheHolder.getUserFromRequest(request);

        JSONArray packageData = createJSONArray();
        for (int i = start; i < count; i++) {
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
            Package pkg = sortedPackages.get(i);
            jsonObject.put("DT_RowId", pkg.getId());
            jsonObject.put("name", SW360Utils.printName(pkg));
            final String relId = pkg.getReleaseId();
            jsonObject.put("relId", CommonUtils.nullToEmptyString(relId));
            if (CommonUtils.isNotNullEmptyOrWhitespace(relId)) {
                try {
                    Release rel = compClient.getReleaseById(relId, user);
                    jsonObject.put("relName", SW360Utils.printName(rel));
                    jsonObject.put("relCS", ThriftEnumUtils.enumToString(rel.getClearingState()));
                } catch (SW360Exception e) {
                    log.error(String.format("An error occure while getting release <%s> info of a package <%s>", relId, pkg.getId()), e);
                } catch (TException e) {
                    log.error(String.format("An error occure while getting release <%s> info of a package <%s>", relId, pkg.getId()), e);
                }
            }
            JSONArray licenseArray = createJSONArray();
            if (pkg.isSetLicenseIds()) {
                pkg.getLicenseIds().stream().sorted().forEach(licenseArray::put);
            }
            jsonObject.put("lics", licenseArray);
            jsonObject.put("pkgMgr", ThriftEnumUtils.enumToString(pkg.getPackageManager()));
            jsonObject.put("writeAccess", SW360Utils.isWriteAccessUser(pkg.getCreatedBy(), user, SW360Constants.PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE));

            packageData.put(jsonObject);
        }

        return packageData;
    }

    private int getPackageDataCount(PaginationParameters packageParameters, int maxSize) {
        if (packageParameters.getDisplayLength() == -1) {
            return maxSize;
        } else {
            return min(packageParameters.getDisplayStart() + packageParameters.getDisplayLength(), maxSize);
        }
    }

    private List<Package> sortPackageList(List<Package> packageList, PaginationParameters packageParameters) {
        boolean isAsc = packageParameters.isAscending().orElse(true);

        switch (packageParameters.getSortingColumn().orElse(PACKAGE_DT_ROW_NAME)) {
            case PACKAGE_DT_ROW_NAME:
                Collections.sort(packageList, compareByName(isAsc));
                break;
            case PACKAGE_DT_ROW_LICENSES:
                Collections.sort(packageList, compareByLicenses(isAsc));
                break;
            case PACKAGE_DT_ROW_PACKAGE_MANAGER:
                Collections.sort(packageList, compareByPackageManager(isAsc));
                break;
            default:
                break;
        }

        return packageList;
    }

    private Comparator<Package> compareByName(boolean isAscending) {
        Comparator<Package> nameComparator = Comparator.comparing(p -> SW360Utils.printName(p).toLowerCase());
        return isAscending ? nameComparator : nameComparator.reversed();
    }

    private Comparator<Package> compareByLicenses(boolean isAscending) {
        Comparator<Package> pkgManagerComparator = Comparator.comparing(p -> sortAndConcat(p.getLicenseIds()));
        return isAscending ? pkgManagerComparator : pkgManagerComparator.reversed();
    }

    private Comparator<Package> compareByPackageManager(boolean isAscending) {
        Comparator<Package> pkgManagerComparator = Comparator.comparing(p -> p.getPackageManager().toString());
        return isAscending ? pkgManagerComparator : pkgManagerComparator.reversed();
    }

    private String sortAndConcat(Set<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        return CommonUtils.COMMA_JOINER.join(strings.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList()));
    }

    private void prepareDetailView(RenderRequest request, RenderResponse response) {
        String id = request.getRenderParameters().getValue(PACKAGE_ID);
        request.setAttribute(DOCUMENT_ID, id);
        final User user = UserCacheHolder.getUserFromRequest(request);
        PackageService.Iface packageClient = thriftClients.makePackageClient();
        Package pkg;
        try {
            pkg = packageClient.getPackageById(id);
            request.setAttribute(PKG, pkg);
            Release release = null;
            if (CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId())) {
                ComponentService.Iface compClient = thriftClients.makeComponentClient();
                release = compClient.getReleaseById(pkg.getReleaseId(), user);
                request.setAttribute(RELEASE, release);
                String releaseName = SW360Utils.printFullname(release);
                request.setAttribute("releaseName", releaseName);
            }
            request.setAttribute(PortalConstants.WRITE_ACCESS_USER, SW360Utils.isWriteAccessUser(pkg.getCreatedBy(), user, SW360Constants.PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE));
            setUsingDocs(request, pkg.getId(), user);
            addPackageBreadcrumb(request, response, pkg);
        } catch (TException e) {
            log.error("An error occured while loading package for detail view: " + id, e);
        }
    }

    private void preparePackageEdit(RenderRequest request) {
        String id = request.getRenderParameters().getValue(PACKAGE_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);
        PackageService.Iface packageClient = thriftClients.makePackageClient();
        Package pkg = (Package) request.getAttribute(PKG);
        final Boolean isPackageAbsent = (pkg == null);
        try {
            if (id == null) {
                if (isPackageAbsent) {
                    pkg = new Package();
                }
            } else {
                if (isPackageAbsent) {
                    pkg = packageClient.getPackageById(id);
                }
            }
            Release release = null;
            if (CommonUtils.isNotNullEmptyOrWhitespace(pkg.getReleaseId())) {
                ComponentService.Iface compClient = thriftClients.makeComponentClient();
                release = compClient.getReleaseById(pkg.getReleaseId(), user);
                request.setAttribute(RELEASE, release);
                String releaseName = SW360Utils.printFullname(release);
                request.setAttribute("releaseName", releaseName);
            }
            setUsingDocsCount(request, id, user);
            request.setAttribute(PKG, pkg);
        } catch (TException e) {
            log.error("An error occured while loading package for edit view: " + id, e);
        }
    }

    @UsedAsLiferayAction
    public void updatePackage(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String packageId = request.getActionParameters().getValue(PACKAGE_ID);
        User user = UserCacheHolder.getUserFromRequest(request);
        PackageService.Iface packageClient = thriftClients.makePackageClient();
        Package pkg;
        RequestStatus requestStatus;
        try {
            MutableRenderParameters parameters = response.getRenderParameters();
            if (packageId != null) {
                // Update existing Package
                pkg = packageClient.getPackageById(packageId);
                PackagePortletUtils.updatePackageFromRequest(request, pkg);
                requestStatus = packageClient.updatePackage(pkg, user);
                setSessionMessage(request, requestStatus, PACKAGE, "update", printName(pkg));
                if (RequestStatus.DUPLICATE.equals(requestStatus) || RequestStatus.NAMINGERROR.equals(requestStatus)
                        || RequestStatus.INVALID_INPUT.equals(requestStatus) || RequestStatus.ACCESS_DENIED.equals(requestStatus)) {
                    if (RequestStatus.ACCESS_DENIED.equals(requestStatus))
                        setSW360SessionError(request, ErrorMessages.PACKAGE_UPDATE_ACCESS_DENIED);
                    else if (RequestStatus.DUPLICATE.equals(requestStatus))
                        setSW360SessionError(request, ErrorMessages.PACKAGE_DUPLICATE);
                    else if (RequestStatus.INVALID_INPUT.equals(requestStatus))
                        setSW360SessionError(request, ErrorMessages.INVALID_PURL_OR_LINKED_RELEASE);
                    else if (RequestStatus.NAMINGERROR.equals(requestStatus))
                        setSW360SessionError(request, ErrorMessages.PACKAGE_NAME_VERSION_ERROR);

                    parameters.setValue(PAGENAME, PAGENAME_EDIT);
                    parameters.setValue(PACKAGE_ID, packageId);
                    prepareRequestForEditAfterError(request, pkg, user);
                } else {
                    parameters.setValue(PAGENAME, PAGENAME_DETAIL);
                    parameters.setValue(PACKAGE_ID, packageId);
                    request.setAttribute(PKG, pkg);
                }
            } else {
                // Add new Package
                pkg = new Package();
                PackagePortletUtils.updatePackageFromRequest(request, pkg);
                AddDocumentRequestSummary summary = packageClient.addPackage(pkg, user);
                AddDocumentRequestStatus status = summary.getRequestStatus();

                if (AddDocumentRequestStatus.DUPLICATE.equals(status) || AddDocumentRequestStatus.NAMINGERROR.equals(status)
                        || AddDocumentRequestStatus.INVALID_INPUT.equals(status)) {
                    if (AddDocumentRequestStatus.DUPLICATE.equals(status))
                        setSW360SessionError(request, ErrorMessages.PACKAGE_DUPLICATE);
                    else if (AddDocumentRequestStatus.INVALID_INPUT.equals(status))
                        setSW360SessionError(request, ErrorMessages.INVALID_PURL_OR_LINKED_RELEASE);
                    else if (AddDocumentRequestStatus.NAMINGERROR.equals(status))
                        setSW360SessionError(request, ErrorMessages.PACKAGE_NAME_VERSION_ERROR);

                    parameters.setValue(PAGENAME, PAGENAME_EDIT);
                    prepareRequestForEditAfterError(request, pkg, user);
                } else if (AddDocumentRequestStatus.SUCCESS.equals(status)) {
                    String successMsg = "Package " + printName(pkg) + " added successfully";
                    SessionMessages.add(request, "request_processed", successMsg);
                    parameters.setValue(PACKAGE_ID, summary.getId());
                    parameters.setValue(PAGENAME, PAGENAME_EDIT);
                    request.setAttribute(PKG, packageClient.getPackageById(summary.getId()));
                } else {
                    setSW360SessionError(request, ErrorMessages.PACKAGE_NOT_ADDED);
                    parameters.setValue(PAGENAME, PAGENAME_VIEW);
                }
            }
        } catch (TException e) {
            log.error("An error occured while updating the package: " + packageId, e);
        }
    }

    @UsedAsLiferayAction
    public void deletePackage(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        User user = UserCacheHolder.getUserFromRequest(request);
        String packageId = request.getActionParameters().getValue(PortalConstants.PACKAGE_ID);
        RequestStatus requestStatus = deletePackage(packageId, user, log);
        setSessionMessage(request, requestStatus, "Package", "delete");
        response.getRenderParameters().setValue(PAGENAME, PAGENAME_VIEW);
    }

    @UsedAsLiferayAction
    public void applyFilters(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        MutableRenderParameters responseParam = response.getRenderParameters();
        ActionParameters requestParam = request.getActionParameters();
        for (Package._Fields componentFilteredField : packageFilteredFields) {
            responseParam.setValue(componentFilteredField.toString(), nullToEmpty(requestParam.getValue(componentFilteredField.toString())));
        }
        responseParam.setValue(PortalConstants.DATE_RANGE, nullToEmpty(requestParam.getValue(PortalConstants.DATE_RANGE)));
        responseParam.setValue(PortalConstants.END_DATE, nullToEmpty(requestParam.getValue(PortalConstants.END_DATE)));
        responseParam.setValue(PortalConstants.EXACT_MATCH_CHECKBOX, nullToEmpty(requestParam.getValue(PortalConstants.EXACT_MATCH_CHECKBOX)));
        responseParam.setValue(PortalConstants.ORPHAN_PACKAGE_CHECKBOX, nullToEmpty(requestParam.getValue(PortalConstants.ORPHAN_PACKAGE_CHECKBOX)));
    }


    private void setUsingDocs(RenderRequest request, String packageId, User user) throws TException {
        Set<Project> usingProjects = null;
        int allUsingProjectsCount = 0;
        if (CommonUtils.isNotNullEmptyOrWhitespace(packageId)) {
            ProjectService.Iface projectClient = thriftClients.makeProjectClient();
            usingProjects = projectClient.searchProjectByPackageId(packageId, user);
            allUsingProjectsCount = projectClient.getProjectCountByPackageId(packageId);
        }
        request.setAttribute(USING_PROJECTS, nullToEmptySet(usingProjects));
        request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectsCount);
    }

    private void setUsingDocsCount(RenderRequest request, String packageId, User user) throws TException {
        int allUsingProjectsCount = 0;
        if (CommonUtils.isNotNullEmptyOrWhitespace(packageId)) {
            ProjectService.Iface projectClient = thriftClients.makeProjectClient();
            allUsingProjectsCount = projectClient.getProjectCountByPackageId(packageId);
        }
        request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectsCount);
    }

    private void prepareRequestForEditAfterError(ActionRequest request, Package pkg, User user) throws TException {
        request.setAttribute(PKG, pkg);
        request.setAttribute(IS_ERROR_IN_UPDATE_OR_CREATE, true);
    }
}
