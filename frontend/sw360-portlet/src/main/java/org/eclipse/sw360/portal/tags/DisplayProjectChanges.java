/*
 * Copyright Siemens AG, 2016-2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.thrift.TException;
import org.apache.thrift.meta_data.FieldMetaData;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.tags.urlutils.LinkedReleaseRenderer;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.add;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.portal.tags.TagUtils.*;

/**
 * Display the fields that have changed in the project
 *
 * @author birgit.heydenreich@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class DisplayProjectChanges extends UserAwareTag {
    private Project actual;
    private Project additions;
    private Project deletions;
    private String tableClasses = "";
    private String idPrefix = "";
    private String defaultLicenseInfoHeaderText = PortalConstants.DEFAULT_LICENSE_INFO_HEADER_TEXT_FOR_DISPALY;
    private String defaultObligationsText = PortalConstants.DEFAULT_OBLIGATIONS_TEXT_FOR_DISPALY;

    public void setActual(Project actual) {
        this.actual = prepareLicenseInfoHeaderTextInProject(actual);
    }

    public void setAdditions(Project additions) {
        this.additions = prepareLicenseInfoHeaderTextInProject(additions);
    }

    public void setDeletions(Project deletions) {
        this.deletions = prepareLicenseInfoHeaderTextInProject(deletions);
    }

    public void setTableClasses(String tableClasses) {
        this.tableClasses = tableClasses;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public void setDefaultLicenseInfoHeaderText(String defaultLicenseInfoHeaderText) {
        this.defaultLicenseInfoHeaderText = defaultLicenseInfoHeaderText;
    }

    public void setDefaultObligationsText(String defaultObligationsText) {
        this.defaultObligationsText = defaultObligationsText;
    }

    public int doStartTag() throws JspException {

        JspWriter jspWriter = pageContext.getOut();

        StringBuilder display = new StringBuilder();
        String namespace = getNamespace();

        if (additions == null || deletions == null) {
            return SKIP_BODY;
        }

        try {
            for (Project._Fields field : Project._Fields.values()) {
                switch (field) {
                    //ignored Fields
                    case ID:
                    case REVISION:
                    case TYPE:
                    case CREATED_BY:
                    case CREATED_ON:
                    case PERMISSIONS:
                    case RELEASE_CLEARING_STATE_SUMMARY:
                    case DOCUMENT_STATE:
                        //Taken care of externally
                    case ATTACHMENTS:

                        //Done in extra tables
                    case LINKED_PROJECTS:
                    case RELEASE_ID_TO_USAGE:
                        break;

                    default:
                        FieldMetaData fieldMetaData = Project.metaDataMap.get(field);
                        displaySimpleFieldOrSet(display, actual, additions, deletions, field, fieldMetaData, "");
                }
            }

            String renderString = display.toString();

            if (Strings.isNullOrEmpty(renderString)) {
                renderString = "<div class=\"alert alert-info\">No changes in basic fields.</div>";
            } else {
                renderString = String.format("<table class=\"%s\" id=\"%schanges\" >", tableClasses, idPrefix)
                        + "<thead>"
                        + String.format("<tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead><tbody>",
                        FIELD_NAME, CURRENT_VAL, DELETED_VAL, SUGGESTED_VAL)
                        + renderString + "</tbody></table>";
            }

            StringBuilder linkedProjectsDisplay = new StringBuilder();
            User user = getUserFromContext("Cannot render project changes without logged in user in request");
            renderLinkedProjects(linkedProjectsDisplay, user);

            StringBuilder releaseUsageDisplay = new StringBuilder();
            renderReleaseIdToUsage(releaseUsageDisplay, user);

            jspWriter.print(renderString + linkedProjectsDisplay.toString() + releaseUsageDisplay.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private void renderLinkedProjects(StringBuilder display, User user) {
       if (ensureSomethingTodoAndNoNullLinkedProjects()) {

            Set<String> changedProjectIds = Sets.intersection(additions.getLinkedProjects().keySet(),
                                                              deletions.getLinkedProjects().keySet());
            Set<String> linkedProjectsInDb = nullToEmptyMap(actual.getLinkedProjects()).keySet();
            //keep only projects that are still in the database
            changedProjectIds = Sets.intersection(changedProjectIds, linkedProjectsInDb );

            Set<String> removedProjectIds = Sets.difference(deletions.getLinkedProjects().keySet(), changedProjectIds);
            removedProjectIds = Sets.intersection(removedProjectIds, linkedProjectsInDb);

            Set<String> addedProjectIds = Sets.difference(additions.getLinkedProjects().keySet(), changedProjectIds);

            renderProjectLinkList(display, deletions.getLinkedProjects(), removedProjectIds, "Removed Project Links", user);
            renderProjectLinkList(display, additions.getLinkedProjects(), addedProjectIds, "Added Project Links", user);
            renderProjectLinkListCompare(
                    display,
                    actual.getLinkedProjects(),
                    deletions.getLinkedProjects(),
                    additions.getLinkedProjects(),
                    changedProjectIds, user);
        }
    }

    private boolean ensureSomethingTodoAndNoNullLinkedProjects() {
        if (!deletions.isSetLinkedProjects() && !additions.isSetLinkedProjects()) {
            return false;
        }
        if(!deletions.isSetLinkedProjects()){
            deletions.setLinkedProjects(new HashMap<>());
        }
        if(!additions.isSetLinkedProjects()){
            additions.setLinkedProjects(new HashMap<>());
        }
        return true;
    }

    private void renderProjectLinkList(StringBuilder display,
                                       Map<String, ProjectRelationship> projectRelationshipMap,
                                       Set<String> projectIds,
                                       String msg,
                                       User user) {
        if (projectIds.isEmpty()) return;

        Map<String, ProjectRelationship> filteredMap = new HashMap<>();
        for(String id : projectIds){
            filteredMap.put(id, projectRelationshipMap.get(id));
        }
        StringBuilder candidate = new StringBuilder();
        try {
            ProjectService.Iface client = new ThriftClients().makeProjectClient();
            for (ProjectLink projectLink : client.getLinkedProjects(filteredMap, user)) {
                candidate.append(
                        String.format("<tr><td>%s</td><td>%s</td></tr>", projectLink.getName(), projectLink.getRelation()));
            }
        } catch (TException ignored) {
        }
        String tableContent = candidate.toString();
        if (!tableContent.isEmpty()) {

            display.append(String.format("<table class=\"%s\" id=\"%s%s\" >", tableClasses, idPrefix, msg));
            display.append(String.format("<thead><tr><th colspan=\"2\">%s</th></tr>" +
                    "<tr><th>Project Name</th><th>Project Relationship</th></tr></thead><tbody>", msg));
            display.append(tableContent);
            display.append("</tbody></table>");
        }
    }


    private void renderProjectLinkListCompare(StringBuilder display,
                                              Map<String, ProjectRelationship> oldProjectRelationshipMap,
                                              Map<String, ProjectRelationship> deleteProjectRelationshipMap,
                                              Map<String, ProjectRelationship> updateProjectRelationshipMap,
                                              Set<String> projectIds, User user) {
        if (projectIds.isEmpty()) return;

        StringBuilder candidate = new StringBuilder();
        try {
            ProjectService.Iface client = new ThriftClients().makeProjectClient();

            Map<String, ProjectRelationship> changeMap= new HashMap<>();

            for (String projectId : projectIds) {
                ProjectRelationship updateProjectRelationship = updateProjectRelationshipMap.get(projectId);
                ProjectRelationship oldProjectRelationship = oldProjectRelationshipMap.get(projectId);

                if (!updateProjectRelationship.equals(oldProjectRelationship)) {
                    changeMap.put(projectId, oldProjectRelationshipMap.get(projectId));
                }
            }
            //! This code doubling is done to reduce the database queries. I.e. one big query instead of multiple small ones
            for (ProjectLink projectLink : client.getLinkedProjects(changeMap, user)) {
                ProjectRelationship updateProjectRelationship = updateProjectRelationshipMap.get(projectLink.getId());
                ProjectRelationship deleteProjectRelationship = deleteProjectRelationshipMap.get(projectLink.getId());
                ProjectRelationship oldProjectRelationship = oldProjectRelationshipMap.get(projectLink.getId());
                candidate.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                        projectLink.getName(),
                        oldProjectRelationship,
                        deleteProjectRelationship,
                        updateProjectRelationship));
            }

        } catch (TException ignored) {
        }

        String tableContent = candidate.toString();
        if (!tableContent.isEmpty()) {
            display.append(String.format("<table class=\"%s\" id=\"%sUpdated\" >", tableClasses, idPrefix));
            display.append("<thead><tr><th colspan=\"4\">Updated Project Links</th></tr>" +
                    "<tr><th>Project Name</th>" +
                    "<th>Current Project Relationship</th>" +
                    "<th>Deleted Project Relationship</th>" +
                    "<th>Suggested Project Relationship</th></tr>" +
                    "</thead><tbody>");
            display.append(tableContent);
            display.append("</tbody></table>");
        }
    }

    private void renderReleaseIdToUsage(StringBuilder display, User user) {

       if (ensureSomethingTodoAndNoNullReleaseIdUsage()) {

           Set<String> changedReleaseIds = Sets.intersection(
                   additions.getReleaseIdToUsage().keySet(),
                   deletions.getReleaseIdToUsage().keySet());
           changedReleaseIds = Sets.intersection(
                   changedReleaseIds,
                   actual.getReleaseIdToUsage().keySet());//remove projects already deleted in database
           Set<String> removedReleaseIds = Sets.difference(
                   deletions.getReleaseIdToUsage().keySet(),
                   changedReleaseIds);
           removedReleaseIds = Sets.intersection(
                   removedReleaseIds,
                   actual.getReleaseIdToUsage().keySet());
           Set<String> addedReleaseIds = Sets.difference(
                   additions.getReleaseIdToUsage().keySet(),
                   changedReleaseIds);

           LinkedReleaseRenderer renderer = new LinkedReleaseRenderer(display, tableClasses, idPrefix, user);
           renderer.renderReleaseLinkList(display, deletions.getReleaseIdToUsage(), removedReleaseIds, "Removed Release Links");
           renderer.renderReleaseLinkList(display, additions.getReleaseIdToUsage(), addedReleaseIds, "Added Release Links");
           renderer.renderReleaseLinkListCompare(display,
                   actual.getReleaseIdToUsage(),
                   deletions.getReleaseIdToUsage(),
                   additions.getReleaseIdToUsage(), changedReleaseIds);
        }
    }

    private boolean ensureSomethingTodoAndNoNullReleaseIdUsage() {
        if (!deletions.isSetReleaseIdToUsage() && !additions.isSetReleaseIdToUsage()) {
            return false;
        }
        if(!deletions.isSetReleaseIdToUsage()){
            deletions.setReleaseIdToUsage(new HashMap<>());
        }
        if(!additions.isSetReleaseIdToUsage()){
            additions.setReleaseIdToUsage(new HashMap<>());
        }
        return true;
    }

    private Project prepareLicenseInfoHeaderTextInProject(Project project) {
        Project modifiedProject = project.deepCopy();

        String defaultTextAsHtmlForDisplay = "<span title=\"" + defaultLicenseInfoHeaderText + "\">" + PortalConstants.DEFAULT_LICENSE_INFO_HEADER_TEXT_FOR_DISPALY + "</span>";

        if(!modifiedProject.isSetLicenseInfoHeaderText()) {
            // if the project contains the default license info header text, we wrap it into an html span-element such that the default text is given as a hover text.
            // this is only done for displaying it in a three-way merge in a moderation request.
            modifiedProject.setLicenseInfoHeaderText(defaultTextAsHtmlForDisplay);
        } else {
            // for a custom text escape html properly
            modifiedProject.setLicenseInfoHeaderText(StringEscapeUtils.escapeHtml(modifiedProject.getLicenseInfoHeaderText()).replace("\n", "<br>") );
        }

        return modifiedProject;
    }

    private Project prepareObligationsTextInProject(Project project) {
        Project modifiedProject = project.deepCopy();

        String defaultTextAsHtmlForDisplay = "<span title=\"" + defaultObligationsText + "\">" + PortalConstants.DEFAULT_OBLIGATIONS_TEXT_FOR_DISPALY + "</span>";

        if(!modifiedProject.isSetObligationsText()) {
            // if the project contains the default clearing summary text, we wrap it into an html span-element such that the default text is given as a hover text.
            // this is only done for displaying it in a three-way merge in a moderation request.
            modifiedProject.setObligationsText(defaultTextAsHtmlForDisplay);
        } else {
            // for a custom text escape html properly
            modifiedProject.setObligationsText(StringEscapeUtils.escapeHtml(modifiedProject.getObligationsText()).replace("\n", "<br>") );
        }

        return modifiedProject;
    }
}
