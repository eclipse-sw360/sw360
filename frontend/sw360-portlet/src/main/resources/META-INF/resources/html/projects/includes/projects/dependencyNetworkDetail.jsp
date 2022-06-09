<%--
  ~ Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
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

<portlet:resourceURL var="loadLinkedProjectToNetwork">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LOAD_LINKED_PROJECTS_TO_NETWORK%>"/>
    <portlet:param name="<%=PortalConstants.LOAD_LINKED_RELEASES_ROWS%>" value='false'/>
</portlet:resourceURL>

<portlet:resourceURL var="listNetworkClearingStatus">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LIST_NETWORK_CLEARING_STATUS%>"/>
    <portlet:param name="<%=PortalConstants.DOCUMENT_ID%>" value="${docid}"/>
</portlet:resourceURL>

<portlet:resourceURL var="dependenceNetworkOnLoadUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.DEPENDENCE_NETWORK_ON_LOAD_URL%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${docid}"/>
</portlet:resourceURL>

<portlet:resourceURL var="licenseToSourceFileUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LICENSE_TO_SOURCE_FILE%>"/>
</portlet:resourceURL>

<c:set var="pageName" value="<%= request.getParameter("pagename") %>" />

<core_rt:if test='${not isCrDisabledForProjectBU}'>
    <%@include file="/html/projects/includes/projects/clearingRequest.jspf" %>
</core_rt:if>

<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"
             scope="request"/>

<div class="tab-content" id="pills-dependencyNetwork">
    <div class="tab-pane fade show active" id="pills-network-treeView" role="tabpanel" aria-labelledby="pills-network-tree-tab">
    <div class="float-right mx-2">
        <input type="search" id="search_network_table" class="form-control form-control-sm mb-1 float-right" placeholder="<liferay-ui:message key="search" />">
    </div>
        <div id="clearingStatusTreeViewSpinner">
            <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
        </div>
        <table class="table table-bordered mt-0 d-none" id="DependencyInfo" data-load-node-url="<%=loadLinkedProjectToNetwork%>"
        data-portlet-namespace="<portlet:namespace/>" data-parent-branch-key="<%=PortalConstants.NETWORK_PARENT_BRANCH_ID%>"
        data-scope-group-id="${httpServletRequest.getAttribute('scopeGroupId')}"
        >
            <thead>
                <tr>
                    <th style="width:36%; cursor: pointer" class="sort">
                        <div class="row px-2">
                            <liferay-ui:message key="name" />
                            <svg class="lexicon-icon lexicon-icon-caret-double-l mt-1"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#caret-double-l"/></svg>
                            <div id="toggle" class="d-none">
                                (<a href="#" id="expandAll" class="text-primary"><liferay-ui:message key="expand.all" /> </a>|
                                <a href="#" id="collapseAll" class="text-primary"> <liferay-ui:message key="collapse.all" /></a>)
                            </div>
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
                                <input type="checkbox" class="form-check-input ml-4" id="scanAvailable" data-releaseclearingstate="Scan available"/>
                                <label class="mb-0"><liferay-ui:message key="scan.available" /></label>
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
   <div class="tab-pane fade" id="pills-network-listView" role="tabpanel" aria-labelledby="pills-network-list-tab">
        <div id="clearingStatusSpinner">
            <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
        </div>
        <table id="clearingNetworkTable" class="table table-bordered d-none"></table>
    </div>
</div>
<div class="dialogs">
    <div id="viewFileListInfo" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-info modal-dialog-scrollable" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <svg class="lexicon-icon">
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open"/>
                        </svg>
                        <span class="title"><span>
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">OK</button>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="dialogs">
    <div id="viewFileListWarn" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-warning modal-dialog-scrollable" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <svg class="lexicon-icon">
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#warning"/>
                        </svg>
                        Warning
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">OK</button>
                </div>
            </div>
        </div>
    </div>
