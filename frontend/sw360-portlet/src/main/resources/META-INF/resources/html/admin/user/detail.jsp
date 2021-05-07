<%--
  ~ Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags"%>

<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil"%>
<%@ page import="javax.portlet.PortletRequest"%>
<portlet:defineObjects />
<liferay-theme:defineObjects />

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="userObj" class="org.eclipse.sw360.datahandler.thrift.users.User" scope="request" />
</c:catch>

<%@include file="/html/utils/includes/logError.jspf"%>

<portlet:renderURL var="editUserURL">
    <portlet:param name="<%=PortalConstants.USER_EMAIL%>" value="${userObj.email}" />
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>" />
</portlet:renderURL>

<div class="container">
    <div class="row">
        <div class="col">
            <core_rt:if test="${userMissingCouchdb}">
                <div class="fade show alert alert-warning alert-dismissible">
                    <strong><liferay-ui:message key="warning" /> !</strong> <span><liferay-ui:message key="user.not.found.in.couch.db" />. <liferay-ui:message
                            key="update.user.with.correct.data.to.synchronize.liferay.and.couch.db" /></span><br>
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                </div>
            </core_rt:if>
            <core_rt:if test="${userMissingLiferay}">
                <div class="fade show alert alert-warning alert-dismissible">
                    <strong><liferay-ui:message key="warning" /> !</strong> <span><liferay-ui:message key="user.not.found.in.liferay.db" />. <liferay-ui:message
                            key="update.user.with.correct.data.to.synchronize.liferay.and.couch.db" /></span><br>
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                </div>
            </core_rt:if>
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-primary" onclick="window.location.href='<%=editUserURL%>' + window.location.hash">
                                <liferay-ui:message key="edit.user" />
                            </button>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="<sw360:UserName user="${userObj}"/>">
                    <sw360:UserName user="${userObj}" />
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <table class="table label-value-table" id="general">
                        <thead>
                            <tr>
                                <th colspan="2"><liferay-ui:message key="user.information" /></th>
                            </tr>
                        </thead>
                        <tr>
                            <td><liferay-ui:message key="given.name" />:</td>
                            <td><sw360:out value="${userObj.givenname}" /></td>
                        </tr>
                        <tr>
                            <td><liferay-ui:message key="last.name" />:</td>
                            <td><sw360:out value="${userObj.lastname}" /></td>
                        </tr>
                        <tr>
                            <td><liferay-ui:message key="email.id" />:</td>
                            <td><sw360:out value="${userObj.email}" /></td>
                        </tr>
                        <tr>
                            <td><liferay-ui:message key="gid" />:</td>
                            <td><sw360:out value="${userObj.externalid}" /></td>
                        </tr>
                        <tr>
                            <td><liferay-ui:message key="active.status" />:</td>
                            <td><core_rt:if test="${not userObj.deactivated}">
                                    <liferay-ui:message key="active" />
                                </core_rt:if> <core_rt:if test="${userObj.deactivated}">
                                    <liferay-ui:message key="deactivated" />
                                </core_rt:if>
                        </tr>
                        <tr>
                            <td><liferay-ui:message key="department" />:</td>
                            <td><sw360:out value="${userObj.department}" /></td>
                        </tr>
                        <tr>
                            <td><liferay-ui:message key="primary.roles" />:</td>
                            <td>
                            <core_rt:if test="${not empty userObj.id}">
                                <sw360:DisplayEnum value='${userObj.userGroup}' bare="true" />
                            </core_rt:if>
                            <core_rt:if test="${empty userObj.id}">
                                <core_rt:forEach var="role" items="${userObj.primaryRoles}" varStatus="loop">
                                    <sw360:out value="${role}" />
                                    <core_rt:if test="${!loop.last}">,</core_rt:if>
                                </core_rt:forEach>
                            </core_rt:if>
                            </td>
                        </tr>
                        <tr>
                            <td><liferay-ui:message key="secondary.departments.and.roles" />:</td>
                            <td><sw360:DisplayMapOfSecondaryGroupAndRoles value="${userObj.secondaryDepartmentsAndRoles}" /></td>
                        </tr>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
	document.title = $("<span></span>").html("<sw360:UserName user="${userObj}"/> - " + document.title).text();
	AUI().use('liferay-portlet-url', function () {
	require(['jquery'], function($) {
	    function makeUserUrl(email, page) {
	        var portletURLToEditUser = Liferay.PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
	            .setParameter('<%=PortalConstants.PAGENAME%>', page)
	            .setParameter('<%=PortalConstants.USER_EMAIL%>', "friendlyEmail");
	        return portletURLToEditUser.toString().replace("friendlyEmail", email);
	    }

	    if (window.history.replaceState) {
	        window.history.replaceState(null, document.title, makeUserUrl("${userObj.email}", "detail"));
	    }
	});
	});
</script>
