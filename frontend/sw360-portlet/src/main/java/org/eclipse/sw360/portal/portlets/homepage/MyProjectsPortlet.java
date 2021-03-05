/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 * With contributions by Siemens Healthcare Diagnostics Inc, 2018.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.homepage;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.PortalUtil;
import org.eclipse.sw360.datahandler.common.CommonUtils;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.PortletUtils;
import org.eclipse.sw360.portal.common.datatables.PaginationParser;
import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Math.min;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_FILTERED;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_TOTAL;
import com.liferay.portal.kernel.language.LanguageUtil;
import static org.eclipse.sw360.portal.common.PortalConstants.MY_PROJECTS_PORTLET_NAME;


@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/user.properties"
    },
    property = {
        "javax.portlet.name=" + MY_PROJECTS_PORTLET_NAME,

        "javax.portlet.display-name=My Projects",
        "javax.portlet.info.short-title=My Projects",
        "javax.portlet.info.title=My Projects",
	    "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/homepage/myprojects/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MyProjectsPortlet extends Sw360Portlet {
    private static final ImmutableList<Project._Fields> projectFilteredFields = ImmutableList.of(Project._Fields.NAME,
            Project._Fields.DESCRIPTION, Project._Fields.RELEASE_CLEARING_STATE_SUMMARY);
    private static final ImmutableList<String> listOfRoles = ImmutableList.of(Project._Fields.CREATED_BY.toString(),
            Project._Fields.MODERATORS.toString(), Project._Fields.CONTRIBUTORS.toString(),
            Project._Fields.PROJECT_OWNER.toString(), Project._Fields.LEAD_ARCHITECT.toString(),
            Project._Fields.PROJECT_RESPONSIBLE.toString(), Project._Fields.SECURITY_RESPONSIBLES.toString());

    private static final ImmutableList<String> listOfClearingState = ImmutableList.of(ProjectClearingState.OPEN.toString(),
	ProjectClearingState.CLOSED.toString(), ProjectClearingState.IN_PROGRESS.toString());

    private static final int PROJECT_NO_SORT = -1;
    private static final int PROJECT_DT_ROW_NAME = 0;
    private static final int PROJECT_DT_ROW_DESCRIPTION = 1;
    private static final int PROJECT_DT_ROW_CLEARING_STATE = 2;

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);

        UserService.Iface userClient = thriftClients.makeUserClient();
        User userByEmail = null;
        try {
            userByEmail = userClient.getByEmail(user.getEmail());
        } catch (TException e) {
            log.error("Could not fetch user from backend with email, " + user.getEmail(), e);
        }

        if (userByEmail != null) {
            request.setAttribute("userRoles", userByEmail.getMyProjectsPreferenceSelection());
        }
        super.doView(request, response);
    }

    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.LOAD_PROJECT_LIST.equals(action)) {
            serveProjectList(request, response);
        }
    }

    private void serveProjectList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        List<Project> myProjects = new ArrayList<>();
        User user = UserCacheHolder.getUserFromRequest(request);
        HttpServletRequest originalServletRequest = PortalUtil
                .getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
        PaginationParameters paginationParameters = PaginationParser.parametersFrom(originalServletRequest);
        PortletUtils.handlePaginationSortOrder(request, paginationParameters, projectFilteredFields, PROJECT_NO_SORT);

        String rolesAndClearingStateSelected = request.getParameter("rolesandclearingstate");
        List<Boolean> listOfRolesAndClearingStateSelected = Arrays.stream(rolesAndClearingStateSelected.split(","))
                .map(role -> Boolean.parseBoolean(role)).collect(Collectors.toList());
        Boolean userChoice = Boolean.parseBoolean(request.getParameter("userChoice"));
        Map<String, Boolean> userRoles = new HashMap<>();
        for (int i = 0; i < listOfRoles.size(); i++) {
            userRoles.put(listOfRoles.get(i), listOfRolesAndClearingStateSelected.get(i));
        }

        Map<String, Boolean> clearingState = new HashMap<>();
        int rolesSize= listOfRoles.size();
        for (int j = 0; j < listOfClearingState.size(); j++) {
            clearingState.put(listOfClearingState.get(j), listOfRolesAndClearingStateSelected.get(j+rolesSize));
        }

        try {
            if (userChoice) {
                UserService.Iface userClient = thriftClients.makeUserClient();
                User userByEmail = userClient.getByEmail(user.getEmail());
                if (userByEmail != null) {
                    userByEmail.setMyProjectsPreferenceSelection(userRoles);
                    userClient.updateUser(userByEmail);
                }
            }
            myProjects = thriftClients.makeProjectClient().getMyProjects(user, userRoles);
        } catch (TException e) {
            log.error("Could not fetch myProjects from backend for user, " + user.getEmail(), e);
        }
        myProjects = getWithFilledClearingStateSummary(myProjects, user);
        myProjects = getWithFilledClearingStatus(myProjects, clearingState);

        JSONArray jsonProjects = getProjectData(myProjects, paginationParameters, request);
        JSONObject jsonResult = JSONFactoryUtil.createJSONObject();
        jsonResult.put("aaData", jsonProjects);
        jsonResult.put(DATATABLE_RECORDS_TOTAL, myProjects.size());
        jsonResult.put(DATATABLE_RECORDS_FILTERED, myProjects.size());

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem generating project list", e);
        }
    }

    private List<Project> getWithFilledClearingStateSummary(List<Project> projects, User user) {
        ProjectService.Iface projectClient = thriftClients.makeProjectClient();
        try {
            return projectClient.fillClearingStateSummary(projects, user);
        } catch (TException e) {
            log.error("Could not get summary of release clearing states for projects and their subprojects!", e);
            return projects;
        }
    }

    public JSONArray getProjectData(List<Project> projectList, PaginationParameters projectParameters, ResourceRequest request) {
        List<Project> sortedProjects = sortProjectList(projectList, projectParameters);
        int count = PortletUtils.getProjectDataCount(projectParameters, projectList.size());

        JSONArray projectData = JSONFactoryUtil.createJSONArray();
        for (int i = projectParameters.getDisplayStart(); i < count; i++) {
            Project project = sortedProjects.get(i);
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

            jsonObject.put("DT_RowId", project.getId());
            jsonObject.put("id", project.getId());
            jsonObject.put("name", SW360Utils.printName(project));
            jsonObject.put("description", Strings.nullToEmpty(project.getDescription()));
            jsonObject.put("releaseClearingState", acceptedReleases(project.getReleaseClearingStateSummary(),request));

            projectData.put(jsonObject);
        }

        return projectData;
    }

    private String acceptedReleases(ReleaseClearingStateSummary releaseClearingStateSummary,ResourceRequest  request) {
        String releaseCounts;

        if (releaseClearingStateSummary == null) {
            //releaseCounts = "not available";
        	releaseCounts = LanguageUtil.get(getResourceBundle(request.getLocale()), "not.available");
        } else {
            int total = releaseClearingStateSummary.newRelease + releaseClearingStateSummary.sentToClearingTool
                    + releaseClearingStateSummary.underClearing + releaseClearingStateSummary.reportAvailable
                    + releaseClearingStateSummary.approved;
            releaseCounts = releaseClearingStateSummary.approved + " / " + Integer.toString(total);
        }

        return releaseCounts;
    }

    private List<Project> sortProjectList(List<Project> projectList, PaginationParameters projectParameters) {
        boolean isAsc = projectParameters.isAscending().orElse(true);

        switch (projectParameters.getSortingColumn().orElse(PROJECT_DT_ROW_NAME)) {
        case PROJECT_DT_ROW_NAME:
            Collections.sort(projectList, PortletUtils.compareByName(isAsc));
            break;
        case PROJECT_DT_ROW_DESCRIPTION:
            Collections.sort(projectList, PortletUtils.compareByDescription(isAsc));
            break;
        case PROJECT_DT_ROW_CLEARING_STATE:
            Collections.sort(projectList, compareByApprovedClearingState(isAsc));
            break;
        default:
            break;
        }

        return projectList;
    }

    private Comparator<Project> compareByApprovedClearingState(boolean isAscending) {
        Comparator<Project> comparator = Comparator.comparing(
                p -> p.getReleaseClearingStateSummary() != null ? p.getReleaseClearingStateSummary().approved : -1);
        return isAscending ? comparator : comparator.reversed();
    }

    private List<Project> getWithFilledClearingStatus(List<Project> projects, Map<String, Boolean> clearingState ) {
        if (!CommonUtils.isNullOrEmptyMap(clearingState)) {
            Boolean open = clearingState.get(ProjectClearingState.OPEN.toString());
            Boolean closed = clearingState.get(ProjectClearingState.CLOSED.toString());
            Boolean inProgress = clearingState.get(ProjectClearingState.IN_PROGRESS.toString());

			projects = projects.stream().filter(project -> {
				if (open != null && open && ProjectClearingState.OPEN.equals(project.getClearingState())) {
					return true;
				} else if (closed != null && closed && ProjectClearingState.CLOSED.equals(project.getClearingState())) {
					return true;
				} else if (inProgress != null && inProgress
						&& ProjectClearingState.IN_PROGRESS.equals(project.getClearingState())) {
					return true;
				}
				return false;
			}).collect(Collectors.toList());
        }
        return projects;
    }


}
