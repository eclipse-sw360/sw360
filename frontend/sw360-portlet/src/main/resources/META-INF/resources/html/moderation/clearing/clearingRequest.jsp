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
<jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
<jsp:useBean id="writeAccessUser" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="isClearingExpert" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="printDate" class="java.util.Date"/>

<core_rt:if test="${not empty clearingRequest.id}">

<core_rt:set var="clearingRequestId"  value="${clearingRequest.id}" />

<portlet:actionURL var="updateClearingRequestURL" name="updateClearingRequest">
    <portlet:param name="<%=PortalConstants.CLEARING_REQUEST_ID%>" value="${clearingRequestId}"/>
</portlet:actionURL>

<portlet:renderURL var="cancelURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_DETAIL_CLEARING_REQUEST%>" />
    <portlet:param name="<%=PortalConstants.CLEARING_REQUEST_ID%>" value="${clearingRequestId}"/>
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
<core_rt:set var="isProjectPresent" value='${not empty clearingRequest.projectId}'/>
<core_rt:set var="isRequestingUser" value='${clearingRequest.requestingUser eq user.emailAddress}'/>
<core_rt:set var="isClearingTeam" value='${clearingRequest.clearingTeam eq user.emailAddress or (isClearingExpert and writeAccessUser)}'/>
<core_rt:set var="isClosedOrRejected" value="${clearingRequest.clearingState eq 'CLOSED' or clearingRequest.clearingState eq 'REJECTED' or empty clearingRequest.projectId}"/>
<core_rt:set var="isEditableForClearingTeam" value="${isClearingTeam and not isClosedOrRejected and pageName eq 'editClearingRequest'}"/>

