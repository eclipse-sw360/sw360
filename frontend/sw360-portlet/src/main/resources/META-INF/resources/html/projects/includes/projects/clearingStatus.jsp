<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>

<%@include file="/html/init.jsp" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="loadLinkedProjectsRowsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_LINKED_PROJECTS_ROWS%>'/>
    <portlet:param name="<%=PortalConstants.LOAD_LINKED_RELEASES_ROWS%>" value='true'/>
</portlet:resourceURL>

<portlet:resourceURL var="clearingStatuslisturl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LIST_CLEARING_STATUS%>"/>
    <portlet:param name="<%=PortalConstants.DOCUMENT_ID%>" value="${docid}"/>
</portlet:resourceURL>

<portlet:resourceURL var="clearingStatuslistOnloadurl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.CLEARING_STATUS_ON_LOAD%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${docid}"/>
</portlet:resourceURL>

<c:set var="pageName" value="<%= request.getParameter("pagename") %>" />

<%@include file="/html/projects/includes/projects/clearingRequest.jspf" %>

<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"
             scope="request"/>

<div class="tab-content" id="pills-clearingStatusTab">
    <div class="tab-pane fade show active" id="pills-treeView" role="tabpanel" aria-labelledby="pills-tree-tab">
        <div id="clearingStatusTreeViewSpinner">
            <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
        </div>
        <table class="table table-bordered d-none" id="LinkedProjectsInfo" data-load-node-url="<%=loadLinkedProjectsRowsURL%>"
        data-portlet-namespace="<portlet:namespace/>" data-parent-branch-key="<%=PortalConstants.PARENT_BRANCH_ID%>"
        data-scope-group-id="${httpServletRequest.getAttribute('scopeGroupId')}"
        >
            <thead>
                <tr>
                    <th style="width:40%"><liferay-ui:message key="name" /></th>
                    <th style="width:7%"><liferay-ui:message key="type" /></th>
                    <th style="width:7%"><liferay-ui:message key="relation" /></th>
                    <th style="width:12%"><liferay-ui:message key="main.licenses" /></th>
                    <th style="width:7%"><liferay-ui:message key="state" /></th>
                    <th style="width:7%"><liferay-ui:message key="release.mainline.state" /></th>
                    <th style="width:7%"><liferay-ui:message key="project.mainline.state" /></th>
                    <th style="width:9%"><liferay-ui:message key="comment" /></th>
                    <th style="width:4%"><liferay-ui:message key="actions" /></th>
               </tr>
           </thead>
           <tbody id="clearingStatusTreeViewTableBody">
               <tr class="d-none" id="noRecordRow">
                    <td colspan="8" class="text-center"><liferay-ui:message key="no.linked.releases.or.projects" /></td>
               </tr>
          </tbody>
       </table>
   </div>
   <div class="tab-pane fade" id="pills-listView" role="tabpanel" aria-labelledby="pills-list-tab">
        <div id="clearingStatusSpinner">
            <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
        </div>
        <table id="clearingStatusTable" class="table table-bordered d-none"></table>
    </div>
