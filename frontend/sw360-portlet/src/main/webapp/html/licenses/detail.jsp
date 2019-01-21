<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
  ~ With modifications by Bosch Software Innovations GmbH, 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>
<c:catch var="attributeNotFoundException">
    <jsp:useBean id="licenseDetail" class="org.eclipse.sw360.datahandler.thrift.licenses.License" scope="request"/>
    <jsp:useBean id="moderationLicenseDetail" class="org.eclipse.sw360.datahandler.thrift.licenses.License"
                 scope="request"/>
    <jsp:useBean id="added_todos_from_moderation_request"
                 type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Todo>" scope="request"/>
    <jsp:useBean id="db_todos_from_moderation_request"
                 type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Todo>" scope="request"/>
    <jsp:useBean id="isUserAtLeastClearingAdmin" class="java.lang.String" scope="request"/>
    <jsp:useBean id="obligationList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>"
                 scope="request"/>
</c:catch>
<%@include file="/html/utils/includes/logError.jspf" %>
<script src="<%=request.getContextPath()%>/webjars/jquery/dist/jquery.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/dist/jquery.validate.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/dist/additional-methods.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-confirm2/dist/jquery-confirm.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/jquery-ui.min.js"></script>

<portlet:actionURL var="editLicenseTodosURL" name="updateWhiteList">
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}"/>
</portlet:actionURL>

<portlet:actionURL var="addLicenseTodoURL" name="addTodo">
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}"/>
</portlet:actionURL>

<portlet:actionURL var="changeLicenseTextURL" name="changeText">
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}"/>
</portlet:actionURL>

<portlet:actionURL var="editExternalLinkURL" name="editExternalLink">
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}" />
</portlet:actionURL>
<core_rt:if test="${empty attributeNotFoundException}">
    <div id="header"></div>
    <p class="pageHeader">
        <span class="pageHeaderBigSpan">
            <core_rt:if test="${licenseDetail.checked == false}">
                <alert>UNCHECKED</alert>
            </core_rt:if>
            License: <sw360:out value="${licenseDetail.fullname}"/> (<sw360:out value="${licenseDetail.shortname}"/>)
        </span>
        <core_rt:if test="${isUserAtLeastClearingAdmin == 'Yes' || licenseDetail.checked == false}">
         <span class="pull-right">
             <input type="button" id="edit" value="Edit License Details and Text" class="addButton">
         </span>
        </core_rt:if>
    </p>
    <core_rt:set var="editMode" value="true" scope="request"/>
    <%@include file="includes/detailOverview.jspf" %>
</core_rt:if>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
	require(['jquery', 'modules/tabview'], function($, tabview) {
	    tabview.create('myTab');

        $('#cancelEditWhitelistButton').on('click', function(event) {
            $(':checkbox').prop('checked', true);
            $('table.todosFromModerationRequest').hide();
            $('table.db_table').show();
        });

        $('#EditWhitelist').on('click', function(event) {
            $('table.todosFromModerationRequest').show();
            $('table.db_table').hide();
        });

	    $('#edit').on('click', function(event) {
            window.location = '<portlet:renderURL>'
                                + '<portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}"/>'
                                + '<portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>'
                            + '</portlet:renderURL>'
        });
	});
</script>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
