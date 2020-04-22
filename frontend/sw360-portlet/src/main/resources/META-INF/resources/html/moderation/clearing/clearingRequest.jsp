<%--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ include file="/html/init.jsp"%>
<%@ include file="/html/utils/includes/logError.jspf" %>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<%@page import="org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest"%>
<%@page import="org.eclipse.sw360.datahandler.thrift.ClearingRequestState"%>
<%@page import="org.eclipse.sw360.portal.common.PortalConstants"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="clearingRequest" class="org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest" scope="request"/>
<jsp:useBean id="project" class="java.lang.String" scope="request"/>
<jsp:useBean id="writeAccessUser" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="isClearingExpert" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="printDate" class="java.util.Date"/>

<core_rt:if test="${not empty clearingRequest.id}">

<core_rt:set var="clearingRequestId"  value="${clearingRequest.id}" />

<portlet:actionURL var="updateClearingRequestURL" name="updateClearingRequest">
    <portlet:param name="<%=PortalConstants.CLEARING_REQUEST_ID%>" value="${clearingRequestId}"/>
</portlet:actionURL>

<portlet:renderURL var="cancelURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_VIEW%>" />
</portlet:renderURL>

<portlet:renderURL var="editlURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT_CLEARING_REQUEST%>" />
    <portlet:param name="<%=PortalConstants.CLEARING_REQUEST_ID%>" value="${clearingRequestId}"/>
</portlet:renderURL>

<portlet:resourceURL var="addCommentUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.ADD_COMMENT%>'/>
    <portlet:param name="<%=PortalConstants.CLEARING_REQUEST_ID%>" value="${clearingRequestId}"/>
</portlet:resourceURL>

<core_rt:set var="pageName"  value="<%= request.getParameter("pagename") %>" />
<core_rt:set var="user" value="<%=themeDisplay.getUser()%>"/>
<core_rt:set var="isRequestingUser" value='${clearingRequest.requestingUser eq user.emailAddress}'/>
<core_rt:set var="isClearingTeam" value='${clearingRequest.clearingTeam eq user.emailAddress or isClearingExpert}'/>
<core_rt:set var="isClosedOrRejected" value="${clearingRequest.clearingState eq 'CLOSED' or clearingRequest.clearingState eq 'REJECTED' or empty project}"/>
<core_rt:set var="isEditableForClearingTeam" value="${isClearingTeam and not isClosedOrRejected and pageName eq 'editClearingRequest'}"/>

<div class="container" id="clearing-request">
<div class="row portlet-toolbar">
    <div class="col-auto">
        <div class="btn-toolbar" role="toolbar">
            <core_rt:if test="${pageName eq 'detailClearingRequest' and not empty project and isClearingTeam and not isClosedOrRejected}">
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=editlURL%>' + window.location.hash">Edit Request</button>
                </div>
            </core_rt:if>
            <core_rt:if test="${isEditableForClearingTeam}">
	           <div class="btn-group" role="group">
	               <button type="button" id="formSubmit" class="btn btn-primary">Update Request</button>
	           </div>
	           <div class="btn-group" role="group">
                   <button type="button" class="btn btn-light" onclick="window.location.href='<%=cancelURL%>' + window.location.hash">Cancel</button>
               </div>
	        </core_rt:if>
        </div>
    </div>
    <div class="col portlet-title text-truncate" title="${clearingRequestId}">
        ${clearingRequestId}
    </div>
</div>

