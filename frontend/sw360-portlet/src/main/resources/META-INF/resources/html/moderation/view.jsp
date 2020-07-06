<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>


<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="deleteModerationRequestAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_MODERATION_REQUEST%>'/>
</portlet:resourceURL>
<portlet:resourceURL var="loadProjectDetailsAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_PROJECT_INFO%>'/>
</portlet:resourceURL>

<liferay-portlet:renderURL var="friendlyClearingURL" portletName="sw360_portlet_moderations">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.CLEARING_REQUEST_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>
<liferay-portlet:renderURL var="friendlyProjectURL" portletName="sw360_portlet_projects">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<jsp:useBean id="moderationRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             scope="request"/>
<jsp:useBean id="closedModerationRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             scope="request"/>
<jsp:useBean id="isUserAtLeastClearingAdmin" class="java.lang.String" scope="request" />
<jsp:useBean id="clearingRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest>"
             scope="request"/>
<jsp:useBean id="closedClearingRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest>"
             scope="request"/>
<jsp:useBean id="isClearingExpert" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="printDate" class="java.util.Date"/>
<core_rt:set var="user" value="<%=themeDisplay.getUser()%>"/>

<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
            </div>
            <div id="requestTabs" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-OpenMR'}">active</core_rt:if>" href="#tab-OpenMR" data-toggle="list" role="tab"><liferay-ui:message key="open.moderation.requests" /></a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-ClosedMR'}">active</core_rt:if>" href="#tab-ClosedMR" data-toggle="list" role="tab"><liferay-ui:message key="closed.moderation.requests" /></a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-OpenCR'}">active</core_rt:if>" href="#tab-OpenCR" data-toggle="list" role="tab"><liferay-ui:message key="open.clearing.requests" /></a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-ClosedCR'}">active</core_rt:if>" href="#tab-ClosedCR" data-toggle="list" role="tab"><liferay-ui:message key="closed.clearing.requests" /></a>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">

				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/${closedModerationRequests.size()})">
					<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/<span id="requestCounter">${closedModerationRequests.size()}</span>)
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <div class="tab-content">
                        <div id="tab-OpenMR" class="tab-pane active show">
                            <table id="moderationsTable" class="table table-bordered aligned-top">
                            <colgroup>
                                <col />
                                <col />
                                <col style="width: 25%;" />
                                <col style="width: 20%;" />
                                <col style="width: 20%;" />
                                <col style="width: 35%;" />
                                <col />
                                <col style="width: 1.7rem;" />
                            </colgroup>
                            </table>
                        </div>
                        <div id="tab-ClosedMR" class="tab-pane">
                            <table id="closedModerationsTable" class="table table-bordered"></table>
                        </div>
                        <div id="tab-OpenCR" class="tab-pane">
                            <table id="clearingRequestsTable" class="table table-bordered">
                            </table>
                        </div>
                        <div id="tab-ClosedCR" class="tab-pane">
                            <table id="closedClearingRequestsTable" class="table table-bordered">
                            </table>
                        </div>
                    </div>
                </div>
            </div>

		</div>
	</div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs"></div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
