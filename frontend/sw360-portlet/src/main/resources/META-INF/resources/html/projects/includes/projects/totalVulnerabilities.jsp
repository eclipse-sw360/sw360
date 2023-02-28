<%--
  ~ Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
  ~ With modifications from Bosch Software Innovations GmbH, 2016.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>

<%@include file="/html/init.jsp"%>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.RequestStatus" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div id="dt_loader" class="spinner text-center">
    <%@ include file="/html/utils/includes/pageSpinner.jspf"%>
</div>

<div id="SummaryPage" class="container" style = "display:none;">
    <div class="row">
        <div class="col">
            <div class="alert alert-info" role="alert">
                <liferay-ui:message key="total.vulnerabilities" /> <strong>${totalVulnerabilityCount}</strong>
            </div>
        </div>
    </div>
    <div class="row mt-4">
        <div class="col">
            <h4><liferay-ui:message key="vulnerabilities" /></h4>
            <form class="form-inline">
                <table id="vulnerabilitySummaryTable" class="table table-bordered">
                </table>
            </form>
        </div>
    </div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<script type="text/javascript">
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'bridges/jquery-ui'], function ($, datatable, dialog) {
        var vulnerabilitySummaryTable;
        vulnerabilitySummaryTable = createVulnerabilitySummaryTable();
        
        $("#allProject-tab").click(function(){
            var totalList=[];
            var vulList =[];
            var fullname;
            vulnerabilitySummaryTable.clear();

            <core_rt:forEach items="${allSubProjectLink}" var="proj">
                $('#vulnerabilityTable_${proj.id}').DataTable().page.len(-1).draw();
                var test=$('#vulnerabilityTable_${proj.id} tbody tr').each(function() {
                vulList=[];
                $(this).find('td').each(function() {
                if($(this).get(0).innerHTML !== "No data available in table") {
                    vulList.push($(this).get(0).innerHTML)
                }})
                if(vulList.length !== 0) {
                    totalList.push(vulList);
                }});
                var name= "${proj.name}";
                var version = "${proj.version}";
                fullname = name;
                if (version.length !== 0) {
                    fullname = name.concat(" ", "(", version, ")");
                }
                for (let pos=0; pos<totalList.length; pos++) {
                    vulnerabilitySummaryTable.row.add([fullname, totalList[pos][1], totalList[pos][2], totalList[pos][3], totalList[pos][4], totalList[pos][5], totalList[pos][6], totalList[pos][7]]).draw(false);
                }
                totalList=[];
                $('#vulnerabilityTable_${proj.id}').DataTable().page.len(10).draw();
            </core_rt:forEach>

            $('#dt_loader').hide()
            $('#SummaryPage').show()
        });
        
        function createVulnerabilitySummaryTable() {
            var table,
                tableDefinition,

            tableDefinition = {
                columns: [
                    { title: "<liferay-ui:message key="project.name" />"},
                    { title: "<liferay-ui:message key="release" />"},
                    { title: "<liferay-ui:message key="external.id" />"},
                    { title: "<liferay-ui:message key="priority" />" },
                    { title: "<liferay-ui:message key="matched.by" />"},
                    { title: "<liferay-ui:message key="title" />"},
                    { title: "<liferay-ui:message key="relevance.for.project" />"},
                    { title: "<liferay-ui:message key="actions" />"}
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                order: [[0, 'asc'], [4, 'desc']],
            }

            table = datatable.create('#vulnerabilitySummaryTable', tableDefinition, [0, 1, 2, 3, 4, 5, 6], [7], true);

            return table;
        }
    });
</script>
