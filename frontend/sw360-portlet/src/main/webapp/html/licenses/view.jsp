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

<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects />
<liferay-theme:defineObjects />

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="isUserAtLeastClearingAdmin" class="java.lang.String" scope="request" />
    <jsp:useBean id="licenseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.License>"
                 scope="request"/>
</c:catch>
<%@include file="/html/utils/includes/logError.jspf" %>

<portlet:resourceURL var="exportLicensesURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EXPORT_TO_EXCEL%>"/>
</portlet:resourceURL>

<portlet:renderURL var="addLicenseURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>
</portlet:renderURL>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/datatables.net-buttons-bs/css/buttons.bootstrap.min.css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader">
    <span class="pageHeaderBigSpan">Licenses</span> <span class="pageHeaderSmallSpan">(${licenseList.size()})</span>
    <span class="pull-right">
        <input type="button" class="addButton" onclick="window.location.href='<%=exportLicensesURL%>'"
               value="Export Licenses">
        <input type="button" class="addButton" onclick="window.location.href='<%=addLicenseURL%>'" value="Add License">
    </span>
</p>

<div id="searchInput" class="content1">
    <%@ include file="/html/utils/includes/quickfilter.jspf" %>
</div>

<div id="licensesTableDiv" class="content2">
    <table id="licensesTable" cellpadding="0" cellspacing="0" border="0" class="display">
        <tfoot>
        <tr>
            <th style="width: 25%;"></th>
            <th style="width: 35%;"></th>
            <th style="width: 20%;"></th>
            <th style="width: 20%;"></th>
        </tr>
        </tfoot>
    </table>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'utils/includes/quickfilter', /* jquery-plugins */ 'datatables.net', 'datatables.net-buttons', 'datatables.net-buttons.print'], function($, quickfilter) {
        var licenseTable;

        Liferay.on('allPortletsReady', function() {
            licenseTable = createLicenseTable();
            quickfilter.addTable(licenseTable);
        });

        // catch ctrl+p and print dataTable
        $(document).on('keydown', function(e){
            if(e.ctrlKey && e.which === 80){
                e.preventDefault();
                licenseTable.buttons('.custom-print-button').trigger();
            }
        });

        function createLicenseTable() {
            var licenseTable,
                result = [];

            <core_rt:forEach items="${licenseList}" var="license">
                result.push({
                    <%-- "DT_RowId": '${license.id}',--%>
                    "0": "<sw360:DisplayLicenseLink licenseId="${license.id}"/>",
                    "1": '<sw360:out value="${license.fullname}"/>',
                    <core_rt:if test="${license.checked}">
                    "2": '',
                    </core_rt:if>
                    <core_rt:if test="${not license.checked}">
                    "2": 'UNCHECKED',
                    </core_rt:if>
                    "3": '<sw360:out value="${license.licenseType.licenseType}" default="--"/>'
                });
            </core_rt:forEach>

            licenseTable = $('#licensesTable').DataTable({
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
                "pageLength": 10,
                "language": {
                  "lengthMenu": 'Display <select>\
                  <option value="5">5</option>\
                  <option value="10">10</option>\
                  <option value="20">20</option>\
                  <option value="50">50</option>\
                  <option value="100">100</option>\
                  </select> licenses'
                },
                "data": result,
                "columns": [
                  { "title": "License Shortname" },
                  { "title": "License Fullname" },
                  { "title": "Is checked?" },
                  { "title": "License Type" }
                  ],
                "autoWidth": false
            });

              return licenseTable;
        }
      });
</script>

