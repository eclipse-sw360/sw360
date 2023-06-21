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

<portlet:defineObjects/>
<liferay-theme:defineObjects/>


<portlet:resourceURL var="viewVendorURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_VENDOR%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="updateReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.RELEASE%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="loadECCURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_ECC_LIST%>'/>
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
				<div class="col portlet-title text-truncate" title="<liferay-ui:message key="ecc.overview" />">
					<liferay-ui:message key="ecc.overview" />
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="eccInfoTable" class="table table-bordered">
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

        require(['jquery', 'utils/includes/quickfilter', 'bridges/datatables', 'bridges/jquery-ui'], function($, quickfilter, datatables) {
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
                    bServerSide: true,
                    sAjaxSource: '<%=loadECCURL%>',
                    columns: [
                    {"title": "<liferay-ui:message key="status" />", data: "status"},
                    {"title": "<liferay-ui:message key="release.name" />", data: function(row){return row["name"];}, render: {display: renderReleaseNameLink}},
                    {"title": "<liferay-ui:message key="release.version" />", data: "version", render: {display: renderReleaseNameLink}},
                    {"title": "<liferay-ui:message key="creator.group" />", data: "group"},
                    {"title": "<liferay-ui:message key="ecc.assessor" />", data: "assessor_contact_person"},
                    {"title": "<liferay-ui:message key="ecc.assessor.group" />", data: "assessor_dept"},
                    {"title": "<liferay-ui:message key="ecc.assessment.date" />", data: "assessment_date"},
                    ],
                    columnDefs: [],
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    searching: true
                }, [0, 1, 2, 3, 4, 5, 6]);
            }
            function renderReleaseNameLink(name, type, row) {
                return $("<span></span>").text(name)[0].outerHTML;
            }
        });
    });
</script>
