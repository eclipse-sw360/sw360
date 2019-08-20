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

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>" scope="request"/>

<portlet:resourceURL var="viewVendorURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_VENDOR%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="updateReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.RELEASE%>"/>
</portlet:resourceURL>


<div class="container">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col portlet-title text-truncate" title="ECC Overview">
					ECC Overview
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="eccInfoTable" class="table table-bordered">
                        <thead>
                            <tr>
                                <th>Status</th>
                                <th>Release Name</th>
                                <th>Release version</th>
                                <th>Creator Group</th>
                                <th>ECC Assessor</th>
                                <th>ECC Assessor Group</th>
                                <th>ECC Assessment Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            <core_rt:forEach items="${releaseList}" var="release">
                                <tr id="TableRow${release.id}">
                                    <td class="content-space-between">
                                        <sw360:DisplayEnum value="${release.eccInformation.eccStatus}"/>
                                        <span
                                            <core_rt:if test="${release.eccInformation.eccStatus.value == 0 || release.eccInformation.eccStatus.value == 3}"> class="badge badge-empty badge-danger" </core_rt:if> <%--ECCStatus.OPEN || ECCStatus.REJECTED--%>
                                            <core_rt:if test="${release.eccInformation.eccStatus.value == 1}"> class="badge badge-empty badge-warning" </core_rt:if> <%--ECCStatus.IN_PROGRESS--%>
                                            <core_rt:if test="${release.eccInformation.eccStatus.value == 2}"> class="badge badge-empty badge-success" </core_rt:if>> <%--ECCStatus.APPROVED--%>
                                            <core_rt:if test="${release.eccInformation.eccStatus.value == 3}">!</core_rt:if> <%--ECCStatus.REJECTED--%>
                                            <core_rt:if test="${release.eccInformation.eccStatus.value != 3}">&nbsp;</core_rt:if> <%--ECCStatus.REJECTED--%>
                                        </span>
                                    </td>
                                    <td><sw360:DisplayReleaseLink showName="true" release="${release}"/></td>
                                    <td><sw360:out value="${release.version}"/></td>
                                    <td><sw360:out value="${release.creatorDepartment}"/></td>
                                    <td><sw360:DisplayUserEmail email="${release.eccInformation.assessorContactPerson}" bare="true"/></td>
                                    <td><sw360:out value="${release.eccInformation.assessorDepartment}"/></td>
                                    <td><sw360:out value="${release.eccInformation.assessmentDate}"/></td>
                                </tr>
                            </core_rt:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
		</div>
	</div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'utils/includes/quickfilter', 'bridges/datatables'], function($, quickfilter, datatables) {
            var eccInfoTable;

            // initializing
            eccInfoTable = configureEccInfoTable();
            quickfilter.addTable(eccInfoTable);

            // catch ctrl+p and print dataTable
            $(document).on('keydown', function(e){
                if(e.ctrlKey && e.which === 80){
                    e.preventDefault();
                    eccInfoTable.buttons('.custom-print-button').trigger();
                }
            });

            function configureEccInfoTable(){
                return datatables.create('#eccInfoTable', {
                    searching: true
                }, [0, 1, 2, 3, 4, 5, 6, 7]);
            }
        });
    });
</script>
