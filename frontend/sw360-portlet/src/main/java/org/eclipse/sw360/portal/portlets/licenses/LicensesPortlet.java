/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.licenses;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.servlet.SessionMessages;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Ternary;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.exporter.LicenseExporter;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.portlet.*;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.TMP_TODO_ID_PREFIX;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CONTENT_TYPE_OPENXML_SPREADSHEET;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + LICENSES_PORTLET_NAME,

        "javax.portlet.display-name=Licenses",
        "javax.portlet.info.short-title=Licenses",
        "javax.portlet.info.title=Licenses",

        "javax.portlet.init-param.view-template=/html/licenses/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class LicensesPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(LicensesPortlet.class);

    /**
     * Excel exporter
     */
    private final LicenseExporter exporter;
    private List<LicenseType> licenseTypes;

    public LicensesPortlet() throws TException {
        Function<Logger,List<LicenseType>> getLicenseTypes = log -> {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            try {
                return client.getLicenseTypes();
            } catch (TException e){
                log.error("Error getting license type list.", e);
                return Collections.emptyList();
            }
        };
        exporter = new LicenseExporter(getLicenseTypes);
    }

    //! Serve resource and helpers
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.EXPORT_TO_EXCEL.equals(action)) {
            exportExcel(request, response);
        }
    }

    private void exportExcel(ResourceRequest request, ResourceResponse response) {
        try {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            List<License> licenses = client.getLicenseSummaryForExport();

            PortletResponseUtil.sendFile(request, response, "Licenses.xlsx", exporter.makeExcelExport(licenses), CONTENT_TYPE_OPENXML_SPREADSHEET);
        } catch (IOException | TException e) {
            log.error("An error occurred while generating the Excel export", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_DETAIL.equals(pageName)) {
            prepareDetailView(request, response);
            include("/html/licenses/detail.jsp", request, response);
        } else if (PAGENAME_EDIT.equals(pageName)) {
            prepareEditView(request, response);
            include("/html/licenses/edit.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareEditView(RenderRequest request, RenderResponse response) {

        String id = request.getParameter(LICENSE_ID);
        User user = UserCacheHolder.getUserFromRequest(request);
        LicenseService.Iface client = thriftClients.makeLicenseClient();
        boolean isAtLeastClearingAdmin = PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user);
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, isAtLeastClearingAdmin ? "Yes" : "No");

        try {
            licenseTypes = client.getLicenseTypes();
            request.setAttribute(LICENSE_TYPE_CHOICE, licenseTypes);
        }catch(TException e){
            log.error("Error fetching license types from backend", e);
            setSW360SessionError(request, ErrorMessages.ERROR_GETTING_LICENSE);
        }

        if (id != null) {
            try {
                License license = client.getByID(id, user.getDepartment());
                request.setAttribute(KEY_LICENSE_DETAIL, license);
                addLicenseBreadcrumb(request, response, license);
            } catch (TException e) {
                log.error("Error fetching license details from backend", e);
                setSW360SessionError(request, ErrorMessages.ERROR_GETTING_LICENSE);
            }
        } else {
            if(isAtLeastClearingAdmin) {
                SessionMessages.add(request, "request_processed", "New License");
            } else {
                SessionMessages.add(request, "request_processed", "You will create a new and UNCHECKED license");
            }
            License license = new License();
            request.setAttribute(KEY_LICENSE_DETAIL, license);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        log.debug("Enter license table view");
        List<License> licenses;
        User user = UserCacheHolder.getUserFromRequest(request);
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user) ? "Yes" : "No");
        try {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            licenses = client.getLicenseSummary();
        } catch (TException e) {
            log.error("Could not fetch license summary from backend!", e);
            licenses = new ArrayList<>();
        }

        request.setAttribute(LICENSE_LIST, licenses);
    }

    private void prepareDetailView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String id = request.getParameter(LICENSE_ID);
        User user = UserCacheHolder.getUserFromRequest(request);
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user) ? "Yes" : "No");
        if (id != null) {
            try {
                LicenseService.Iface client = thriftClients.makeLicenseClient();
                License moderationLicense = client.getByIDWithOwnModerationRequests(id, user.getDepartment(), user);

                List<Todo> allTodos = nullToEmptyList(moderationLicense.getTodos());
                List<Todo> addedTodos = allTodos
                        .stream()
                        .filter(CommonUtils::isTemporaryTodo)
                        .collect(Collectors.toList());
                List<Todo> currentTodos = allTodos
                        .stream()
                        .filter(t -> !CommonUtils.isTemporaryTodo(t))
                        .collect(Collectors.toList());

                request.setAttribute(ADDED_TODOS_FROM_MODERATION_REQUEST, addedTodos);
                request.setAttribute(DB_TODOS_FROM_MODERATION_REQUEST, currentTodos);

                request.setAttribute(MODERATION_LICENSE_DETAIL, moderationLicense);

                License dbLicense = client.getByID(id, user.getDepartment());
                request.setAttribute(KEY_LICENSE_DETAIL, dbLicense);

                List<Obligation> obligations = client.getObligations();
                request.setAttribute(KEY_OBLIGATION_LIST, obligations);

                addLicenseBreadcrumb(request, response, moderationLicense);

            } catch (TException e) {
                log.error("Error fetching license details for id " + id + " from backend", e);
                setSW360SessionError(request, ErrorMessages.ERROR_GETTING_LICENSE);
            }
        }
    }

    private void addLicenseBreadcrumb(RenderRequest request, RenderResponse response, License license) {
        PortletURL componentUrl = response.createRenderURL();
        componentUrl.setParameter(PAGENAME, PAGENAME_DETAIL);
        componentUrl.setParameter(LICENSE_ID, license.getId());

        addBreadcrumbEntry(request, license.getId(), componentUrl);
    }

    @UsedAsLiferayAction
    public void update(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        LicenseService.Iface client = thriftClients.makeLicenseClient();
        String licenseId = request.getParameter(LICENSE_ID);
        User user = UserCacheHolder.getUserFromRequest(request);

        License license = prepareLicenseForUpdate(request, client, licenseId, user);
        boolean isNewLicense = isNullOrEmpty(licenseId);
        boolean isAttemptToOverwriteExistingByNew = isAttemptToOverwriteExistingByNew(license, user, isNewLicense, client);

        RequestStatus requestStatus = updateLicense(license, user, isAttemptToOverwriteExistingByNew, client);

        if (isAttemptToOverwriteExistingByNew){
            response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
            setSW360SessionError(request, ErrorMessages.LICENSE_SHORTNAME_TAKEN);
            request.setAttribute(KEY_LICENSE_DETAIL, license);
        } else if (isNewLicense) {
            response.setRenderParameter(PAGENAME, PAGENAME_VIEW);
            setSessionMessage(request, requestStatus, "License", "adde");
        } else {
            response.setRenderParameter(LICENSE_ID, licenseId);
            response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
            request.setAttribute(SELECTED_TAB, "tab-Details");
            setSessionMessage(request, requestStatus, "License", "update");
        }
    }

    private RequestStatus updateLicense(License license, User user, boolean isAttemptToOverwriteExistingByNew, LicenseService.Iface client) {
        RequestStatus requestStatus;
        try {
            requestStatus = isAttemptToOverwriteExistingByNew ? RequestStatus.FAILURE : client.updateLicense(license, user, user);
        } catch (TException e) {
            log.error("Could not add or update license:" + e);
            requestStatus = RequestStatus.FAILURE;
        }
        return requestStatus;
    }

    private boolean isAttemptToOverwriteExistingByNew(License license, User user, boolean isNewLicense, LicenseService.Iface client) {
        return isNewLicense && checkLicenseExists(license, user, client);
    }

    private License prepareLicenseForUpdate(ActionRequest request, LicenseService.Iface client, String licenseId, User user) {
        License license = new License();;
        if (!isNullOrEmpty(licenseId)) {
            try {
                license = client.getByID(licenseId, user.getDepartment());
            } catch (TException e) {
                log.error("Could not find license to update:", e);
            }
        }

        return updateLicenseFromRequest(license, request);
    }

    private boolean checkLicenseExists(License license, User user, LicenseService.Iface client) {
        try {
            client.getByID(license.getShortname(), user.getDepartment());
            return true;
        } catch (TException e1) {
            log.info("No existing license found:", e1);
        }
        return false;
    }

    private License updateLicenseFromRequest(License license, ActionRequest request) {
        String text = request.getParameter(License._Fields.TEXT.name());
        String fullname = request.getParameter(License._Fields.FULLNAME.name());
        String shortname = request.getParameter(License._Fields.SHORTNAME.name());
        Ternary gpl2compatibility = Ternary.findByValue(Integer.parseInt(request.getParameter(License._Fields.GPLV2_COMPAT.toString())));
        Ternary gpl3compatibility = Ternary.findByValue(Integer.parseInt(request.getParameter(License._Fields.GPLV3_COMPAT.toString())));
        boolean checked = "true".equals(request.getParameter(License._Fields.CHECKED.toString()));
        String licenseTypeString =
                request.getParameter(License._Fields.LICENSE_TYPE.toString() + LicenseType._Fields.LICENSE_TYPE.toString());
        license.setText(CommonUtils.nullToEmptyString(text));
        license.setFullname(CommonUtils.nullToEmptyString(fullname));
        license.setShortname((CommonUtils.nullToEmptyString(shortname)));
        license.setGPLv2Compat(gpl2compatibility);
        license.setGPLv3Compat(gpl3compatibility);
        license.setChecked(checked);
        try {
            Optional<String> licenseTypeDatabaseId = getDatabaseIdFromLicenseType(licenseTypeString);
            if(licenseTypeDatabaseId.isPresent()) {
                license.setLicenseTypeDatabaseId(licenseTypeDatabaseId.get());
                final LicenseType licenseType = thriftClients.makeLicenseClient().getLicenseTypeById(license.getLicenseTypeDatabaseId());
                license.setLicenseType(licenseType);
            } else {
                license.unsetLicenseTypeDatabaseId();
            }
        } catch (TException e) {
            log.error("Could not set licenseTypeDatabaseId:" + e);
            license.unsetLicenseTypeDatabaseId();
        }
        return license;
    }

    private Optional<String> getDatabaseIdFromLicenseType(String licenseTypeIdString) throws TException {
        if (licenseTypeIdString != null && licenseTypeIdString.equals("")){
            return Optional.empty();
        }
        if (licenseTypes == null) {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            try {
                licenseTypes = client.getLicenseTypes();
            } catch (TException e){
                throw new SW360Exception("Error getting license type list:"+ e);
            }
        }
        for (LicenseType licenseType : licenseTypes) {
            if (licenseType.getLicenseTypeId() == Integer.parseInt(licenseTypeIdString)) {
                return Optional.of(licenseType.getId());
            }
        }
        throw new SW360Exception("Wrong license type!");
    }

    @UsedAsLiferayAction
    public void updateWhiteList(ActionRequest request, ActionResponse response) throws PortletException, IOException {

        // we get a list of todoDatabaseIds and Booleans and we have to update the whiteList of each todo if it changed
        String licenseId = request.getParameter(LICENSE_ID);
        String[] whiteList = request.getParameterValues("whiteList");
        if (whiteList == null) whiteList = new String[0]; // As empty arrays are not passed as parameters

        final User user = UserCacheHolder.getUserFromRequest(request);
        String moderationComment = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
        if(moderationComment != null) {
            user.setCommentMadeDuringModerationRequest(moderationComment);
        }

        try {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            RequestStatus requestStatus = client.updateWhitelist(licenseId, ImmutableSet.copyOf(whiteList), user);

            setSessionMessage(request, requestStatus, "License", "update");

        } catch (TException e) {
            log.error("Error updating whitelist!", e);
        }

        response.setRenderParameter(LICENSE_ID, licenseId);
        response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
        request.setAttribute(SELECTED_TAB, "tab-TodosAndObligations");
    }

    @UsedAsLiferayAction
    public void changeText(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String licenseId = request.getParameter(LICENSE_ID);
        String text = request.getParameter(License._Fields.TEXT.name());

        if(!isNullOrEmpty(licenseId)) {
            try {
                User user = UserCacheHolder.getUserFromRequest(request);
                LicenseService.Iface client = thriftClients.makeLicenseClient();
                final License license = client.getByID(licenseId,user.getDepartment());

                license.setText(CommonUtils.nullToEmptyString(text));
                final RequestStatus requestStatus = client.updateLicense(license, user, user);

                renderRequestStatus(request,response,requestStatus);
            } catch (TException e) {
                log.error("Error updating license", e);
            }
        }
        response.setRenderParameter(LICENSE_ID, licenseId);
        response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
        request.setAttribute(SELECTED_TAB, "tab-LicenseText");
    }

    @UsedAsLiferayAction
    public void addTodo(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String licenseID = request.getParameter(LICENSE_ID);
        String[] obligationIds = request.getParameterValues("obligations");
        String todoText = request.getParameter("todoText");
        String[] bools = request.getParameterValues("bools");


        Todo todo = new Todo();
        //add temporary id
        todo.setId(TMP_TODO_ID_PREFIX + UUID.randomUUID().toString());
        if (obligationIds != null) {
            for (String obligationId : obligationIds) {
                if (obligationId != null && !obligationId.isEmpty()) {
                    todo.addToObligationDatabaseIds(obligationId);
                }
            }
        } else {
            todo.setObligationDatabaseIds(Collections.emptySet());
        }
        todo.setText(todoText);
        todo.setTitle(StringUtils.EMPTY);

        User user = UserCacheHolder.getUserFromRequest(request);
        String moderationComment = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
        if(moderationComment != null) {
            user.setCommentMadeDuringModerationRequest(moderationComment);
        }

        todo.addToWhitelist(user.getDepartment());

        if (bools != null) {
            List<String> theBools = Arrays.asList(bools);
            todo.setDevelopment(theBools.contains("development"));
            todo.setDistribution(theBools.contains("distribution"));
        } else {
            todo.setDevelopment(false);
            todo.setDistribution(false);
        }
        try {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            RequestStatus requestStatus = client.addTodoToLicense(todo, licenseID, user);

            setSessionMessage(request, requestStatus, "License", "update");

        } catch (TException e) {
            log.error("Error updating license details from backend", e);
        }
        response.setRenderParameter(LICENSE_ID, licenseID);
        response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
        request.setAttribute(SELECTED_TAB, "tab-TodosAndObligations");
    }

    @UsedAsLiferayAction
    public void delete(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        RequestStatus requestStatus = deleteLicense(request);
        setSessionMessage(request, requestStatus, "License", "remove");
    }

    private RequestStatus deleteLicense(PortletRequest request) {
        String licenseId = request.getParameter(PortalConstants.LICENSE_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);

        try {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            return client.deleteLicense(licenseId, user);
        } catch (TException e) {
            log.error("Error deleting license from backend", e);
        }

        return RequestStatus.FAILURE;
    }

    @UsedAsLiferayAction
    public void editExternalLink(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String licenseId = request.getParameter(LICENSE_ID);
        String remoteLink = request.getParameter(License._Fields.EXTERNAL_LICENSE_LINK.name());

        if(!Strings.isNullOrEmpty(licenseId)) {
            try {
                User user = UserCacheHolder.getUserFromRequest(request);
                LicenseService.Iface client = thriftClients.makeLicenseClient();
                final License license = client.getByID(licenseId,user.getDepartment());

                license.setExternalLicenseLink(CommonUtils.nullToEmptyString(remoteLink));
                final RequestStatus requestStatus = client.updateLicense(license, user, user);

                renderRequestStatus(request,response,requestStatus);
            } catch (TException e) {
                log.error("Error updating license", e);
            }
        }
        response.setRenderParameter(LICENSE_ID, licenseId);
        response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
        request.setAttribute(SELECTED_TAB, "tab-Details");
    }
}
