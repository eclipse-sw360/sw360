<%--
  ~ Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="deleteModerationRequestAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_MODERATION_REQUEST%>'/>
</portlet:resourceURL>

<jsp:useBean id="moderationRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             scope="request"/>
<jsp:useBean id="closedModerationRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             scope="request"/>
<jsp:useBean id="isUserAtLeastClearingAdmin" class="java.lang.String" scope="request" />


<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/datatables.net-buttons-dt/1.1.2/css/buttons.dataTables.min.css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader">
    <span class="pageHeaderBigSpan">Moderations</span> <span class="pageHeaderSmallSpan" title="Count of open/closed moderation requests">(${moderationRequests.size()}/${closedModerationRequests.size()})</span>
</p>

<div id="content">
    <div class="container-fluid">
        <div id="myTab" class="row-fluid">
            <ul class="nav nav-tabs span2">
                <div id="searchInput">
                    <%@ include file="/html/utils/includes/quickfilter.jspf" %>
                </div>
                <br/>
                <li class="active"><a href="#tab-Open">Open</a></li>
                <li><a href="#tab-Closed">Closed</a></li>
            </ul>
            <div class="tab-content span10">
                <div id="tab-Open" class="tab-pane">
                    <table id="moderationsTable" cellpadding="0" cellspacing="0" border="0" class="display" style="width:100%">
                        <colgroup>
                            <col style="width: 5%;" />
                            <col style="width: 10%;" />
                            <col style="width: 20%;" />
                            <col style="width: 15%;" />
                            <col style="width: 15%;" />
                            <col style="width: 30%;" />
                            <col style="width: 5%;" />
                        </colgroup>
                        <tfoot>
                        <tr>
                            <th colspan="7"></th>
                        </tr>
                        </tfoot>
                    </table>
                </div>
                <div id="tab-Closed">
                    <table id="closedModerationsTable" cellpadding="0" cellspacing="0" border="0" class="display" style="width:100%">
                        <colgroup>
                            <col style="width: 5%;" />
                            <col style="width: 10%;" />
                            <col style="width: 20%;" />
                            <col style="width: 15%;" />
                            <col style="width: 15%;" />
                            <col style="width: 30%;" />
                            <col style="width: 5%;" />
                        </colgroup>
                        <tfoot>
                        <tr>
                            <th colspan="7"></th>
                        </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    YUI().use('aui-tabview', function (Y) {
           new Y.TabView({
            srcNode: '#myTab',
            stacked: true,
            type: 'tab'
        }).render();
    });

    require(['jquery', 'utils/includes/quickfilter', 'modules/confirm', /* jquery-plugins: */ 'datatables', 'datatables_buttons', 'buttons.print', 'jquery-confirm'], function($, quickfilter, confirm) {
        var moderationsDataTable,
            closedModerationsDataTable;

        Liferay.on('allPortletsReady', function() {
            moderationsDataTable = createModerationsTable("#moderationsTable", prepareModerationsData());
            closedModerationsDataTable = createModerationsTable("#closedModerationsTable", prepareClosedModerationsData());

            quickfilter.addTable(moderationsDataTable);
            quickfilter.addTable(closedModerationsDataTable);

            $('.TogglerModeratorsList').on('click', toggleModeratorsList );
        });

        $('#closedModerationsTable').on('click', 'img.delete', function(event) {
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


    function useSearch(searchFieldId) {
        var searchText = $('#'+searchFieldId).val();
        moderationsDataTable.search(searchText).draw();
        closedModerationsDataTable.search(searchText).draw();
    }

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
                "7": "<img class='delete' src='<%=request.getContextPath()%>/images/Trash.png' onclick=\"deleteModerationRequest('<sw360:out value="${moderation.id}"/>','<b><sw360:out value="${moderation.documentName}"/></b>')\"  alt='Delete' title='Delete'>"
                </core_rt:if>
                <core_rt:if test="${isUserAtLeastClearingAdmin != 'Yes'}">
                "7": "READY"
                </core_rt:if>

                });
        </core_rt:forEach>
        return result;

    }

    function createModerationsTable(tableId, tableData) {
        var tbl = $(tableId).DataTable({
            pagingType: "simple_numbers",
            dom: "lBrtip",
            buttons: [
                {
                    extend: 'print',
                    text: 'Print',
                    autoPrint: true,
                    className: 'custom-print-button',
                    exportOptions: {
                        columns: [0,1,2,3,4,5,6]
                    }
                }
            ],
            data: tableData,
            autowidth: false,
            columns: [
                {title: "Date", render: {display: renderTimeToReadableFormat}},
                {title: "Type"},
                {title: "Document Name"},
                {title: "Requesting User"},
                {title: "Department"},
                {title: "Moderators", render: {display: renderModeratorsListExpandable}},
                {title: "State"},
                {title: "Actions"}
            ]
        });

        return tbl;
    }

    function deleteModerationRequest(id, docName) {
        function deleteModerationRequestInternal() {
            jQuery.ajax({
                type: 'POST',
                url: '<%=deleteModerationRequestAjaxURL%>',
                cache: false,
                data: {
                    <portlet:namespace/>moderationId: id
                },
                success: function (data) {
                    if (data.result == 'SUCCESS') {
                        closedModerationsDataTable.row('#' + id).remove().draw(false);
                    }
                    else {
                        $.alert("I could not delete the moderation request!");
                    }
                },
                error: function () {
                    $.alert("I could not delete the moderation request!");
                }
            });
        }

            confirm.confirmDeletion("Do you really want to delete the moderation request for <b>" + docName + "</b> ?", deleteModerationRequestInternal);
        }
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
        htmlString  = "<div>"
        htmlString += "<div class=\"TogglerModeratorsList\" style=\"display: block; float: left\"><div class=\"Toggler_off\">&#x25BA</div><div class=\"Toggler_on\" style=\"display: none; float: left\">&#x25BC</div></div>";
        htmlString += "<div class=\"ModeratorsListHidden\" style=\"display: block; float: left\">" + cutModeratorsList(moderators) + "</div>";
        htmlString += "<div class=\"ModeratorsListShown\" style=\"display: none; float: left\">" + moderators + "</div>";
        htmlString += "</div>";
        return htmlString;
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
</script>