</div>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
AUI().use('liferay-portlet-url', function () {
    var PortletURL = Liferay.PortletURL;
    require(['jquery', 'modules/ajax-treetable', 'utils/render', 'bridges/datatables'], function($, ajaxTreeTable, render, datatables) {
        var clearingStatuslisturl= '<%=clearingStatuslisturl%>';
        $.ajax({url: clearingStatuslisturl,
                type: 'GET',
                dataType: 'json'
               }).done(function(result){
            createClearingStatusTable(result);
            $("#clearingStatusSpinner").addClass("d-none");
            $("#clearingStatusTable").removeClass("d-none");
          });

        function createClearingStatusTable(clearingStatusJsonData) {
            var clearingStatusTable;
            clearingStatusTable = datatables.create('#clearingStatusTable', {
                data: clearingStatusJsonData.data,
                columns: [
                    {title: "<liferay-ui:message key="name" />", data : "name", "defaultContent": "", render: {display: detailUrl}},
                    {title: "<liferay-ui:message key="type" />", data : "type", "defaultContent": ""},
                    {title: "<liferay-ui:message key="project.path" />", data : "projectOrigin", "defaultContent": "", render: $.fn.dataTable.render.text() },
                    {title: "<liferay-ui:message key="release.path" />", data : "releaseOrigin", "defaultContent": "", render: $.fn.dataTable.render.text() },
                    {title: "<liferay-ui:message key="relation" />", data : "relation", "defaultContent": ""},
                    {title: "<liferay-ui:message key="main.licenses" />", data : "mainLicenses", "defaultContent": "", render: {display: mainLicenseUrl}},
                    {title: "<liferay-ui:message key="state" />", "data": function(row) {
                        let ps=row.projectState;
                        let cs=row.clearingState;
                        if(ps === null || ps === undefined) ps="0";
                        if(cs === null || cs === undefined) cs="0";
                        return ps + priorityOfClearingState(cs) + cs;
                    }, render: {display: renderState}, "defaultContent": ""},
                    {title: "<liferay-ui:message key="release.mainline.state" />", data : "releaseMainlineState", "defaultContent": ""},
                    {title: "<liferay-ui:message key="project.mainline.state" />", data : "projectMainlineState", "defaultContent": ""},
                    {title: "<liferay-ui:message key="comment" />",  data: "comment", "defaultContent": "", render: $.fn.dataTable.render.ellipsis},
                    {title: "<liferay-ui:message key="actions" />",  data: "id", "orderable": false, "defaultContent": "", render: {display: renderActions}, className: "two actions" }
                ],
                "columnDefs": [
                    {
                        "targets": 9,
                        "createdCell": function (td) {
                            $(td).css('max-width', '10rem');
                        }
                    }
                    ],
                "order": [[ 0, "asc" ]],
                fnDrawCallback: datatables.showPageContainer,
            language: {
                url: "<liferay-ui:message key="datatables.lang" />",
                loadingRecords: "<liferay-ui:message key="loading" />"
            }}, [0, 1, 2, 3, 4, 5, 6, 7, 8], undefined, true);
            return clearingStatusTable;
        }

        $("#clearingStatusTable").on('init.dt', function() {
            $('#pills-listView input').on('keyup change clear', function () {
                $("#clearingStatusTable").DataTable().search($(this).val(), false, true).draw();
            });
        });

        var homeUrl = themeDisplay.getURLHome().replace(/\/web\//, '/group/');

        function detailUrl(name, type, row)
        {
            let url;
            if(row.isRelease === "true"){
                url = makeReleaseViewUrl(row.id);
            }
            else {
                url = makeProjectViewUrl(row.id);
            }
            let viewUrl = $("<a></a>").attr("href",url).css("word-break","break-word").text(name);
            return viewUrl[0].outerHTML;
        }

        function mainLicenseUrl(mainLicenses, type, row)
        {
            if(mainLicenses !== null && mainLicenses !== undefined) {
                let licArr = mainLicenses.split(",");
                let licLinkArr = new Array();
                for(let i=0;i<licArr.length;i++){
                    let licLink=$("<a></a>").attr("href", makeLicenseViewUrl(licArr[i])).text(licArr[i]);
                    licLinkArr.push(licLink[0].outerHTML);
                }
                return licLinkArr.join(", ");
            }
            return "";
        }

        function renderActions(id, type, row)
        {
            let url;
            if(row.isRelease === "true"){
                url = makeReleaseUrl(id);
            }
            else {
                url = makeProjectUrl(id);
            }

            return createActions(url);
        }

        function createActions(url) {
            var $actions = $('<div>', {
                    'class': 'actions'
                }),
                $editAction = render.linkTo(
                    url,
                    "",
                    '<svg class="lexicon-icon"><title><liferay-ui:message key="edit" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>'
                );

            $actions.append($editAction);
            return $actions[0].outerHTML;
        }

        function renderState(data, type, row) {
            if(row.isRelease==="true") {
                return renderClearingStateBox(row.clearingState);
            }
            return renderProjectStateBox(row.projectState,row.clearingState)
        }

        function renderProjectStateBox(stateVal, cStateVal) {
            var $state = $('<div>', {
                'class': 'content-center'
            });
            var $psBox = $('<div>', {
                'class': 'stateBox capsuleLeft ' + getProjectStateBackgroundColour(stateVal)
            }).text('PS').attr("title", "Project state: "+ stateVal);

            var $csBox = $('<div>', {
                'class': 'stateBox capsuleRight ' + getClearingStateBackgroundColour(cStateVal)
            }).text('CS').attr("title", "Project clearing state: " + cStateVal);

            $state.append($psBox,$csBox);
            return $state[0].outerHTML;
        }

        function renderClearingStateBox(stateVal) {
            var $state = $('<div>', {
                'class': 'content-center'
            });
            var $csBox = $('<div>', {
                'class': 'stateBox capsuleLeft capsuleRight ' + getClearingStateBackgroundColour(stateVal)
            }).text('CS').attr("title", "Release clearing state: " + stateVal);

            $state.append($csBox);
            return $state[0].outerHTML;
        }

        function getProjectStateBackgroundColour(state) {
            if (state != null && state === 'Active') { // -> green
                return '<%=PortalConstants.PROJECT_STATE_ACTIVE__CSS%>';
            } else {
                return '<%=PortalConstants.PROJECT_STATE_INACTIVE__CSS%>';
            }
        }

        function getClearingStateBackgroundColour(cState) {
                switch (cState) {
                    case 'Report approved':
                    case 'Closed': // -> green
                        return '<%=PortalConstants.CLEARING_STATE_CLOSED__CSS%>';
                    case 'In Progress': // -> yellow
                    case 'Under clearing':
                        return '<%=PortalConstants.CLEARING_STATE_INPROGRESS__CSS%>';
                    case 'Open': // -> red
                    case 'New':
                        return '<%=PortalConstants.CLEARING_STATE_OPEN__CSS%>';
                    case 'Report available':  //->blue
                        return 'bg-info';
                    case 'Sent to clearing tool':  //->orange
                        return 'bg-primary';
                }
            return '<%=PortalConstants.CLEARING_STATE_UNKNOWN__CSS%>';
        }

        function priorityOfClearingState(cState) {
            switch (cState) {
                case 'Report approved':
                case 'Closed':
                    return '1';
                case 'Report available':
                    return '2';
                case 'In Progress':
                case 'Under clearing':
                    return '3';
                case 'Sent to clearing tool':
                    return '4';
                case 'Open':
                case 'New':
                    return '5';
            }
            return '6';
        }

        function makeReleaseUrl(releaseId) {
            return homeUrl + '/components/-/component/release/editRelease/' + releaseId;
        }

        function makeReleaseViewUrl(releaseId) {
            return homeUrl + '/components/-/component/release/detailRelease/' + releaseId;
        }

        function makeProjectViewUrl(projectId) {
            return homeUrl + '/projects/-/project/detail/' + projectId;
        }

        function makeLicenseViewUrl(licenseId) {
            return homeUrl + '/licenses/-/license/detail/' + licenseId;
        }

        function makeProjectUrl(projectId) {
            var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_EDIT%>')
                .setParameter('<%=PortalConstants.PROJECT_ID%>', projectId);
            return portletURL.toString();
        }

        var config = $('#LinkedProjectsInfo').data();

        function dataCallbackTreeTable(table, node) {
            var data = {};
            data[config.portletNamespace + config.parentBranchKey] = node.id;
            data[config.portletNamespace + 'parentScopeGroupId'] =  config.scopeGroupId;
            if(node.row===null||node.row===undefined||node.row.length==0)
                return data;

            $(node.row[0]).each(function(){
                let isReleaseRow=$(this).attr("data-is-release-row");
                if(isReleaseRow !== null && isReleaseRow !== undefined) {
                    data[config.portletNamespace + 'overrideToRelease'] = true;
                    return;
                }
            });
            return data;
        }

        function renderCallbackTreeTable(table, node, result) {
            var rows = $(result).filter("tr");
            $(rows).each(function(){
                $(this).find(".editAction:eq(0)").each(function(){
                    $(this).html(createActions(makeReleaseUrl($(this).data("releaseid"))));
                });

                $(this).find(".editProjectAction:eq(0)").each(function(){
                    $(this).html(createActions(makeProjectUrl($(this).data("projectid"))));
                });

                $(this).find(".projectState:eq(0)").each(function(){
                    $(this).html(renderProjectStateBox($(this).data("projectstate"), $(this).data("projectclearingstate")));
                });

                $(this).find(".releaseClearingState:eq(0)").each(function(){
                    $(this).html(renderClearingStateBox($(this).data("releaseclearingstate")));
                });
            });
            table.treetable("loadBranch", node, rows);
        }

        var clearingStatuslistOnloadurl= '<%=clearingStatuslistOnloadurl%>';
        $.ajax({url: clearingStatuslistOnloadurl, success: function(resultTreeView){
            if(resultTreeView.trim().length===0) {
                $("#noRecordRow").removeClass("d-none");
            }
            else {
                $("#noRecordRow").remove();
                $("#clearingStatusTreeViewTableBody").html(resultTreeView);

                $('#LinkedProjectsInfo').find(".editAction").each(function(){
                    $(this).html(createActions(makeReleaseUrl($(this).data("releaseid"))));
                });

                $('#LinkedProjectsInfo').find(".editProjectAction").each(function(){
                    $(this).html(createActions(makeProjectUrl($(this).data("projectid"))));
                });

                $('#LinkedProjectsInfo').find(".projectState").each(function(){
                    $(this).html(renderProjectStateBox($(this).data("projectstate"),$(this).data("projectclearingstate")));
                });

                $('#LinkedProjectsInfo').find(".releaseClearingState").each(function(){
                    $(this).html(renderClearingStateBox($(this).data("releaseclearingstate")));
                });

            }
            $("#clearingStatusTreeViewSpinner").remove();
            $("#LinkedProjectsInfo").removeClass("d-none");
            ajaxTreeTable.setup('LinkedProjectsInfo', config.loadNodeUrl, dataCallbackTreeTable, renderCallbackTreeTable);
          }});

        $('#btnExportGroup a.dropdown-item').on('click', function(event) {
            exportSpreadsheet($(event.currentTarget).data('type'));
        });

        $('#downloadLicenseInfo a.dropdown-item').on('click', function(event) {
            var type=$(event.currentTarget).data('type');
            downloadLicenseInfo(type);
        });

        $('#downloadSourceCode a.dropdown-item').on('click', function(event) {
            var type=$(event.currentTarget).data('type');
            downloadSourceCodeBundleButton(type);
        });

        function downloadLicenseInfo(type) {
            var portletURL = Liferay.PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>');
            portletURL.setParameter('<%=PortalConstants.PROJECT_ID%>', '${project.id}');
            portletURL.setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_LICENSE_INFO%>');
            portletURL.setParameter('<%=PortalConstants.PROJECT_WITH_SUBPROJECT%>', type === 'projectWithSubProject' ? 'true' : 'false');

            window.location.href = portletURL.toString();
        }

        function downloadSourceCodeBundleButton(type) {
            var portletURL = Liferay.PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>');
            portletURL.setParameter('<%=PortalConstants.PROJECT_ID%>', '${project.id}');
            portletURL.setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_SOURCE_CODE_BUNDLE%>');
            portletURL.setParameter('<%=PortalConstants.PROJECT_WITH_SUBPROJECT%>', type === 'projectWithSubProject' ? 'true' : 'false');

            window.location.href = portletURL.toString();
        }

        function exportSpreadsheet(type) {
            var portletURL = Liferay.PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RESOURCE_PHASE) %>')
                    .setParameter('<%=PortalConstants.ACTION%>', '<%=PortalConstants.EXPORT_TO_EXCEL%>');
            portletURL.setParameter('<%=Project._Fields.ID%>','${project.id}');
            portletURL.setParameter('<%=PortalConstants.EXTENDED_EXCEL_EXPORT%>', type === 'projectWithReleases' ? 'true' : 'false');

            window.location.href = portletURL.toString() + window.location.hash;
        }
    });
});
</script>