</div>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%@ include file="/html/utils/includes/licenseToSrcMappingForNetwork.jspf" %>
<script>
AUI().use('liferay-portlet-url', function () {
    var PortletURL = Liferay.PortletURL;
    require(['jquery', 'modules/ajax-treetable', 'utils/render', 'bridges/datatables', 'modules/dialog'], function($, ajaxTreeTable, render, datatables, dialog) {
        var listNetworkClearingStatus= '<%=listNetworkClearingStatus%>';
        var emptyMsg = '<liferay-ui:message key="no.linked.releases.or.projects" />';

        $.ajax({url: listNetworkClearingStatus,
                type: 'GET',
                dataType: 'json'
               }).done(function(result){
            createClearingNetworkTable(result);
            $("#pills-network-listView #clearingStatusSpinner").addClass("d-none");
            $("#clearingNetworkTable").removeClass("d-none");
          });

        $('#search_network_table').on('input', function() {
            $("#DependencyInfo div#stateFilterForTT #dropdownmenu input[type=checkbox]:checked").each(function() {
                $(this).prop('checked', false);
            });
            search_network_table($(this).val().trim());
        });

        $('a[href="#"]').click(function(e) {
            e.preventDefault();
            $('#DependencyInfo').treetable(e.target.id);
            return false;
        });

        function filterByClearingState() {
            let isChecked, isPresent, checkedData = [];
            $("#DependencyInfo div#stateFilterForTT #dropdownmenu input[type=checkbox]:checked").each(function() {
                let val = $(this).data().releaseclearingstate;
                isChecked = true;
                if (val) {
                    checkedData.push(val.trim().toLowerCase());
                }
            });

            if (isChecked) {
                $('#DependencyInfo tbody tr').each(function() {
                    let relState = $(this).find('td:eq(5)').data().releaseclearingstate;
                    if (relState && checkedData.includes(relState.trim().toLowerCase())) {
                        showDependenceRow(relState, $(this));
                        isPresent = true;
                    } else {
                        $(this).hide();
                    }
                });
                if (!isPresent) {
                    if (!$('#DependencyInfo tbody tr#noDataRow').length) {
                        $('#DependencyInfo tbody tr:last').after('<tr id="noDataRow"><td colspan="8"> ' + emptyMsg + '</td></tr>');
                    } else {
                        $("#DependencyInfo #noDataRow").show();
                    }
                }
            } else {
                $("#DependencyInfo #noDataRow").remove();
                $('#DependencyInfo tbody tr').show();
            }
        }

        function showDependenceRow(value, $thiz) {
            let parentId = $thiz.data().ttParentId;
            while (parentId) {
                let $parentRow = $('#DependencyInfo tbody tr[data-tt-id='+parentId+']');
                $parentRow.show();
                if (value) {
                    $parentRow.removeClass('collapsed').addClass('expanded');
                }
                parentId = $parentRow.data().ttParentId;
            }
            $thiz.show();
        }

        $("#DependencyInfo div#stateFilterForTT input:checkbox").on('change', function() {
            $('#search_network_table').val('');
            search_network_table('');
            filterByClearingState();
        });

        function search_network_table(value) {
            let count = 0;
            if (value === "" || value === undefined || value === null) {
                $('#DependencyInfo tbody tr').show();
                $("#DependencyInfo #noDataRow").remove();
                return;
            }
            $('#DependencyInfo tbody tr').each(function() {
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
                    $("#DependencyInfo #noDataRow").remove();
                    showDependenceRow(value, $(this));
                } else {
                    $(this).hide();
                }
            });
            if (!count) {
                if (!$('#DependencyInfo tbody tr#noDataRow').length) {
                    $('#DependencyInfo tbody tr:last').after('<tr id="noDataRow"><td colspan="8"> ' + emptyMsg + '</td></tr>');
                } else {
                    $("#DependencyInfo #noDataRow").show();
                }
            }
        }

        $('#DependencyInfo th.sort').click(function() {
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

            $("#DependencyInfo tbody tr").each(function(index,key) {
                let node = $("#DependencyInfo").treetable("node", $(this).attr('data-tt-id'));
                $("#DependencyInfo").treetable("sortBranch", node);
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

        function createClearingNetworkTable(clearingStatusJsonData) {
            var clearingNetworkTable;
            clearingNetworkTable = datatables.create('#clearingNetworkTable', {
                data: clearingStatusJsonData.data.map(function(row){
                	if(row.isAccessible === "false"){
                		row["name"]="<liferay-ui:message key="inaccessible.release" />";
                	}
                	return row;
                }),
                columns: [
                    {title: "<liferay-ui:message key="name" />", data : "name", "defaultContent": "", render: {display: detailUrl} },
                    {title: "<liferay-ui:message key="type" />", data : "type", "defaultContent": ""},
                    {title: "<liferay-ui:message key="project.path" />", data : "projectOrigin", "defaultContent": "", render: $.fn.dataTable.render.text() },
                    {title: "<liferay-ui:message key="release.path" />", data : "releaseOrigin", "defaultContent": "", render: $.fn.dataTable.render.text() },
                    {title: "<liferay-ui:message key="relation" />", data : "relation", "defaultContent": ""},
                    {title: "<liferay-ui:message key="main.licenses" />", data : "mainLicenses", "defaultContent": "", render: {display: mainLicenseUrl}},
                    {title: "", "data": function(row) {
                    	if(row.isAccessible === "false"){
                    		return "";
                    	}
                        let ps=row.projectState;
                        let cs=row.clearingState;
                        if (ps === null || ps === undefined) ps="";
                        if (cs === null || cs === undefined) cs="";
                        return ps + cs;
                    }, render: {display: renderState}, "defaultContent": ""},
                    {title: "<liferay-ui:message key="release.mainline.state" />", data : "releaseMainlineState", "defaultContent": ""},
                    {title: "<liferay-ui:message key="project.mainline.state" />", data : "projectMainlineState", "defaultContent": ""},
                    {title: "<liferay-ui:message key="comment" />",  data: "comment", "defaultContent": "", render: $.fn.dataTable.render.ellipsis},
                    {title: "<liferay-ui:message key="actions" />",  data: "releaseId", "orderable": false, "defaultContent": "", render: {display: renderActions}, className: "two actions" }
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
                        var stateFilterForLT = $("#DependencyInfo div#stateFilterForTT").clone();
                        $(stateFilterForLT).attr('id', 'stateFilterForLT');
                        var select = $(stateFilterForLT)
                            .appendTo($(column.header()))
                            .on('change', function(event) {
                                var values = $('input:checked', this).map(function(index, element) {
                                    return $.fn.dataTable.util.escapeRegex($(element).data().releaseclearingstate);
                                }).toArray().join('|');
                                column.search(values.length > 0 ? '^(' + values + ')$' : '', true, false).draw();
                            });
                        $("#DependencyInfo div#stateFilterForLT #dropdownmenu").on('click', function(e) {
                            e.stopPropagation();
                        });
                    });
                }
            }, [0, 1, 2, 3, 4, 5, 6, 7, 8], undefined, true);
            return clearingNetworkTable;
        }

        $("#clearingNetworkTable").on('init.dt', function() {
            $('#pills-listView input').on('keyup clear', function () {
                $("#clearingNetworkTable").DataTable().search($(this).val(), false, true).draw();
            });
        });

        var homeUrl = themeDisplay.getURLHome().replace(/\/web\//, '/group/');

        function detailUrl(name, type, row)
        {
            if(row.isAccessible === "true"){
            let url;
            if(row.isRelease === "true"){
                url = makeReleaseViewUrl(row.releaseId);
            }
            else {
                url = makeProjectViewUrl(row.id);
            }
            let viewUrl = $("<a></a>").attr("href",url).css("word-break","break-word").text(name),
                $infoIcon = '';
            if (row.clearingState === 'Scan available') {
                $infoIcon = "<span class='actions'><svg class='cursor lexicon-icon m-2 isr' data-doc-id="+ row.id +"> <title><liferay-ui:message key='view.scanner.findings.license'/></title> <use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle'/></svg></span>";
            }
            return viewUrl[0].outerHTML + $infoIcon;
            } else {
                return "<liferay-ui:message key="inaccessible.release" />";
            }
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
            if(row.isAccessible === "true"){
            let url;
            if(row.isRelease === "true"){
                url = makeReleaseUrl(id);
            }
            else {
                url = makeProjectUrl(id);
            }

            return createActions(url);

            } else {
                return "";
            }
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
                return renderClearingStateBox(row.clearingState, row.id);
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

        function renderClearingStateBox(stateVal, docId) {
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
                    case 'Scan available':
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

        var config = $('#DependencyInfo').data();

        function dataCallbackNetworkTreeTable(table, node) {
            var data = {};
            data[config.portletNamespace + config.parentBranchKey] = node.id;
            data[config.portletNamespace + 'parentScopeGroupId'] =  config.scopeGroupId;
            if(node.row===null||node.row===undefined||node.row.length==0)
                return data;

            $(node.row[0]).each(function(){
                let isReleaseRow=$(this).attr("data-is-release-row");
                if(isReleaseRow !== null && isReleaseRow !== undefined) {
                    data[config.portletNamespace + 'overrideToRelease'] = true;
                    data[config.portletNamespace + 'projectId'] = $(this).attr("data-project-id");
                    let releaseLayer = $(this).attr("data-layer");
                    let parentId = $(this).attr("data-parent-release");
                    let currentIndex = $(this).attr("data-index");
                    let trace = [];
                    trace.unshift(currentIndex);
                    if (releaseLayer > 0) {
                        for (let layer = releaseLayer - 1; layer >= 0; layer--) {
                            trace.unshift($(this).prevAll('tr[data-layer="'+layer+'"][data-release-id="'+parentId+'"]').first().attr('data-index'));
                            parentId = $(this).prevAll('tr[data-layer="'+layer+'"][data-release-id="'+parentId+'"]').first().attr('data-parent-release');
                        }
                    }
                    data[config.portletNamespace + 'trace[]'] = trace;
                    return;
                }
            });
            return data;
        }

        function renderCallbackNetworkTreeTable(table, node, result) {
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
                    $(this).html(renderClearingStateBox($(this).data("releaseclearingstate"), $(this).data("releaseid")));
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
        $('#DependencyInfo tbody').on('click', 'td .TogglerLicenseList', function () {
            render.toggleExpandableList($(this), 'License');
        });

        var dependenceNetworkOnLoadUrl= '<%=dependenceNetworkOnLoadUrl%>';
        $.ajax({url: dependenceNetworkOnLoadUrl, success: function(resultTreeView){
            if(resultTreeView.trim().length===0) {
                $("#DependencyInfo #noRecordRow").removeClass("d-none");
            }
            else {
                $("#DependencyInfo #noRecordRow").remove();
                $("#DependencyInfo div#toggle").removeClass("d-none");
                $("#DependencyInfo tbody").html(resultTreeView);

                $('#DependencyInfo').find(".editAction").each(function(){
                    $(this).html(createActions(makeReleaseUrl($(this).data("releaseid"))));
                });

                $('#DependencyInfo').find(".editProjectAction").each(function(){
                    $(this).html(createActions(makeProjectUrl($(this).data("projectid"))));
                });

                $('#DependencyInfo').find(".projectState").each(function(){
                    $(this).html(renderProjectStateBox($(this).data("projectstate"),$(this).data("projectclearingstate")));
                });

                $('#DependencyInfo').find(".releaseClearingState").each(function(){
                    $(this).html(renderClearingStateBox($(this).data("releaseclearingstate"), $(this).data("releaseid")));
                });
                $('#DependencyInfo tr').find("td.actions").each(function() {
                    renderLicenses($(this));
                });
            }
            $("#clearingStatusTreeViewSpinner").remove();
            $("#DependencyInfo").removeClass("d-none");
            ajaxTreeTable.setup('DependencyInfo', config.loadNodeUrl, dataCallbackNetworkTreeTable, renderCallbackNetworkTreeTable);
          }});
    });
});
</script>
