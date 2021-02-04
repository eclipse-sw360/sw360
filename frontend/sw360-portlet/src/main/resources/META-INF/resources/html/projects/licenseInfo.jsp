<%--
  ~ Copyright Siemens AG, 2016-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@ include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<liferay-ui:error key="attachment_error" message="${fn:escapeXml(attachmentLoadingError)}" embed="false"/>
<%-- enable requirejs for this page --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<portlet:resourceURL var="downloadLicenseInfoURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_INFO%>'/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${requestScope.project.id}"/>
    <portlet:param name="<%=PortalConstants.LICENSE_INFO_EMPTY_FILE%>" value="No"/>
</portlet:resourceURL>

<portlet:resourceURL var="checkIfAttachmentExists">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.PROJECT_CHECK_FOR_ATTACHMENTS%>'/>
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
            <div class="col portlet-title left text-truncate" title="<liferay-ui:message key="generate.license.information" />">
                <liferay-ui:message key="generate.license.information" />
            </div>
            <div class="col portlet-title text-truncate" title="${sw360:printProjectName(project)}">
                <sw360:ProjectName project="${project}"/>
            </div>
        </div>
        <div class="row">
            <div class="col" >
            <button id="selectVariantAndDownload" type="button" class="btn btn-primary"><liferay-ui:message key="download" /></button>
                <form id="downloadLicenseInfoForm" class="form-inline" name="downloadLicenseInfoForm" action="<%=downloadLicenseInfoURL%>" method="post">
                    <%@include file="/html/projects/includes/attachmentSelectTable.jspf" %>
                </form>
            </div>
        </div>
    </div>
    <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
</core_rt:if>

<c:set var="clReportTmplateMappings" value="<%=PortalConstants.CLEARING_REPORT_TEMPLATE_TO_FILENAMEMAPPING%>"/>
<c:set var="clearingReportTemplate" value="${fn:split(clReportTmplateMappings, ',')}"/>

<c:set var="templateFormats" value="<%=PortalConstants.CLEARING_REPORT_TEMPLATE_FORMAT%>"/>
<c:set var="tmpFormat" value="${fn:split(templateFormats, ',')}"/>

<div class="dialogs auto-dialogs">
	<div id="downloadLicenseInfoDialog" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-info" role="document">
			<!-- <div class="modal-dialog" role="document"> -->
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title">
						<liferay-ui:message key="select.other.options" />
					</h5>
					<button id="closeModalButton" type="button" class="close" data-dismiss="modal" aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
				</div>
				<div class="modal-body" style="position: relative; overflow-y: auto; max-height: 400px;">
					<c:if test="${not empty relations}">
						<div class="form-group form-check">
							<label for="projectRelation" class="font-weight-bold h3">Uncheck project release relationships to be excluded:</label>
							<c:forEach var="projectReleaseRelation" items="${relations}">
								<div class="checkbox form-check">
									<label> <input name="releaseRelationSelection" type="checkbox"
										<c:if test = "${empty usedProjectReleaseRelations}">checked="checked"</c:if>
										<c:if test = "${not empty usedProjectReleaseRelations and (fn:contains(usedProjectReleaseRelations, projectReleaseRelation))}">checked="checked"</c:if>
										value="${projectReleaseRelation}"> <sw360:DisplayEnum value='${projectReleaseRelation}' bare="true" /> </input>
									</label>
								</div>
							</c:forEach>
						</div>
					</c:if>
					<c:if test="${not empty linkedProjectRelation}">
						<div class="form-group form-check">
							<label for="projectRelation" class="font-weight-bold h3">Uncheck Linked Project Relationships to be excluded:</label>
							<c:forEach var="projectRelation" items="${linkedProjectRelation}">
								<div class="checkbox form-check">
									<label> <input name="projectRelationSelection" type="checkbox"
										<c:if test = "${usedLinkedProjectRelation == null}">checked="checked"</c:if>
										<c:if test = "${usedLinkedProjectRelation != null and (fn:contains(usedLinkedProjectRelation, projectRelation))}">checked="checked"</c:if>
										value="${projectRelation}"> <sw360:DisplayEnum value='${projectRelation}' bare="true" /> </input>
									</label>
								</div>
							</c:forEach>
						</div>
					</c:if>
					<c:if test="${not onlyClearingReport}">
						<c:if test="${not empty externalIds}">
							<div class="form-group form-check">
								<label for="externalIdLabel" class="font-weight-bold h3"><liferay-ui:message key="select.the.external.ids" />:</label>
								<c:forEach var="extId" items="${externalIds}">
									<div class="checkbox form-check">
										<label><input id="<%=PortalConstants.EXTERNAL_ID_SELECTED_KEYS%>" name="externalIdsSelection" type="checkbox" value="${extId}">
											<c:out value="${extId}" /></input></label>
									</div>
								</c:forEach>
							</div>
						</c:if>
						<div class="form-group form-check">
							<label for="outputFormatLabel"
								class="licenseInfoOpFormat font-weight-bold h3"><liferay-ui:message key="select.output.format" />:</label>
							<sw360:DisplayOutputFormats options='${licenseInfoOutputFormats}' variantToSkip="<%=OutputFormatVariant.REPORT%>" />
						</div>
					</c:if>
					<c:if test="${onlyClearingReport eq 'true' and not empty clearingReportTemplate[0]}">
						<label for="OrganisationSelection" class="font-weight-bold h3"><liferay-ui:message key="templates" />:</label>
						<c:forEach var="clRepTemp" items="${clearingReportTemplate}">
							<c:set var="org" value="${fn:split(clRepTemp, ':')}" />
							<div class="radio form-check">
								<label><input type="radio" name="org" value="${org[0]}" checked>${org[0]}</label>
							</div>
						</c:forEach>
					</c:if>
					<c:if test="${onlyClearingReport eq 'true' and not empty  tmpFormat[0]}">
						<div class="form-group form-check">
							<label for="outputFormatLabel" class="licenseInfoOpFormat font-weight-bold h3"><liferay-ui:message key="select.output.format" />:</label>
							<sw360:DisplayOutputFormats options='${licenseInfoOutputFormats}' variantToSkip="<%=OutputFormatVariant.DISCLOSURE%>"
								formatsToShow='<%=Arrays.asList(PortalConstants.CLEARING_REPORT_TEMPLATE_FORMAT.split(","))%>' />
						</div>
					</c:if>
				</div>
				<div class="modal-footer">
					<button id="downloadFileModal" type="button" value="Download" class="btn btn-primary">
						<liferay-ui:message key="download" />
					</button>
					<button type="button" class="btn btn-secondary" data-dismiss="modal">
						<liferay-ui:message key="close" />
					</button>
				</div>
			</div>
		</div>
	</div>
