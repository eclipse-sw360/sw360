<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

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

<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" onclick="window.location.href='<%=addLicenseURL%>'"><liferay-ui:message key="add.license" /></button>
						</div>
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-secondary" onclick="window.location.href='<%=exportLicensesURL%>'"><liferay-ui:message key="export.spreadsheet" /></button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="licenses" /> (${licenseList.size()})">
					<liferay-ui:message key="licenses" /> (${licenseList.size()})
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="licensesTable" class="table table-bordered"></table>
                </div>
            </div>

		</div>
	</div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'utils/includes/quickfilter' ], function($, datatables, quickfilter) {
        var licenseTable;

        licenseTable = createLicenseTable();
        quickfilter.addTable(licenseTable);

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
                    "2": '<span class="text-danger"><clay:icon symbol="times-circle" /></span>',
                    </core_rt:if>
                    <core_rt:if test="${license.checked}">
                    "2": '<span class="text-success"><clay:icon symbol="check-circle" /></span>',
                    </core_rt:if>
                    "3": '<sw360:out value="${license.licenseType.licenseType}" default="--"/>'
                });
            </core_rt:forEach>

            licenseTable = datatables.create('#licensesTable', {
                searching: true,
                data: result,
                columns: [
                    { "title": "<liferay-ui:message key="license.shortname" />" },
                    { "title": "<liferay-ui:message key="license.fullname" />" },
                    { "title": "<liferay-ui:message key="is.checked" />?", className: 'text-center' },
                    { "title": "<liferay-ui:message key="license.type" />" }
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                initComplete: datatables.showPageContainer
            }, [0, 1, 2, 3]);

            return licenseTable;
        }
    });
</script>

