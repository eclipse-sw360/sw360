/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.admin;

import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@Component(
        immediate = true,
        properties = {
                "/org/eclipse/sw360/portal/portlets/base.properties",
                "/org/eclipse/sw360/portal/portlets/admin.properties"
        },
        property = {
                "javax.portlet.name=" + DEPARTMENT_PORTLET_NAME,
                "javax.portlet.display-name=Department",
                "javax.portlet.info.short-title=Department",
                "javax.portlet.info.title=Department",
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.init-param.view-template=/html/admin/department/view.jsp",
        },
        service = Portlet.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class DepartmentPortlet extends Sw360Portlet {
    private static final Logger log = LogManager.getLogger(DepartmentPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_EDIT.equals(pageName)) {
            prepareDepartmentEdit(request);
            include("/html/admin/department/edit.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }

    }

    private void prepareStandardView(RenderRequest request) {
        User user = UserCacheHolder.getUserFromRequest(request);
        ScheduleService.Iface scheduleClient = new ThriftClients().makeScheduleClient();
        UserService.Iface userClient = thriftClients.makeUserClient();
        try {
            boolean isDepartmentScheduled = isDepartmentScheduled(scheduleClient, user);
            request.setAttribute(PortalConstants.DEPARTMENT_IS_SCHEDULED, isDepartmentScheduled);
            int offsetInSeconds = scheduleClient.getFirstRunOffset(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            request.setAttribute(PortalConstants.DEPARTMENT_OFFSET, CommonUtils.formatTime(offsetInSeconds));
            int intervalInSeconds = scheduleClient.getInterval(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            request.setAttribute(PortalConstants.DEPARTMENT_INTERVAL, CommonUtils.formatTime(intervalInSeconds));
            String nextSync = scheduleClient.getNextSync(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            request.setAttribute(PortalConstants.DEPARTMENT_NEXT_SYNC, nextSync);
            Map<String, List<User>> listMap = userClient.getAllUserByDepartment();
            request.setAttribute(PortalConstants.DEPARTMENT_LIST, listMap);
            Map<String, List<String>> listContentFileLog = userClient.getAllContentFileLog()
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
            request.setAttribute(PortalConstants.LIST_CONTENT_FILE_LOG, listContentFileLog);
            String pathConfigFolderDepartment = userClient.getPathConfigDepartment();
            request.setAttribute(PortalConstants.PATH_CONFIG_FOLDER_DEPARTMENT, pathConfigFolderDepartment);
            request.setAttribute(PortalConstants.LAST_FILE_NAME, userClient.getLastModifiedFileName());
            request.setAttribute(PortalConstants.LAST_RUNNING_TIME, userClient.getLastRunningTime());
        } catch (TException e) {
            log.error("Error server: {}", e.getMessage());
        }
    }

    private boolean isDepartmentScheduled(ScheduleService.Iface scheduleClient, User user) throws TException {
        RequestStatusWithBoolean requestStatus = scheduleClient.isServiceScheduled(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
        if (RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus())) {
            return requestStatus.isAnswerPositive();
        } else {
            throw new SW360Exception("Backend query for schedule status of department failed.");
        }
    }

    @UsedAsLiferayAction
    public void scheduleImportDepartment(ActionRequest request, ActionResponse response) throws PortletException {
        try {
            RequestSummary requestSummary =
                    new ThriftClients().makeScheduleClient().scheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            setSessionMessage(request, requestSummary.getRequestStatus(), "Task", "schedule");
            removeParamUrl(request, response);
        } catch (TException e) {
            log.error("Schedule import department: {}", e.getMessage());
        }
    }

    @UsedAsLiferayAction
    public void unScheduleImportDepartment(ActionRequest request, ActionResponse response) throws PortletException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
            setSessionMessage(request, requestStatus, "Task", "unschedule");
            removeParamUrl(request, response);
        } catch (TException e) {
            log.error("Cancel Schedule import department: {}", e.getMessage());
        }
    }

    @UsedAsLiferayAction
    public void writePathFolder(ActionRequest request, ActionResponse response) throws PortletException {
        String path = request.getParameter(PortalConstants.DEPARTMENT_URL);
        try {
            UserService.Iface userClient = thriftClients.makeUserClient();
            userClient.writePathFolderConfig(path);
            setSessionMessage(request, RequestStatus.SUCCESS, "Edit folder path", "Success");
            removeParamUrl(request, response);
        } catch (TException e) {
            log.error("Error edit folder path in backend!", e);
            setSW360SessionError(request, ErrorMessages.DEFAULT_ERROR_MESSAGE);
        }
    }

    public void removeParamUrl(ActionRequest request, ActionResponse response) {
        try {
            String portletId = (String) request.getAttribute(WebKeys.PORTLET_ID);
            ThemeDisplay tD = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            long plid = tD.getPlid();
            LiferayPortletURL redirectUrl = PortletURLFactoryUtil.create(request, portletId, plid, PortletRequest.RENDER_PART);
            request.setAttribute(WebKeys.REDIRECT, redirectUrl.toString());
            response.sendRedirect(redirectUrl.toString());
        } catch (IOException e) {
            log.info("Error remove param url: {}", e.getMessage());
        }
    }

    private void prepareDepartmentEdit(RenderRequest request) {
        String key = request.getParameter(DEPARTMENT_KEY);
        if (!isNullOrEmpty(key)) {
            try {
                UserService.Iface userClient = thriftClients.makeUserClient();
                String jsonEmailsByDepartment = userClient.convertUsersByDepartmentToJson(key);
                String jsonEmailsOtherDepartment = userClient.convertEmailsOtherDepartmentToJson(key);
                request.setAttribute(EMAIL_OTHER_DEPARTMENT_JSON, jsonEmailsOtherDepartment);
                request.setAttribute(EMAIL_BY_DEPARTMENT_JSON, jsonEmailsByDepartment);
                request.setAttribute(PortalConstants.DEPARTMENT_KEY, key);
            } catch (TException e) {
                log.error("Problem retrieving department");
            }
        }
    }

    @UsedAsLiferayAction
    public void updateDepartment(ActionRequest request, ActionResponse response) throws TException {
        String key = request.getParameter(DEPARTMENT_KEY);
        List<User> usersAdd = ComponentPortletUtils.updateUserAddFromRequest(request, log);
        List<User> usersDelete = ComponentPortletUtils.updateUserDeleteFromRequest(request, log);
        if (key != null) {
            try {
                UserService.Iface userClient = thriftClients.makeUserClient();
                if (usersAdd != null) {
                    userClient.updateDepartmentToListUser(usersAdd, key);
                }
                if (usersDelete != null) {
                    userClient.deleteDepartmentByListUser(usersDelete, key);
                }
                removeParamUrl(request, response);
            } catch (TException e) {
                log.error("Error fetching User from backend!", e);
            }
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (action == null) {
            log.error("Invalid action 'null'");
            return;
        }
        switch (action) {
            case PortalConstants.IMPORT_DEPARTMENT_MANUALLY:
                try {
                    importDepartmentManually(request, response);
                } catch (TException | IOException e) {
                    log.error("Something went wrong with the department", e);
                }
                break;
            default:
                log.warn("The DepartmentPortlet was called with unsupported action=[" + action + "]");
        }
    }

    private void importDepartmentManually(ResourceRequest request, ResourceResponse response) throws TException, IOException {
        UserService.Iface userClient = thriftClients.makeUserClient();
        RequestSummary requestSummary = userClient.importFileToDB();
        renderRequestSummary(request, response, requestSummary);
    }

}
