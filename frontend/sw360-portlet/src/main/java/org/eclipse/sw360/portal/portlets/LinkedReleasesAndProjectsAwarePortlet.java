/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.portal.common.PortalConstants.PARENT_BRANCH_ID;
import static org.eclipse.sw360.portal.common.PortalConstants.PROJECT_LIST;
import static org.eclipse.sw360.portal.common.PortalConstants.RELEASE_LIST;
import static org.eclipse.sw360.portal.common.PortalConstants.TOTAL_INACCESSIBLE_ROWS;

/**
 * linked releases and projects-aware portlet implementation
 *
 * @author alex.borodin@evosoft.com
 */
public abstract class LinkedReleasesAndProjectsAwarePortlet extends AttachmentAwarePortlet {

    private static final Logger log = LogManager.getLogger(LinkedReleasesAndProjectsAwarePortlet.class);

    protected LinkedReleasesAndProjectsAwarePortlet() {
        this(new ThriftClients());
    }

    public LinkedReleasesAndProjectsAwarePortlet(ThriftClients thriftClients) {
        super(thriftClients);
    }


    protected boolean isLinkedObjectsAwareAction(String action) {
        return action.startsWith(PortalConstants.LINKED_OBJECTS_PREFIX);
    }

    @Override
    protected boolean isGenericAction(String action) {
        return super.isGenericAction(action) || isLinkedObjectsAwareAction(action);
    }

    @Override
    protected void dealWithGenericAction(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException {
        if (super.isGenericAction(action)) {
            super.dealWithGenericAction(request, response, action);
        } else {
            dealWithLinkedObjects(request, response, action);
        }
    }

    protected void dealWithLinkedObjects(ResourceRequest request, ResourceResponse response, String action) throws PortletException, IOException {
        if (PortalConstants.LOAD_LINKED_PROJECTS_ROWS.equals(action)) {
            boolean overrideToRelease = Boolean.parseBoolean(request.getParameter("overrideToRelease"));
            if (overrideToRelease) {
                try {
                    loadLinkedReleaseOfProject(request, response);
                    include("/html/utils/ajax/linkedReleaseInNetWorkRows.jsp", request, response,
                            PortletRequest.RESOURCE_PHASE);
                } catch (TException e) {
                    log.error("Error when create new release row for tree view");
                }
                return;
            }
            serveLoadLinkedProjectsRows(request, response);
        } else if (PortalConstants.LOAD_LINKED_RELEASES_ROWS.equals(action)) {
            serveLoadLinkedReleasesRows(request, response);
        }
    }

    protected void putDirectlyLinkedReleaseRelationsInRequest(PortletRequest request, Release release) {
        List<ReleaseLink> linkedReleaseRelations = SW360Utils.getLinkedReleaseRelations(release, thriftClients, log);
        linkedReleaseRelations = linkedReleaseRelations.stream().filter(Objects::nonNull).sorted(Comparator.comparing(
                rl -> SW360Utils.getVersionedName(nullToEmptyString(rl.getName()), rl.getVersion()), String.CASE_INSENSITIVE_ORDER)
                ).collect(Collectors.toList());
        request.setAttribute(RELEASE_LIST, linkedReleaseRelations);
    }

    protected void putDirectlyLinkedReleaseRelationsWithAccessibilityInRequest(PortletRequest request, Release release, User user) {
        List<ReleaseLink> linkedReleaseRelations = SW360Utils.getLinkedReleaseRelationsWithAccessibility(release, thriftClients, log, user);
        linkedReleaseRelations = linkedReleaseRelations.stream().filter(Objects::nonNull).sorted(Comparator.comparing(
                rl -> rl.isAccessible() ? SW360Utils.getVersionedName(nullToEmptyString(rl.getName()), rl.getVersion()) : "~", String.CASE_INSENSITIVE_ORDER)
                ).collect(Collectors.toList());
        request.setAttribute(RELEASE_LIST, linkedReleaseRelations);
        
        int totalInaccessibleRow = 0;
        for (ReleaseLink link : linkedReleaseRelations) {
            if (!link.isAccessible()) {
                totalInaccessibleRow++;
            }
        }               
        request.setAttribute(TOTAL_INACCESSIBLE_ROWS, totalInaccessibleRow);
    }

    protected List<ProjectLink> createLinkedProjects(Project project, User user) {
        return createLinkedProjects(project, Function.identity(), user);
    }

    protected List<ProjectLink> createLinkedProjects(Project project, Function<ProjectLink, ProjectLink> projectLinkMapper, User user) {
        return createLinkedProjects(project, projectLinkMapper, false, user);
    }

    protected List<ProjectLink> createLinkedProjects(Project project, Function<ProjectLink, ProjectLink> projectLinkMapper, boolean deep,
            User user) {
        final Collection<ProjectLink> linkedProjects = SW360Utils.getLinkedProjectsAsFlatList(project, deep, thriftClients, log, user);
        return linkedProjects.stream().map(projectLinkMapper).collect(Collectors.toList());
    }

    protected List<ProjectLink> createLinkedProjects(Project project,
            Function<ProjectLink, ProjectLink> projectLinkMapper, boolean deep, User user,
            Set<ProjectRelationship> selectedProjectRelationAsList) {
        final Collection<ProjectLink> linkedProjects = SW360Utils.getLinkedProjectsAsFlatList(project, deep,
                thriftClients, log, user, selectedProjectRelationAsList);
        return linkedProjects.stream().map(projectLinkMapper).collect(Collectors.toList());
    }

    protected void putDirectlyLinkedProjectsInRequest(PortletRequest request, Project project, User user) {
        request.setAttribute(PROJECT_LIST, SW360Utils.getDirectlyLinkedProjects(project, user));
    }

    public Function<ProjectLink, ProjectLink> createProjectLinkMapper(Function<ReleaseLink, ReleaseLink> releaseLinkMapper){
        return (projectLink) -> {
            List<ReleaseLink> mappedReleaseLinks = nullToEmptyList(projectLink
                    .getLinkedReleases())
                    .stream()
                    .map(releaseLinkMapper)
                    .collect(Collectors.toList());
            projectLink.setLinkedReleases(mappedReleaseLinks);
            return projectLink;
        };
    }

    protected void serveLoadLinkedProjectsRows(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        prepareLinkedProjects(request);
        include("/html/utils/ajax/linkedProjectsRows.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    protected void prepareLinkedProjects(ResourceRequest request) throws PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String branchId = request.getParameter(PortalConstants.NETWORK_PARENT_BRANCH_ID);
        Optional<String> projectIdOpt = getProjectIdFromBranchId(branchId);
        request.setAttribute(PortalConstants.NETWORK_PARENT_BRANCH_ID, branchId);
        final Project project;
        if (projectIdOpt.isPresent()) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                project = client.getProjectById(projectIdOpt.get(), user);
            } catch (TException e) {
                log.error("Error getting projects!", e);
                throw new PortletException("cannot load project " + projectIdOpt.get(), e);
            }
            String parentProjectPath = request.getParameter(PortalConstants.PARENT_PROJECT_PATH);
            if (parentProjectPath != null) {
                request.setAttribute(PortalConstants.PARENT_PROJECT_PATH,
                        parentProjectPath.concat(":").concat(projectIdOpt.get()));
            }
        } else {
            project = new Project();
        }
        List<ProjectLink> mappedProjectLinks = createLinkedProjects(project, user);

        mappedProjectLinks = sortProjectLink(mappedProjectLinks);
        request.setAttribute(PROJECT_LIST, mappedProjectLinks);
        request.setAttribute(PortalConstants.PARENT_SCOPE_GROUP_ID, request.getParameter(PortalConstants.PARENT_SCOPE_GROUP_ID));
    }

