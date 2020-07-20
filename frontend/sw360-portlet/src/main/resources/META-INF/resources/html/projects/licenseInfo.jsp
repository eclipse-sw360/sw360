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
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant" %>

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

<div class="dialogs auto-dialogs">
<div id="downloadLicenseInfoDialog" class="modal fade" tabindex="-1" role="dialog">
<div class="modal-dialog modal-lg modal-dialog-centered modal-info" role="document">
  <!-- <div class="modal-dialog" role="document"> -->
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><liferay-ui:message key="select.other.options" /></h5>
        <button id="closeModalButton" type="button" class="close" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <div class="modal-body"
                    style="position: relative; overflow-y: auto; max-height: 400px;">
                    <c:if test="${not empty relations}">
                        <div class="form-group form-check">
                            <label for="projectRelation"
                                class="font-weight-bold h3">Uncheck project
                                release relationships to be excluded:</label>
                            <c:forEach var="projectReleaseRelation" items="${relations}">
                                <div class="checkbox form-check">
                                    <label> <input name="releaseRelationSelection" type="checkbox"
                                        <c:if test = "${empty usedProjectReleaseRelations}">checked="checked"</c:if>
                                        <c:if test = "${not empty usedProjectReleaseRelations and (fn:contains(usedProjectReleaseRelations, projectReleaseRelation))}">checked="checked"</c:if>
                                        value="${projectReleaseRelation}">
                                        <sw360:DisplayEnum value='${projectReleaseRelation}' bare="true" /> </input>
                                    </label>
                                </div>
                            </c:forEach>
                        </div>
                    </c:if>
                    <c:if test="${not empty linkedProjectRelation}">
                        <div class="form-group form-check">
                            <label for="projectRelation"
                                class="font-weight-bold h3">Uncheck Linked Project
                                Relationships to be excluded:</label>
                            <c:forEach var="projectRelation" items="${linkedProjectRelation}">
                                <div class="checkbox form-check">
                                    <label> <input name="projectRelationSelection" type="checkbox"
                                        <c:if test = "${usedLinkedProjectRelation == null}">checked="checked"</c:if>
                                        <c:if test = "${usedLinkedProjectRelation != null and (fn:contains(usedLinkedProjectRelation, projectRelation))}">checked="checked"</c:if>
                                        value="${projectRelation}">
                                        <sw360:DisplayEnum value='${projectRelation}' bare="true" /> </input>
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
						    <label for="outputFormatLabel" class="licenseInfoOpFormat font-weight-bold h3"><liferay-ui:message key="select.output.format" />:</label>
						    <sw360:DisplayOutputFormats options='${licenseInfoOutputFormats}' variantToSkip="<%=OutputFormatVariant.REPORT%>"/>
					    </div>
                    </c:if>
	  </div>
      <div class="modal-footer">
        <button id="downloadFileModal" type="button" value="Download" class="btn btn-primary"><liferay-ui:message key="download" /></button>
        <button type="button" class="btn btn-secondary" data-dismiss="modal"><liferay-ui:message key="close" /></button>
      </div>
    </div>
  </div>
</div>
</div>

<script>
require(['jquery', 'modules/dialog'], function($, dialog) {
    let onlyClearingReport = '${onlyClearingReport}';
    $('#selectVariantAndDownload').on('click', selectVariantAndSubmit);
    function selectVariantAndSubmit(){
        dialog.open('#downloadLicenseInfoDialog','',function(submit, callback) {
            callback(true);
        });
    }
    if(onlyClearingReport == 'true') {
        $('#downloadFileModal').on('click', downloadClearingReportOnly);
    } else {
        $('#downloadFileModal').on('click', downloadFile);
    }

    function downloadFile(){
        var licenseInfoSelectedOutputFormat = $('input[name="outputFormat"]:checked').val();
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

        $('#downloadLicenseInfoForm').submit();
    }

    function downloadClearingReportOnly() {
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
        $('#downloadLicenseInfoForm').append('<input id="licensInfoFileFormat" type="hidden" value="DocxGenerator::REPORT" name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>" />');
        $('#downloadLicenseInfoForm').append('<input id="isSubProjPresent" type="hidden" name="<portlet:namespace/><%=PortalConstants.IS_LINKED_PROJECT_PRESENT%>"/>');
        $('#downloadLicenseInfoForm').append('<input id="releaseRelationship" type="hidden" name="<portlet:namespace/><%=PortalConstants.SELECTED_PROJECT_RELEASE_RELATIONS%>"/>');
        $('#downloadLicenseInfoForm').append('<input id="selectedProjectRelations" type="hidden" name="<portlet:namespace/><%=PortalConstants.SELECTED_PROJECT_RELATIONS%>"/>');
        $("#isSubProjPresent").val(${not empty linkedProjectRelation});
        $("#releaseRelationship").val(releaseRelationsHidden);
        $("#selectedProjectRelations").val(selectedProjectRelationsHidden);
        $('#downloadLicenseInfoForm').submit();
    }
});
</script>