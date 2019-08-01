<%--
  ~ Copyright Siemens AG, 2016-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="java.util.Map"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@ include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%-- enable requirejs for this page --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<portlet:resourceURL var="downloadLicenseInfoURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_INFO%>'/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${requestScope.project.id}"/>
</portlet:resourceURL>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
    <jsp:useBean id="sw360User" class="org.eclipse.sw360.datahandler.thrift.users.User" scope="request"/>
    <jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"
                 scope="request"/>
    <jsp:useBean id="projectPaths" type="java.util.Map<java.lang.String, java.lang.String>" scope="request"/>
    <jsp:useBean id="licenseInfoOutputFormats"
                 type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo>"
                 scope="request"/>
</c:catch>
<core_rt:if test="${empty attributeNotFoundException}">
    <div class="container" style="display: none;">
	<div class="row">
            <div class="col portlet-title left text-truncate" title="Generate License Information">
                Generate License Information
            </div>
            <div class="col portlet-title text-truncate" title="${sw360:printProjectName(project)}">
                <sw360:ProjectName project="${project}"/>
            </div>
        </div>
        <div class="row">
            <div class="col" >
            <button id="selectVariantAndDownload" type="button" class="btn btn-primary">Download</button>
                <form id="downloadLicenseInfoForm" class="form-inline" name="downloadLicenseInfoForm" action="<%=downloadLicenseInfoURL%>" method="post">
                    <%@include file="/html/projects/includes/attachmentSelectTable.jspf" %>
                </form>
            </div>
        </div>
    </div>
    <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
</core_rt:if>

<div class="dialogs auto-dialogs">
<div id="downloadLicenseInfoDialog" class="modal fade" tabindex="-1" role="dialog">
<div class="modal-dialog modal-lg modal-dialog-centered modal-info" role="document">
  <!-- <div class="modal-dialog" role="document"> -->
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">Select Other Options</h5>
        <button id="closeModalButton" type="button" class="close" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <div class="modal-body">
						<c:if test="${not empty externalIds}">
								<div class="form-group form-check">
									<label for="externalIdLabel" class="font-weight-bold h3">Select the external Ids:</label>
							        <c:forEach var="extId" items="${externalIds}">
									   <div class="checkbox form-check">
										  <label><input id="<%=PortalConstants.EXTERNAL_ID_SELECTED_KEYS%>" name="externalIdsSelection" type="checkbox" value="${extId}">
									      <c:out value="${extId}" /></input></label>
									   </div>
							        </c:forEach>
								</div>
						</c:if>
					<div class="form-group form-check">
						<label for="outputFormatLabel" class="licenseInfoOpFormat font-weight-bold h3">Select output format and variant:</label>
						<sw360:DisplayOutputFormats options='${licenseInfoOutputFormats}' />
					</div>
	  </div>
      <div class="modal-footer">
        <button id="downloadFileModal" type="button" value="Download" class="btn btn-primary">Download</button>
        <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>
</div>

<script>
require(['jquery', 'modules/dialog'], function($, dialog) {
    $('#selectVariantAndDownload').on('click', selectVariantAndSubmit);
    function selectVariantAndSubmit(){
        dialog.open('#downloadLicenseInfoDialog','',function(submit, callback) {
            callback(true);
        });
    }
    $('#downloadFileModal').on('click', downloadFile);
    function downloadFile(){
        var licenseInfoSelectedOutputFormat = $('input[name="outputFormat"]:checked').val();
        var externalIds = [];
        $.each($("input[name='externalIdsSelection']:checked"), function(){
            externalIds.push($(this).val());
        });
        var extIdsHidden = externalIds.join(',');

        $('#downloadLicenseInfoForm').append('<input id="extIdHidden" type="hidden" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_SELECTED_KEYS%>"/>');
        $('#downloadLicenseInfoForm').append('<input id="licensInfoFileFormat" type="hidden" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>"/>');

        $("#extIdHidden").val(extIdsHidden);
        $("#licensInfoFileFormat").val(licenseInfoSelectedOutputFormat);

        $('#downloadLicenseInfoForm').submit();
    }
});
</script>