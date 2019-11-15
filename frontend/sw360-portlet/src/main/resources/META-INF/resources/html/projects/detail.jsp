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
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>"
                 scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="numberOfUncheckedVulnerabilities" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="numberOfVulnerabilities" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="defaultLicenseInfoHeaderText" class="java.lang.String" scope="request" />
    <jsp:useBean id="defaultObligationsText" class="java.lang.String" scope="request" />
    <jsp:useBean id="licInfoAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>
    <jsp:useBean id="sourceAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>
    <jsp:useBean id="manualAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>
    <core_rt:set var="isProjectObligationsEnabled" value='<%=PortalConstants.IS_PROJECT_OBLIGATIONS_ENABLED%>'/>
</c:catch>

<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">
    <core_rt:set var="inProjectDetailsContext" value="true" scope="request"/>
    <core_rt:set var="isObligationPresent"  value="${not empty project.linkedObligations}" />
    <core_rt:if test="${isObligationPresent}">
        <jsp:useBean id="projectReleaseLicenseInfo" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult>" scope="request" />
        <jsp:useBean id="approvedObligationsCount" type="java.lang.Integer" scope="request"/>
    </core_rt:if>
    <%@include file="/html/projects/includes/detailOverview.jspf"%>
</core_rt:if>
