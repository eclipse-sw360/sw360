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
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.attachments.Attachment" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Release" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.portal.portlets.projects.ProjectPortlet" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.RequestedAction" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:actionURL var="updateComponentURL" name="updateComponent">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
</portlet:actionURL>

<portlet:renderURL var="addReleaseURL">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT_RELEASE%>"/>
</portlet:renderURL>

<portlet:actionURL var="deleteComponentURL" name="deleteComponent">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
</portlet:actionURL>

<portlet:actionURL var="deleteAttachmentsOnCancelURL" name='<%=PortalConstants.ATTACHMENT_DELETE_ON_CANCEL%>'>
</portlet:actionURL>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="component" class="org.eclipse.sw360.datahandler.thrift.components.Component" scope="request"/>
    <jsp:useBean id="documentID" class="java.lang.String" scope="request"/>
    <jsp:useBean id="documentType" class="java.lang.String" scope="request"/>

    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>"
                 scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="usingComponents" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.components.Component>"
                 scope="request"/>
</c:catch>

<%--These variables are used as a trick to allow referencing enum values in EL expressions below--%>
<c:set var="WRITE" value="<%=RequestedAction.WRITE%>"/>
<c:set var="DELETE" value="<%=RequestedAction.DELETE%>"/>
<c:set var="hasWritePermissions" value="${component.permissions[WRITE]}"/>

<%@include file="/html/utils/includes/logError.jspf" %>
<core_rt:if test="${empty attributeNotFoundException}">
    <core_rt:set var="softwarePlatformsAutoC" value='<%=PortalConstants.SOFTWARE_PLATFORMS%>'/>
    <core_rt:set var="componentCategoriesAutocomplete" value='<%=PortalConstants.COMPONENT_CATEGORIES%>'/>

    <core_rt:set var="componentDivAddMode" value="${empty component.id}"/>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

    <script src="<%=request.getContextPath()%>/js/releaseTools.js"></script>
    <!--include jQuery -->
    <script src="<%=request.getContextPath()%>/webjars/jquery/1.12.4/jquery.min.js" type="text/javascript"></script>
    <!--  needed for some dialogs mostly regarding attachments -->
    <script src="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.min.js"></script>
    <!-- needed in mapEdit.jspf -->
    <script src="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.js" type="text/javascript"></script>
    <div id="where" class="content1">
        <p class="pageHeader"><span class="pageHeaderBigSpan"><sw360:out value="${component.name}"/></span>
            <core_rt:if test="${not componentDivAddMode}">
                <input id="deleteComponentButton" type="button" class="addButton"
                       value="Delete <sw360:out value="${component.name}"/>"
                <core_rt:if test="${usingComponents.size()>0 or usingProjects.size()>0}"> disabled="disabled" title="Deletion is disabled as the component is used." </core_rt:if>
                <core_rt:if test="${component.releasesSize>0}"> disabled="disabled" title="Deletion is disabled as the component contains releases." </core_rt:if>
                >
            </core_rt:if>
        </p>
        <core_rt:if test="${not componentDivAddMode}">
            <core_rt:forEach items="${component.releases}" var="myRelease">
                <p><span onclick="window.location=createDetailURLfromReleaseId( '${myRelease.id}')"
                         class="clickAble"><sw360:ReleaseName release="${myRelease}"/></span></p>
            </core_rt:forEach>
            <input type="button" class="addButton" onclick="window.location.href='<%=addReleaseURL%>'" value="Add Release">
            <br>
            <hr>
            <input type="button" id="formSubmit" value="Update Component" class="addButton">
            <br>
        </core_rt:if>
        <core_rt:if test="${componentDivAddMode}">
            <input type="button" id="formSubmit" value="Add Component" class="addButton">
        </core_rt:if>
        <input type="button" value="Cancel" class="cancelButton" id="componentEditCancelButton">
        <div id="moderationRequestCommentDialog" style="display: none">
            <hr>
            <label class="textlabel stackedLabel">Comment your changes</label>
            <textarea form=componentEditForm name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="moderationCreationComment" placeholder="Leave a comment on your request"></textarea>
            <input type="button" class="addButton" id="moderationRequestCommentSendButton" value="Send moderation request">
        </div>
    </div>

    <%@ include file="/html/utils/includes/requirejs.jspf" %>
    <div id="editField" class="content2">
        <form id="componentEditForm" name="componentEditForm" action="<%=updateComponentURL%>" method="post">
            <%@include file="/html/components/includes/components/editBasicInfo.jspf" %>
            <core_rt:set var="externalIdsSet" value="${component.externalIds.entrySet()}"/>
            <%@include file="/html/utils/includes/editExternalIds.jsp" %>
            <core_rt:if test="${not componentDivAddMode}">
                <%@include file="/html/utils/includes/editAttachments.jspf" %>
            <core_rt:set var="documentName"><sw360:out value='${component.name}'/></core_rt:set>
            <%@include file="/html/utils/includes/usingProjectsTable.jspf" %>
            <%@include file="/html/utils/includes/usingComponentsTable.jspf"%>
            </core_rt:if>
        </form>
    </div>

    <jsp:include page="/html/utils/includes/searchAndSelectUsers.jsp" />
    <jsp:include page="/html/utils/includes/searchUsers.jsp" />
