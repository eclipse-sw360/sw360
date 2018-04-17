<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal User.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.moderation.DocumentType" %>

<jsp:useBean id="newuser" type="org.eclipse.sw360.datahandler.thrift.users.User" scope="request"/>
<jsp:useBean id="moderationRequest" class="org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest" scope="request"/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
<script src="<%=request.getContextPath()%>/webjars/jquery/1.12.4/jquery.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/jquery.validate.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/additional-methods.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.min.js"></script>

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Moderation New User:  <sw360:UserName user="${newuser}"/></span>
</p>
<%@include file="/html/moderation/includes/moderationActionButtons.jspf"%>

<h2>Proposed User Attributes</h2>
<div id="content">
<table class="table info_table" id="userOverview">
    <thead>
    <tr>
        <th colspan="2"><sw360:out value="User Details: ${newuser.fullname}"/></th>
    </tr>
    </thead>
    <tr>
        <td>First Name:</td>
        <td><sw360:out value="${newuser.givenname}"/></td>
    </tr>
    <tr>
        <td>Last Name:</td>
        <td><sw360:out value="${newuser.lastname}"/></td>
    </tr>
    <tr>
        <td>Email:</td>
        <td><sw360:out value="${newuser.email}"/></td>
    </tr>
    <tr>
        <td>Group:</td>
        <td><sw360:out value="${newuser.department}"/></td>
    </tr>
    <tr>
        <td>Role:</td>
        <td><sw360:DisplayEnum value="${newuser.userGroup}"/></td>
    </tr>
    <tr>
        <td>External Id:</td>
        <td><sw360:out value="${newuser.externalid}"/></td>
    </tr>
</table>
</div>

<script>
    function getBaseURL(){
        var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
        var portletURL = Liferay.PortletURL.createURL(baseUrl)
                .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_ACTION%>')
                .setParameter('<%=PortalConstants.MODERATION_ID%>', '${moderationRequest.id}')
                .setParameter('<%=PortalConstants.DOCUMENT_TYPE%>', '<%=DocumentType.USER%>');

        return portletURL;
    }
</script>
<%@include file="/html/moderation/includes/moderationActions.jspf"%>
