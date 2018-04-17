<%--
  ~ Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
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


<portlet:defineObjects />
<liferay-theme:defineObjects />

<portlet:actionURL var="updateURL" name="update">
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}" />
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

    <core_rt:set  var="addMode"  value="${empty project.id}" />
</c:catch>

<%--These variables are used as a trick to allow referencing enum values in EL expressions below--%>
<c:set var="WRITE" value="<%=RequestedAction.WRITE%>"/>
<c:set var="DELETE" value="<%=RequestedAction.DELETE%>"/>
<c:set var="hasWritePermissions" value="${project.permissions[WRITE]}"/>

<core_rt:if test="${empty attributeNotFoundException}">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan"><sw360:out value="${project.name}"/></span>
    <span class="pull-right">
        <core_rt:if test="${not addMode}">
                   <input id="deleteProjectButton" type="button" class="addButton"
                          value="Delete <sw360:ProjectName project="${project}"/>"
                   <core_rt:if test="${ usingProjects.size()>0}"> disabled="disabled" title="Deletion is disabled as the project is used." </core_rt:if>
                   >
        </core_rt:if></span>
</p>


<div id="content" >
    <div class="container-fluid">
        <form  id="projectEditForm" name="projectEditForm" action="<%=updateURL%>" method="post" >
            <div id="myTab" class="row-fluid">
                <ul class="nav nav-tabs span2">
                    <li class="active"><a href="#tab-Summary">Summary</a></li>
                    <li><a href="#tab-Administration">Administration</a></li>
                    <li><a href="#tab-linkedProjects">Linked Releases And Projects</a></li>
                    <core_rt:if test="${not addMode}" >
                    <li><a href="#tab-Attachments">Attachments</a></li>
                    </core_rt:if>
                </ul>
                <div class="tab-content span10">
                    <div id="tab-Summary" class="tab-pane" >
                        <%@include file="/html/projects/includes/projects/basicInfo.jspf" %>
                        <core_rt:set var="documentName"><sw360:ProjectName project="${project}"/></core_rt:set>
                        <%@include file="/html/utils/includes/usingProjectsTable.jspf" %>
                        <%@include file="/html/utils/includes/usingComponentsTable.jspf"%>
                        <core_rt:set var="externalIdsSet" value="${project.externalIds.entrySet()}"/>
                        <%@include file="/html/utils/includes/editExternalIds.jsp" %>
                    </div>
                    <div id="tab-Administration" >
                        <%@include file="/html/projects/includes/projects/administrationEdit.jspf" %>
                    </div>
                    <div id="tab-linkedProjects" >
                        <%@include file="/html/projects/includes/linkedProjectsEdit.jspf" %>
                        <%@include file="/html/utils/includes/linkedReleasesEdit.jspf" %>
                    </div>
                    <core_rt:if test="${not addMode}" >
                    <div id="tab-Attachments" >
                        <%@include file="/html/utils/includes/editAttachments.jspf" %>
                    </div>
                    </core_rt:if>
                </div>
            </div>

            <core_rt:if test="${not addMode}" >
                <input type="button" id="formSubmit" value="Update Project" class="addButton">
            </core_rt:if>
            <core_rt:if test="${addMode}" >
                <input type="button" id="formSubmit" value="Add Project" class="addButton">
            </core_rt:if>
            <input id="cancelEditButton" type="button" value="Cancel" class="cancelButton">
            <div id="moderationRequestCommentDialog" style="display: none">
            <hr>
            <label class="textlabel stackedLabel">Comment your changes</label>
            <textarea form=projectEditForm name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="moderationCreationComment" placeholder="Leave a comment on your request"></textarea>
            <input type="button" class="addButton" id="moderationRequestCommentSendButton" value="Send moderation request">
            </div>

        </form>
    </div>
</div>


<jsp:include page="/html/projects/includes/searchProjects.jsp" />
<core_rt:set var="enableSearchForReleasesFromLinkedProjects" value="${true}" scope="request"/>
<jsp:include page="/html/utils/includes/searchReleases.jsp" />
<jsp:include page="/html/utils/includes/searchAndSelectUsers.jsp" />
<jsp:include page="/html/utils/includes/searchUsers.jsp" />

</core_rt:if>

<script>
var tabView;
var Y = YUI().use(
    'aui-tabview',
    function(Y) {
        tabView = new Y.TabView(
            {
                srcNode: '#myTab',
                stacked: true,
                type: 'tab'
            }
        ).render();
    }
);