</div>

<script>
require(['jquery', 'modules/dialog'], function($, dialog) {
    var onlyClearingReport = '${onlyClearingReport}';
    let downloadEmptyTemplate = '${showSessionError}';
    var outputFormat = '${lcInfoSelectedOutputFormat}';

    var url = window.location.href;
    var searchKey = "&"+"<portlet:namespace/><%=PortalConstants.SHOW_ATTACHMENT_MISSING_ERROR%>";
    var searchKeyOpFormat = "&"+"<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>";
    var reloading = sessionStorage.getItem("reloading");
    var clean_uri;
    if (reloading) {
        sessionStorage.removeItem("reloading");
        clean_uri = removeUnusedParam(url);
    }
    if (url.indexOf(searchKey) > 0 || url.indexOf(searchKeyOpFormat) > 0) {
        if (window.history.replaceState) {
            window.history.replaceState({}, document.title, clean_uri);
        }
    }

    function removeUnusedParam(uri) {
        var cleaned_uri;
	if (uri.indexOf(searchKey) > 0) {
	     cleaned_uri = uri.substring(0, uri.indexOf(searchKey));
	}
	if (uri.indexOf(searchKeyOpFormat) > 0) {
	    cleaned_uri = uri.substring(0, uri.indexOf(searchKeyOpFormat));
	}
	return cleaned_uri;
    }

    $('#selectVariantAndDownload').on('click', selectVariantAndSubmit);
    function selectVariantAndSubmit(){
        dialog.open('#downloadLicenseInfoDialog','',function(submit, callback) {
            callback(true);
        });
    }

    if(downloadEmptyTemplate) {
        if(onlyClearingReport == 'true') {
            downloadClearingReportOnly(true);
        } else {
            downloadFile(true);
        }
    }

    $('#downloadFileModal').on('click', function() {
        checkIfSelectedAttachmentsExist().done(function(data){
            if(data.attachmentNames) {
                downloadLicenseInfo(data.attachmentNames);
            } else {
                if(onlyClearingReport == 'true') {
                    downloadClearingReportOnly(false);
                } else {
                    downloadFile(false);
                }
            }
        });
    });

    function downloadFile(isEmptyFile){
        let licenseInfoSelectedOutputFormat = $('input[name="outputFormat"]:checked').val();
        if(isEmptyFile === "undefined" || !isEmptyFile){
            var externalIds = [];
            var releaseRelations = [];
            var selectedProjectRelations = [];
            $.each($("input[name='externalIdsSelection']:checked"), function(){
                externalIds.push($(this).val());
            });
            var extIdsHidden = externalIds.join(',');

            $.each($("input[name='releaseRelationSelection']:checked"), function(){
                releaseRelations.push($(this).val());
            });
            var releaseRelationsHidden = releaseRelations.join();

            $.each($("input[name='projectRelationSelection']:checked"), function(){
                selectedProjectRelations.push($(this).val());
            });
            var selectedProjectRelationsHidden = selectedProjectRelations.join();

            $('#downloadLicenseInfoForm').append('<input id="extIdHidden" type="hidden" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_SELECTED_KEYS%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="licensInfoFileFormat" type="hidden" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="releaseRelationship" type="hidden" name="<portlet:namespace/><%=PortalConstants.SELECTED_PROJECT_RELEASE_RELATIONS%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="selectedProjectRelations" type="hidden" name="<portlet:namespace/><%=PortalConstants.SELECTED_PROJECT_RELATIONS%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="isSubProjPresent" type="hidden" name="<portlet:namespace/><%=PortalConstants.IS_LINKED_PROJECT_PRESENT%>"/>');

            $("#extIdHidden").val(extIdsHidden);
            $("#licensInfoFileFormat").val(licenseInfoSelectedOutputFormat);
            $("#releaseRelationship").val(releaseRelationsHidden);
            $("#selectedProjectRelations").val(selectedProjectRelationsHidden);
            $("#isSubProjPresent").val(${not empty linkedProjectRelation});
            let actionUrl = $('#downloadLicenseInfoForm').attr('action');
            cleanedActionUrl = removeUnusedParam(actionUrl);
            $('#downloadLicenseInfoForm').attr('action', cleanedActionUrl);
            let actionUrlaft = $('#downloadLicenseInfoForm').attr('action');
            $('#downloadLicenseInfoForm').submit();
        } else {
            $('#downloadLicenseInfoForm').append('<input id="licensInfoFileFormat" type="hidden" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="isEmptyFile" type="hidden" value="Yes" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_EMPTY_FILE%>" />');
            if(outputFormat) {
                $("#licensInfoFileFormat").val(outputFormat);
            } else {
                $("#licensInfoFileFormat").val(licenseInfoSelectedOutputFormat);
            }
            let actionUrl = $('#downloadLicenseInfoForm').attr('action');
            cleanedActionUrl = removeUnusedParam(actionUrl);
            $('#downloadLicenseInfoForm').attr('action', cleanedActionUrl);
            let actionUrlaft = $('#downloadLicenseInfoForm').attr('action');
            $('#downloadLicenseInfoForm').submit();
        }
    }

    function downloadClearingReportOnly(isEmptyFile) {
        let licenseInfoSelectedOutputFormat = $('input[name="outputFormat"]:checked').val();
        if(isEmptyFile === "undefined" || !isEmptyFile) {
            let releaseRelations = [];
            let selectedProjectRelations = [];
            $.each($("input[name='releaseRelationSelection']:checked"), function(){
                releaseRelations.push($(this).val());
            });
            let releaseRelationsHidden = releaseRelations.join();

            $.each($("input[name='projectRelationSelection']:checked"), function(){
                selectedProjectRelations.push($(this).val());
            });
            var selectedProjectRelationsHidden = selectedProjectRelations.join();
            $('#downloadLicenseInfoForm').append('<input id="licensInfoFileFormat" type="hidden" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>" />');
            $('#downloadLicenseInfoForm').append('<input id="isSubProjPresent" type="hidden" name="<portlet:namespace/><%=PortalConstants.IS_LINKED_PROJECT_PRESENT%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="releaseRelationship" type="hidden" name="<portlet:namespace/><%=PortalConstants.SELECTED_PROJECT_RELEASE_RELATIONS%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="selectedProjectRelations" type="hidden" name="<portlet:namespace/><%=PortalConstants.SELECTED_PROJECT_RELATIONS%>"/>');
            $('#downloadLicenseInfoForm').append('<input id="template" type="hidden" name="<portlet:namespace/>tmplate"/>');
            selectedtemplate = $("input[name='org']:checked").val();
            $("#template").val(selectedtemplate);

            if(licenseInfoSelectedOutputFormat){
                $("#licensInfoFileFormat").val(licenseInfoSelectedOutputFormat);
            } else {
                $("#licensInfoFileFormat").val("DocxGenerator::REPORT");
            }
            $("#isSubProjPresent").val(${not empty linkedProjectRelation});
            $("#releaseRelationship").val(releaseRelationsHidden);
            $("#selectedProjectRelations").val(selectedProjectRelationsHidden);
            let actionUrl = $('#downloadLicenseInfoForm').attr('action');
            cleanedActionUrl = removeUnusedParam(actionUrl);
            $('#downloadLicenseInfoForm').attr('action', cleanedActionUrl);
            let actionUrlaft = $('#downloadLicenseInfoForm').attr('action');
            $('#downloadLicenseInfoForm').submit();
        } else {
            $('#downloadLicenseInfoForm').append('<input id="licensInfoFileFormat" type="hidden" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>" />');
            $('#downloadLicenseInfoForm').append('<input id="isEmptyFile" type="hidden" value="Yes" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_EMPTY_FILE%>" />');
            $("#licensInfoFileFormat").val(licenseInfoSelectedOutputFormat);
            let actionUrl = $('#downloadLicenseInfoForm').attr('action');
            cleanedActionUrl = removeUnusedParam(actionUrl);
            $('#downloadLicenseInfoForm').attr('action', cleanedActionUrl);
            let actionUrlaft = $('#downloadLicenseInfoForm').attr('action');
            $('#downloadLicenseInfoForm').submit();
        }
    }

    function checkIfSelectedAttachmentsExist() {
        var attchmntIdToFilename = [];
        var selectedAttachmentWithPathArray = [];
        var releaseRelations = [];
        var projectRelations = [];

        $.each($("input[name='releaseRelationSelection']:checked"), function(){
            releaseRelations.push($(this).val());
        });
        var selectedReleaseRelations = releaseRelations.join();

        $.each($("input[name='projectRelationSelection']:checked"), function(){
            projectRelations.push($(this).val());
        });
        var selectedProjectRelations = projectRelations.join();

        $.each($("input[name='<portlet:namespace/><%=PortalConstants.LICENSE_INFO_RELEASE_TO_ATTACHMENT%>']:checked"), function() {
            let selectedAttachmentIdsWithPath = $(this).val();
            let selectedAttachmentIdsWithPathArray = selectedAttachmentIdsWithPath.split(":");
            let attchmntId = selectedAttachmentIdsWithPathArray[selectedAttachmentIdsWithPathArray.length - 1];
            let projectReleaseReln = selectedAttachmentIdsWithPathArray[selectedAttachmentIdsWithPathArray.length - 2];
            if(selectedReleaseRelations.includes(projectReleaseReln)) {
                selectedAttachmentWithPathArray.push(selectedAttachmentIdsWithPath);
                let fileName = $(this).closest('td').next('td').next('td').text();
                let fnameToAttchmntId = fileName.trim()+":"+attchmntId;
                attchmntIdToFilename.push(fnameToAttchmntId);
            }
        });

        return jQuery.ajax({
            type: 'POST',
            url: '<%=checkIfAttachmentExists%>',
            async: false,
            data: {
                "<portlet:namespace/><%=PortalConstants.ATTACHMENT_ID_TO_FILENAMES%>": attchmntIdToFilename,
                "<portlet:namespace/><%=PortalConstants.SELECTED_ATTACHMENTS_WITH_FULL_PATH%>": selectedAttachmentWithPathArray,
                "<portlet:namespace/><%=PortalConstants.SELECTED_PROJECT_RELATIONS%>": selectedProjectRelations
            },
            success: function (data) {

            }
        });
    }

    function downloadLicenseInfo(attachmentNames) {
        var portletURL = Liferay.PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>');
        let licenseInfoSelectedOutputFormat;
        if(onlyClearingReport == 'true') {
            licenseInfoSelectedOutputFormat = "DocxGenerator::REPORT";
            portletURL.setParameter('<%=PortalConstants.PREPARE_LICENSEINFO_OBL_TAB%>', 'true');
        } else {
            licenseInfoSelectedOutputFormat = $('input[name="outputFormat"]:checked').val();
        }
        portletURL.setParameter('<%=PortalConstants.PROJECT_ID%>', '${project.id}');
        portletURL.setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_LICENSE_INFO%>');
        portletURL.setParameter('<%=PortalConstants.PROJECT_WITH_SUBPROJECT%>', '${projectOrWithSubProjects}');
        portletURL.setParameter('<%=PortalConstants.SHOW_ATTACHMENT_MISSING_ERROR%>', 'Yes');
        portletURL.setParameter('attachmentNames', attachmentNames);
        portletURL.setParameter('<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>', licenseInfoSelectedOutputFormat);
        sessionStorage.setItem("reloading", "true");
        window.location.href = portletURL.toString();
    }
});
</script>