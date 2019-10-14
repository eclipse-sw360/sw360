<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~ With modifications by Bosch Software Innovations GmbH, 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectType" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectState" %>
<%@ page import="org.eclipse.sw360.portal.common.FossologyConnectionHelper" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="projectType" class="java.lang.String" scope="request"/>
<jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
<jsp:useBean id="projectResponsible" class="java.lang.String" scope="request"/>
<jsp:useBean id="releaseClearingStateSummary" class="org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary" scope="request"/>
<jsp:useBean id="businessUnit" class="java.lang.String" scope="request"/>
<jsp:useBean id="tag" class="java.lang.String" scope="request"/>
<jsp:useBean id="name" class="java.lang.String" scope="request"/>
<jsp:useBean id="state" class="java.lang.String" scope="request"/>

<core_rt:set var="stateAutoC" value='<%=PortalConstants.STATE%>'/>
<core_rt:set var="projectTypeAutoC" value='<%=PortalConstants.PROJECT_TYPE%>'/>
<core_rt:set var="FOSSOLOGY_CONNECTION_ENABLED" value="<%=FossologyConnectionHelper.getInstance().isFossologyConnectionEnabled()%>"/>

<portlet:resourceURL var="exportProjectsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EXPORT_TO_EXCEL%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_PROJECT%>'/>
</portlet:resourceURL>

<portlet:renderURL var="impProjectURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_IMPORT%>"/>
</portlet:renderURL>

<portlet:renderURL var="addProjectURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>
</portlet:renderURL>

<portlet:renderURL var="friendlyProjectURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</portlet:renderURL>

<portlet:actionURL var="applyFiltersURL" name="applyFilters">
</portlet:actionURL>

<portlet:resourceURL var="loadClearingStateAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.GET_CLEARING_STATE_SUMMARY%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="loadProjectsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_PROJECT_LIST%>'/>
</portlet:resourceURL>

<div class="container" style="display: none;">
    <div class="row">
        <div class="col-3 sidebar">
            <div class="card-deck">
                <div id="searchInput" class="card">
                    <div class="card-header">
                        Advanced Search
                    </div>
                    <div class="card-body">
                    <form action="<%=applyFiltersURL%>" method="post">
                        <div class="form-group">
                            <label for="project_name">Project Name</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.NAME%>"
                                           value="<sw360:out value="${name}"/>" id="project_name">
                            </div>
                            <div class="form-group">
                                <label for="project_version">Project Version</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.VERSION%>"
                                       value="<sw360:out value="${version}"/>" id="project_version">
                            </div>
                            <div class="form-group">
                                <label for="project_type">Project Type</label>
                                <select class="form-control form-control-sm" id="project_type" name="<portlet:namespace/><%=Project._Fields.PROJECT_TYPE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>"></option>
                                    <sw360:DisplayEnumOptions type="<%=ProjectType.class%>" selectedName="${projectType}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="project_responsible">Project Responsible (Email)</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.PROJECT_RESPONSIBLE%>"
                                       value="<sw360:out value="${projectResponsible}"/>" id="project_responsible">
                            </div>
                            <div class="form-group">
                                <label for="group">Group</label>
                                <select class="form-control form-control-sm" id="group" name="<portlet:namespace/><%=Project._Fields.BUSINESS_UNIT%>">
                                    <option value=""
                                            <core_rt:if test="${empty businessUnit}"> selected="selected"</core_rt:if>></option>
                                    <core_rt:forEach items="${organizations}" var="org">
                                        <option value="<sw360:out value="${org.name}"/>"
                                                <core_rt:if test="${org.name == businessUnit}"> selected="selected"</core_rt:if>
                                        ><sw360:out value="${org.name}"/></option>
                                    </core_rt:forEach>
                                </select>
                            </div>
                            <div class="form-group">
                               <label for="project_state">State</label>
                                <select class="form-control form-control-sm" id="project_state" name="<portlet:namespace/><%=Project._Fields.STATE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>"></option>
                                    <sw360:DisplayEnumOptions type="<%=ProjectState.class%>" selectedName="${state}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="tag">Tag</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.TAG%>"
                                        value="<sw360:out value="${tag}"/>" id="tag">
                            </div>
                            <button type="submit" class="btn btn-primary btn-sm btn-block">Search</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-primary" onclick="window.location.href='<%=addProjectURL%>'">Add Project</button>
                        </div>
                        <div id="btnExportGroup" class="btn-group" role="group">
                            <button id="btnExport" type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                Export Spreadsheet
                                <clay:icon symbol="caret-bottom" />
                            </button>
                            <div class="dropdown-menu" aria-labelledby="btnExport">
                                <a class="dropdown-item" href="#" data-type="projectOnly">Projects only</a>
                                <a class="dropdown-item" href="#" data-type="projectWithReleases">Projects with linked releases</a>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="Projects">
                    Projects
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <table id="projectsTable" class="table table-bordered"></table>
                </div>
            </div>

        </div>
    </div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs">

    <div id="deleteProjectDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <clay:icon symbol="question-circle" />
                        Delete Project?
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p>Do you really want to delete the project <b data-name="name"></b>?</p>
                    <div data-hide="hasNoDependencies">
                        <p>
                        This project <b data-name="name"></b> contains:
                        </p>
                        <ul>
                            <li data-hide="hasNoLinkedProjects"><span data-name="linkedProjects"></span> linked projects</li>
                            <li data-hide="hasNoLinkedReleases"><span data-name="linkedReleases"></span> linked releases</li>
                            <li data-hide="hasNoAttachments"><span data-name="attachments"></span> attachments</li>
                        </ul>
                    </div>
                    <hr/>
                    <form>
                        <div class="form-group">
                            <label for="moderationDeleteCommentField">Please comment your changes</label>
                            <textarea id="moderationDeleteCommentField" class="form-control" data-name="comment" rows="4" placeholder="Comment your request..."></textarea>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-danger">Delete Project</button>
                </div>
            </div>
        </div>
    </div>