require(['jquery', 'modules/sw360Validate', 'modules/confirm' ], function($, sw360Validate, confirm) {

    Liferay.on('allPortletsReady', function() {
        var contextpath = '<%=request.getContextPath()%>',
            deletionMessage;

        $('#moderationRequestCommentSendButton').on('click', submitForm);
        $('#cancelEditButton, #cancelEditButton2').on('click', cancel);
        $('#deleteProjectButton').on('click', openDeleteDialog);

        sw360Validate.validateWithInvalidHandlerNoIgnore('#projectEditForm');

        $('#formSubmit, #formSubmit2').click(
            function () {
                <core_rt:choose>
                    <core_rt:when test="${addMode || project.permissions[WRITE]}">
                        submitForm();
                    </core_rt:when>
                    <core_rt:otherwise>
                        showCommentField();
                    </core_rt:otherwise>
                </core_rt:choose>
            }
        );
    });

    function cancel() {
        deleteAttachmentsOnCancel();

        var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
            portletURL = Liferay.PortletURL.createURL(baseUrl)
        <core_rt:if test="${not addMode}">
                .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
        </core_rt:if>
        <core_rt:if test="${addMode}">
                .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>')
        </core_rt:if>
                .setParameter('<%=PortalConstants.PROJECT_ID%>', '${project.id}');
        window.location = portletURL.toString();
    }

    function deleteAttachmentsOnCancel() {
        $.ajax({
            type: 'POST',
            url: '<%=deleteAttachmentsOnCancelURL%>',
            cache: false,
            data: {
                "<portlet:namespace/><%=PortalConstants.DOCUMENT_ID%>": "${project.id}"
            },
        });
    }

    function openDeleteDialog() {
        var htmlDialog  = '' + '<div>' +
                          'Do you really want to delete the project <b><sw360:ProjectName project="${project}"/></b> ?' +
                          '<core_rt:if test="${not empty project.linkedProjects or not empty project.releaseIdToUsage or not empty project.attachments}" ><br/><br/>The project <b><sw360:ProjectName project="${project}"/></b> contains<br/><ul></core_rt:if>' +
                          '<core_rt:if test="${not empty project.linkedProjects}" ><li><sw360:out value="${project.linkedProjectsSize}"/> linked projects</li></core_rt:if>' +
                          '<core_rt:if test="${not empty project.releaseIdToUsage}" ><li><sw360:out value="${project.releaseIdToUsageSize}"/> linked releases</li></core_rt:if>'  +
                          '<core_rt:if test="${not empty project.attachments}" ><li><sw360:out value="${project.attachmentsSize}"/> attachments</li></core_rt:if>'  +
                          '<core_rt:if test="${not empty project.linkedProjects or not empty project.releaseIdToUsage or not empty project.attachments}" ></ul></core_rt:if>' +
                          '</div>'+
                          '<div ' + styleAsHiddenIfNeccessary(${project.permissions[DELETE] == true}) + '><hr><label class=\'textlabel stackedLabel\'>Comment your changes</label><textarea id=\'moderationDeleteCommentField\' class=\'moderationCreationComment\' placeholder=\'Comment on request...\'></textarea></div>';
        deleteConfirmed(htmlDialog, deleteProject);
    }

    function deleteProject() {
        var commentText_encoded = btoa($("#moderationDeleteCommentField").val());
        var baseUrl = '<%=deleteURL%>';
        var deleteURL = Liferay.PortletURL.createURL( baseUrl ).setParameter('<%=PortalConstants.MODERATION_REQUEST_COMMENT%>',commentText_encoded);
        window.location.href = deleteURL;
    }

    function focusOnCommentField() {
        $("#moderationRequestCommentField").focus();
        $("#moderationRequestCommentField").select();
    }

    function showCommentField() {
        $("#moderationRequestCommentDialog").show();
        $("#formSubmit, #formSubmit2").attr("disabled","disabled");
        focusOnCommentField();
    }

    function submitForm() {
        disableLicenseInfoHeaderTextIfNecessary();
        $('#projectEditForm').submit();
    }

    function disableLicenseInfoHeaderTextIfNecessary() {
        if($('#licenseInfoHeaderText').val() == $('#licenseInfoHeaderText').data("defaulttext")) {
            $('#licenseInfoHeaderText').prop('disabled', true);
        }
    }
});
</script>
