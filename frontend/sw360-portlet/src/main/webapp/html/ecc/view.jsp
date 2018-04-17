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

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>"
             scope="request"/>

<%--TODO--%>
<portlet:resourceURL var="viewVendorURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_VENDOR%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="updateReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.RELEASE%>"/>
</portlet:resourceURL>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/datatables.net-buttons-dt/1.1.2/css/buttons.dataTables.min.css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">ECC Overview</span>
</p>
<div id="searchInput" class="content1">
    <%@ include file="/html/utils/includes/quickfilter.jspf" %>
</div>

<div id="content" class="content2">
    <table class="table info_table" id="eccInfoTable">
        <thead>
        <tr>
            <th width="10%">Status</th>
            <th width="20%">Release Name</th>
            <th width="10%">Release version</th>
            <th width="10%">Creator Group</th>
            <th width="20%">ECC Assessor</th>
            <th width="20%">ECC Assessor Group</th>
            <th width="10%">ECC Assessment Date</th>
        </tr>
        </thead>
        <core_rt:forEach items="${releaseList}" var="release">
            <tr id="TableRow${release.id}">
                <td width="10%">
                <div id="eccStatusDiv"
                        <core_rt:if test="${release.eccInformation.eccStatus.value == 0 || release.eccInformation.eccStatus.value == 3}"> class="notificationBulletSpan backgroundAlert" </core_rt:if> <%--ECCStatus.OPEN || ECCStatus.REJECTED--%>
                        <core_rt:if test="${release.eccInformation.eccStatus.value == 1}"> class="notificationBulletSpan backgroundWarning" </core_rt:if> <%--ECCStatus.IN_PROGRESS--%>
                        <core_rt:if test="${release.eccInformation.eccStatus.value == 2}"> class="notificationBulletSpan backgroundOK" </core_rt:if>> <%--ECCStatus.APPROVED--%>
                    <core_rt:if test="${release.eccInformation.eccStatus.value == 3}">!</core_rt:if> <%--ECCStatus.REJECTED--%>
                    <core_rt:if test="${release.eccInformation.eccStatus.value != 3}">&nbsp;</core_rt:if> <%--ECCStatus.REJECTED--%>
                </div>
                    <sw360:DisplayEnum value="${release.eccInformation.eccStatus}"/></td>
                <td width="20%"><sw360:DisplayReleaseLink showName="true" release="${release}"/></td>
                <td width="10%"><sw360:out value="${release.version}"/></td>
                <td width="10%"><sw360:out value="${release.creatorDepartment}"/></td>
                <td width="20%"><sw360:DisplayUserEmail email="${release.eccInformation.assessorContactPerson}" bare="true"/></td>
                <td width="20%"><sw360:out value="${release.eccInformation.assessorDepartment}"/></td>
                <td width="10%"><sw360:out value="${release.eccInformation.assessmentDate}"/></td>
            </tr>
        </core_rt:forEach>
    </table>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'utils/includes/quickfilter', /* jquery-plugins: */ 'datatables', 'datatables_buttons', 'buttons.print'], function($, quickfilter) {
            var eccInfoTable;

            // initializing
            load();

            // helper functions
            function load() {
                eccInfoTable = configureEccInfoTable();
                quickfilter.addTable(eccInfoTable);
            }

            // catch ctrl+p and print dataTable
            $(document).on('keydown', function(e){
                if(e.ctrlKey && e.which === 80){
                    e.preventDefault();
                    eccInfoTable.buttons('.custom-print-button').trigger();
                }
            });

            function configureEccInfoTable(){
                var tbl;
                tbl = $('#eccInfoTable').DataTable({
                    "pagingType": "simple_numbers",
                    "dom": "lBrtip",
                    "buttons": [
                        {
                            extend: 'print',
                            text: 'Print',
                            autoPrint: true,
                            className: 'custom-print-button'
                        }
                    ],
                    "autoWidth": false,
                    "order": [],
                    "pageLength": 25
                });

                return tbl;
            }
        });
    });
</script>
