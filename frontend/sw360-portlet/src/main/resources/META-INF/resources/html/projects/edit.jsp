<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.common.SW360Constants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.attachments.Attachment" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Release" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.moderation.DocumentType" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.RequestedAction" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus" %>

<%@ include file="/html/init.jsp"%>
<%@ include file="/html/utils/includes/logError.jspf" %>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:resourceURL var="obligationediturl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LOAD_OBLIGATIONS_EDIT%>"/>
    <portlet:param name="<%=PortalConstants.DOCUMENT_ID%>" value="${project.id}"/>
</portlet:resourceURL>

<liferay-ui:error key="custom_error" message="${fn:escapeXml(cyclicError)}" embed="false"/>
<portlet:defineObjects />
<liferay-theme:defineObjects />

<portlet:actionURL var="updateURL" name="update">
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}" />
    <c:if test="${not empty sourceProjectId}">
        <portlet:param name="sourceProjectId" value="${sourceProjectId}" />
    </c:if>
</portlet:actionURL>

<portlet:actionURL var="deleteAttachmentsOnCancelURL" name='<%=PortalConstants.ATTACHMENT_DELETE_ON_CANCEL%>'>
</portlet:actionURL>

<portlet:actionURL var="deleteURL" name="delete">
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/>
</portlet:actionURL>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request" />
    <jsp:useBean id="documentID" class="java.lang.String" scope="request" />
    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>" scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"  scope="request"/>
    <jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink>"  scope="request"/>
    <jsp:useBean id="attachments" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment>" scope="request"/>
    <jsp:useBean id="defaultLicenseInfoHeaderText" class="java.lang.String" scope="request" />
    <jsp:useBean id="defaultObligationsText" class="java.lang.String" scope="request" />
    <jsp:useBean id="isUserAtLeastClearingAdmin" type="java.lang.Boolean" scope="request" />
    <jsp:useBean id="customFields" type="java.util.List<org.eclipse.sw360.portal.common.customfields.CustomField>" scope="request"/>

    <core_rt:set var="addMode"  value="${empty project.id}" />
    <core_rt:set var="pageName"  value="<%= request.getParameter("pagename") %>" />
    <core_rt:set var="isProjectObligationsEnabled" value='<%=PortalConstants.IS_PROJECT_OBLIGATIONS_ENABLED%>'/>
    <core_rt:set var="tagAutocomplete" value='<%=PortalConstants.PREDEFINED_TAGS%>'/>
</c:catch>

<%--These variables are used as a trick to allow referencing enum values in EL expressions below--%>
<c:set var="WRITE" value="<%=RequestedAction.WRITE%>"/>
<c:set var="DELETE" value="<%=RequestedAction.DELETE%>"/>
<c:set var="hasWritePermissions" value="${project.permissions[WRITE]}"/>

<core_rt:if test="${empty attributeNotFoundException}">

<core_rt:set var="isObligationPresent" value="${not empty project.releaseIdToUsage}" />
<core_rt:set var="isProjectObligationsEnabled"  value="${isProjectObligationsEnabled and hasWritePermissions}" />