<div class="row">
    <div class="col">
        <div id="clearing-wizard" class="accordion">
            <div class="card">
                <form id="updateCRForm" name="updateCRForm" action="<%=updateClearingRequestURL%>" class="needs-validation" method="post" novalidate>
                    <div id="clearing-header-heading" class="card-header">
                        <h2 class="mb-0">
                            <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#clearing-header" aria-expanded="true" aria-controls="clearing-header">
                                <svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle"/></svg>
                                <core_rt:choose>
                                    <core_rt:when test="${not empty project}">
                                        Clearing request information for project: <a href="<sw360:DisplayProjectLink projectId="${clearingRequest.projectId}" bare="true" />"><sw360:out value="${project}" maxChar="50"/></a>
                                    </core_rt:when>
                                    <core_rt:otherwise>
                                        Clearing request information for DELETED project:
                                    </core_rt:otherwise>
                                </core_rt:choose>
                            </button>
                        </h2>
                    </div>
                    <div id="clearing-header" class="collapse show" aria-labelledby="clearing-header-heading" data-parent="#clearing-wizard">
                        <div class="card-body">
                            <div class="row">
                                <div class="col-6">
                                    <table class="table label-value-table mt-2" id="clearingRequestData">
                                        <thead>
                                            <tr>
                                                <th colspan="2">Clearing Request</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td><label class="form-group">Requesting user:</label></td>
                                                <td><sw360:DisplayUserEmail email="${clearingRequest.requestingUser}" /></td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group">Request submitted on:</label></td>
                                                <td>
                                                    <jsp:setProperty name="printDate" property="time" value="${clearingRequest.timestamp}"/>
                                                    <fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd"/>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group">Requested clearing date:</label></td>
                                                <td><sw360:out value="${clearingRequest.requestedClearingDate}" bare="true"/></td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group">Project BU:</label></td>
                                                <td><sw360:out value="${clearingRequest.projectBU}" bare="true"/></td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group">Requester comment:</label></td>
                                                <td><sw360:out value="${clearingRequest.requestingUserComment}" bare="true"/></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                                <div class="col-6">
                                    <table class="table label-value-table mt-2" id="clearingDecisionData">
                                        <thead>
                                            <tr>
                                                <th colspan="2">Clearing Decision</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td><label class="form-group">Status:</label></td>
                                                <td>
                                                <core_rt:choose>
                                                    <core_rt:when test="${isEditableForClearingTeam}">
                                                        <select class="form-control"
                                                            name="<portlet:namespace/><%=ClearingRequest._Fields.CLEARING_STATE%>">
                                                            <sw360:DisplayEnumOptions type="<%=ClearingRequestState.class%>" selected="${clearingRequest.clearingState}"/>
                                                        </select>
                                                        <small class="form-text">
                                                            <sw360:DisplayEnumInfo type="<%=ClearingRequestState.class%>"/>
                                                            Learn more about clearing request status.
                                                        </small>
                                                    </core_rt:when>
                                                    <core_rt:otherwise>
                                                        <sw360:DisplayEnum value="${clearingRequest.clearingState}"/>
                                                    </core_rt:otherwise>
                                                </core_rt:choose>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group">Clearing team:</label></td>
                                                <td><sw360:DisplayUserEmail email="${clearingRequest.clearingTeam}" /></td>
                                            </tr>
                                            <tr>
                                                <td>
                                                    <label class="form-group <core_rt:if test='${isEditableForClearingTeam}'> mandatory</core_rt:if>">Agreed clearing date:</label>
                                                </td>
                                                <td>
                                                <core_rt:choose>
                                                    <core_rt:when test="${isEditableForClearingTeam}">
                                                        <input class="form-control datepicker" required
                                                            name="<portlet:namespace/><%=ClearingRequest._Fields.AGREED_CLEARING_DATE%>" type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                            value="<sw360:out value="${clearingRequest.agreedClearingDate}"/>" placeholder="Agreed clearing date YYYY-MM-DD"/>
                                                    </core_rt:when>
                                                    <core_rt:otherwise>
                                                        <sw360:out value="${clearingRequest.agreedClearingDate}" bare="true"/>
                                                    </core_rt:otherwise>
                                                </core_rt:choose>
                                                </td>
                                            </tr>
                                            <core_rt:if test="${clearingRequest.isSetTimestampOfDecision()}">
                                                <tr>
                                                    <td><label class="form-group">Request closed on:</label></td>
                                                    <td>
                                                        <jsp:setProperty name="printDate" property="time" value="${clearingRequest.timestampOfDecision}"/>
                                                        <fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd"/>
                                                    </td>
                                                </tr>
                                            </core_rt:if>
                                            <tr>
                                                <td>
                                                    <label class="form-group">
                                                        Comment on clearing decision:
                                                    </label>
                                                </td>
                                                <td>
                                                <core_rt:choose>
                                                    <core_rt:when test="${isEditableForClearingTeam and empty clearingRequest.clearingTeamComment}">
                                                        <textarea name="<portlet:namespace/><%=ClearingRequest._Fields.CLEARING_TEAM_COMMENT%>" placeholder="Comment your decision..."
                                                            class="form-control"><sw360:out value="${clearingRequest.clearingTeamComment}" /></textarea>
                                                    </core_rt:when>
                                                    <core_rt:otherwise>
                                                        <sw360:out value="${clearingRequest.clearingTeamComment}" bare="true"/>
                                                    </core_rt:otherwise>
                                                </core_rt:choose>
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="card">
                <div id="clearing-comments-heading" class="card-header">
                    <h2 class="mb-0">
                        <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#clearing-comments" aria-expanded="false" aria-controls="clearing-comments">
                            <svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#comments"/></svg>
                                Clearing request comments: <span id="commentCount" class="badge badge-light text-dark"><sw360:out default="(0)" value="(${not empty clearingRequest.comments ? clearingRequest.comments.size() : '0'})"/></span>
                        </button>
                    </h2>
                </div>
                <div id="clearing-comments" class="collapse" aria-labelledby="clearing-comments-heading" data-parent="#clearing-wizard">
                    <div class="card-body">
                        <div class="m-auto">
                            <table class="table label-value-table mt-2" id="clearingCommentsTable">
                                <thead>
                                    <tr><th>Comments</th></tr>
                                <thead>
                                <tbody>
                                    <core_rt:forEach items="${clearingRequest.comments}" var="comment" varStatus="loop">
                                        <tr>
                                            <td>
                                                <core_rt:set var="by" value="${comment.commentedBy}" />
                                                <core_rt:set var="iconTextArray" value="${fn:split(by, '_|.')}"/>
                                                <core_rt:set var="iconText" value="NA"/>
                                                <core_rt:choose>
                                                    <core_rt:when test="${fn:length(iconTextArray) gt 2}">
                                                        <core_rt:set var="iconText" value="${fn:substring(iconTextArray[0], 0, 1)}${fn:substring(iconTextArray[1], 0, 1)}"/>
                                                    </core_rt:when>
                                                    <core_rt:otherwise>
                                                        <core_rt:set var="iconText" value="${fn:substring(iconTextArray[0], 0, 1)}${fn:substring(iconTextArray[0], 1, 2)}"/>
                                                    </core_rt:otherwise>
                                                </core_rt:choose>
                                                <jsp:setProperty name="printDate" property="time" value="${comment.commentedOn}"/>
                                                <div class="m-auto row">
                                                    <div class="col-0 user-icon user-icon-info text-uppercase"><span>${iconText}</span></div>
                                                    <div class="col-11">
                                                        <div class="comment-text"><sw360:out value="${comment.text}" stripNewlines="false" bare="true"/></div>
                                                            <footer class="blockquote-footer">by <cite><b><sw360:DisplayUserEmail email="${by}"/></b></cite> on <cite>
                                                                <b><fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd HH:mm"/></b></cite>
                                                            </footer>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                        </core_rt:forEach>
                                        <tr>
                                            <td>
                                                <core_rt:if test="${not empty project and (isClearingTeam or writeAccessUser)}">
	                                                <textarea id="clearingRequestComment" placeholder="Enter comment..." class="h-25 form-control"></textarea>
	                                                <div class="mt-3 btn-group" role="group">
	                                                    <button id="addComment" type="button" class="btn btn-success">Add comment</button>
	                                                </div>
	                                                <span id="addCommentStatusMessage" class="my-0 mb-0 alert alert-danger" style="display: none;"></span>
                                                </core_rt:if>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="dialogs auto-dialogs"></div>

