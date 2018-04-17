<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
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
<%-- Note that the necessary includes are in liferay-portlet.xml --%>
<% assert ("moderationRequests".equals(PortalConstants.MODERATION_REQUESTS)); %>

<jsp:useBean id="moderationRequests"
             type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             class="java.util.ArrayList" scope="request"/>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_MODERATION_REQUEST%>'/>
</portlet:resourceURL>

<br>
<br>

<div class="homepageheading">
    My Task Submissions
</div>
<div id="tasksubmissionDiv" class="homepageListingTable">
    <table id="tasksubmissionTable" cellpadding="0" cellspacing="0" border="0" class="display">
    </table>
</div>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
<script src="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.js" type="text/javascript"></script>
<script>
    var moderationRequestsTable;

    Liferay.on('allPortletsReady', function() {
        var result = [];

        <core_rt:forEach items="${moderationRequests}" var="moderation">
        result.push({
            "DT_RowId": "${moderation.id}",
            "0": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
            "1": "<sw360:DisplayEnum value="${moderation.moderationState}"/>",
            "2": "<img src='<%=request.getContextPath()%>/images/Trash.png' onclick=\"deleteModerationRequest('${moderation.id}','<b><sw360:out value="${moderation.documentName}"/></b>')\"  alt='Delete' title='Delete'>"
        });
        </core_rt:forEach>

        moderationRequestsTable = $('#tasksubmissionTable').DataTable({
            pagingType: "simple_numbers",
            dom: "rtip",
            data: result,
            pageLength: 10,
            columns: [
                {"title": "Document Name"},
                {"title": "Status"},
                {"title": "Actions"}
            ],
            autoWidth: false
        });
    });

    function deleteModerationRequest(id, docName) {

        function deleteModerationRequestInternal() {
            jQuery.ajax({
                type: 'POST',
                url: '<%=deleteAjaxURL%>',
                cache: false,
                data: {
                    <portlet:namespace/>moderationId: id
                },
                success: function (data) {
                    if (data.result == 'SUCCESS') {
                        moderationRequestsTable.row('#' + id).remove().draw(false);
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

        deleteConfirmed("Do you really want to delete the moderation request for " + docName + " ?", deleteModerationRequestInternal);
    }

</script>


