<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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


<liferay-ui:error key="custom_error" message="${cyclicError}" embed="false"/>
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

    <core_rt:set  var="addMode"  value="${empty project.id}" />
    <core_rt:set  var="pageName"  value="<%= request.getParameter("pagename") %>" />
    <core_rt:set var="isProjectObligationsEnabled" value='<%=PortalConstants.IS_PROJECT_OBLIGATIONS_ENABLED%>'/>
</c:catch>

<%--These variables are used as a trick to allow referencing enum values in EL expressions below--%>
<c:set var="WRITE" value="<%=RequestedAction.WRITE%>"/>
<c:set var="DELETE" value="<%=RequestedAction.DELETE%>"/>
<c:set var="hasWritePermissions" value="${project.permissions[WRITE]}"/>

<core_rt:if test="${empty attributeNotFoundException}">

<core_rt:set var="isObligationEnabled"  value="${isProjectObligationsEnabled and hasWritePermissions and isUserAtLeastClearingAdmin}" />
<core_rt:if test="${isObligationEnabled}">
    <core_rt:set var="isObligationPresent"  value="${not empty project.linkedObligations}" />
    <core_rt:if test="${isObligationPresent}">
    <jsp:useBean id="projectReleaseLicenseInfo" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult>" scope="request" />
    <jsp:useBean id="approvedObligationsCount" type="java.lang.Integer" scope="request"/>
    </core_rt:if>
</core_rt:if>

<div class="container" style="display: none;">
    <div class="row">
        <div class="col-3 sidebar">
            <div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab">Summary</a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Administration'}">active</core_rt:if>" href="#tab-Administration" data-toggle="list" role="tab">Administration</a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-linkedProjects'}">active</core_rt:if>" href="#tab-linkedProjects" data-toggle="list" role="tab">Linked Releases And Projects</a>
                <core_rt:if test="${not addMode}" >
                    <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Attachments'}">active</core_rt:if>" href="#tab-Attachments" data-toggle="list" role="tab">Attachments</a>
                    <core_rt:if test="${isObligationEnabled}">
                        <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Obligations'}">active</core_rt:if>" href="#tab-Obligations" data-toggle="list" role="tab">Obligations
                        <core_rt:if test="${isObligationPresent}">
                            <span id="obligtionsCount"
                                <core_rt:choose>
                                    <core_rt:when test="${approvedObligationsCount == 0}">
                                        class="badge badge-danger"
                                    </core_rt:when>
                                    <core_rt:when test="${approvedObligationsCount == project.linkedObligations.size()}">
                                        class="badge badge-success"
                                    </core_rt:when>
                                    <core_rt:otherwise>
                                        class="badge badge-light"
                                    </core_rt:otherwise>
                                </core_rt:choose>
                            >
                                ${approvedObligationsCount} / ${project.linkedObligations.size()}
                            </span>
                        </core_rt:if>
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
                                <button type="button" id="formSubmit" class="btn btn-primary">Create Project</button>
                            </core_rt:if>

                            <core_rt:if test="${not addMode}" >
                                <button type="button" id="formSubmit" class="btn btn-primary">Update Project</button>
                            </core_rt:if>
                        </div>

                        <core_rt:if test="${not addMode}" >
                            <div class="btn-group" role="group">
                                <button id="deleteProjectButton" type="button" class="btn btn-danger"
                                    <core_rt:if test="${ usingProjects.size()>0}"> disabled="disabled" title="Deletion is disabled as the project is used." </core_rt:if>
                                >Delete Project</button>
                            </div>
                        </core_rt:if>

                        <div class="btn-group" role="group">
                            <button id="cancelEditButton" type="button" class="btn btn-light">Cancel</button>
                        </div>
                        <div class="list-group-companion" data-belong-to="tab-Obligations">
                            <core_rt:if test="${not addMode and isObligationEnabled and isObligationPresent}">
                                <div class="nav nav-pills justify-content-center bg-light font-weight-bold" id="pills-tab" role="tablist">
                                    <a class="nav-item nav-link active" id="pills-obligations-tab" data-toggle="pill" href="#pills-obligationsView" role="tab" aria-controls="pills-obligationsView" aria-selected="true">
                                    <svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg> &nbsp;Edit Obligations</a>
                                    <a class="nav-item nav-link" id="pills-releases-tab" data-toggle="pill" href="#pills-releasesView" role="tab" aria-controls="pills-releasesView" aria-selected="false">
                                    <svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#view"/></svg> &nbsp;View by Releases</a>
                                    <span>
                                        <button id="saveObligationsButton" type="button" class="btn btn-primary" data-btn-type="saveObligations" disabled style="display: none;">Save Obligations</button>
                                    </span>
                                    <span id="saveObligationMessage" class="p-2 mb-0 alert" style="display: none;"></span>
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
                                <core_rt:if test="${isObligationEnabled}">
                                    <div id="tab-Obligations" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Obligations'}">active show</core_rt:if>">
                                        <%@include file="/html/projects/includes/projects/linkedObligations.jspf" %>
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
                    Delete Project?
                </h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
                <div class="modal-body">
                    <p>Do you really want to delete the project <b data-name="name"></b>?</p>
                    <div data-hide="hasNoDependencies">
                        <p>
                        This project <span data-name="name"></span> contains:
                        </p>
                    <ul>
                        <li data-hide="hasNoLinkedProjects"><span data-name="linkedProjects"></span> linked projects</li>
                        <li data-hide="hasNoLinkedReleases"><span data-name="linkedReleases"></span> linked releases</li>
                        <li data-hide="hasNoAttachments"><span data-name="attachments"></span> attachments</li>
                    </ul>
                </div>
                    <hr/>
                    <form>
                    <div class="form-group">
                            <label for="deleteProjectDialogComment">Please comment your changes</label>
                            <textarea id="deleteProjectDialogComment" class="form-control" data-name="comment" rows="4" placeholder="Comment your request..."></textarea>
                    </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-danger">Delete Project</button>
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
require(['jquery', 'modules/dialog', 'modules/listgroup', 'modules/validation', 'bridges/jquery-ui' ], function($, dialog, listgroup, validation) {
    document.title = "${project.name} - " + document.title;

    listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');

    validation.enableForm('#projectEditForm');
    validation.jumpToFailedTab('#projectEditForm');

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

    <core_rt:if test="${not addMode and isObligationEnabled and isObligationPresent}">
        $(document).ready(toggleSubmitAndDeleteButton(window.location.href.indexOf('#/tab-Obligations') != -1));
        $('#detailTab a').on('click', function(){
            toggleSubmitAndDeleteButton($(this).attr('href') === '#tab-Obligations');
        });
    </core_rt:if>
    function submitForm() {
        disableLicenseInfoHeaderTextIfNecessary();
        disableObligationsTextIfNecessary();
        $('#LinkedReleasesInfo tbody tr #mainlineState').prop('disabled', false);
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
            'Create moderation request',
            '<form>' +
                '<div class="form-group">' +
                    '<label for="deleteProjectDialogComment">Please comment your changes</label>' +
                    '<textarea form=projectEditForm name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="form-control" placeholder="Leave a comment on your request" data-name="comment"></textarea>' +
                '</div>' +
            '</form>',
            'Send moderation request',
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

    function toggleSubmitAndDeleteButton(hide) {
        if (hide) {
            $("#formSubmit").hide();
            $("#cancelEditButton").hide();
            $("#deleteProjectButton").hide();
        } else {
            $("#formSubmit").show();
            $("#cancelEditButton").show();
            $("#deleteProjectButton").show();
       }
    }
});
</script>
