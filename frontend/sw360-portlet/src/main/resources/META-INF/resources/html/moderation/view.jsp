<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@page import="org.eclipse.sw360.datahandler.thrift.ClearingRequestState"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import ="java.util.Date" %>
<%@ page import ="java.text.SimpleDateFormat" %>

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
<jsp:useBean id="createdOn" class="java.util.Date"/>
<jsp:useBean id="modifiedOn" class="java.util.Date"/>
<jsp:useBean id="closedOn" class="java.util.Date"/>
<jsp:useBean id="babl" class="org.eclipse.sw360.portal.common.customfields.CustomField" scope="request"/>
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
            <div class="card-deck hidden" id="date-quickfilter">
                <div class="card">
                    <div class="card-header">
                        <liferay-ui:message key="advanced.filter" />
                    </div>
                <div class="card-body">
                <form>
                    <div class="form-group">
                        <label for="date_type"><liferay-ui:message key="select.date.type.and.range" />:</label>
                        <select class="form-control form-control-sm cr_filter" id="date_type">
                            <option value="" class="textlabel stackedLabel" ></option>
                            <option value="<%=ClearingRequest._Fields.TIMESTAMP%>" class="textlabel stackedLabel"><liferay-ui:message key="created.on" /></option>
                            <option value="<%=ClearingRequest._Fields.REQUESTED_CLEARING_DATE%>" class="textlabel stackedLabel"><liferay-ui:message key="preferred.clearing.date" /></option>
                            <option value="<%=ClearingRequest._Fields.AGREED_CLEARING_DATE%>" class="textlabel stackedLabel"><liferay-ui:message key="agreed.clearing.date" /></option>
                            <option value="<%=ClearingRequest._Fields.MODIFIED_ON%>" class="textlabel stackedLabel"><liferay-ui:message key="last.updated.on" /></option>
                            <option value="<%=ClearingRequest._Fields.TIMESTAMP_OF_DECISION%>" class="textlabel stackedLabel"><liferay-ui:message key="request.closed.on" /></option>
                        </select>
                    </div>
                    <div class="form-group">
                        <select class="form-control form-control-sm cr_filter" id="date_range" >
                            <option value="" class="textlabel stackedLabel" ></option>
                            <option value="0" class="textlabel stackedLabel"><liferay-ui:message key="today" /></option>
                            <option value="-30" class="textlabel stackedLabel"><liferay-ui:message key="last.30.days" /></option>
                            <option value="-7" class="textlabel stackedLabel"><liferay-ui:message key="last.7.days" /></option>
                            <option value="7" class="textlabel stackedLabel"><liferay-ui:message key="next.7.days" /></option>
                            <option value="30" class="textlabel stackedLabel"><liferay-ui:message key="next.30.days" /></option>
                        </select>
                    </div>
                    <div class="form-group" id="cr_priority_div">
                        <label for="date_type"><liferay-ui:message key="priority" />:</label>
                        <select class="form-control form-control-sm cr_filter" id="cr_priority">
                            <option value="" class="textlabel stackedLabel" ></option>
                            <option value="P0" class="textlabel stackedLabel">P0</option>
                            <option value="P1" class="textlabel stackedLabel">P1</option>
                            <option value="P2" class="textlabel stackedLabel">P2</option>
                            <option value="P3" class="textlabel stackedLabel">P3</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="date_type"><liferay-ui:message key="ba-bl.slash.group" />:</label>
                        <select class="form-control form-control-sm cr_filter" id="ba_bl">
                            <option value=""></option>
                            <core_rt:if test="${babl.fieldType == 'DROPDOWN' and babl.fieldLabel == 'BA BL'}">
                            <option value="" class="textlabel stackedLabel" disabled="disabled">---- <liferay-ui:message key="business.area.line" /> ----</option>
                            <core_rt:forEach var="opt" items="${babl.options}">
                                <option value="${opt}">${opt}</option>
                            </core_rt:forEach>
                            </core_rt:if>
                            <option value="" class="textlabel stackedLabel" disabled="disabled">---- <liferay-ui:message key="group" /> ----</option>
                            <core_rt:forEach items="${organizations}" var="org">
                                <option value="<sw360:out value="${org.name}"/>"><sw360:out value="${org.name}"/></option>
                            </core_rt:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="date_type"><liferay-ui:message key="status" />:</label>
                        <select class="form-control form-control-sm cr_filter" id="cr_status">
                            <option value="" class="textlabel stackedLabel" ></option>
                            <sw360:DisplayEnumOptions type="<%=ClearingRequestState.class%>"/>
                        </select>
                    </div>
                </form>
                </div>
                </div>
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
                        <div id="tab-OpenMR" class="tab-pane <core_rt:if test="${empty selectedTab}">active show</core_rt:if>">
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
                        <div id="tab-OpenCR" class="tab-pane <core_rt:if test="${selectedTab == 'tab-OpenCR'}">active show</core_rt:if>">
                            <table id="clearingRequestsTable" class="table table-bordered">
                            </table>
                        </div>
                        <div id="tab-ClosedCR" class="tab-pane <core_rt:if test="${selectedTab == 'tab-ClosedCR'}">active show</core_rt:if>">
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
    const buColIndex = 1, projectColIndex = 2, componentColIndex = 3, progressColIndex = 6, maxTextLength = 22;
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'modules/validation', 'modules/listgroup', 'utils/includes/quickfilter', 'utils/render', 'bridges/jquery-ui'], function($, datatables, dialog, validation, listgroup, quickfilter, render) {
        var moderationsDataTable,
            closedModerationsDataTable,
            clearingRequestsDataTable,
            closedClearingRequestsDataTable;

        listgroup.initialize('requestTabs', $('#requestTabs').data('initial-tab') || 'tab-OpenMR');

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

        // Event listener to the two range filtering inputs to redraw on input
        $('#date_type, #date_range, #cr_priority, #ba_bl, #cr_status').on('change', function(e) {
            filterChanged();
        });

        function filterChanged() {
            let $priority = $('#cr_priority'),
                $babl = $('#ba_bl'),
                $status = $('#cr_status'),
                crPriority = $priority.find(":selected").val(),
                babl = $babl.find(":selected").val(),
                crStatus = $status.find(":selected").text(),
                tab = $('#requestTabs').find('a.active').attr('href');

            if (tab === '#tab-OpenCR') {
                clearingRequestsDataTable
                .column(0).search(crPriority)
                .column(1).search(babl)
                .column(4).search(crStatus)
                .draw();
            } else if (tab === '#tab-ClosedCR') {
                closedClearingRequestsDataTable
                .column(1).search(babl)
                .column(4).search(crStatus)
                .draw();
            }

            let $dateType = $("#date_type"),
                $dateRange = $('#date_range'),
                dateType = $dateType.find(":selected").val();
                if (dateType) {
                    $dateRange.show();
                    if ( dateType === "<%=ClearingRequest._Fields.TIMESTAMP%>" ||
                            dateType === "<%=ClearingRequest._Fields.MODIFIED_ON%>" ||
                            dateType === "<%=ClearingRequest._Fields.TIMESTAMP_OF_DECISION%>" ) {
                          //iterate through each option
                        $('#date_range option').each(function() {
                            if ($(this).attr("value") > 0) {
                                $(this).hide().prop("disabled", true);
                            }
                        });
                    } else {
                        $('#date_range option').each(function() {
                            if ($(this).attr("value") > 0) {
                                $(this).show().prop("disabled", false);
                            }
                        });
                    }
                } else {
                    $dateRange.val("").hide();
                }
            $.fn.dataTable.ext.search.push(
                    function( settings, data, dataIndex ) {
                        let today = new Date(),
                            dateType = $dateType.find(":selected").val(),
                            days = $dateRange.find(":selected").val(),
                            dateRange = new Date();
                        if (dateType && days) {
                            (days >= 0) ? dateRange.setDate(dateRange.getDate() + Math.abs(days)) : dateRange.setDate(dateRange.getDate() - Math.abs(days));
                            dateRange.setHours(0,0,0,0);
                        } else {
                            return true;
                        }
                        today.setHours(0,0,0,0);
                        let filterDate = new Date("1970-01-01"); // use data for the date column
                        if (dateType === "<%=ClearingRequest._Fields.TIMESTAMP%>" && data[8] && days <= 0) {
                            filterDate = new Date( data[8] );
                        } else if (dateType === "<%=ClearingRequest._Fields.REQUESTED_CLEARING_DATE%>" && data[9]) {
                            filterDate = new Date( data[9] );
                        } else if (dateType === "<%=ClearingRequest._Fields.AGREED_CLEARING_DATE%>" && data[10]) {
                            filterDate = new Date( data[10] );
                        }  else if (dateType === "<%=ClearingRequest._Fields.MODIFIED_ON%>" && data[11] && days <= 0) {
                            filterDate = new Date( data[11] );
                        }  else if (dateType === "<%=ClearingRequest._Fields.TIMESTAMP_OF_DECISION%>" && data[12] && days <= 0) {
                            filterDate = new Date( data[12] );
                        }
                        filterDate.setHours(0,0,0,0);

                        if ( ( !dateType && !days ) || ( dateType && !days ) ||
                             ( days > 0 && filterDate >= today && filterDate <= dateRange ) ||
                             ( days < 0 && filterDate <= today && filterDate >= dateRange ) ||
                             ( days == 0 && filterDate.getTime() == today.getTime() && filterDate.getTime() == dateRange.getTime() ) )
                        {
                            return true;
                        }
                        return false;
                    }
                );
            if ($('.list-group .list-group-item.active').attr('href') === "#tab-OpenCR") {
                clearingRequestsDataTable.draw();
            } else {
                closedClearingRequestsDataTable.draw();
            }
        }

        // catch ctrl+p and print dataTable
        $(document).on('keydown', function(e){
            if(e.ctrlKey && e.which === 80){
                e.preventDefault();
                moderationsDataTable.buttons('.custom-print-button').trigger();
            }
        });

        $(document).ready(function() {
            let tab = $('#requestTabs').find('a.active').attr('href');
            $('#date_range').hide();
            changePortletToolBar(tab);
        });

        function changePortletToolBar(tab) {
            if (tab === '#tab-OpenCR' || tab === '#tab-ClosedCR') {
                let msg = '<liferay-ui:message key="clearing" /> (${clearingRequests.size()}/${closedClearingRequests.size()})';
                $('.portlet-title').attr('title', msg);
                $('.portlet-title').html(msg);
                $('#date-quickfilter').show();
                $('.cr_filter').val("");
                if (tab === '#tab-OpenCR') {
                    $("#date_type option[value="+"<%=ClearingRequest._Fields.TIMESTAMP_OF_DECISION%>"+"]").hide().attr("disabled", "");
                    $("#cr_priority_div").show();
                    $('#cr_status option').each(function() {
                        let val = $(this).attr("value");
                        if (val === "2" || val === "5") {
                            $(this).hide().prop("disabled", true);
                        } else {
                            $(this).show().prop("disabled", false);
                        }
                    });
                } else {
                    $("#date_type option[value="+"<%=ClearingRequest._Fields.TIMESTAMP_OF_DECISION%>"+"]").show().removeAttr("disabled");
                    $("#cr_priority_div").hide();
                    $('#cr_status option').each(function() {
                        let val = $(this).attr("value");
                        if (val === "" || val === "2" || val === "5") {
                            $(this).show().prop("disabled", false);
                        } else {
                            $(this).hide().prop("disabled", true);
                        }
                    });
                }
            } else {
                $('.portlet-title').attr('title', '<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/${closedModerationRequests.size()})');
                $('.portlet-title').html('<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/<span id="requestCounter">${closedModerationRequests.size()}</span>)');
                $('#date-quickfilter').hide();
            }
        }

        $('a[data-toggle="list"]').on('shown.bs.tab', function (e) {
            changePortletToolBar(e.target.hash);
            filterChanged();
        })

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
            <jsp:setProperty name="createdOn" property="time" value="${request.timestamp}"/>
            <core_rt:if test="${request.modifiedOn > 0}">
                <jsp:setProperty name="modifiedOn" property="time" value="${request.modifiedOn}"/>
            </core_rt:if>
                result.push({
                    "DT_RowId": "${request.id}",
                    "0": "${request.id}",
                    "1": "<liferay-ui:message key="not.loaded.yet" />",
                    "2": "<liferay-ui:message key="not.loaded.yet" />",
                    "3": "<liferay-ui:message key="not.loaded.yet" />",
                    "4": "<sw360:DisplayEnum value="${request.clearingState}"/>",
                    "5": '<sw360:DisplayUserEmail email="${request.requestingUser}" />',
                    "6": "<liferay-ui:message key="not.loaded.yet" />",
                    "7": '<sw360:DisplayUserEmail email="${request.clearingTeam}" />',
                    "8": '<fmt:formatDate value="${createdOn}" pattern="yyyy-MM-dd"/>',
                    "9": '<sw360:out value="${request.requestedClearingDate}"/>',
                    "10": '<sw360:out value="${request.agreedClearingDate}"/>',
                    "11": '',
                    <core_rt:if test="${request.modifiedOn > 0}">
                        "11": '<fmt:formatDate value="${modifiedOn}" pattern="yyyy-MM-dd"/>',
                    </core_rt:if>
                    "12": '',
                    "13": "${request.projectId}",
                });
            </core_rt:forEach>
            return result;
        }

        function prepareClosedClearingRequestsData() {
            var result = [];
            <core_rt:forEach items="${closedClearingRequests}" var="request">
            <jsp:setProperty name="createdOn" property="time" value="${request.timestamp}"/>
            <core_rt:if test="${request.modifiedOn > 0}">
                <jsp:setProperty name="modifiedOn" property="time" value="${request.modifiedOn}"/>
            </core_rt:if>
            <jsp:setProperty name="closedOn" property="time" value="${request.timestampOfDecision}"/>
                result.push({
                    "DT_RowId": "${request.id}",
                    "0": "${request.id}",
                    "1": "<liferay-ui:message key="not.loaded.yet" />",
                    "2": "<liferay-ui:message key="not.loaded.yet" />",
                    "3": "<liferay-ui:message key="not.loaded.yet" />",
                    "4": "<sw360:DisplayEnum value="${request.clearingState}"/>",
                    "5": '<sw360:DisplayUserEmail email="${request.requestingUser}" />',
                    "6": "<liferay-ui:message key="not.loaded.yet" />",
                    "7": '<sw360:DisplayUserEmail email="${request.clearingTeam}" />',
                    "8": '<fmt:formatDate value="${createdOn}" pattern="yyyy-MM-dd"/>',
                    "9": '<sw360:out value="${request.requestedClearingDate}"/>',
                    "10": '<sw360:out value="${request.agreedClearingDate}"/>',
                    "11": '',
                    <core_rt:if test="${request.modifiedOn > 0}">
                        "11": '<fmt:formatDate value="${modifiedOn}" pattern="yyyy-MM-dd"/>',
                    </core_rt:if>
                    "12": '<fmt:formatDate value="${closedOn}" pattern="yyyy-MM-dd"/>',
                    "13": "${request.projectId}",
                });
            </core_rt:forEach>
            return result;
        }

        function createClearingRequestsTable(tableId, tableData) {
            let hiddenCol = (tableId === '#clearingRequestsTable') ? [8, 9, 10, 11, 12] : [3, 6, 11];
            return datatables.create(tableId, {
                searching: true,
                deferRender: false, // do not change this value
                data: tableData,
                columns: [
                    {title: "<liferay-ui:message key="request.id" />", render: {display: renderClearingRequestUrl}, className: 'text-nowrap', width: "5%" },
                    {title: "<liferay-ui:message key="ba-bl.slash.group" />", className: 'text-nowrap', width: "10%" },
                    {title: "<liferay-ui:message key="project" />", width: "15%" },
                    {title: "<liferay-ui:message key="open.components" />", width: "8%" },
                    {title: "<liferay-ui:message key="status" />", width: "10%" },
                    {title: "<liferay-ui:message key="requesting.user" />", className: 'text-nowrap', width: "10%" },
                    {title: "<liferay-ui:message key="clearing.progress" />", className: 'text-nowrap', width: "21%" },
                    {title: "<liferay-ui:message key="clearing.team" />", className: 'text-nowrap', width: "15%" },
                    {title: "<liferay-ui:message key="created.on" />", className: 'text-nowrap', width: "7%" },
                    {title: "<liferay-ui:message key="preferred.clearing.date" />", width: "8%" },
                    {title: "<liferay-ui:message key="agreed.clearing.date" />", width: "7%" },
                    {title: "<liferay-ui:message key="modified.on" />", width: "7%" },
                    {title: "<liferay-ui:message key="request.closed.on" />", width: "7%" },
                    {title: "<liferay-ui:message key="actions" />", render: {display: renderClearingRequestAction}, className: 'one action',  width: "5%" },
                ],
                language: {
                    emptyTable: "<liferay-ui:message key='no.clearing.request.found'/>"
                },
                columnDefs: [
                    {
                        targets: [0],
                        type: 'natural-nohtml'
                    },
                    {
                        "targets": hiddenCol,
                        "visible": false
                    }
                ],
                order: [[0, 'asc']],
                initComplete: function (oSettings) {
                    datatables.showPageContainer;
                    loadProjectDetails(tableId, tableData);
                }
            }, [0,1,2,3,4,5,7,8,9,10,11,12], [6,13]);
        }

        function renderClearingRequestUrl(tableData, type, row) {
            let portletURL = '<%=friendlyClearingURL%>',
                rcd = row[9],
                span =  document.createElement('span'),
                isClosed = row[4].includes('sw360-tt-ClearingRequestState-CLOSED') || row[4].includes('sw360-tt-ClearingRequestState-REJECTED'),
                url = render.linkTo(replaceFriendlyUrlParameter(portletURL.toString(), row.DT_RowId, '<%=PortalConstants.PAGENAME_DETAIL_CLEARING_REQUEST%>'), "", row.DT_RowId);
            if (!isClosed) {
                span.setAttribute("class","align-top badge");
                if (validation.isValidDate(rcd, 21)) { // green --> greater than 21 days
                    $(span).html('P3').addClass('badge-info');
                } else if (validation.isValidDate(rcd, 14)) { // yellow --> greater than 14 days but less than 21 days
                    $(span).html('P2').addClass('badge-primary');
                } else if (validation.isValidDate(rcd, 7)) { // orange --> greater than 7 days but less than 14 days
                    $(span).html('P1').addClass('badge-warning');
                } else if (validation.isValidDate(rcd, 1)) { // red --> greater than today but less than 7 days
                    $(span).html('P0').addClass('badge-danger');
                } else { // red --> today
                    $(span).html('P0').addClass('badge-danger');
                }
                return url + " &nbsp; " + $(span)[0].outerHTML;
            }
            return url;
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
            let email = extractEmailFromHTMLElement(row[7]);
            if (row[13] && (email === '${user.emailAddress}' || ${isClearingExpert})) {
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
            let projectIds = [], crIds = [], $table = $(tableId), crTable = clearingRequestsDataTable,
                isOpenCrTable = tableId === '#clearingRequestsTable';
            tableData.forEach(myFunction);

            function myFunction(value, index, array) {
                let $buCell = $(tableId).find('tr#'+value.DT_RowId).find('td:eq('+buColIndex+')'),
                    $projCell = $(tableId).find('tr#'+value.DT_RowId).find('td:eq('+projectColIndex+')'),
                    $compCell = $(tableId).find('tr#'+value.DT_RowId).find('td:eq('+componentColIndex+')'),
                    $progressCell = $(tableId).find('tr#'+value.DT_RowId).find('td:eq('+progressColIndex+')');
                if (value[13]) {
                    projectIds.push(value[13]);
                    $buCell.html('<liferay-ui:message key="loading" />');
                    $projCell.html('<liferay-ui:message key="loading" />');
                    if (isOpenCrTable) {
                        $compCell.html('<liferay-ui:message key="loading" />');
                        $progressCell.html('<liferay-ui:message key="loading" />');
                    }
                    value[13] = "";
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
                    function setProgress(totalCount, approvedCount, $pBar, pCell) {
                        if (approvedCount == 0) {
                            let progressText = "(0/"+totalCount+") "+"<liferay-ui:message key="none.of.the.directly.linked.releases.are.cleared" />";
                            $pBar.find('span').text("0%").removeClass('text-dark').addClass('text-danger');
                            $(pCell.node()).attr("title", progressText);
                        } else if (approvedCount === totalCount) {
                            let progressText = "("+totalCount+"/"+totalCount+") "+"<liferay-ui:message key="all.of.the.directly.linked.releases.are.cleared" />";
                            $pBar.find('span').text("100%");
                            $pBar.attr("aria-valuenow", "100").css("width", "100%").addClass("closed");
                            $(pCell.node()).attr("title", progressText);
                        } else {
                            let progressPercentage = ((approvedCount / totalCount) * 100).toFixed(0),
                                progressText = "("+ approvedCount +"/"+totalCount+") "+"<liferay-ui:message key="directly.linked.releases.are.cleared" />";
                            $pBar.find("span").text(progressPercentage + "%");
                            $pBar.attr("aria-valuenow", progressPercentage).css("width", progressPercentage + "%").addClass("progress-bar-animated inProgress");
                            $(pCell.node()).attr("title", progressText);
                        }
                        return $pBar;
                    }

                    let table = isOpenCrTable ? clearingRequestsDataTable : closedClearingRequestsDataTable,
                        $progressBar = $('<div/>', {
                            'class': 'progress h-100 rounded-0',
                            'style': 'font-size: 100%;'
                        }),
                        $innerDiv = $('<div/>', {
                            'class': 'progress-bar progress-bar-striped',
                            'role': "progressbar",
                            'aria-valuenow': '0',
                            'aria-valuemin': '0',
                            'aria-valuemax': '100',
                            'style': 'width: 0%; overflow: visible;'
                        }),
                        $span = $('<span/>', {
                            'class': 'text-dark font-weight-bold'
                        });
                    $innerDiv.append('<span class="text-dark font-weight-bold"></span>');

                    for (let i = 0; i < response.length; i++) {
                        let crId = response[i].crId,
                            crIdCell = table.cell('#'+crId, 0),
                            buCell = table.cell('#'+crId, buColIndex),
                            projCell = table.cell('#'+crId, projectColIndex),
                            compCell = table.cell('#'+crId, componentColIndex),
                            progressCell = table.cell('#'+crId, progressColIndex),
                            projName = response[i].name,
                            clearing = response[i].clearing,
                            totalCount = d(clearing.newRelease) + d(clearing.underClearing) + d(clearing.sentToClearingTool) + d(clearing.reportAvailable) + d(clearing.approved),
                            approvedCount = d(clearing.reportAvailable) + d(clearing.approved);

                        buCell.data(response[i].bu);
                        projCell.data(renderLinkToProject(response[i].id, projName));
                        if (isOpenCrTable) {
                            crIdCell.data (crId + ' ' + $(crIdCell.node()).find('span.badge').text());
                            compCell.data(totalCount - approvedCount);
                            if (!totalCount || $(table.cell('#'+crId, 4).node()).find('span.sw360-tt-ClearingRequestState-NEW').text()) {
                                progressCell.data('<liferay-ui:message key="not.available" />');
                            } else {
                                progressCell.data($progressBar.clone().append(setProgress(totalCount, approvedCount, $innerDiv.clone(), progressCell)[0].outerHTML)[0].outerHTML);
                            }
                        }
                        if (projName.length > maxTextLength) {
                            $(projCell.node()).attr("title", projName);
                        }
                    }
                    for (let i = 0; i < crIds.length; i++) {
                        let crId = crIds[i],
                            buCell = table.cell('#'+crId, buColIndex),
                            projCell = table.cell('#'+crId, projectColIndex),
                            compCell = table.cell('#'+crId, componentColIndex),
                            progressCell = table.cell('#'+crId, progressColIndex);
                        buCell.data('<liferay-ui:message key="not.available" />');
                        projCell.data('<liferay-ui:message key="deleted.project" />');
                        if (isOpenCrTable) {
                            compCell.data('<liferay-ui:message key="not.available" />');
                            progressCell.data('<liferay-ui:message key="not.available" />');
                        }
                    }
                    quickfilter.addTable(table);
                },
                error: function () {
                    for (var i = 0; i < tableData.length; i++) {
                        $table.find('tr#'+tableData[i].DT_RowId).find('td:eq('+buColIndex+')').html('<liferay-ui:message key="failed.to.load" />');
                        $table.find('tr#'+tableData[i].DT_RowId).find('td:eq('+projectColIndex+')').html('<liferay-ui:message key="failed.to.load" />');
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
                            $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.moderation.request" />');
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.moderation.request" />');
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
    require(['jquery', 'utils/link'], function($, linkutil) {
        if (window.history.replaceState) {
            window.history.replaceState(null, document.title, linkutil.to('moderationRequest', 'list', ""));
        }
    });
});
</script>