</div>


<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;
        const clearingSummaryColumnIndex = 4;

        require(['jquery', 'modules/autocomplete', 'modules/dialog', 'bridges/datatables', 'utils/render'], function($, autocomplete, dialog, datatables, render) {
            var projectsTable;

             // initializing
            autocomplete.prepareForMultipleHits('state', ${stateAutoC});
            autocomplete.prepareForMultipleHits('project_type', ${projectTypeAutoC});
            projectsTable = createProjectsTable();

            // catch ctrl+p and print dataTable
            $(document).on('keydown', function(e){
                if(e.ctrlKey && e.which === 80){
                    e.preventDefault();
                    projectsTable.buttons('.custom-print-button').trigger();
                }
            });

             // register event handlers
            $('#searchInput input, #searchInput select, #searchInput textarea').on('input', function() {
                $('#btnExport').prop('disabled', true);
                <%--when filters are actually applied, page is refreshed and exportSpreadsheetButton enabled automatically--%>
            });
            $('#projectsTable').on('click', 'svg.delete', function(event) {
                var data = $(event.currentTarget).data();
                deleteProject(data.projectId, data.projectName, data.linkedProjectsCount, data.linkedReleasesCount, data.projectAttachmentCount);
            });
            $('#btnExportGroup a.dropdown-item').on('click', function(event) {
                exportSpreadsheet($(event.currentTarget).data('type'));
            });

             // helper functions
            function makeProjectUrl(projectId, page) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                    .setParameter('<%=PortalConstants.PAGENAME%>', page)
                    .setParameter('<%=PortalConstants.PROJECT_ID%>', projectId);
                return portletURL.toString();
            }

            function replaceFriendlyUrlParameter(projectId, page) {
                var portletURL = '<%=friendlyProjectURL%>'
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', projectId);
                return portletURL;
            }

            // create and render datatable
            function createProjectsTable() {
                var projectsTable;

                projectsTable = datatables.create('#projectsTable', {
                    // the following two parameters must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
                    bServerSide: true,
                    sAjaxSource: '<%=loadProjectsURL%>',

                    columns: [
                        {title: "Project Name", data: "name", render: {display: renderProjectNameLink}},
                        {title: "Description", data: "desc", render: {display: renderDescription}},
                        {title: "Project Responsible", data: "resp", render: {display: renderProjectResponsible}},
                        {title: "State", data: "state", render: {display: renderStateBoxes} },
                        {title: "<span title=\"Release clearing state\">Clearing Status</span>", data: "clearing" },
                        {title: "Actions", data: "id", render: {display: renderProjectActions}, className: "four actions" }
                    ],
                    drawCallback: function (oSettings) {
                        loadClearingStateSummaries();
                    },
                    initComplete: datatables.showPageContainer
                }, [0, 1, 2, 3, 4], 5);

                return projectsTable;
            }

            function renderProjectActions(id, type, row) {
                var $actions = $('<div>', {
                        'class': 'actions'
                    }),
                    $editAction,
                    $copyAction = render.linkTo(
                        makeProjectUrl(id, '<%=PortalConstants.PAGENAME_DUPLICATE%>'),
                        "",
                        '<svg class="lexicon-icon" title="Duplicate"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#paste"/></svg>'
                    ),
                    $deleteAction = $('<svg>', {
                        'class': 'delete lexicon-icon',
                        title: 'Delete',
                        'data-project-id': id,
                        'data-project-name': row.name,
                        'data-linked-projects-count': row.lProjSize,
                        'data-linked-releases-count': row.lRelsSize,
                        'data-project-attachment-count': row.attsSize,
                    });

                $deleteAction.append($('<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

                if(row.cState == 'CLOSED' && ${isUserAdmin != 'Yes'}) {
                    $editAction = $('<svg class="lexicon-icon disabled"><title>Only administrators can edit a closed project.</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>');
                } else {
                    $editAction = render.linkTo(
                        makeProjectUrl(id, '<%=PortalConstants.PAGENAME_EDIT%>'),
                        "",
                        '<svg class="lexicon-icon"><title>Edit</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>'
                    );
                }

                $actions.append($editAction, $copyAction, $deleteAction);
                return $actions[0].outerHTML;
            }

            function renderProjectNameLink(name, type, row) {
                return render.linkTo(replaceFriendlyUrlParameter(row.id, '<%=PortalConstants.PAGENAME_DETAIL%>'), name);
            }

            function renderStateBoxes(state, type, row) {
                var projectStateBackgroundColour = getProjectStateBackgroundColour(state);
                var clearingStateBackgroundColour = getClearingStateBackgroundColour(row);

                var $state = $('<div>', {
                    'class': 'content-center'
                });
                var $psBox = $('<div>', {
                    'class': 'stateBox capsuleLeft ' + projectStateBackgroundColour
                }).text('PS');
                var $csBox = $('<div>', {
                    'class': 'stateBox capsuleRight ' + clearingStateBackgroundColour
                }).text('CS');

                $state.append($psBox, $csBox);
                return $state[0].outerHTML;
            }

            function getProjectStateBackgroundColour(state) {
                if (state != null && state === 'ACTIVE') { // -> green
                    return '<%=PortalConstants.PROJECT_STATE_ACTIVE__CSS%>';
                } else {
                    return '<%=PortalConstants.PROJECT_STATE_INACTIVE__CSS%>';
                }
            }

            function getClearingStateBackgroundColour(row) {
                if (row != null && row.cState != null) {
                    switch (row.cState) {
                        case 'CLOSED': // -> green
                            return '<%=PortalConstants.CLEARING_STATE_CLOSED__CSS%>';
                        case 'IN_PROGRESS': // -> yellow
                            return '<%=PortalConstants.CLEARING_STATE_INPROGRESS__CSS%>';
                        case 'OPEN': // -> red
                            return '<%=PortalConstants.CLEARING_STATE_OPEN__CSS%>';
                    }
                }
                return '<%=PortalConstants.CLEARING_STATE_UNKNOWN__CSS%>';
            }

            function renderDescription(description, type, row) {
                if (description) {
                    return render.truncate(description, 140);
                } else {
                    return "";
                }
            }

            function renderProjectResponsible(responsible, type, row) {
                if (responsible) {
                    return render.userEmail(responsible);
                } else {
                    return "";
                }
            }

            function loadClearingStateSummaries() {
                var tableData = projectsTable.data(),
                    queryPageSize = 10,
                    numberOfQueryPages = Math.ceil(tableData.length / queryPageSize),
                    queryPagesPageIds = [],
                    pageIds,
                    cell,
                    i, j, idx;

                for (i = 0; i < numberOfQueryPages; i++) {
                    pageIds = [];
                    for (j = 0; j < queryPageSize; j++) {
                        idx = (i * queryPageSize) + j;
                        /* make sure last page is handled correctly as it might not be a full page */
                        if (tableData[idx]) {
                            pageIds.push(tableData[idx].id);
                            cell = projectsTable.cell(idx, clearingSummaryColumnIndex);
                            cell.data("Loading...");
                        } else {
                            break;
                        }
                    }
                    queryPagesPageIds.push(pageIds);
                }
                postClearingStateSummaryRequest(queryPagesPageIds);
            }

            function postClearingStateSummaryRequest(idSets) {
                if (idSets.length < 1) {
                    return;
                }

                var ids = idSets.shift();
                $.ajax({
                    type: 'POST',
                    url: '<%=loadClearingStateAjaxURL%>',
                    cache: false,
                    data: {
                        "<portlet:namespace/><%=Project._Fields.ID%>": ids
                    },
                    success: function (response) {
                        for (var i = 0; i < response.length; i++) {
                            var cell_clearingsummary = projectsTable.cell("#" + response[i].id, clearingSummaryColumnIndex);
                            cell_clearingsummary.data(displayClearingStateSummary(response[i].clearing));
                        }
                    },
                    error: function () {
                        for (var i = 0; i < ids.length; i++) {
                            var cell = projectsTable.cell("#" + ids[i], clearingSummaryColumnIndex);
                            cell.data("Failed to load");
                        }
                    },
                    complete: function(xhr, status) {
                        /* even though former requests might have failed, we want to start all further ones */
                        postClearingStateSummaryRequest(idSets);
                    }
                });
            }

            function displayClearingStateSummary(clearing){
                function d(v) { return v == undefined ? 0 : v; }

                var resultElementAsString = "<span class=\"clearingstate\" title=\"Necessary data not found on server!\">Not available</span>",
                    releaseCount,
                    approvedCount;

                if (clearing) {
                    releaseCount = d(clearing.newRelease) + d(clearing.underClearing) + d(clearing.underClearingByProjectTeam) + d(clearing.reportAvailable) + d(clearing.approved);
                    approvedCount = d(clearing.approved);
                    resultElementAsString = "<span class=\"clearingstate content-center\" title=\"" + approvedCount + (approvedCount === 1 ? " release" : " releases") + " out of " + releaseCount + (approvedCount === 1 ? " has" : " have") + " approved clearing reports (including subprojects).\">" + approvedCount + "/" + releaseCount + "</span>";
                }

                return resultElementAsString;
            }

            // Export Spreadsheet action
            function exportSpreadsheet(type) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RESOURCE_PHASE) %>')
                    .setParameter('<%=PortalConstants.ACTION%>', '<%=PortalConstants.EXPORT_TO_EXCEL%>');
                portletURL.setParameter('<%=Project._Fields.NAME%>', $('#project_name').val());
                portletURL.setParameter('<%=Project._Fields.TYPE%>', $('#project_type').val());
                portletURL.setParameter('<%=Project._Fields.PROJECT_RESPONSIBLE%>', $('#project_responsible').val());
                portletURL.setParameter('<%=Project._Fields.BUSINESS_UNIT%>', $('#group').val());
                portletURL.setParameter('<%=Project._Fields.STATE%>', $('#state').val());
                portletURL.setParameter('<%=Project._Fields.TAG%>', $('#tag').val());
                portletURL.setParameter('<%=PortalConstants.EXTENDED_EXCEL_EXPORT%>', type === 'projectWithReleases' ? 'true' : 'false');

                window.location.href = portletURL.toString();
            }

            // delete action
            function deleteProject(projectId, name, linkedProjectsSize, linkedReleasesSize, attachmentsSize) {
                var $dialog;

                function deleteProjectInternal(callback) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=deleteAjaxURL%>',
                        cache: false,
                        data: {
                            "<portlet:namespace/><%=PortalConstants.PROJECT_ID%>": projectId,
                            "<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>": btoa($("#moderationDeleteCommentField").val())
                        },
                        success: function (data) {
                            callback();

                            if (data.result == 'SUCCESS') {
                                projectsTable.row('#' + projectId).remove().draw(false);
                                $dialog.close();
                            }
                            else if (data.result == 'SENT_TO_MODERATOR') {
                                $dialog.info("You may not delete the project, but a request was sent to a moderator!", true);
                            } else if (data.result == 'IN_USE') {
                                $dialog.warning("The project cannot be deleted, since it is used by another project!");
                            }
                            else {
                                $dialog.alert("I could not delete the project!");
                            }
                        },
                        error: function () {
                            callback();
                            $dialog.alert("I could not delete the project!");
                        }
                    });

                }

                $dialog = dialog.open('#deleteProjectDialog', {
                    name: name,
                    linkedProjects: linkedProjectsSize,
                    linkedReleases: linkedReleasesSize,
                    attachments: attachmentsSize,
                    hasNoDependencies: linkedProjectsSize == 0 && linkedReleasesSize == 0 && attachmentsSize == 0,
                    hasNoLinkedProjects: linkedProjectsSize == 0,
                    hasNoLinkedReleases: linkedReleasesSize == 0,
                    hasNoAttachments: attachmentsSize == 0
                }, function(submit, callback) {
                    deleteProjectInternal(callback);
                });
            }
        });
    });
</script>
