<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="moderationRequests"
             type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             class="java.util.ArrayList" scope="request"/>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_MODERATION_REQUEST%>'/>
</portlet:resourceURL>


<h4>My Task Submissions</h4>
<div class="row">
    <div class="col">
        <table id="tasksubmissionTable" class="table table-bordered table-lowspace">
            <colgroup>
                <col style="width: 60%;"/>
                <col style="width: 40%;"/>
                <col style="width: 1.7rem"/>
            </colgroup>
        </table>
    </div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'modules/dialog' ], function($, datatables, dialog) {
        var $datatable;

        $datatable = createTable();
        $('#tasksubmissionTable').on('click', 'svg.delete', function(event) {
            var data = $(event.currentTarget).data();
            deleteModerationRequest(data.moderationId, data.documentName);
        });

        function createTable() {
            var result = [];

            <core_rt:forEach items="${moderationRequests}" var="moderation">
                result.push({
                    "DT_RowId": "${moderation.id}",
                    "0": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
                    "1": "<sw360:DisplayEnum value="${moderation.moderationState}"/>",
                    "2": '<div class="actions"><svg class="delete lexicon-icon" data-moderation-id="${moderation.id}" data-document-name="${moderation.documentName}"><title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/></svg></div>'
                });
            </core_rt:forEach>

            return datatables.create('#tasksubmissionTable', {
                data: result,
                dom:
                    "<'row'<'col-sm-12'tr>>" +
                    "<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
                columns: [
                    {"title": "Document Name"},
                    {"title": "Status"},
                    {"title": "Actions", className: "one action", orderable: false}
                ],
                language: {
                    emptyTable: 'You do not have any open moderation requests.'
                }
            });
        }

        function deleteModerationRequest(id, docName) {
            var $dialog;

            function deleteModerationRequestInternal(callback) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=deleteAjaxURL%>',
                    cache: false,
                    data: {
                        <portlet:namespace/>moderationId: id
                    },
                    success: function (data) {
                        if (data.result == 'SUCCESS') {
                            callback(true);
                            moderationRequestsTable.row('#' + id).remove().draw(false);
                        }
                        else {
                            callback();
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
                '<p>Do you really want to delete the moderation request for <b data-name="name"></b>?</p>',
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
</script>
