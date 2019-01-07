<%--
  ~ Copyright Siemens AG, 2013-2017.
  ~ Copyright Bosch Software Innovations GmbH, 2016.
  ~ Part of the SW360 Portal Project.
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

<portlet:actionURL var="updateLicenseArchiveURL" name="updateLicenses">
</portlet:actionURL>

<portlet:resourceURL var="deleteAllLicenseInformationURL">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.ACTION_DELETE_ALL_LICENSE_INFORMATION%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="importSpdxLicenseInformationURL">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.ACTION_IMPORT_SPDX_LICENSE_INFORMATION%>'/>
</portlet:resourceURL>

<script src="<%=request.getContextPath()%>/webjars/jquery/dist/jquery.min.js"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/jquery-ui.min.js"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-confirm2/dist/jquery-confirm.min.js" type="text/javascript"></script>

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">License Administration</span></p>

<table class="info_table">
    <thead>
    <tr>
        <th colspan="2"> Actions</th>
    </tr>
    </thead>

    <tbody>
    <tr>
        <td>Download License Archive</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_BACKUP%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    <tr>
        <td colspan="2">
            <span> Upload License Archive </span>
            <form id="uploadLicenseArchiveForm" name="uploadLicenseArchiveForm" action="<%=updateLicenseArchiveURL%>" method="POST" enctype="multipart/form-data" style="margin-left: 30px;">
                <div class="fileupload-buttons">
                        <input id="<portlet:namespace/>LicenseArchivefileuploadInput" type="file" name="<portlet:namespace/>file">
                    <label for="overwriteIfExternalIdMatches">
                        <input type="checkbox" id="overwriteIfExternalIdMatches" name="<portlet:namespace/>overwriteIfExternalIdMatches" value="true" />
                        overwrite if external IDs match
                    </label>
                    <label for="overwriteIfIdMatchesEvenWithoutExternalIdMatch">
                        <input type="checkbox" id="overwriteIfIdMatchesEvenWithoutExternalIdMatch" name="<portlet:namespace/>overwriteIfIdMatchesEvenWithoutExternalIdMatch" value="true" />
                        overwrite if IDs match
                    </label>
                    <span class="fileinput-button">
                        <input type="submit" value="Upload License Archive" class="addButton" id="<portlet:namespace/>LicenseArchive-Submit" disabled>
                    </span>
                </div>
            </form>
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

<script>
    document.getElementById("<portlet:namespace/>LicenseArchivefileuploadInput").onchange = function () {
        if (this.value) {
            document.getElementById("<portlet:namespace/>LicenseArchive-Submit").disabled = false;
        }
    };

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

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-confirm2/dist/jquery-confirm.min.css">