<div class="container" style="display: none;">
    <div class="row">
        <div class="col-3 sidebar">
            <div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab"><liferay-ui:message key="summary" /></a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Administration'}">active</core_rt:if>" href="#tab-Administration" data-toggle="list" role="tab"><liferay-ui:message key="administration" /></a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-linkedProjects'}">active</core_rt:if>" href="#tab-linkedProjects" data-toggle="list" role="tab"><liferay-ui:message key="linked.releases.and.projects" /></a>
                <core_rt:if test="${not addMode}" >
                    <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Attachments'}">active</core_rt:if>" href="#tab-Attachments" data-toggle="list" role="tab"><liferay-ui:message key="attachments" /></a>
                    <core_rt:if test="${isProjectObligationsEnabled}">
                        <a id="obligationCountBadge" class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Obligations'}">active</core_rt:if>" href="#tab-Obligations" data-toggle="list" role="tab"><liferay-ui:message key="obligations" />
                        </a>
                    </core_rt:if>
                </core_rt:if>
            </div>
        </div>
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <core_rt:if test="${addMode}" >
                                <button type="button" id="formSubmit" class="btn btn-primary"><liferay-ui:message key="create.project" /></button>
                            </core_rt:if>

                            <core_rt:if test="${not addMode}" >
                                <button type="button" id="formSubmit" class="btn btn-primary"><liferay-ui:message key="update.project" /></button>
                            </core_rt:if>
                        </div>

                        <core_rt:if test="${not addMode}" >
                            <div class="btn-group" role="group">
                                <button id="deleteProjectButton" type="button" class="btn btn-danger"
                                    <core_rt:if test="${ usingProjects.size()>0}"> disabled="disabled" title="<liferay-ui:message key="deletion.is.disabled.as.the.project.is.used" />" </core_rt:if>
                                ><liferay-ui:message key="delete.project" /></button>
                            </div>
                        </core_rt:if>

                        <div class="btn-group" role="group">
                            <button id="cancelEditButton" type="button" class="btn btn-light"><liferay-ui:message key="cancel" /></button>
                        </div>
                        <div class="list-group-companion" data-belong-to="tab-Obligations">
                            <core_rt:if test="${not addMode and isProjectObligationsEnabled and isObligationPresent}">
                                <div class="nav nav-pills justify-content-center bg-light font-weight-bold" id="pills-tab" role="tablist">
                                    <a class="nav-item nav-link active" id="pills-obligations-tab" data-toggle="pill" href="#pills-obligationsView" role="tab" aria-controls="pills-obligationsView" aria-selected="true">
                                    <liferay-ui:message key="obligations.view" /></a>
                                    <a class="nav-item nav-link" id="pills-releases-tab" data-toggle="pill" href="#pills-releasesView" role="tab" aria-controls="pills-releasesView" aria-selected="false">
                                    <liferay-ui:message key="release.view" /></a>
                                </div>
                            </core_rt:if>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="${sw360:printProjectName(project)}">
                    <sw360:ProjectName project="${project}"/>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <form  id="projectEditForm" name="projectEditForm" action="<%=updateURL%>" class="needs-validation" method="post" novalidate
                        data-delete-url="<%=deleteURL%>"
                        data-comment-parameter-name="<%=PortalConstants.MODERATION_REQUEST_COMMENT%>"
                        data-linked-projects="${project.linkedProjectsSize}"
                        data-linked-releases="${project.releaseIdToUsageSize}"
                        data-attachments="${project.attachmentsSize}"
                    >
                        <div class="tab-content">
                            <div id="tab-Summary" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Summary'}">active show</core_rt:if>" >
                                <%@include file="/html/projects/includes/projects/basicInfo.jspf" %>

                                <core_rt:set var="externalIdsSet" value="${project.externalIds.entrySet()}"/>
                                <core_rt:set var="externalIdKeys" value="<%=PortalConstants.PROJECT_EXTERNAL_ID_KEYS%>"/>
                                <%@include file="/html/utils/includes/editExternalIds.jsp" %>

                                <core_rt:set var="additionalDataSet" value="${project.additionalData.entrySet()}"/>
                                <%@include file="/html/utils/includes/editAdditionalData.jsp" %>

                                <core_rt:set var="documentName"><sw360:ProjectName project="${project}"/></core_rt:set>
                                <%@include file="/html/utils/includes/usingProjectsTable.jspf" %>
                                <%@include file="/html/utils/includes/usingComponentsTable.jspf"%>
                            </div>
                            <div id="tab-Administration" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Administration'}">active show</core_rt:if>">
                                <%@include file="/html/projects/includes/projects/administrationEdit.jspf" %>
                            </div>
                            <div id="tab-linkedProjects" class="tab-pane <core_rt:if test="${selectedTab == 'tab-linkedProjects'}">active show</core_rt:if>">
                                <%@include file="/html/projects/includes/linkedProjectsEdit.jspf" %>
                                <%@include file="/html/utils/includes/linkedReleasesEdit.jspf" %>
                            </div>
                            <core_rt:if test="${not addMode}" >
                                <div id="tab-Attachments" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Attachments'}">active show</core_rt:if>">
                                    <%@include file="/html/utils/includes/editAttachments.jspf" %>
                                </div>
                                <core_rt:if test="${isProjectObligationsEnabled}">
                                    <div id="tab-Obligations" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Obligations'}">active show</core_rt:if>">
                                        <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
                                    </div>
                                </core_rt:if>
                            </core_rt:if>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs">
    <div id="deleteProjectDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">
                    <clay:icon symbol="question-circle" />
                    <liferay-ui:message key="delete.project" />?
                </h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
                <div class="modal-body">
                    <p><liferay-ui:message key="do.you.really.want.to.delete.the.project.x" /></p>
                    <div data-hide="hasNoDependencies">
                        <p>
                        <liferay-ui:message key="this.project.x.contains" />
                        </p>
                    <ul>
                        <li data-hide="hasNoLinkedProjects"><span data-name="linkedProjects"></span> <liferay-ui:message key="linked.projects" /></li>
                        <li data-hide="hasNoLinkedReleases"><span data-name="linkedReleases"></span> <liferay-ui:message key="linked.releases" /></li>
                        <li data-hide="hasNoAttachments"><span data-name="attachments"></span> <liferay-ui:message key="attachments" /></li>
                    </ul>
                </div>
                    <hr/>
                    <form>
                    <div class="form-group">
                            <label for="deleteProjectDialogComment"><liferay-ui:message key="please.comment.your.changes" /></label>
                            <textarea id="deleteProjectDialogComment" class="form-control" data-name="comment" rows="4" placeholder="<liferay-ui:message key="comment.your.request" />"></textarea>
                    </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="cancel" /></button>
                    <button type="button" class="btn btn-danger"><liferay-ui:message key="delete.project" /></button>
                </div>
            </div>
        </div>
    </div>