<script>
require(['jquery', 'modules/dialog', 'modules/validation', 'bridges/jquery-ui' ], function($, dialog, validation) {
    validation.enableForm('#updateCRForm');
    validation.jumpToFailedTab('#updateCRForm');

    $('#formSubmit').click(
        function() {
            $('#updateCRForm').submit();
        }
    );

    $('.datepicker').datepicker({
        minDate: new Date(),
        changeMonth: true,
        changeYear: true,
        dateFormat: "yy-mm-dd"
    });

    /* Add event listener for saving the comment */
    $("#addComment").on("click", function (event) {

        if (!$.trim($("#clearingRequestComment").val())) {
            displayErrorMessage('Invalid comment!');
            return;
        }

        let comment = $("#clearingRequestComment").val(),
            email = '${user.emailAddress}',
            by = '<sw360:DisplayUserEmail email="${user.emailAddress}"/>',
            on = new Date().toISOString().slice(0,16).replace('T', ' '),
            count = Number($('#commentCount').text().replace(/[()]/g, '')) + 1,
            iconTextArray = email.split(/[_.]/),
            iconText = 'NA';

            if (iconTextArray.length > 2) {
                iconText = iconTextArray[0].substring(0,1) + iconTextArray[1].substring(0,1);
            } else {
                iconText = iconTextArray[0].substring(0,1) + iconTextArray[0].substring(1,2);
            }

        jQuery.ajax({
            type: 'POST',
            url: '<%=addCommentUrl%>',
            cache: false,
            data: {
                "<portlet:namespace/><%=PortalConstants.CLEARING_REQUEST_COMMENT%>": $("#clearingRequestComment").val()
            },
            success: function (data) {
                if (data.result == 'SUCCESS') {
                    $('#clearingRequestComment').val('');
                    $('#commentCount').text('('+count+')');
                    $('#clearingCommentsTable tbody tr:last')
                        .before('<tr><td>'
                                    +'<div class="m-auto row">'
                                    +'<div class="col-0 user-icon user-icon-info text-uppercase"><span>'+iconText+'</span></div>'
                                    +'<div class="col-11">'
                                    +'<div class="comment-text">' + comment +'<footer class="blockquote-footer">by <cite><b>'+by+'</b></cite> on <cite><b>'+on+'</b></cite></footer></div>'
                                    +'</div></div>'
                                +'</td></tr>');
                }
                else {
                    displayErrorMessage('Failed to add comment!');
                }
            },
            error: function () {
                displayErrorMessage('Error addimng comment to clearing request!');
            }
        });
    });

    function displayErrorMessage(message) {
        $("#addCommentStatusMessage").html(message).show().delay(5000).fadeOut();
    }
});
</script>
</core_rt:if>