    protected Optional<String> getProjectIdFromBranchId(String branchId) {
        String[] split = nullToEmptyString(branchId).split("_");
        return split.length > 0 ? Optional.of(split[0]) : Optional.empty();
    }

    protected void serveLoadLinkedReleasesRows(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        loadLinkedReleasesRows(request, response);
        include("/html/utils/ajax/linkedReleasesRows.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    protected List<ProjectLink> sortProjectLink(List<ProjectLink> mappedProjectLinks) {
        if (CommonUtils.isNotEmpty(mappedProjectLinks)) {
            ProjectLink parentProjectLink = mappedProjectLinks.remove(0);
            if (parentProjectLink != null && CommonUtils.isNotEmpty(parentProjectLink.getLinkedReleases())) {
                List<ReleaseLink> sortedLinkedRelease = parentProjectLink.getLinkedReleases().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(
                                rl -> SW360Utils.getVersionedName(nullToEmptyString(rl.getName()), rl.getVersion()), String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
                parentProjectLink.setLinkedReleases(sortedLinkedRelease);
            }
            mappedProjectLinks = mappedProjectLinks.stream().filter(Objects::nonNull)
                    .sorted(Comparator.comparing(
                            pl -> SW360Utils.getVersionedName(nullToEmptyString(pl.getName()), pl.getVersion()), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            mappedProjectLinks.add(0, parentProjectLink);
        }
        return mappedProjectLinks;
    }

    private void loadLinkedReleasesRows(ResourceRequest request, ResourceResponse response)
            throws PortletException, IOException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String branchId = request.getParameter(PARENT_BRANCH_ID);
        request.setAttribute(PARENT_BRANCH_ID, branchId);
        ComponentService.Iface client = thriftClients.makeComponentClient();
        if (branchId != null) {
            String id = branchId.split("_")[0];
            try {
                Release release = client.getReleaseById(id, user);
                putDirectlyLinkedReleaseRelationsWithAccessibilityInRequest(request, release, user);
            } catch (TException e) {
                log.error("Error getting projects!", e);
                throw new PortletException("cannot get projects", e);
            }
        } else {
            putDirectlyLinkedReleaseRelationsWithAccessibilityInRequest(request, new Release(), user);
        }
        List<ReleaseLink> releaseLinkList = (List<ReleaseLink>) request.getAttribute(RELEASE_LIST);
        Set<String> releaseIds = releaseLinkList.stream().map(ReleaseLink::getId).collect(Collectors.toSet());
        request.setAttribute("relMainLineState", fillMainLineState(releaseIds, client, user));
        request.setAttribute(PortalConstants.PARENT_SCOPE_GROUP_ID, request.getParameter(PortalConstants.PARENT_SCOPE_GROUP_ID));
    }

    public Map<String, String> fillMainLineState(Set<String> releaseIds, ComponentService.Iface client, User user) {
        Map<String, String> relMainLineState = new HashMap<>();
        releaseIds.stream().forEach(releaseId -> wrapTException(() -> {
            MainlineState releaseMainlineState = client.getReleaseById(releaseId, user).getMainlineState();
            relMainLineState.put(releaseId, ThriftEnumUtils.enumToString(releaseMainlineState));
        }));
        return relMainLineState;
    }

    protected void loadLinkedReleaseOfProject(ResourceRequest request, ResourceResponse response) throws TException {
        String projectId = request.getParameter("projectId");
        String[] trace = request.getParameterValues("trace[]");
        String branchId = request.getParameter(PortalConstants.NETWORK_PARENT_BRANCH_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);
        try {
            ProjectService.Iface projectClient = thriftClients.makeProjectClient();
            List<ReleaseLink> linkedReleases = projectClient.getReleaseLinksOfProjectNetWorkByTrace(projectId, Arrays.asList(trace), user);
            request.setAttribute(PortalConstants.NETWORK_PARENT_BRANCH_ID, branchId);
            request.setAttribute(PortalConstants.PARENT_SCOPE_GROUP_ID, request.getParameter(PortalConstants.PARENT_SCOPE_GROUP_ID));
            request.setAttribute(PortalConstants.NETWORK_RELEASE_LIST, linkedReleases);
            int totalInaccessibleRow = 0;
            for (ReleaseLink link : linkedReleases) {
                if (!link.isAccessible()) {
                    totalInaccessibleRow++;
                }
            }
            request.setAttribute(PortalConstants.NETWORK_TOTAL_INACCESSIBLE_ROWS, totalInaccessibleRow);
        } catch (TException e) {
            log.error("Error getting releases!", e);
            request.setAttribute(PortalConstants.NETWORK_PARENT_BRANCH_ID, branchId);
            request.setAttribute(PortalConstants.PARENT_SCOPE_GROUP_ID, request.getParameter(PortalConstants.PARENT_SCOPE_GROUP_ID));
            request.setAttribute(PortalConstants.NETWORK_RELEASE_LIST, Collections.emptyList());
            request.setAttribute(PortalConstants.NETWORK_TOTAL_INACCESSIBLE_ROWS, 0);
        }
    }

    protected List<ProjectLink> createLinkedProjectsWithAllReleases(Project project, User user) {
        return createLinkedProjectsWithAllReleases(project, Function.identity(), user);
    }

    protected List<ProjectLink> createLinkedProjectsWithAllReleases(Project project, Function<ProjectLink, ProjectLink> projectLinkMapper, User user) {
        return createLinkedProjectsWithAllReleases(project, projectLinkMapper, false, user);
    }

    protected List<ProjectLink> createLinkedProjectsWithAllReleases(Project project, Function<ProjectLink, ProjectLink> projectLinkMapper, boolean deep,
                                                     User user) {
        final Collection<ProjectLink> linkedProjects = SW360Utils.getLinkedProjectsWithAllReleasesAsFlatList(project, deep, thriftClients, log, user);
        return linkedProjects.stream().map(projectLinkMapper).collect(Collectors.toList());
    }

    protected void prepareLinkedProjectsForAttachmentUsage(ResourceRequest request) throws PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String branchId = request.getParameter(PortalConstants.NETWORK_PARENT_BRANCH_ID);
        Optional<String> projectIdOpt = getProjectIdFromBranchId(branchId);
        request.setAttribute(PortalConstants.NETWORK_PARENT_BRANCH_ID, branchId);
        final Project project;
        if (projectIdOpt.isPresent()) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                project = client.getProjectById(projectIdOpt.get(), user);
            } catch (TException e) {
                log.error("Error getting projects!", e);
                throw new PortletException("cannot load project " + projectIdOpt.get(), e);
            }
            String parentProjectPath = request.getParameter(PortalConstants.PARENT_PROJECT_PATH);
            if (parentProjectPath != null) {
                request.setAttribute(PortalConstants.PARENT_PROJECT_PATH,projectIdOpt.get());
            }
        } else {
            project = new Project();
        }
        List<ProjectLink> mappedProjectLinks = createLinkedProjectsWithAllReleases(project, user);

        mappedProjectLinks = sortProjectLink(mappedProjectLinks);
        request.setAttribute(PROJECT_LIST, mappedProjectLinks);
        request.setAttribute(PortalConstants.PARENT_SCOPE_GROUP_ID, request.getParameter(PortalConstants.PARENT_SCOPE_GROUP_ID));
    }
}