AUI().use('liferay-portlet-url', function () {
    var PortletURL = Liferay.PortletURL;
    const buColIndex = 1, projectColIndex = 2, componentColIndex = 3, maxTextLength = 22;
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'modules/validation', 'modules/listgroup', 'utils/includes/quickfilter', 'utils/render', 'bridges/jquery-ui'], function($, datatables, dialog, validation, listgroup, quickfilter, render) {
        var moderationsDataTable,
            closedModerationsDataTable,
            clearingRequestsDataTable,
            closedClearingRequestsDataTable;

        listgroup.initialize('requestTabs', $('#requestTabs').data('initial-tab') || 'tab-Open');

        moderationsDataTable = createModerationsTable("#moderationsTable", prepareModerationsData());
        closedModerationsDataTable = createModerationsTable("#closedModerationsTable", prepareClosedModerationsData());
        clearingRequestsDataTable = createClearingRequestsTable("#clearingRequestsTable", prepareClearingRequestsData());
        closedClearingRequestsDataTable = createClearingRequestsTable("#closedClearingRequestsTable", prepareClosedClearingRequestsData());

        quickfilter.addTable(moderationsDataTable);
        quickfilter.addTable(closedModerationsDataTable);

        $('.TogglerModeratorsList').on('click', toggleModeratorsList );
        $('#closedModerationsTable').on('click', 'svg.delete', function(event) {
            var data = $(event.currentTarget).data();
            deleteModerationRequest(data.moderationRequest, data.documentName);
        });

        // catch ctrl+p and print dataTable
        $(document).on('keydown', function(e){
            if(e.ctrlKey && e.which === 80){
                e.preventDefault();
                moderationsDataTable.buttons('.custom-print-button').trigger();
            }
        });

        $(document).ready(function() {
            let tab = $('#requestTabs').find('a.active').attr('href');
            changePortletToolBar(tab);
        });

        function changePortletToolBar(tab) {
            if (tab === '#tab-OpenCR' || tab === '#tab-ClosedCR') {
                let msg = '<liferay-ui:message key="clearing" /> (${clearingRequests.size()}/${closedClearingRequests.size()})';
                $('.portlet-title').attr('title', msg);
                $('.portlet-title').html(msg);
            } else {
                $('.portlet-title').attr('title', '<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/${closedModerationRequests.size()})');
                $('.portlet-title').html('<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/<span id="requestCounter">${closedModerationRequests.size()}</span>)');
            }
        }

        $('.list-group-item').on('click', function(e) {
                let tab = $(this).attr('href');
                changePortletToolBar(tab);
        });

        function prepareModerationsData() {
            var result = [];
            <core_rt:forEach items="${moderationRequests}" var="moderation">
                result.push({
                    "DT_RowId": "${moderation.id}",
                    "0": '<sw360:out value="${moderation.timestamp}"/>',
                    "1": "<sw360:DisplayEnum value="${moderation.componentType}"/>",
                    "2": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
                    "3": '<sw360:DisplayUserEmail email="${moderation.requestingUser}" bare="true"/>',
                    "4": '<sw360:out value="${moderation.requestingUserDepartment}"/>',
                    "5": '<sw360:DisplayUserEmailCollection value="${moderation.moderators}" bare="true"/>',
                    "6": "<sw360:DisplayEnum value="${moderation.moderationState}"/>",
                    "7": ''
                });
            </core_rt:forEach>
            return result;
        }

        function prepareClosedModerationsData() {
            var result = [];
            <core_rt:forEach items="${closedModerationRequests}" var="moderation">
                result.push({
                    "DT_RowId": "${moderation.id}",
                    "0": '<sw360:out value="${moderation.timestamp}"/>',
                    "1": "<sw360:DisplayEnum value="${moderation.componentType}"/>",
                    "2": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
                    "3": '<sw360:DisplayUserEmail email="${moderation.requestingUser}" bare="true"/>',
                    "4": '<sw360:out value="${moderation.requestingUserDepartment}"/>',
                    "5": '<sw360:DisplayUserEmailCollection value="${moderation.moderators}" bare="true"/>',
                    "6": "<sw360:DisplayEnum value="${moderation.moderationState}"/>",
                    <core_rt:if test="${isUserAtLeastClearingAdmin == 'Yes'}">
                        "7": '<div class="actions"><svg class="delete lexicon-icon" data-moderation-request="<sw360:out value="${moderation.id}"/>" data-document-name="<sw360:out value="${moderation.documentName}"/>"><title><liferay-ui:message key="delete" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/></svg></div>'
                    </core_rt:if>
                    <core_rt:if test="${isUserAtLeastClearingAdmin != 'Yes'}">
                        "7": '<span class="badge badge-success">READY</span>'
                    </core_rt:if>

                    });
            </core_rt:forEach>
            return result;
        }

        function createModerationsTable(tableId, tableData) {
            return datatables.create(tableId, {
                searching: true,
                data: tableData,
                columns: [
                    {title: "<liferay-ui:message key="date" />", render: {display: render.renderTimestamp}, className: 'text-nowrap' },
                    {title: "<liferay-ui:message key="type" />", className: 'text-nowrap'},
                    {title: "<liferay-ui:message key="document.name" />"},
                    {title: "<liferay-ui:message key="requesting.user" />"},
                    {title: "<liferay-ui:message key="department" />"},
                    {title: "<liferay-ui:message key="moderators" />", render: {display: renderModeratorsListExpandable}},
                    {title: "<liferay-ui:message key="state" />", className: 'text-nowrap'},
                    {title: "<liferay-ui:message key="actions" />", className: 'one action'}
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    //emptyTable: "<liferay-ui:message key="no.moderation.requests.found" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                initComplete: datatables.showPageContainer
            }, [0,1,2,3,4,5,6], [7]);
        }


        function prepareClearingRequestsData() {
            var result = [];
            <core_rt:forEach items="${clearingRequests}" var="request">
            <jsp:setProperty name="printDate" property="time" value="${request.timestamp}"/>
                result.push({
                    "DT_RowId": "${request.id}",
                    "0": "${request.id}",
                    "1": "<liferay-ui:message key="not.loaded.yet" />",
                    "2": "<liferay-ui:message key="not.loaded.yet" />",
                    "3": "<liferay-ui:message key="not.loaded.yet" />",
                    "4": "<sw360:DisplayEnum value="${request.clearingState}"/>",
                    "5": '<sw360:DisplayUserEmail email="${request.requestingUser}" />',
                    "6": '<fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd"/>',
                    "7": '<sw360:out value="${request.requestedClearingDate}"/>',
                    "8": '<sw360:out value="${request.agreedClearingDate}"/>',
                    "9": '<sw360:DisplayUserEmail email="${request.clearingTeam}" />',
                    "10": "${request.projectId}"
                });
            </core_rt:forEach>
            return result;
        }

        function prepareClosedClearingRequestsData() {
            var result = [];
            <core_rt:forEach items="${closedClearingRequests}" var="request">
            <jsp:setProperty name="printDate" property="time" value="${request.timestamp}"/>
                result.push({
                    "DT_RowId": "${request.id}",
                    "0": "${request.id}",
                    "1": "<liferay-ui:message key="not.loaded.yet" />",
                    "2": "<liferay-ui:message key="not.loaded.yet" />",
                    "3": "<liferay-ui:message key="not.loaded.yet" />",
                    "4": "<sw360:DisplayEnum value="${request.clearingState}"/>",
                    "5": '<sw360:DisplayUserEmail email="${request.requestingUser}" />',
                    "6": '<fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd"/>',
                    "7": '<sw360:out value="${request.requestedClearingDate}"/>',
                    "8": '<sw360:out value="${request.agreedClearingDate}"/>',
                    "9": '<sw360:DisplayUserEmail email="${request.clearingTeam}" />',
                    "10": "${request.projectId}"
                });
            </core_rt:forEach>
            return result;
        }

        function createClearingRequestsTable(tableId, tableData) {
            return datatables.create(tableId, {
                searching: true,
                deferRender: false, // do not change this value
                data: tableData,
                columns: [
                    {title: "<liferay-ui:message key="request.id" />", render: {display: renderClearingRequestUrl}, className: 'text-nowrap', width: "5%" },
                    {title: "<liferay-ui:message key="business.area.line" />", className: 'text-nowrap', width: "10%" },
                    {title: "<liferay-ui:message key="project" />", width: "15%" },
                    {title: "<liferay-ui:message key="components.to.be.cleared" />", width: "8%" },
                    {title: "<liferay-ui:message key="status" />", width: "10%" },
                    {title: "<liferay-ui:message key="requesting.user" />", className: 'text-nowrap', width: "10%" },
                    {title: "<liferay-ui:message key="created.on" />", className: 'text-nowrap', width: "7%" },
                    {title: "<liferay-ui:message key="preferred.clearing.date" />", width: "7%" },
                    {title: "<liferay-ui:message key="agreed.clearing.date" />", width: "7%" },
                    {title: "<liferay-ui:message key="clearing.team" />", className: 'text-nowrap', width: "15%" },
                    {title: "<liferay-ui:message key="actions" />", render: {display: renderClearingRequestAction}, className: 'one action',  width: "5%" }
                ],
                language: {
                    emptyTable: "<liferay-ui:message key='no.clearing.request.found'/>"
                },
                columnDefs: [
                    {
                        targets: [0, 3],
                        type: 'natural-nohtml'
                    },
                ],
                order: [[0, 'asc']],
                initComplete: function (oSettings) {
                    datatables.showPageContainer;
                    loadProjectDetails(tableId, tableData);
                }
            }, [0,1,2,3,4,5,6,7,8,9], [10]);
        }

        function renderClearingRequestUrl(tableData, type, row) {
            let portletURL = '<%=friendlyClearingURL%>';
            return render.linkTo(replaceFriendlyUrlParameter(portletURL.toString(), row.DT_RowId, '<%=PortalConstants.PAGENAME_DETAIL_CLEARING_REQUEST%>'), row.DT_RowId);
        }

        function renderLinkToProject(id, name) {
            if (id && name) {
                if (name.length > maxTextLength) {
                    name = name.substring(0, 20) + '...';
                }
                let requestPortletURL = '<%=friendlyProjectURL%>'.replace(/moderation/g, "projects");
                return render.linkTo(replaceFriendlyUrlParameter(requestPortletURL.toString(), id, '<%=PortalConstants.PAGENAME_DETAIL%>'), name);
            } else {
                return '<liferay-ui:message key="deleted.project" />';
            }
        }

        function renderClearingRequestAction(tableData, type, row) {
            if (row[10] && ($(row[9]).attr('href').replace('mailto:', '') === '${user.emailAddress}' || ${isClearingExpert})) {
                let portletURL = '<%=friendlyClearingURL%>';
                return render.linkTo(replaceFriendlyUrlParameter(portletURL.toString(), row.DT_RowId, '<%=PortalConstants.PAGENAME_EDIT_CLEARING_REQUEST%>'),
                        "",
                        '<div class="actions"><svg class="edit lexicon-icon"><title>Edit</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg></div>'
                        );
            } else {
                return '';
            }
        }

        // helper functions
        function replaceFriendlyUrlParameter(portletUrl, id, page) {
            return portletUrl
                .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
                .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', id);
        }

        function loadProjectDetails(tableId, tableData) {
            if (!tableData.length) {
                return;
            }
            let projectIds = [], crIds = [], $table = $(tableId), crTable = clearingRequestsDataTable;
            tableData.forEach(myFunction);

            function myFunction(value, index, array) {
                let $buCell = $(tableId).find('tr#'+value.DT_RowId).find('td:eq('+buColIndex+')'),
                    $projCell = $(tableId).find('tr#'+value.DT_RowId).find('td:eq('+projectColIndex+')'),
                    $compCell = $(tableId).find('tr#'+value.DT_RowId).find('td:eq('+componentColIndex+')');
                if (value[10]) {
                    projectIds.push(value[10]);
                    $buCell.html("<liferay-ui:message key='loading' />");
                    $projCell.html("<liferay-ui:message key='loading' />");
                    $compCell.html("<liferay-ui:message key='loading' />");
                    value[10] = "";
                } else {
                    crIds.push(value.DT_RowId);
                }
            }

            $.ajax({
                type: 'POST',
                url: '<%=loadProjectDetailsAjaxURL%>',
                cache: false,
                data: {
                    "<portlet:namespace/>projectIds": projectIds
                },
                success: function (response) {
                    function d(v) { return v == undefined ? 0 : v; }
                    let table = (tableId === '#clearingRequestsTable') ? clearingRequestsDataTable : closedClearingRequestsDataTable;
                    for (let i = 0; i < response.length; i++) {
                        let crId = response[i].crId,
                            buCell = table.cell('#'+crId, buColIndex),
                            projCell = table.cell('#'+crId, projectColIndex),
                            compCell = table.cell('#'+crId, componentColIndex),
                            projName = response[i].name;
                        buCell.data(response[i].bu);
                        projCell.data(renderLinkToProject(response[i].id, projName));
                        compCell.data(d(response[i].clearing.newRelease));
                        if (projName.length > maxTextLength) {
                            $(projCell.node()).attr("title", projName);
                        }
                    }
                    for (let i = 0; i < crIds.length; i++) {
                        let crId = crIds[i],
                            buCell = table.cell('#'+crId, buColIndex),
                            projCell = table.cell('#'+crId, projectColIndex),
                            compCell = table.cell('#'+crId, componentColIndex);
                        buCell.data("<liferay-ui:message key='not.available' />");
                        projCell.data("<liferay-ui:message key='deleted.project' />");
                        compCell.data("<liferay-ui:message key='not.available' />");
                    }
                    quickfilter.addTable(table);
                },
                error: function () {
                    for (var i = 0; i < tableData.length; i++) {
                        $table.find('tr#'+tableData[i].DT_RowId).find('td:eq('+buColIndex+')').html("<liferay-ui:message key='failed.to.load' />");
                        $table.find('tr#'+tableData[i].DT_RowId).find('td:eq('+projectColIndex+')').html("<liferay-ui:message key='failed.to.load' />");
                    }
                }
            });
        }

        function deleteModerationRequest(id, docName) {
            var $dialog;

            function deleteModerationRequestInternal(callback) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=deleteModerationRequestAjaxURL%>',
                    cache: false,
                    data: {
                        <portlet:namespace/>moderationId: id
                    },
                    success: function (data) {
                        callback();

                        if (data.result == 'SUCCESS') {
                            closedModerationsDataTable.row('#' + id).remove().draw(false);
                            $('#requestCounter').text(parseInt($('#requestCounter').text()) - 1);
                            $('#requestCounter').parent().attr('title', $('#requestCounter').parent().text());
                            $dialog.close();
                        } else {
                            $dialog.alert("<liferay-ui:message key="i.could.not.delete.the.moderation.request" />");
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert("<liferay-ui:message key="i.could.not.delete.the.moderation.request" />");
                    }
                });
            }

            $dialog = dialog.confirm(
                'danger',
                'question-circle',
                '<liferay-ui:message key="delete.moderation.request" />?',
                '<p><liferay-ui:message key="do.you.really.want.to.delete.the.moderation.request.x" /></p>',
                '<liferay-ui:message key="delete.moderation.request" />',
                {
                    name: docName,
                },
                function(submit, callback) {
                    deleteModerationRequestInternal(callback);
                }
            );
        }

        function stringToHtml(htmlText, trim) {
            if (typeof trim === 'number') {
                return htmlText = '<span title="'+htmlText+'">'+htmlText.substring(0, trim)+'...</span>';
            }
        }

            $('.datepicker').datepicker({
                minDate: new Date(),
                changeMonth: true,
                changeYear: true,
                dateFormat: "yy-mm-dd"
            });
    });

    function extractEmailFromHTMLElement(link) {
        var dummyHTML = document.createElement('div');
        dummyHTML.innerHTML = link;
        return dummyHTML.textContent;
    }

    function cutModeratorsList(moderators) {
        var firstEmail = extractEmailFromHTMLElement(moderators.split(",")[0]);
        return  firstEmail.substring(0,20) + "...";
    }

    function renderModeratorsListExpandable(moderators) {
        var $container = $('<div/>', {
                style: 'display: flex;'
            }),
            $toggler = $('<div/>', {
                'class': 'TogglerModeratorsList',
                'style': 'margin-right: 0.25rem; cursor: pointer;'
            }),
            $togglerOn = $('<div/>', {
                'class': 'Toggler_on'
            }).html('&#x25BC'),
            $togglerOff = $('<div/>', {
                'class': 'Toggler_off'
            }).html('&#x25BA'),
            $collapsed = $('<div/>', {
                'class': 'ModeratorsListHidden'
            }).text(cutModeratorsList(moderators)),
            $expanded = $('<div/>', {
                'class': 'ModeratorsListShown'
            }).html(moderators);

        $togglerOn.hide();
        $expanded.hide();
        $toggler.append($togglerOff, $togglerOn);
        $container.append($toggler, $collapsed, $expanded);
        return $container[0].outerHTML;
    }

    function toggleModeratorsList() {
        var toggler_off = $(this).find('.Toggler_off');
        var toggler_on = $(this).find('.Toggler_on');
        var parent = $(this).parent();
        var ModeratorsListHidden = parent.find('.ModeratorsListHidden');
        var ModeratorsListShown = parent.find('.ModeratorsListShown');

        toggler_off.toggle();
        toggler_on.toggle();
        ModeratorsListHidden.toggle();
        ModeratorsListShown.toggle();
    }
});
</script>
