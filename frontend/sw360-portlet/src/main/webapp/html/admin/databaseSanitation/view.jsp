<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  - With contributions by Bosch Software Innovations GmbH, 2016-2017.
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
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="getDuplicatesURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DUPLICATES%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="deleteAllLicenseInformationURL">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.ACTION_DELETE_ALL_LICENSE_INFORMATION%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="importSpdxLicenseInformationURL">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.ACTION_IMPORT_SPDX_LICENSE_INFORMATION%>'/>
</portlet:resourceURL>

<script src="<%=request.getContextPath()%>/webjars/jquery/1.12.4/jquery.min.js"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.min.js"></script>
<script src="<%=request.getContextPath()%>/webjars/datatables/1.10.15/js/jquery.dataTables.min.js"></script>

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">DB Administration</span></p>

<table class="info_table">
    <thead>
    <tr>
        <th colspan="2"> Actions</th>
    </tr>
    </thead>

    <tbody>
    <tr>
        <td>Search DB for duplicate identifiers</td>
        <td><img src="<%=request.getContextPath()%>/images/search.png" alt="CleanUp" onclick="findDuplicates()"
                 width="25px" height="25px">
        </td>
    </tr>
    <tr>
        <td>Import all SPDX license information</td>
        <td><a id="importSPDXLink" href="#">Import</a>
        </td>
    </tr>
    <tr>
        <td>Delete all license information</td>
        <td><img src="<%=request.getContextPath()%>/images/Trash.png"
                 alt="Delete all license information"
                 onclick="deleteAllLicenseInformation()">
        </td>
    </tr>
    </tbody>
</table>

<div id="DuplicateSearch">
</div>

<br/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
<script src="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.js" type="text/javascript"></script>
<script>
    function findDuplicates() {

        var field = $('#DuplicateSearch');
        field.html("Looking for duplicate identifiers ... ");
        jQuery.ajax({
            type: 'POST',
            url: '<%=getDuplicatesURL%>',
            cache: false,
            data: "",
            success: function (data) {
                var html;
                if (data.result == 'SUCCESS')
                    html = "No duplicate identifiers were found";
                else if (data.result == 'FAILURE') {
                    html = "Error in looking for duplicate identifiers";
                } else {
                    html = data;
                }
                field.html(html);
                setupPagination('#duplicateReleasesTable');
                setupPagination('#duplicateReleaseSourcesTable');
                setupPagination('#duplicateComponentsTable');
                setupPagination('#duplicateProjectsTable');
            },
            error: function () {
                html = "Error in looking for duplicate identifiers";
                field.html(html);
            }
        });
    }
    function setupPagination(tableId) {
        if ($(tableId)) {
            $(tableId).DataTable({
                pagingType: "simple_numbers",
                dom: "lrtip",
                autoWidth: false
            });
        }
    }

    function deleteAllLicenseInformation() {

        function deleteAllLicenseInformationInternal() {
            $.confirm({
                title: "Delete",
                content: function () {
                    var self = this;
                    return $.ajax({
                        type: 'POST',
                        url: '<%=deleteAllLicenseInformationURL%>',
                        cache: false,
                        dataType: 'json'
                    }).done(function (data) {
                        if (data.result == 'SUCCESS') {
                            self.setTitle("Success");
                            self.setContent("I deleted " + data.totalAffectedObjects + " of " + data.totalObjects + " total documents in the DB.");
                        }else {
                            self.setTitle("Failure");
                            self.setContent("I could not delete the license information!");
                        }
                    }).fail(function(){
                        self.setContent('Something went wrong.');
                    });
                }
            });
        }

        var confirmMessage = "Do you really want to delete all licenses, license types, todos, obligations, risks, risk categories and todo custom properties from the db? " +
                "\nN.B.: other documents might use the licenses." +
                "\nThis function is meant to be followed by a new license import.";
        deleteConfirmed(confirmMessage, deleteAllLicenseInformationInternal);
    }

    function importSpdxLicenseInformation() {

        function importSpdxLicenseInformationInternal() {
            $.confirm({
                title: "Import",
                content: function () {
                    var self = this;
                    return $.ajax({
                        type: 'POST',
                        url: '<%=importSpdxLicenseInformationURL%>',
                        cache: false,
                        dataType: 'json'
                    }).done(function (data) {
                        if (data.result == 'SUCCESS') {
                            self.setTitle("Success");
                            self.setContent("I imported " + data.totalAffectedObjects + " of " + data.totalObjects + " SPDX licenses. " + data.message);
                        }else {
                            self.setTitle("Failure");
                            self.setContent("I could not import all SPDX license information!");
                        }
                    }).fail(function(){
                        self.setContent('Something went wrong.');
                    });
                }
            });
        }

        var confirmMessage = "Do you really want to import all SPDX licenses";
        deleteConfirmed(confirmMessage, importSpdxLicenseInformationInternal);
    }

    window.onload = function() {
        var a = document.getElementById("importSPDXLink");
        a.onclick = importSpdxLicenseInformation;
    }
</script>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/search.css">
