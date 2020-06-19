<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
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
<liferay-portlet:renderURL var="friendlyClearingURL" portletName="sw360_portlet_moderations">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.CLEARING_REQUEST_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
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
                            <colgroup>
                                <col style="width: 2%;" />
                                <col style="width: 10%;" /> <!-- Project BU -->
                                <col style="width: 10%" /> <!-- Clearing Request ID -->
                                <col style="width: 10%;" /> <!-- Status -->
                                <col style="width: 12%;" /> <!-- Requested Date -->
                                <col style="width: 19%;" /> <!-- Requesting User -->
                                <col style="width: 20%;" /> <!-- Clearing Team -->
                                <col style="width: 12%;" /> <!-- Agreed Date -->
                                <col style="width: 5%;" /> <!-- Action -->
                            </colgroup>
                            </table>
                        </div>
                        <div id="tab-ClosedCR" class="tab-pane">
                            <table id="closedClearingRequestsTable" class="table table-bordered">
                            <colgroup>
                                <col style="width: 2%;" />
                                <col style="width: 10%;" /> <!-- Project BU -->
                                <col style="width: 10%;" /> <!-- Clearing Request ID -->
                                <col style="width: 10%;" /> <!-- Status -->
                                <col style="width: 12%;" /> <!-- Requested Date -->
                                <col style="width: 19%;" /> <!-- Requesting User -->
                                <col style="width: 20%;" /> <!-- Clearing Team -->
                                <col style="width: 12%;" /> <!-- Agreed Date -->
                                <col style="width: 5%;" /> <!-- Action -->
                            </colgroup>
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
        quickfilter.addTable(clearingRequestsDataTable);
        quickfilter.addTable(closedClearingRequestsDataTable);

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

        $('.list-group-item').on('click', function(e) {
                let ref = $(this).attr('href');
                if (ref === '#tab-OpenCR' || ref === '#tab-ClosedCR') {
                    let msg = '<liferay-ui:message key="clearing" /> (${clearingRequests.size()}/${closedClearingRequests.size()})';
                    $('.portlet-title').attr('title', msg);
                    $('.portlet-title').html(msg);
                } else {
                    $('.portlet-title').attr('title', '<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/${closedModerationRequests.size()})');
                    $('.portlet-title').html('<liferay-ui:message key="moderations" /> (${moderationRequests.size()}/<span id="requestCounter">${closedModerationRequests.size()}</span>)');
                }
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
                        "7": '<div class="actions"><svg class="delete lexicon-icon" data-moderation-request="<sw360:out value="${moderation.id}"/>" data-document-name="${moderation.documentName}"><title><liferay-ui:message key="delete" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/></svg></div>'
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
                    {title: "<liferay-ui:message key="date" />", render: {display: renderTimeToReadableFormat}, className: 'text-nowrap' },
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
                result.push({
                    "DT_RowId": "${request.id}",
                    "0": '',
                    "1": '<sw360:out value="${request.projectBU}"/>',
                    "2": "<sw360:DisplayClearingRequestLink clearingRequestId="${request.id}"/>",
                    "3": "<sw360:DisplayEnum value="${request.clearingState}"/>",
                    "4": '<sw360:out value="${request.requestedClearingDate}"/>',
                    "5": '<sw360:DisplayUserEmail email="${request.requestingUser}" />',
                    "6": '<sw360:DisplayUserEmail email="${request.clearingTeam}" />',
                    "7": '<sw360:out value="${request.agreedClearingDate}"/>',
                    "8": '<sw360:out value="${request.requestingUserComment}" maxChar="150" jsQuoting="true" />',
                    "9": '<sw360:out value="${request.clearingTeamComment}" maxChar="150" jsQuoting="true" />',
                    "10": '${request.projectId}'
                });
            </core_rt:forEach>
            return result;
        }

        function prepareClosedClearingRequestsData() {
            var result = [];
            <core_rt:forEach items="${closedClearingRequests}" var="request">
                result.push({
                    "DT_RowId": "${request.id}",
                    "0": '',
                    "1": '<sw360:out value="${request.projectBU}"/>',
                    "2": "<sw360:DisplayClearingRequestLink clearingRequestId="${request.id}"/>",
                    "3": "<sw360:DisplayEnum value="${request.clearingState}"/>",
                    "4": '<sw360:out value="${request.requestedClearingDate}"/>',
                    "5": '<sw360:DisplayUserEmail email="${request.requestingUser}" />',
                    "6": '<sw360:DisplayUserEmail email="${request.clearingTeam}" />',
                    "7": '<sw360:out value="${request.agreedClearingDate}"/>',
                    "8": '<sw360:out value="${request.requestingUserComment}" maxChar="150" jsQuoting="true" />',
                    "9": '<sw360:out value="${request.clearingTeamComment}" maxChar="150" jsQuoting="true" />',
                    "10": '${request.projectId}'
                });
            </core_rt:forEach>
            return result;
        }

        function createClearingRequestsTable(tableId, tableData) {
            return datatables.create(tableId, {
                searching: true,
                data: tableData,
                columns: [
                    {title: '<svg class="lexicon-icon"><title>Expand to see comments</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open"/></svg>', className: 'details-control', /* 'orderable': false, */ data: null, defaultContent: '&#x25BA'},
                    {title: "Project BU", className: 'text-nowrap'},
                    {title: "Request ID", className: 'text-nowrap' },
                    {title: "Status", className: 'text-nowrap'},
                    {title: "Requested Date", className: 'text-nowrap'},
                    {title: "Requesting User"},
                    {title: "Clearing Team"},
                    {title: "Agreed Date"},
                    {title: "Actions", render: {display: renderClearingRequestAction}, className: 'one action'}
                ],
                language: {
                    emptyTable: "<liferay-ui:message key='no.clearing.request.found'/>"
                },
                columnDefs: [
                    {
                        targets: [2],
                        type: 'natural'
                    },
                ],
                "order": [[2, 'asc']],
                initComplete: datatables.showPageContainer
            }, [1,2,3,4,5,6,7], [0,8]);
        }

        function renderClearingRequestAction(tableData, type, row) {
            if (row[10] && ($(row[6]).attr('href').replace('mailto:', '') === '${user.emailAddress}' || ${isClearingExpert})) {
                return render.linkTo(
                        makeClearingRequestUrl(row.DT_RowId, '<%=PortalConstants.PAGENAME_EDIT_CLEARING_REQUEST%>'),
                        "",
                        '<div class="actions"><svg class="edit lexicon-icon"><title>Edit</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg></div>'
                        );
            } else {
                return '';
            }
        }

        // helper functions
        function makeClearingRequestUrl(crId, page) {
            var portletURL = PortletURL.createURL('<%=PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE)%>')
                .setParameter('<%=PortalConstants.PAGENAME%>', page)
                .setParameter('<%=PortalConstants.CLEARING_REQUEST_ID%>', crId);
            return portletURL.toString();
        }

        /* Add event listener for opening and closing details as child row */
        $('#clearingRequestsTable tbody').on('click', 'td.details-control', function () {
            var tr = $(this).closest('tr');
            displayMoreInformation(tr, 'openCR');
        });

        $('#closedClearingRequestsTable tbody').on('click', 'td.details-control', function () {
            var tr = $(this).closest('tr');
            displayMoreInformation(tr, 'closedCR');
        });

        function displayMoreInformation(tr, table) {
            var row;
            if (table === 'closedCR') {
                row = closedClearingRequestsDataTable.row(tr)
            } else if (table === 'openCR') {
                row = clearingRequestsDataTable.row(tr)
            }

            if (row.child.isShown()) {
                tr.find("td:first").html('&#x25BA')
                row.child.hide();
                tr.removeClass('shown');
                row.child().removeClass('active')
            } else {
                tr.find("td:first").html('&#x25BC')
                row.child(createChildRow(row.data())).show();
                tr.addClass('shown');
                row.child().addClass('active')
            }
        }

        /*
         * Define function for child row creation, which will contain comments for a clicked table row
         */
        function createChildRow(rowData) {
            let requesterComment = rowData[8],
                approverComment = rowData[9];

            if (!requesterComment) {
                requesterComment = 'N/A';
            }

            if (!approverComment) {
                approverComment = 'N/A';
            } else if (approverComment.length > 150) {
                approverComment = stringToHtml(approverComment, 150);
            }

            return '<table cellpadding="5" cellspacing="0" border="0" style="padding-left:10px;">'+
			            '<tr>'+
			                '<td>Requesting User Comment:</td>'+
			                '<td>'+requesterComment+'</td>'+
			            '</tr>'+
			            '<tr>'+
			                '<td>Clearing Team Comment:</td>'+
			                '<td>'+approverComment+'</td>'+
			            '</tr>'+
			        '</table>';
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

    function renderTimeToReadableFormat(timeInSeconds) {
        var date = new Date(Number(timeInSeconds));
        return date.toISOString().substring(0,10);
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
