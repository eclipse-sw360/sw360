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
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="updateComponentsURL" name="updateComponents">
</portlet:actionURL>

<portlet:actionURL var="updateAttachmentsURL" name="updateComponentAttachments">
</portlet:actionURL>

<portlet:actionURL var="updateReleaseLinksURL" name="updateReleaseLinks">
</portlet:actionURL>

<portlet:actionURL var="updateLicenseArchiveURL" name="updateLicenses">
</portlet:actionURL>

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Component Administration</span></p>

<table class="info_table">
    <thead>
    <tr>
        <th colspan="2"> Downloads</th>
    </tr>
    </thead>

    <tbody>
    <tr>
        <td>Download Component CSV</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    <tr>
        <td>Download Upload Component template CSV</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_SAMPLE%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    <tr>
        <td>Download Attachment Sample Infos</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_SAMPLE_ATTACHMENT_INFO%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    <tr>
        <td>Download Attachment Infos</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_ATTACHMENT_INFO%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    <tr>
        <td>Download Relase Link Sample Infos</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_SAMPLE_RELEASE_LINK_INFO%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    <tr>
        <td>Download Relase Link Infos</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_RELEASE_LINK_INFO%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    <tr>
        <td>Download License Archive</td>
        <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_BACKUP%>'/>
                         </portlet:resourceURL>">
            <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
        </a>
        </td>
    </tr>
    </tbody>

</table>

<form id="uploadForm" name="uploadForm" action="<%=updateComponentsURL%>" method="POST" enctype="multipart/form-data">
    <div class="fileupload-buttons">
            <span class="fileinput-button">
                <span>Upload Component CSV</span>
                <input id="<portlet:namespace/>componentCSVfileuploadInput" type="file" name="<portlet:namespace/>file">
            </span>
        <input type="submit" value="Update Components" class="addButton" id="<portlet:namespace/>componentCSV-Submit" disabled>
    </div>
</form>

<form id="uploadAttachmentsForm" name="uploadAttachmentsForm" action="<%=updateAttachmentsURL%>" method="POST" enctype="multipart/form-data">
    <div class="fileupload-buttons">
            <span class="fileinput-button">
                <span>Upload Attachment Info CSV</span>
                <input id="<portlet:namespace/>attachmentfileuploadInput" type="file" name="<portlet:namespace/>file">
            </span>
        <input type="submit" value="Update Component Attachments" class="addButton" id="<portlet:namespace/>attachmentCSV-Submit" disabled>
    </div>
</form>

<form id="uploadRelaseLinksForm" name="uploadRelaseLinksForm" action="<%=updateReleaseLinksURL%>" method="POST" enctype="multipart/form-data">
    <div class="fileupload-buttons">
            <span class="fileinput-button">
                <span>Upload Release Link Info CSV</span>
                <input id="<portlet:namespace/>releaselinkfileuploadInput" type="file" name="<portlet:namespace/>file">
            </span>
        <input type="submit" value="Update Release Links" class="addButton" id="<portlet:namespace/>releaseLinkCSV-Submit" disabled>
    </div>
</form>

<form id="uploadLicenseArchiveForm" name="uploadLicenseArchiveForm" action="<%=updateLicenseArchiveURL%>" method="POST" enctype="multipart/form-data">
    <div class="fileupload-buttons">
            <span class="fileinput-button">
                <span>Upload License Archive</span>
                <input id="<portlet:namespace/>LicenseArchivefileuploadInput" type="file" name="<portlet:namespace/>file">
            </span>
        <input type="submit" value="Upload License Archive" class="addButton" id="<portlet:namespace/>LicenseArchive-Submit" disabled>
    </div>
</form>

<script>
    document.getElementById("<portlet:namespace/>componentCSVfileuploadInput").onchange = function () {
        if (this.value) {
            document.getElementById("<portlet:namespace/>componentCSV-Submit").disabled = false;
        }
    };

    document.getElementById("<portlet:namespace/>attachmentfileuploadInput").onchange = function () {
        if (this.value) {
            document.getElementById("<portlet:namespace/>attachmentCSV-Submit").disabled = false;
        }
    };

    document.getElementById("<portlet:namespace/>releaselinkfileuploadInput").onchange = function () {
        if (this.value) {
            document.getElementById("<portlet:namespace/>releaseLinkCSV-Submit").disabled = false;
        }
    };

    document.getElementById("<portlet:namespace/>LicenseArchivefileuploadInput").onchange = function () {
        if (this.value) {
            document.getElementById("<portlet:namespace/>LicenseArchive-Submit").disabled = false;
        }
    };

</script>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/search.css">