<div class="container" id="clearing-request">
<div class="row portlet-toolbar">
    <div class="col-auto">
        <div class="btn-toolbar" role="toolbar">
            <core_rt:if test="${pageName eq 'detailClearingRequest' and isProjectPresent and isClearingTeam and not isClosedOrRejected}">
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=editlURL%>' + window.location.hash"><liferay-ui:message key="edit.request" /></button>
                </div>
            </core_rt:if>
            <core_rt:if test="${isProjectPresent and isClosedOrRejected and isClearingTeam}">
                <div class="btn-group" role="group">
                    <button type="button" id="reOpenRequest" class="btn btn-primary"><liferay-ui:message key="reopen.request" /></button>
                </div>
            </core_rt:if>
            <core_rt:if test="${isEditableForClearingTeam}">
	           <div class="btn-group" role="group">
	               <button type="button" id="formSubmit" class="btn btn-primary"><liferay-ui:message key="update.request" /></button>
	           </div>
	           <div class="btn-group" role="group">
                   <button type="button" class="btn btn-light" onclick="window.location.href='<%=cancelURL%>' + window.location.hash"><liferay-ui:message key="cancel" /></button>
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
                                    <core_rt:when test="${isProjectPresent}">
                                        <liferay-ui:message key="clearing.request.information.for.project" />: <sw360:DisplayProjectLink project="${project}"/>
                                    </core_rt:when>
                                    <core_rt:otherwise>
                                        <liferay-ui:message key="clearing.request.information.for.deleted.project" />:
                                    </core_rt:otherwise>
                                </core_rt:choose>
                            </button>
                        </h2>
                    </div>
                    <div id="clearing-header" class="collapse show" aria-labelledby="clearing-header-heading" >
                        <div class="card-body">
                            <div class="row">
                                <div class="col-6">
                                    <table class="table label-value-table mt-2" id="clearingRequestData">
                                        <thead>
                                            <tr>
                                                <th colspan="2"><liferay-ui:message key="clearing.request" /></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td><label class="form-group"><liferay-ui:message key="requesting.user" />:</label></td>
                                                <td><sw360:DisplayUserEmail email="${clearingRequest.requestingUser}" /></td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group"><liferay-ui:message key="created.on" />:</label></td>
                                                <td>
                                                    <jsp:setProperty name="printDate" property="time" value="${clearingRequest.timestamp}"/>
                                                    <fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd"/>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group"><liferay-ui:message key="preferred.clearing.date" />:</label></td>
                                                <td><sw360:out value="${clearingRequest.requestedClearingDate}" bare="true"/></td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group"><liferay-ui:message key="business.area.line" />:</label></td>
                                                <td>
                                                <core_rt:choose>
                                                    <core_rt:when test="${isProjectPresent}">
                                                        <sw360:out value="${project.businessUnit}" bare="true"/>
                                                    </core_rt:when>
                                                    <core_rt:otherwise>
                                                        <sw360:out value="${clearingRequest.projectBU}" bare="true"/>
                                                    </core_rt:otherwise>
                                                </core_rt:choose>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group"><liferay-ui:message key="requester.comment" />:</label></td>
                                                <td><sw360:out value="${clearingRequest.requestingUserComment}" bare="true"/></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                                <div class="col-6">
                                    <table class="table label-value-table mt-2" id="clearingDecisionData">
                                        <thead>
                                            <tr>
                                                <th colspan="2"><liferay-ui:message key="clearing.decision" /></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td><label class="form-group"><liferay-ui:message key="request.status" />:</label></td>
                                                <td>
                                                <core_rt:choose>
                                                    <core_rt:when test="${isEditableForClearingTeam}">
                                                        <select class="form-control"
                                                            name="<portlet:namespace/><%=ClearingRequest._Fields.CLEARING_STATE%>">
                                                            <sw360:DisplayEnumOptions type="<%=ClearingRequestState.class%>" selected="${clearingRequest.clearingState}"/>
                                                        </select>
                                                        <small class="form-text">
                                                            <sw360:DisplayEnumInfo type="<%=ClearingRequestState.class%>"/>
                                                            <liferay-ui:message key="learn.more.about.clearing.request.status" />
                                                        </small>
                                                    </core_rt:when>
                                                    <core_rt:otherwise>
                                                        <sw360:DisplayEnum value="${clearingRequest.clearingState}"/>
                                                    </core_rt:otherwise>
                                                </core_rt:choose>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><label class="form-group"><liferay-ui:message key="clearing.team" />:</label></td>
                                                <td><sw360:DisplayUserEmail email="${clearingRequest.clearingTeam}" /></td>
                                            </tr>
                                            <tr>
                                                <td>
                                                    <label class="form-group <core_rt:if test='${isEditableForClearingTeam}'> mandatory</core_rt:if>"><liferay-ui:message key="agreed.clearing.date" />:</label>
                                                </td>
                                                <td>
                                                <core_rt:choose>
                                                    <core_rt:when test="${isEditableForClearingTeam}">
                                                        <input class="form-control datepicker" required
                                                            name="<portlet:namespace/><%=ClearingRequest._Fields.AGREED_CLEARING_DATE%>" type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                            value="<sw360:out value="${clearingRequest.agreedClearingDate}"/>" placeholder="<liferay-ui:message key='agreed.clearing.date.yyyy.mm.dd' />" />
                                                    </core_rt:when>
                                                    <core_rt:otherwise>
                                                        <sw360:out value="${clearingRequest.agreedClearingDate}" bare="true"/>
                                                    </core_rt:otherwise>
                                                </core_rt:choose>
                                                </td>
                                            </tr>
                                            <core_rt:if test="${clearingRequest.isSetTimestampOfDecision() and clearingRequest.timestampOfDecision > 0}">
                                                <tr>
                                                    <td><label class="form-group"><liferay-ui:message key="request.closed.on" />:</label></td>
                                                    <td>
                                                        <jsp:setProperty name="printDate" property="time" value="${clearingRequest.timestampOfDecision}"/>
                                                        <fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd"/>
                                                    </td>
                                                </tr>
                                            </core_rt:if>
                                            <tr>
                                                <td>
                                                    <label class="form-group">
                                                        <liferay-ui:message key="last.updated.on" />:
                                                    </label>
                                                </td>
                                                <td>
                                                    <core_rt:if test="${clearingRequest.isSetModifiedOn() and clearingRequest.modifiedOn > 0}">
                                                        <jsp:setProperty name="printDate" property="time" value="${clearingRequest.modifiedOn}"/>
                                                        <fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd"/>
                                                    </core_rt:if>
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
                                <liferay-ui:message key="clearing.request.comments" />: <span id="commentCount" class="badge badge-light text-dark"><sw360:out default="(0)" value="(${not empty clearingRequest.comments ? clearingRequest.comments.size() : '0'})"/></span>
                        </button>
                    </h2>
                </div>
                <div id="clearing-comments" class="collapse" aria-labelledby="clearing-comments-heading" >
                    <div class="card-body">
                        <div class="m-auto">
                            <table class="table label-value-table mt-2" id="clearingCommentsTable">
                                <thead>
                                    <tr><th><liferay-ui:message key="comments" /><input id="commentSearch" type="input" placeholder="<liferay-ui:message key="search" />" class="float-right"></th></tr>
                                <thead>
                                <tbody>
                                        <tr>
                                            <td>
                                                <core_rt:if test="${isProjectPresent and (isClearingTeam or isRequestingUser)}">
	                                                <textarea id="clearingRequestComment" placeholder="<liferay-ui:message key='enter.comment' />..." class="h-25 form-control"></textarea>
	                                                <div class="my-2 btn-group" role="group">
	                                                    <button id="addCommentBtn" type="button" class="btn btn-success"><liferay-ui:message key='add.comment' /></button>
	                                                </div>
	                                                <span id="addCommentStatusMessage" class="py-2 my-2 alert alert-danger" style="display: none;"></span>
                                                </core_rt:if>
                                            </td>
                                        </tr>
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
                                                        <div class="comment-text"><core_rt:if test="${comment.autoGenerated}"> <liferay-ui:message key="this.is.auto.generated.comment" /></core_rt:if>${comment.text}</div>
                                                            <footer class="blockquote-footer"><liferay-ui:message key="by" /> <cite><b><sw360:DisplayUserEmail email="${by}"/></b></cite> <liferay-ui:message key="on" /> <cite>
                                                                <b><fmt:formatDate value="${printDate}" pattern="yyyy-MM-dd HH:mm"/></b></cite>
                                                            </footer>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                        </core_rt:forEach>
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
require(['jquery', 'modules/dialog', 'modules/validation', 'modules/button', 'bridges/jquery-ui' ], function($, dialog, validation, button) {
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

    $("#commentSearch").on("keyup", function(event) {
        let value = this.value.toLowerCase().trim();
        if (!value) {
            $("#clearingCommentsTable tbody").find('tr').each(function(index) {
                $(this).show();
            });
            return;
        }
        $("#clearingCommentsTable tbody tr").first().hide();
        $("#clearingCommentsTable tbody tr").not(":first").each(function(index) {
            let id = $(this).find("td").first().text().toLowerCase();
            $(this).toggle(id.indexOf(value) !== -1);
        });
    });

    /* Add event listener for saving the comment */
    $("#addCommentBtn").on("click", function (event) {

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
            iconText = 'NA',
            $button = $("#addCommentBtn");

            if (iconTextArray.length > 2) {
                iconText = iconTextArray[0].substring(0,1) + iconTextArray[1].substring(0,1);
            } else {
                iconText = iconTextArray[0].substring(0,1) + iconTextArray[0].substring(1,2);
            }
            button.wait($button);
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
                    $('#clearingCommentsTable tbody tr:first')
                        .after('<tr><td>'
                                    +'<div class="m-auto row">'
                                    +'<div class="col-0 user-icon user-icon-info text-uppercase"><span>'+iconText+'</span></div>'
                                    +'<div class="col-11">'
                                    +'<div class="comment-text">' + comment +'<footer class="blockquote-footer">'
                                    +' <liferay-ui:message key="by" /> <cite><b>' + by + '</b></cite>'
                                    +' <liferay-ui:message key="on" /> <cite><b>' + on + '</b></cite></footer></div>'
                                    +'</div></div>'
                                +'</td></tr>');
                }
                else {
                    displayErrorMessage('<liferay-ui:message key="failed.to.add.comment" />');
                }
                button.finish($button);
            },
            error: function () {
                displayErrorMessage('<liferay-ui:message key="error.adding.comment.to.clearing.request" />!');
                button.finish($button);
            }
        });
    });

    /* Add event listener for re opening the CR */
    $("#reOpenRequest").on("click", function (event) {
        $dialog = dialog.confirm('info',
                    'question-circle',
                    '<liferay-ui:message key="reopen.clearing.request" />?',
                    '<p><liferay-ui:message key="clearing.request.will.be.reopened.in.new.state" /></p>',
                    '<liferay-ui:message key="reopen.clearing.request" />',
                    {},
                function(submit, callback) {
                    $('#updateCRForm').append('<input type="hidden" value="true" name="<portlet:namespace/><%=PortalConstants.RE_OPEN_REQUEST%>"/>');
                    $('#updateCRForm').submit();
        });
    });

    function displayErrorMessage(message) {
        $("#addCommentStatusMessage").html(message).show().delay(5000).fadeOut();
    }
});
</script>
</core_rt:if>

