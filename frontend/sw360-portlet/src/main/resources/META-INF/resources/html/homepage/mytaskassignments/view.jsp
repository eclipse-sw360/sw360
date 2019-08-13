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

<h4>My Task Assignments</h4>
<div class="row">
    <div class="col">
        <table id="taskassignmentTable" class="table table-bordered table-lowspace">
            <colgroup>
                <col style="width: 60%;"/>
                <col style="width: 40%;"/>
            </colgroup>
        </table>
    </div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables' ], function($, datatables) {
        var result = [];

        <core_rt:forEach items="${moderationRequests}" var="moderation">
            result.push({
                "DT_RowId": "${moderation.id}",
                "0": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
                "1": "<sw360:DisplayEnum value="${moderation.moderationState}"/>"
            });
        </core_rt:forEach>

        datatables.create('#taskassignmentTable', {
            data: result,
            dom:
				"<'row'<'col-sm-12'tr>>" +
				"<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            columns: [
                {"title": "Document Name"},
                {"title": "Status"},
            ],
            language: {
                emptyTable: 'There are no tasks assigned to you.'
            }
        });
    });
</script>