</core_rt:if>

<script>
    /* variables used in releaseTools.js ... */
    var releaseIdInURL = '<%=PortalConstants.RELEASE_ID%>',
        compIdInURL = '<%=PortalConstants.COMPONENT_ID%>',
        componentId = '${component.id}',
        pageName = '<%=PortalConstants.PAGENAME%>',
        pageDetail = '<%=PortalConstants.PAGENAME_EDIT_RELEASE%>',
        /* baseUrl also used in method in require block */
        baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';

require(['jquery', 'modules/sw360Validate', 'modules/autocomplete', 'modules/confirm' ], function($, sw360Validate, autocomplete, confirm) {

    Liferay.on('allPortletsReady', function() {
        var contextpath = '<%=request.getContextPath()%>',
            deletionMessage;

        $('#moderationRequestCommentSendButton').on('click', submitModerationRequest);
        $('#componentEditCancelButton').on('click', cancel);
        $('#deleteComponentButton').on('click', openDeleteDialog);

        autocomplete.prepareForMultipleHits('comp_platforms', ${softwarePlatformsAutoC});
        autocomplete.prepareForMultipleHits('comp_categories', ${componentCategoriesAutocomplete});

        sw360Validate.validateWithInvalidHandler('#componentEditForm');

        $('#formSubmit').click(
            function () {
                <core_rt:choose>
                <core_rt:when test="${componentDivAddMode || component.permissions[WRITE]}">
                $('#componentEditForm').submit();
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

        var portletURL = Liferay.PortletURL.createURL(baseUrl);
        <core_rt:choose>
            <core_rt:when test="${not empty component.id}">
                portletURL.setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
                          .setParameter('<%=PortalConstants.COMPONENT_ID%>', '${component.id}');
            </core_rt:when>
            <core_rt:otherwise>
                portletURL.setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>');
            </core_rt:otherwise>
        </core_rt:choose>
        window.location = portletURL.toString();
    }

    function deleteAttachmentsOnCancel() {
        $.ajax({
            type: 'POST',
            url: '<%=deleteAttachmentsOnCancelURL%>',
            cache: false,
            data: {
                "<portlet:namespace/><%=PortalConstants.DOCUMENT_ID%>": "${component.id}"
            },
        });
    }

    function openDeleteDialog() {
        var htmlDialog  = '' + '<div>' +
            'Do you really want to delete the component <b><sw360:out value="${component.name}"/></b> ?' +
            '<core_rt:if test="${not empty component.attachments}" ><br/><br/>The component <b><sw360:out value="${component.name}"/></b>contains<br/><ul><li><sw360:out value="${component.attachmentsSize}"/> attachments</li></ul></core_rt:if>' +
            '</div>' +
            '<div ' + styleAsHiddenIfNeccessary(${component.permissions[DELETE] == true}) + '><hr><label class=\'textlabel stackedLabel\'>Comment your changes</label><textarea id=\'moderationDeleteCommentField\' class=\'moderationCreationComment\' placeholder=\'Comment on request...\'></textarea></div>';
        deleteConfirmed(htmlDialog, deleteComponent);
    }

    function deleteComponent() {
        var commentText_encoded = btoa($("#moderationDeleteCommentField").val());
        var baseUrl = '<%=deleteComponentURL%>';
        var deleteURL = Liferay.PortletURL.createURL( baseUrl ).setParameter('<%=PortalConstants.MODERATION_REQUEST_COMMENT%>',commentText_encoded);
        window.location.href = deleteURL;
    }

    function focusOnCommentField() {
        $("#moderationRequestCommentField").focus();
        $("#moderationRequestCommentField").select();
    }

    function showCommentField() {
        $("#moderationRequestCommentDialog").show();
        $("#formSubmit").attr("disabled","disabled");
        focusOnCommentField();
    }

    function submitModerationRequest() {
        $('#componentEditForm').submit();
    }

});
</script>
