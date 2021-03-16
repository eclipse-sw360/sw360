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

<portlet:resourceURL var="licenseToSourceFileUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LICENSE_TO_SOURCE_FILE%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="addLicenseToReleaseUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.ADD_LICENSE_TO_RELEASE%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${docid}"/>
</portlet:resourceURL>

<c:set var="pageName" value="<%= request.getParameter("pagename") %>" />

<%@include file="/html/projects/includes/projects/clearingRequest.jspf" %>

<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"
             scope="request"/>

<div class="tab-content" id="pills-clearingStatusTab">
    <div class="tab-pane fade show active" id="pills-treeView" role="tabpanel" aria-labelledby="pills-tree-tab">
    <div class="btn-group mx-1" role="group">
        <button type="button" class="btn btn-outline-dark" id="addLicenseToRelease"><liferay-ui:message key="add.license.info.to.release" /></button>
    </div>
    <div class="float-right mx-2">
        <input type="search" id="search_table" class="form-control form-control-sm mb-1 float-right" placeholder="<liferay-ui:message key="search" />">
    </div>
        <div id="clearingStatusTreeViewSpinner">
            <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
        </div>
        <table class="table table-bordered mt-0 d-none" id="LinkedProjectsInfo" data-load-node-url="<%=loadLinkedProjectsRowsURL%>"
        data-portlet-namespace="<portlet:namespace/>" data-parent-branch-key="<%=PortalConstants.PARENT_BRANCH_ID%>"
        data-scope-group-id="${httpServletRequest.getAttribute('scopeGroupId')}"
        >
            <thead>
                <tr>
                    <th style="width:36%; cursor: pointer" class="sort">
                        <div class="row px-2">
                            <liferay-ui:message key="name" />
                            <svg class="lexicon-icon lexicon-icon-caret-double-l mt-1"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#caret-double-l"/></svg>
                            <core_rt:if test="${projectList.size() > 1 or (projectList.size() == 1 and not empty projectList.get(0).linkedReleases)}">
                            <div id="toggle" class="d-none">
                                (<a href="#" id="expandAll" class="text-primary"><liferay-ui:message key="expand.all" /> </a>|
                                <a href="#" id="collapseAll" class="text-primary"> <liferay-ui:message key="collapse.all" /></a>)
                            </div>
                            </core_rt:if>
                        </div>
                    </th>
                    <th style="width:6%; cursor: pointer" class="sort"><liferay-ui:message key="type" /><clay:icon symbol="caret-double-l" /></th>
                    <th style="width:7%; cursor: pointer" class="sort"><liferay-ui:message key="relation" /><clay:icon symbol="caret-double-l" /></th>
                    <th style="width:12%; cursor: pointer" class="sort"><liferay-ui:message key="main.licenses" /><clay:icon symbol="caret-double-l" /></th>
                    <th style="width:11%"><liferay-ui:message key="other.licenses" /></th>
                    <th style="width:6%">
                    <div class="dropdown d-inline text-capitalize" id="stateFilterForTT">
                        <span title="<liferay-ui:message key="release.clearing.state" /> <liferay-ui:message key="filter" />" class="dropdown-toggle float-none" data-toggle="dropdown" id="configId">
                            <liferay-ui:message key="state" /> <clay:icon symbol="select-from-list" />
                        </span>
                        <ul class="dropdown-menu" id="dropdownmenu" name="<portlet:namespace/>roles"
                            aria-labelledby="configId">
                            <li class="dropdown-header"><liferay-ui:message key="release.clearing.state" /></li>
                            <li><hr class="my-2" /></li>
                            <li>
                                <input type="checkbox" class="form-check-input ml-4" id="new" data-releaseclearingstate="New"/>
                                <label class="mb-0"><liferay-ui:message key="new" /></label>
                            </li>
                            <li>
                                <input type="checkbox" class="form-check-input ml-4" id="reportApproved" data-releaseclearingstate="Report approved"/>
                                <label class="mb-0"><liferay-ui:message key="report.approved" /></label>
                            </li>
                            <li>
                                <input type="checkbox" class="form-check-input ml-4" id="reportAvailable" data-releaseclearingstate="Report available"/>
                                <label class="mb-0"><liferay-ui:message key="report.available" /></label>
                            </li>
                            <li>
                                <input type="checkbox" class="form-check-input ml-4" id="sentToClearing" data-releaseclearingstate="Sent to clearing tool"/>
                                <label class="mb-0"><liferay-ui:message key="sent.to.clearing.tool" /></label>
                            </li>
                            <li>
                                <input type="checkbox" class="form-check-input ml-4" id="underClearing" data-releaseclearingstate="Under clearing" />
                                <label class="mb-0"><liferay-ui:message key="under.clearing" /></label>
                            </li>
                        </ul>
                    </div>
                    </th>
                    <th style="width:5%; cursor: pointer" class="sort"><liferay-ui:message key="release.mainline.state" /><clay:icon symbol="caret-double-l" /></th>
                    <th style="width:5%; cursor: pointer" class="sort"><liferay-ui:message key="project.mainline.state" /><clay:icon symbol="caret-double-l" /></th>
                    <th style="width:9%"><liferay-ui:message key="comment" /></th>
                    <th style="width:3%"><liferay-ui:message key="actions" /></th>
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
<div class="dialogs auto-dialogs"></div>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
AUI().use('liferay-portlet-url', function () {
    var PortletURL = Liferay.PortletURL;
    require(['jquery', 'modules/ajax-treetable', 'utils/render', 'bridges/datatables', 'modules/dialog', 'utils/render'], function($, ajaxTreeTable, render, datatables, dialog, render) {
        var clearingStatuslisturl= '<%=clearingStatuslisturl%>';
        var emptyMsg = '<liferay-ui:message key="no.linked.releases.or.projects" />';
        var licenseToSourceFileMap = new Map();
        $.ajax({url: clearingStatuslisturl,
                type: 'GET',
                dataType: 'json'
               }).done(function(result){
            createClearingStatusTable(result);
            $("#clearingStatusSpinner").addClass("d-none");
            $("#clearingStatusTable").removeClass("d-none");
          });

        $('#search_table').on('input', function() {
            $("div#stateFilterForTT #dropdownmenu input[type=checkbox]:checked").each(function() {
                $(this).prop('checked', false);
            });
            search_table($(this).val().trim());
        });

        $('a[href="#"]').click(function(e) {
            e.preventDefault();
            $('#LinkedProjectsInfo').treetable(e.target.id);
            return false;
        });

        function filterByClearingState() {
            let isChecked, isPresent, checkedData = [];
            $("div#stateFilterForTT #dropdownmenu input[type=checkbox]:checked").each(function() {
                let val = $(this).data().releaseclearingstate;
                isChecked = true;
                if (val) {
                    checkedData.push(val.trim().toLowerCase());
                }
            });

            if (isChecked) {
                $('#LinkedProjectsInfo tbody tr').each(function() {
                    let relState = $(this).find('td:eq(5)').data().releaseclearingstate;
                    if (relState && checkedData.includes(relState.trim().toLowerCase())) {
                        showRow(relState, $(this));
                        isPresent = true;
                    } else {
                        $(this).hide();
                    }
                });
                if (!isPresent) {
                    if (!$('#LinkedProjectsInfo tbody tr#noDataRow').length) {
                        $('#LinkedProjectsInfo tbody tr:last').after('<tr id="noDataRow"><td colspan="8"> ' + emptyMsg + '</td></tr>');
                    } else {
                        $("#noDataRow").show();
                    }
                }
            } else {
                $("#noDataRow").remove();
                $('#LinkedProjectsInfo tbody tr').show();
            }
        }

        function showRow(value, $thiz) {
            let parentId = $thiz.data().ttParentId;
            while (parentId) {
                let $parentRow = $('#LinkedProjectsInfo tbody tr[data-tt-id='+parentId+']');
                $parentRow.show();
                if (value) {
                    $parentRow.removeClass('collapsed').addClass('expanded');
                }
                parentId = $parentRow.data().ttParentId;
            }
            $thiz.show();
        }

        $("div#stateFilterForTT input:checkbox").on('change', function() {
            $('#search_table').val('');
            search_table('');
            filterByClearingState();
        });

        function search_table(value) {
            let count = 0;
            if (value === "" || value === undefined || value === null) {
                $('#LinkedProjectsInfo tbody tr').show();
                $("#noDataRow").remove();
                return;
            }
            $('#LinkedProjectsInfo tbody tr').each(function() {
                let match = false;
                $(this).find('td').each(function(index) {
                    // search for data in case of release/project clearing state
                    let stateData = $(this).data();
                    if (index === 5 &&
                            ( (stateData.releaseclearingstate && stateData.releaseclearingstate.trim().toLowerCase().indexOf(value.toLowerCase()) >= 0)
                          ||  (stateData.projectclearingstate && stateData.projectclearingstate.trim().toLowerCase().indexOf(value.toLowerCase()) >= 0)
                          ||  (stateData.projectstate && stateData.projectstate.trim().toLowerCase().indexOf(value.toLowerCase()) >= 0) )) {
                        match = true;
                        return;
                    }
                    // disable search for empty string and Action (index 8) coulmn in table
                    if (index !== 5 && index !== 9 && $(this).text().trim() && $(this).text().trim().toLowerCase().indexOf(value.toLowerCase()) >= 0) {
                        match = true;
                        return;
                    }
                });
                if (match) {
                    count++;
                    $("#noDataRow").remove();
                    showRow(value, $(this));
                } else {
                    $(this).hide();
                }
            });
            if (!count) {
                if (!$('#LinkedProjectsInfo tbody tr#noDataRow').length) {
                    $('#LinkedProjectsInfo tbody tr:last').after('<tr id="noDataRow"><td colspan="8"> ' + emptyMsg + '</td></tr>');
                } else {
                    $("#noDataRow").show();
                }
            }
        }

        $('#LinkedProjectsInfo th.sort').click(function() {
            let table = $(this).parents('table').eq(0),
                rows = table.find('tr:gt(0)').toArray().sort(comparer($(this).index()));
            this.asc = !this.asc;
            $(table.find('th.sort svg path.text-primary')).each(function() {
                $(this).removeClass('text-primary');
            });
            if (!this.asc) {
                rows = rows.reverse();
                $($(this).find('svg path.caret-double-l-bottom').get(0)).addClass('text-primary');
            } else {
                $($(this).find('svg path.caret-double-l-top').get(0)).addClass('text-primary');
            }

            for (let i = 0; i < rows.length; i++) {
                table.append(rows[i])
            }

            $("#LinkedProjectsInfo tbody tr").each(function(index,key) {
                let node = $("#LinkedProjectsInfo").treetable("node", $(this).attr('data-tt-id'));
                $("#LinkedProjectsInfo").treetable("sortBranch", node);
            });
        });

        function comparer(index) {
            return function(a, b) {
                let valA = getCellValue(a, index), valB = getCellValue(b, index);
                return $.isNumeric(valA) && $.isNumeric(valB) ? valA - valB : valA.toString().trim().localeCompare(valB.toString().trim());
            }
        }

        function getCellValue(row, index) {
            return $(row).children('td').eq(index).text();
        }

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
                    {title: "", "data": function(row) {
                        let ps=row.projectState;
                        let cs=row.clearingState;
                        if (ps === null || ps === undefined) ps="";
                        if (cs === null || cs === undefined) cs="";
                        return ps + cs;
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
                },
                initComplete: function() {
                    this.api().columns([6]).every(function() {
                        var column = this;
                        var stateFilterForLT = $("div#stateFilterForTT").clone();
                        $(stateFilterForLT).attr('id', 'stateFilterForLT');
                        var select = $(stateFilterForLT)
                            .appendTo($(column.header()))
                            .on('change', function(event) {
                                var values = $('input:checked', this).map(function(index, element) {
                                    return $.fn.dataTable.util.escapeRegex($(element).data().releaseclearingstate);
                                }).toArray().join('|');
                                column.search(values.length > 0 ? '^(' + values + ')$' : '', true, false).draw();
                            });
                        $("div#stateFilterForLT #dropdownmenu").on('click', function(e) {
                            e.stopPropagation();
                        });
                    });
                }
            }, [0, 1, 2, 3, 4, 5, 6, 7, 8], undefined, true);
            return clearingStatusTable;
        }

        $("#clearingStatusTable").on('init.dt', function() {
            $('#pills-listView input').on('keyup clear', function () {
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

                $(this).find("td.actions").each(function() {
                    renderLicenses($(this));
                });
            });
            table.treetable("loadBranch", node, rows);
        }

        function renderLicenses(thiz) {
            let licType = $(thiz).find("svg[data-tag]").length ? $($(thiz).find("svg[data-tag]")[0]).data().tag.split("-")[1] : "";
                licList = $(thiz).html().trim().split(", <br>");
            if ((licType === "ol" || licType === "ml") && licList.length > 1) {
                $(thiz).html(render.renderExpandableUrls(licList, 'License', 21));
            }
        }
        /* Add event listener for opening and closing list of licenses */
        $('#LinkedProjectsInfo tbody').on('click', 'td .TogglerLicenseList', function () {
            render.toggleExpandableList($(this), 'License');
        });

        var clearingStatuslistOnloadurl= '<%=clearingStatuslistOnloadurl%>';
        $.ajax({url: clearingStatuslistOnloadurl, success: function(resultTreeView){
            if(resultTreeView.trim().length===0) {
                $("#noRecordRow").removeClass("d-none");
            }
            else {
                $("#noRecordRow").remove();
                $("div#toggle").removeClass("d-none");
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
                $('#LinkedProjectsInfo tr').find("td.actions").each(function() {
                    renderLicenses($(this));
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

        $("table").on("click", "svg.cursor[data-tag]", function(event) {
            let releaseId = $(event.currentTarget).data().tag.split("-")[0],
                index = $(event.currentTarget).data().tag.split("-")[2],
                licenseArray = $(this).closest('td.actions').text().replace(/<liferay-ui:message key="view.file.list" />/g, '').split(","),
                licenseName = licenseArray[index].trim();
            if (index === "0" && licenseArray.length > 1) {
                let subStrIndex = licenseName.indexOf('...') + 3;
                licenseName = licenseName.substr(subStrIndex);
            }
            getLicenseToSourceFileMapping(releaseId, licenseName);
        });

        function getLicenseToSourceFileMapping(releaseId, licenseName) {
            if (licenseToSourceFileMap.has(releaseId)) {
                displayLicenseToSrcFileMapping(releaseId, licenseName, licenseToSourceFileMap.get(releaseId));
                return;
            }
            jQuery.ajax({
                type: 'GET',
                url: '<%=licenseToSourceFileUrl%>',
                cache: false,
                data: {
                    "<portlet:namespace/><%=PortalConstants.RELEASE_ID%>": releaseId
                },
                success: function (response) {
                    if (response.status == 'success') {
                        licenseToSourceFileMap.set(releaseId, response);
                        let licenseToSourceFiles = response.data;
                        displayLicenseToSrcFileMapping(releaseId, licenseName, response);
                    }
                    else {
                        dialog.warn('<liferay-ui:message key="failed.to.load.source.file.with.error" />: <b>' + response.message + '!</b>');
                    }
                },
                error: function () {
                    dialog.warn('<liferay-ui:message key="error.fetching.license.to.source.file.mapping" />! <br>' + error.statusText + ' (' + error.status + ').');
                }
            });
        }

        function displayLicenseToSrcFileMapping(releaseId, licenseName, response) {
            list = $('<ul/>');
            let relId = response.relId,
                licType = "";
                list.append('<li><liferay-ui:message key="source.file.information.not.found.in.cli"/>!</li>');
            if (relId === releaseId) {
                response.data.forEach(function (item, index) {
                    let licName = item.licName;
                    if (licenseName.toUpperCase() === licName.toUpperCase() && item.srcFiles) {
                        $(list).empty();
                        licType = item.licType;
                        let sourceFiles = item.srcFiles.split("\n");
                        sourceFiles.forEach(function (file, index) {
                            list.append('<li>' + file + '</li>');
                        });
                    }
                });
            }
            dialog.info(response.relName,
                '<liferay-ui:message key="file.name"/>: <b>' + response.attName + '</b><br><liferay-ui:message key="license.type"/>: <b>' + licType + '</b><br><liferay-ui:message key="license"/> <liferay-ui:message key="name"/>: <b>' + licenseName + '<b/><br>' + $(list)[0].outerHTML);
        }

        $("button#addLicenseToRelease").on("click", function(event) {
            list = $('<ul id="releaseList" />');
            $("#LinkedProjectsInfo tbody tr:not([id=noRecordRow],[data-tt-parent-id])").each(function() {
                list.append('<li>' + $(this).find("td:first").text().trim() + '</li>');
            });
            addLicenseToLinkedRelease(list);
        });

        function addLicenseToLinkedRelease(releases) {
            if (!$(releases).find('li').length) {
                dialog.warn('<liferay-ui:message key="no.linked.releases.yet" />')
                return;
            }
            function addLicenseToLinkedReleaseInternal(callback) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=addLicenseToReleaseUrl%>',
                    cache: false,
                    success: function (response) {
                        callback();
                        $("div.modal button:contains('<liferay-ui:message key="cancel" />')").text('<liferay-ui:message key="close" />');
                        $("div.modal button:contains('<liferay-ui:message key="add" />')").attr('disabled', true);
                        $("div.modal .modal-body p#addLicenseToReleaseInfo").remove();
                        $("div.modal .modal-body ul#releaseList").remove();
                        if (response && response.status) {
                            oneList = $('<ul/>');
                            multipleList = $('<ul/>');
                            nilList = $('<ul/>');
                            if (response.one) {
                                let oneCli = response.one.split(",");
                                oneCli.pop();
                                oneCli.forEach(function (val, index) {
                                    oneList.append('<li>' + val + '</li>');
                                });
                            }
                            if (response.mul) {
                                let mulCli = response.mul.split(",");
                                mulCli.pop();
                                mulCli.forEach(function (val, index) {
                                    multipleList.append('<li>' + val + '</li>');
                                });
                            }
                            if (response.nil) {
                                let nilCli = response.nil.split(",");
                                nilCli.pop();
                                nilCli.forEach(function (val, index) {
                                    nilList.append('<li>' + val + '</li>');
                                });
                            }
                            if($(oneList).find('li').length) {
                                $dialog.success('<liferay-ui:message key="success.please.reload.page.to.see.the.changes" />:' + $(oneList)[0].outerHTML);
                            }
                            if($(multipleList).find('li').length) {
                                $dialog.warning('<liferay-ui:message key="multiple.approved.cli.are.found.in.the.release" />: ' + $(multipleList)[0].outerHTML);
                            }
                            if($(nilList).find('li').length) {
                                $dialog.warning('<liferay-ui:message key="approved.cli.not.found.in.the.release" />:' + $(nilList)[0].outerHTML);
                            }
                            return;
                        }
                        $dialog.success('<liferay-ui:message key="success.please.reload.page.to.see.the.changes" />.');
                    },
                    error: function () {
                        callback();
                        $dialog.alert('<liferay-ui:message key="failed.to.add.licenses" />!');
                    }
                });
            }
            $dialog = dialog.confirm(
                    'info',
                    'question-circle',
                    '<liferay-ui:message key="add.license" />?',
                    '<p id="addLicenseToReleaseInfo"><liferay-ui:message key="do.you.really.want.to.add.licenses.to.all.the.directly.linked.releases" />? </p>' + $(releases)[0].outerHTML,
                    '<liferay-ui:message key="add" />',
                    undefined,
                    function(submit, callback) {
                        addLicenseToLinkedReleaseInternal(callback);
                    }
                );
        }
    });
});
</script>
