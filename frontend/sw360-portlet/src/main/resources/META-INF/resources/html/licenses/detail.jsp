<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
  ~ With modifications by Bosch Software Innovations GmbH, 2016.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
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
    <jsp:useBean id="added_obligations_from_moderation_request"
                 type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>" scope="request"/>
    <jsp:useBean id="db_obligations_from_moderation_request"
                 type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>" scope="request"/>
    <jsp:useBean id="isUserAtLeastClearingAdmin" class="java.lang.String" scope="request"/>
    <jsp:useBean id="obligationList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>"
                 scope="request"/>
</c:catch>
<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">
    <core_rt:set var="editMode" value="true" scope="request"/>
    <%@include file="includes/detailOverview.jspf" %>
</core_rt:if>