</div>

<core_rt:set var="enableSearchForReleasesFromLinkedProjects" value="${true}" scope="request"/>

<jsp:include page="/html/projects/includes/searchProjects.jsp" />
<jsp:include page="/html/utils/includes/searchReleases.jsp" />
<jsp:include page="/html/utils/includes/searchAndSelectUsers.jsp" />
<jsp:include page="/html/utils/includes/searchUsers.jsp" />

</core_rt:if>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
require(['jquery', 'modules/autocomplete', 'modules/dialog', 'modules/listgroup', 'modules/validation', 'bridges/jquery-ui' ], function($, autocomplete, dialog, listgroup, validation) {
    document.title = $("<span></span>").html("<sw360:ProjectName project="${project}"/> - " + document.title).text();

    listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');

    validation.enableForm('#projectEditForm');
    validation.jumpToFailedTab('#projectEditForm');

    autocomplete.prepareForMultipleHits('proj_tag', ${tagAutocomplete});

    $('#formSubmit').click(
        function () {
            <core_rt:choose>
                <core_rt:when test="${addMode || project.permissions[WRITE]}">
                    submitForm();
                </core_rt:when>
                <core_rt:otherwise>
                    showCommentDialog();
                </core_rt:otherwise>
            </core_rt:choose>
        }
    );
    $('#cancelEditButton').on('click', cancel);
    $('#deleteProjectButton').on('click', deleteProject);

    function submitForm() {
        disableLicenseInfoHeaderTextIfNecessary();
        disableObligationsTextIfNecessary();
        $('#LinkedReleasesInfo tbody tr #mainlineState').prop('disabled', false);
        <core_rt:if test = "${(project.clearingState eq 'CLOSED') && (isUserAdmin != 'Yes') && isProjectMember && not addMode}">
            $("form#projectEditForm select").prop("disabled", false);
            $("form#projectEditForm input").prop("disabled", false);
        </core_rt:if>
        <core_rt:if test="${not addMode and isProjectObligationsEnabled and isObligationPresent}">
            $('#updateObligationsButtonHidden').trigger('click');
        </core_rt:if>
        <core_rt:if test="${not addMode and isProjectObligationsEnabled}">
            $('#project-updateObligationsButtonHidden').trigger('click');
        </core_rt:if>
        <core_rt:if test="${not addMode and isProjectObligationsEnabled}">
            $('#comp-updateObligationsButtonHidden').trigger('click');
        </core_rt:if>
        <core_rt:if test="${not addMode and isProjectObligationsEnabled}">
            $('#org-updateObligationsButtonHidden').trigger('click');
        </core_rt:if>
        $('#projectEditForm').submit();
    }

    function cancel() {
        $.ajax({
            type: 'POST',
            url: '<%=deleteAttachmentsOnCancelURL%>',
            cache: false,
            data: {
                "<portlet:namespace/><%=PortalConstants.DOCUMENT_ID%>": "${project.id}"
            }
        }).always(function() {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
                portletURL = Liferay.PortletURL.createURL(baseUrl)
            <core_rt:if test="${not addMode}">
                    .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
            </core_rt:if>
            <core_rt:if test="${addMode}">
                    .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>')
            </core_rt:if>
                    .setParameter('<%=PortalConstants.PROJECT_ID%>', '${project.id}');

            window.location = portletURL.toString() + window.location.hash;
        });
    }

    function deleteProject() {
        var $dialog,
            data = $('#projectEditForm').data(),
            linkedProjectsSize = data.linkedProjects,
            linkedReleasesSize = data.linkedReleases,
            attachmentsSize = data.attachments;

        function deleteProjectInternal() {
            var baseUrl = data.deleteUrl,
                deleteURL = Liferay.PortletURL.createURL( baseUrl ).setParameter(data.commentParameterName, btoa($("#moderationDeleteCommentField").val()));
            window.location.href = deleteURL;
        }

        $dialog = dialog.open('#deleteProjectDialog', {
            name: name,
            linkedProjects: linkedProjectsSize,
            linkedReleases: linkedReleasesSize,
            attachments: attachmentsSize,
            hasNoDependencies: linkedProjectsSize == 0 && linkedReleasesSize == 0 && attachmentsSize == 0,
            hasNoLinkedProjects: linkedProjectsSize == 0,
            hasNoLinkedReleases: linkedReleasesSize == 0,
            hasNoAttachments: attachmentsSize == 0
        }, function(submit, callback) {
            deleteProjectInternal();
        });
    }

    function showCommentDialog() {
        var $dialog;

        // validate first to be sure that form can be submitted
        if(!validation.validate('#projectEditForm')) {
            return;
        }

        $dialog = dialog.confirm(
            null,
            'pencil',
            '<liferay-ui:message key="create.moderation.request" />',
            '<form>' +
                '<div class="form-group">' +
                    '<label for="deleteProjectDialogComment"><liferay-ui:message key="please.comment.your.changes" /></label>' +
                    '<textarea form=projectEditForm name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="form-control" placeholder="<liferay-ui:message key="leave.a.comment.on.your.request" />" data-name="comment"></textarea>' +
                '</div>' +
            '</form>',
            '<liferay-ui:message key="send.moderation.request" />',
            {
                comment: ''
            },
            submitForm
        );
        $dialog.$.on('shown.bs.modal', function() {
            $dialog.$.find('textarea').focus();
        });
    }

    function disableLicenseInfoHeaderTextIfNecessary() {
        if($('#licenseInfoHeaderText').val() == $('#licenseInfoHeaderText').data("defaulttext")) {
            $('#licenseInfoHeaderText').prop('disabled', true);
        }
    }

    function disableObligationsTextIfNecessary() {
        if($('#obligationsText').val() == $('#obligationsText').data("defaulttext")) {
            $('#obligationsText').prop('disabled', true);
        }
    }

    <core_rt:if test = "${(project.clearingState eq 'CLOSED') && (isUserAdmin != 'Yes') && isProjectMember && not addMode}">
        $("form#projectEditForm :input:not([name^='_sw360_portlet_projects_externalIdKey'], [name^='_sw360_portlet_projects_externalIdValue'], [name='_sw360_portlet_projects_ENABLE_SVM'], [name='_sw360_portlet_projects_ENABLE_VULNERABILITIES_DISPLAY'], [type='hidden'])").prop("disabled", true);
        $("form#projectEditForm select").prop("disabled", true);
        $("form#projectEditForm button").prop("disabled", true);
        $("form#projectEditForm button[id='add-external-id']").prop("disabled", false);
        $("form#projectEditForm button[id='formSubmit']").prop("disabled", false);
    </core_rt:if>

    <core_rt:if test="${isProjectObligationsEnabled}">
      $.ajax({
        url: '<%=obligationediturl%>',
        type: "GET",
        success: function(result){
            $("#tab-Obligations").html("").append(result);
      }});
    </core_rt:if>
});
</script>
