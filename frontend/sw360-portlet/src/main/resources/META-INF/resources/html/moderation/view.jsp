<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>


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


<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
            </div>
            <div id="moderationTabs" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                <a class="list-group-item list-group-item-action active" href="#tab-Open" data-toggle="list" role="tab">Open</a>
                <a class="list-group-item list-group-item-action" href="#tab-Closed" data-toggle="list" role="tab">Closed</a>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">

				</div>
                <div class="col portlet-title text-truncate" title="Moderations (${moderationRequests.size()}/${closedModerationRequests.size()})">
					Moderations (${moderationRequests.size()}/<span id="requestCounter">${closedModerationRequests.size()}</span>)
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <div class="tab-content">
                        <div id="tab-Open" class="tab-pane active show">
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
                        <div id="tab-Closed" class="tab-pane">
                            <table id="closedModerationsTable" class="table table-bordered"></table>
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
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'modules/listgroup', 'utils/includes/quickfilter'], function($, datatables, dialog, listgroup, quickfilter) {
        var moderationsDataTable,
            closedModerationsDataTable;

        listgroup.initialize('moderationTabs', 'tab-Open');

        moderationsDataTable = createModerationsTable("#moderationsTable", prepareModerationsData());
        closedModerationsDataTable = createModerationsTable("#closedModerationsTable", prepareClosedModerationsData());

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
                        "7": '<div class="actions"><svg class="delete lexicon-icon" data-moderation-request="<sw360:out value="${moderation.id}"/>" data-document-name="${moderation.documentName}"><title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/></svg></div>'
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
                    {title: "Date", render: {display: renderTimeToReadableFormat}, className: 'text-nowrap' },
                    {title: "Type", className: 'text-nowrap'},
                    {title: "Document Name"},
                    {title: "Requesting User"},
                    {title: "Department"},
                    {title: "Moderators", render: {display: renderModeratorsListExpandable}},
                    {title: "State", className: 'text-nowrap'},
                    {title: "Actions", className: 'one action'}
                ],
                language: {
                    emptyTable: "No moderation requests found."
                },
                initComplete: datatables.showPageContainer
            }, [0,1,2,3,4,5,6], [7]);
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
                            $dialog.alert("I could not delete the moderation request!");
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert("I could not delete the moderation request!");
                    }
                });
            }

            $dialog = dialog.confirm(
                'danger',
                'question-circle',
                'Delete Moderation Request?',
                '<p>Do you really want to delete the moderation request <b data-name="name"></b>?</p>',
                'Delete Moderation Request',
                {
                    name: docName,
                },
                function(submit, callback) {
                    deleteModerationRequestInternal(callback);
                }
            );
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
</script>
