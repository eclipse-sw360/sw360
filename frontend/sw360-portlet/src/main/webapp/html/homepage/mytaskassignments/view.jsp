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



<% assert ("moderationRequests".equals(PortalConstants.MODERATION_REQUESTS)); %>

<jsp:useBean id="moderationRequests"
             type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             class="java.util.ArrayList" scope="request"/>
<br>
<br>

<%-- Note that the necessary includes are in life-ray-portlet.xml --%>

<div class="homepageheading">
    My Task Assignments
</div>

<div id="taskassignmentDiv" class="homepageListingTable">
    <table id="taskassignmentTable" cellpadding="0" cellspacing="0" border="0" class="display">
    </table>
</div>


<script>

    Liferay.on('allPortletsReady', function() {
        var result = [];

        <core_rt:forEach items="${moderationRequests}" var="moderation">
        result.push({
            "DT_RowId": "${moderation.id}",
            "0": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
            "1": "<sw360:DisplayEnum value="${moderation.moderationState}"/>"
        });
        </core_rt:forEach>

        $('#taskassignmentTable').dataTable({
            pagingType: "simple_numbers",
            dom: "rtip",
            data: result,
            pageLength:10,
            columns: [
                {"title": "Document Name"},
                {"title": "Status"},
            ],
            autoWidth: false
        });
    });

</script>
