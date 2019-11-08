/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.moderation;

import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.servlet.SessionMessages;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.FossologyAwarePortlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.eclipse.sw360.portal.users.UserUtils;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + MODERATION_PORTLET_NAME,

        "javax.portlet.display-name=Moderations",
        "javax.portlet.info.short-title=Moderations",
        "javax.portlet.info.title=Moderations",

        "javax.portlet.init-param.view-template=/html/moderation/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ModerationPortlet extends FossologyAwarePortlet {

    private static final Logger log = Logger.getLogger(ModerationPortlet.class);

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (PortalConstants.ACTION_REMOVEME.equals(action)) {
            removeMeFromModerators(request, response);
        } else if (PortalConstants.DELETE_MODERATION_REQUEST.equals(action)) {
            serveDeleteModerationRequest(request, response);
        } else if (isGenericAction(action)) {
            dealWithGenericAction(request, response, action);
        }
    }

    private void serveDeleteModerationRequest(ResourceRequest request, ResourceResponse response) throws IOException {
        RequestStatus requestStatus = ModerationPortletUtils.deleteModerationRequest(request, log);
        serveRequestStatus(request, response, requestStatus, "Problem removing moderation request", log);
    }

    private void removeMeFromModerators(ResourceRequest request, ResourceResponse response){
        final User user = UserCacheHolder.getUserFromRequest(request);
        final String id = request.getParameter(MODERATION_ID);
        ModerationService.Iface client = thriftClients.makeModerationClient();
        RemoveModeratorRequestStatus status = null;
        try {
            status = client.removeUserFromAssignees(id, user);
            request.setAttribute(PortalConstants.REQUEST_STATUS, status);
        } catch(TException e) {
            log.error("Error in Moderation ", e);
        }
        renderRemoveModerationRequestStatus(request, response, status);
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        if (PAGENAME_EDIT.equals(request.getParameter(PAGENAME))) {
            renderEditView(request, response);
        } else if (PAGENAME_ACTION.equals(request.getParameter(PAGENAME))) {
            renderActionView(request, response);
        } else {
            renderStandardView(request, response);
        }
    }

    private void renderActionView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        final String id = request.getParameter(MODERATION_ID);
        String sessionMessage;
        if (id != null) {
            try {
                ModerationService.Iface client = thriftClients.makeModerationClient();
                ModerationRequest moderationRequest = client.getModerationRequestById(id);
                String action = request.getParameter(ACTION);
                String encodedModerationComment = request.getParameter(MODERATION_DECISION_COMMENT);
                String moderationComment = "";
                if (encodedModerationComment != null) {
                    moderationComment = new String(Base64.getDecoder().decode(encodedModerationComment));
                }

                if (ACTION_CANCEL.equals(action)) {
                    client.cancelInProgress(id);

                    sessionMessage = "You have cancelled working on the previous moderation request.";
                } else if (ACTION_DECLINE.equals(action)) {
                    declineModerationRequest(user, moderationRequest, request);

                    client.refuseRequest(id, moderationComment, user.getEmail());
                    sessionMessage = "You have declined the previous moderation request.";
                } else if (ACTION_ACCEPT.equals(action)) {
                    String requestingUserEmail = moderationRequest.getRequestingUser();
                    User requestingUser = UserCacheHolder.getUserFromEmail(requestingUserEmail);
                    acceptModerationRequest(user, requestingUser, moderationRequest, request);

                    client.acceptRequest(moderationRequest, moderationComment, user.getEmail());
                    sessionMessage = "You have accepted the previous moderation request.";
                } else if (ACTION_POSTPONE.equals(action)) {
                    // keep me assigned but do it later... so nothing to be done here, just update the comment message
                    moderationRequest.setCommentDecisionModerator(moderationComment);
                    client.updateModerationRequest(moderationRequest);
                    sessionMessage = "You have postponed the previous moderation request.";
                } else if (ACTION_RENDER_NEXT_AFTER_UNSUBSCRIBE.equals(action)) {
                    sessionMessage = "You are removed from the list of moderators for the previous moderation request.";
                } else {
                   throw new PortletException("Unknown action");
                }

                //! Actions are processed now we go and render the next one
                renderNextModeration(request, response, user, sessionMessage, client, moderationRequest);
            } catch (TException e) {
                log.error("Error in Moderation ", e);
            }
        }
    }

    private void declineModerationRequest(User user, ModerationRequest moderationRequest, RenderRequest request) throws TException {
        switch (moderationRequest.getDocumentType()) {
            case USER:
                UserUtils.deleteLiferayUser(request, moderationRequest.getUser());
                UserService.Iface userClient = thriftClients.makeUserClient();
                userClient.deleteUser(UserCacheHolder.getRefreshedUserFromEmail(moderationRequest.getUser().getEmail()), user);
                break;
            default:
                break;
        }
    }
    private void acceptModerationRequest(User user, User requestingUser, ModerationRequest moderationRequest, RenderRequest request) throws TException {
        switch (moderationRequest.getDocumentType()) {
            case COMPONENT: {
                ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                if (moderationRequest.isRequestDocumentDelete()) {
                    componentClient.deleteComponent(moderationRequest.getDocumentId(), user);
                } else {
                    componentClient.updateComponentFromModerationRequest(
                            moderationRequest.getComponentAdditions(),
                            moderationRequest.getComponentDeletions(),
                            user);
                }
            }
            break;
            case RELEASE: {
                ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                if (moderationRequest.isRequestDocumentDelete()) {
                    componentClient.deleteRelease(moderationRequest.getDocumentId(), user);
                } else {
                    componentClient.updateReleaseFromModerationRequest(
                            moderationRequest.getReleaseAdditions(),
                            moderationRequest.getReleaseDeletions(),
                            user);
                }
            }
            break;
            case PROJECT: {
                ProjectService.Iface projectClient = thriftClients.makeProjectClient();
                if (moderationRequest.isRequestDocumentDelete()) {
                    projectClient.deleteProject(moderationRequest.getDocumentId(), user);
                } else {
                    projectClient.updateProjectFromModerationRequest(
                            moderationRequest.getProjectAdditions(),
                            moderationRequest.getProjectDeletions(),
                            user);
                }
            }
            break;
            case LICENSE: {
                LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
                    licenseClient.updateLicenseFromModerationRequest(
                            moderationRequest.getLicenseAdditions(),
                            moderationRequest.getLicenseDeletions(),
                            user,
                            requestingUser);
            }
            break;
            case USER: {
                UserUtils.activateLiferayUser(request, moderationRequest.getUser());
            }
            break;
        }
    }

    private void renderNextModeration(RenderRequest request, RenderResponse response, final User user, String sessionMessage, ModerationService.Iface client, ModerationRequest moderationRequest) throws IOException, PortletException, TException {
        if (ACTION_CANCEL.equals(request.getParameter(ACTION))) {
            SessionMessages.add(request, "request_processed", sessionMessage);
            renderStandardView(request, response);
            return;
        }

        List<ModerationRequest> requestsByModerator = client.getRequestsByModerator(user);
        List<ModerationRequest> openModerationRequests = requestsByModerator
                .stream()
                .filter(input-> ModerationState.PENDING.equals(input.getModerationState()))
                .collect(Collectors.toList());

        Collections.sort(openModerationRequests, compareByTimeStamp());

        int nextIndex = openModerationRequests.indexOf(moderationRequest) + 1;
        if (nextIndex < openModerationRequests.size()) {
            renderEditViewForId(request, response, openModerationRequests.get(nextIndex).getId());
        } else {
            List<ModerationRequest> requestsInProgressAndAssignedToMe = requestsByModerator
                    .stream()
                    .filter(input-> ModerationState.INPROGRESS.equals(input.getModerationState()) && user.getEmail().equals(input.getReviewer()))
                    .collect(Collectors.toList());

            if (requestsInProgressAndAssignedToMe.size()>0) {
                sessionMessage += " You have returned to your first open request.";
                SessionMessages.add(request, "request_processed", sessionMessage);
                renderEditViewForId(request, response, Collections.min(requestsInProgressAndAssignedToMe, compareByTimeStamp()).getId());
            } else {
                sessionMessage += " You have no open Requests.";
                SessionMessages.add(request, "request_processed", sessionMessage);
                renderStandardView(request, response);
            }
        }
    }

    @NotNull
    private Comparator<ModerationRequest> compareByTimeStamp() {
        return new Comparator<ModerationRequest>() {
            @Override
            public int compare(ModerationRequest o1, ModerationRequest o2) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
        };
    }

    private void addModerationBreadcrumb(RenderRequest request, RenderResponse response, ModerationRequest moderationRequest) {
        PortletURL baseUrl = response.createRenderURL();
        baseUrl.setParameter(PAGENAME, PAGENAME_EDIT);
        baseUrl.setParameter(MODERATION_ID, moderationRequest.getId());

        addBreadcrumbEntry(request, moderationRequest.getDocumentName(), baseUrl);
    }

    public void renderStandardView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);

        List<ModerationRequest> openModerationRequests = null;
        List<ModerationRequest> closedModerationRequests = null;

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            List<ModerationRequest> moderationRequests = client.getRequestsByModerator(user);
            Map<Boolean, List<ModerationRequest>> partitionedModerationRequests = moderationRequests
                    .stream()
                    .collect(Collectors.groupingBy(ModerationPortletUtils::isOpenModerationRequest));
            openModerationRequests = partitionedModerationRequests.get(true);
            closedModerationRequests = partitionedModerationRequests.get(false);
        } catch (TException e) {
            log.error("Could not fetch moderation requests from backend!", e);
        }

        request.setAttribute(MODERATION_REQUESTS, CommonUtils.nullToEmptyList(openModerationRequests));
        request.setAttribute(CLOSED_MODERATION_REQUESTS, CommonUtils.nullToEmptyList(closedModerationRequests));
        request.setAttribute(IS_USER_AT_LEAST_CLEARING_ADMIN, PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user) ? "Yes" : "No");
        super.doView(request, response);
    }

    public void renderEditView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String id = request.getParameter(MODERATION_ID);
        try {
            renderEditViewForId(request, response, id);
        } catch (TException e) {
            log.error("Thrift error", e);
        }
    }

    private void renderEditViewForId(RenderRequest request, RenderResponse response, String id) throws IOException, PortletException, TException {
        if (id != null) {
            ModerationRequest moderationRequest = null;
            User user = UserCacheHolder.getUserFromRequest(request);
            try {

                ModerationService.Iface client = thriftClients.makeModerationClient();
                moderationRequest = client.getModerationRequestById(id);
                boolean actionsAllowed = moderationRequest.getModerators().contains(user.getEmail()) && ModerationPortletUtils.isOpenModerationRequest(moderationRequest);
                request.setAttribute(PortalConstants.MODERATION_ACTIONS_ALLOWED, actionsAllowed);
                if(actionsAllowed) {
                    SessionMessages.add(request, "request_processed", "You have assigned yourself to this moderation request.");
                    client.setInProgress(id, user);
                }
                request.setAttribute(PortalConstants.MODERATION_REQUEST, moderationRequest);
                addModerationBreadcrumb(request, response, moderationRequest);

            } catch (TException e) {
                log.error("Error fetching moderation  details from backend", e);
            }

            if (moderationRequest != null) {
                switch (moderationRequest.getDocumentType()) {
                    case COMPONENT:
                        renderComponentModeration(request, response, moderationRequest, user);
                        break;
                    case RELEASE:
                        VendorService.Iface vendorClient = thriftClients.makeVendorClient();
                        Release additions = moderationRequest.getReleaseAdditions();
                        if(additions.isSetVendorId()){
                            additions.setVendor(vendorClient.getByID(additions.getVendorId()));
                        }
                        Release deletions = moderationRequest.getReleaseDeletions();
                        if(deletions.isSetVendorId()){
                            deletions.setVendor(vendorClient.getByID(deletions.getVendorId()));
                        }
                        renderReleaseModeration(request, response, moderationRequest, user);
                        break;
                    case PROJECT:
                        renderProjectModeration(request, response, moderationRequest, user);
                        break;
                    case LICENSE:
                        renderLicenseModeration(request, response, moderationRequest, user);
                        break;
                    case USER:
                        renderUserModeration(request, response, moderationRequest, user);
                        break;
                }
            }
        }
    }

    public void renderComponentModeration(RenderRequest request, RenderResponse response, ModerationRequest moderationRequest, User user) throws IOException, PortletException, TException {

        final boolean requestDocumentDelete = moderationRequest.isRequestDocumentDelete();
        Boolean is_used = false;

        Component actual_component = null;

        try {
            ComponentService.Iface client = thriftClients.makeComponentClient();
            actual_component = client.getComponentById(moderationRequest.getDocumentId(), user);
            is_used = client.componentIsUsed(actual_component.getId());
        } catch (TException e) {
            log.error("Could not retrieve component", e);
        }

        if (actual_component == null) {
            renderNextModeration(request, response, user, "Ignored unretrievable target", thriftClients.makeModerationClient(), moderationRequest);
            return;
        }

        if (refuseToDeleteUsedDocument(request, response, moderationRequest, user, requestDocumentDelete, is_used))
            return;

        prepareComponent(request, user, actual_component);
        request.setAttribute(PortalConstants.ACTUAL_COMPONENT, actual_component);
        if (moderationRequest.isRequestDocumentDelete()) {
            include("/html/moderation/components/delete.jsp", request, response);
        } else {
            include("/html/moderation/components/merge.jsp", request, response);
        }
    }

    private void prepareComponent(RenderRequest request, User user, Component actualComponent) {
        List<Release> releases;

        releases = CommonUtils.nullToEmptyList(actualComponent.getReleases());
        Set<String> releaseIds = SW360Utils.getReleaseIds(releases);

        Set<Project> usingProjects = null;
        int allUsingProjectsCount = 0;

        if (releaseIds != null && releaseIds.size() > 0) {
            try {
                ProjectService.Iface projectClient = thriftClients.makeProjectClient();
                usingProjects = projectClient.searchByReleaseIds(releaseIds, user);
                allUsingProjectsCount = projectClient.getCountByReleaseIds(releaseIds);
            } catch (TException e) {
                log.error("Could not retrieve using projects", e);
            }
        }
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_COMPONENT);
        setAttachmentsInRequest(request, actualComponent);
        request.setAttribute(USING_PROJECTS, nullToEmptySet(usingProjects));
        request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectsCount);
    }

    public void renderReleaseModeration(RenderRequest request, RenderResponse response, ModerationRequest moderationRequest, User user) throws IOException, PortletException, TException {
        Release actual_release = null;

        final boolean requestDocumentDelete = moderationRequest.isRequestDocumentDelete();
        Boolean is_used = false;

        try {
            ComponentService.Iface client = thriftClients.makeComponentClient();
            actual_release = client.getReleaseById(moderationRequest.getDocumentId(), user);
            is_used = client.releaseIsUsed(actual_release.getId());
        } catch (TException e) {
            log.error("Could not retrieve release", e);
        }

        if (actual_release == null) {
            renderNextModeration(request, response, user, "Ignored unretrievable target", thriftClients.makeModerationClient(), moderationRequest);
            return;
        }

        if (refuseToDeleteUsedDocument(request, response, moderationRequest, user, requestDocumentDelete, is_used))
            return;

        prepareRelease(request, user, actual_release);
        request.setAttribute(PortalConstants.ACTUAL_RELEASE, actual_release);
        if (requestDocumentDelete) {
            include("/html/moderation/releases/delete.jsp", request, response);
        } else {
            include("/html/moderation/releases/merge.jsp", request, response);
        }
    }

    private boolean refuseToDeleteUsedDocument(RenderRequest request, RenderResponse response, ModerationRequest moderationRequest, User user, boolean requestDocumentDelete, Boolean is_used) throws TException, IOException, PortletException {
        if (requestDocumentDelete && is_used) {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.refuseRequest(moderationRequest.getId(), "You cannot delete a document still used by others", user.getEmail());
            renderNextModeration(request, response, user, "Ignored delete of used target", client, moderationRequest);
            return true;
        }
        return false;
    }

    private void prepareRelease(RenderRequest request, User user, Release actualRelease) {

        String actualReleaseId = actualRelease.getId();
        request.setAttribute(DOCUMENT_ID, actualReleaseId);
        request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_RELEASE);
        setAttachmentsInRequest(request, actualRelease);
        try {
            ProjectService.Iface projectClient = thriftClients.makeProjectClient();
            Set<Project> usingProjects = projectClient.searchByReleaseId(actualReleaseId, user);
            request.setAttribute(USING_PROJECTS, nullToEmptySet(usingProjects));
            int allUsingProjectsCount = projectClient.getCountByReleaseIds(Collections.singleton(actualReleaseId));
            request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectsCount);
            putDirectlyLinkedReleaseRelationsInRequest(request, actualRelease);
        } catch (TException e) {
            log.error("Could not retrieve using projects", e);
        }

        try {
            request.setAttribute(COMPONENT, thriftClients.makeComponentClient().getComponentById(actualRelease.getComponentId(), user));
        } catch (TException e) {
            log.error("Could not fetch component from Backend ", e);
        }

    }

    public void renderProjectModeration(RenderRequest request, RenderResponse response, ModerationRequest moderationRequest, User user) throws IOException, PortletException, TException {
        final boolean requestDocumentDelete = moderationRequest.isRequestDocumentDelete();
        Boolean is_used = false;
        Project actual_project = null;
        try {
            ProjectService.Iface client = thriftClients.makeProjectClient();
            actual_project = client.getProjectById(moderationRequest.getDocumentId(), user);
            actual_project = client.fillClearingStateSummary(Collections.singletonList(actual_project), user).get(0);
            is_used = client.projectIsUsed(actual_project.getId());
            request.setAttribute(PortalConstants.ACTUAL_PROJECT, actual_project);
            request.setAttribute(PortalConstants.DEFAULT_LICENSE_INFO_HEADER_TEXT, getDefaultLicenseInfoHeaderText());
        } catch (TException e) {
            log.error("Could not retrieve project", e);
        }

        if (actual_project == null) {
            renderNextModeration(request, response, user, "Ignored unretrievable target", thriftClients.makeModerationClient(), moderationRequest);
            return;
        }

        if (refuseToDeleteUsedDocument(request, response, moderationRequest, user, requestDocumentDelete, is_used))
            return;

        prepareProject(request, user, actual_project);
        if (moderationRequest.isRequestDocumentDelete()) {
            include("/html/moderation/projects/delete.jsp", request, response);
        } else {
            //updateProjectFromModerationRequest and add updated project to request.
            include("/html/moderation/projects/merge.jsp", request, response);
        }
    }

    private void prepareProject(RenderRequest request, User user, Project actual_project) {
        try {
            ProjectService.Iface client = thriftClients.makeProjectClient();
            List<ProjectLink> mappedProjectLinks = createLinkedProjects(actual_project, user);
            request.setAttribute(PROJECT_LIST, mappedProjectLinks);
            putDirectlyLinkedReleasesInRequest(request, actual_project);
            Set<Project> usingProjects = client.searchLinkingProjects(actual_project.getId(), user);
            request.setAttribute(USING_PROJECTS, usingProjects);
            int allUsingProjectsCount = client.getCountByProjectId(actual_project.getId());
            request.setAttribute(ALL_USING_PROJECTS_COUNT, allUsingProjectsCount);
            putReleasesAndProjectIntoRequest(request, actual_project.getId(), user);
            request.setAttribute(DOCUMENT_TYPE, SW360Constants.TYPE_PROJECT);
            setAttachmentsInRequest(request, actual_project);
        } catch (TException e) {
            log.error("Error fetching project from backend!", e);
        }
    }

    public void renderLicenseModeration(RenderRequest request, RenderResponse response, ModerationRequest moderationRequest, User user) throws IOException, PortletException, TException {
        License actual_license = null;
        User requestingUser = UserCacheHolder.getUserFromEmail(moderationRequest.getRequestingUser());
        try {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            actual_license = client.getByID(moderationRequest.getDocumentId(),requestingUser.getDepartment());
            request.setAttribute(KEY_LICENSE_DETAIL, actual_license);
            List<Obligation> obligations = client.getObligations();
            request.setAttribute(KEY_OBLIGATION_LIST, obligations);
        } catch (TException e) {
            log.error("Could not retrieve license", e);
        }

        if (actual_license == null) {
            renderNextModeration(request, response, user, "Ignored unretrievable target", thriftClients.makeModerationClient(), moderationRequest);
            return;
        }

        include("/html/moderation/licenses/merge.jsp", request, response);
    }

    public void  renderUserModeration(RenderRequest request, RenderResponse response, ModerationRequest moderationRequest, User user) throws IOException, PortletException, TException {
        User changedUser = null;
        try {
            UserService.Iface client = thriftClients.makeUserClient();
            changedUser = client.getByEmail(moderationRequest.getUser().getEmail());
            request.setAttribute(PortalConstants.USER, changedUser);
        } catch (TException e) {
            log.error("Could not retrieve user", e);
        }

        if (changedUser == null) {
            renderNextModeration(request, response, user, "Ignored unretrievable target", thriftClients.makeModerationClient(), moderationRequest);
            return;
        }

        List<Organization> organizations = UserUtils.getOrganizations(request);
        request.setAttribute(PortalConstants.ORGANIZATIONS, organizations);

        include("/html/moderation/users/merge.jsp", request, response);
    }

    private UnsupportedOperationException unsupportedActionException() {
        throw new UnsupportedOperationException("cannot call this action on the moderation portlet");
    }

    @Override
    protected void dealWithFossologyAction(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException {
        throw unsupportedActionException();
    }

    @Override
    protected Set<Attachment> getAttachments(String documentId, String documentType, User user) {
        throw unsupportedActionException();
    }

    private String getDefaultLicenseInfoHeaderText() {
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
