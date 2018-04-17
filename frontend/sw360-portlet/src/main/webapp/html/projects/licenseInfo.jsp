<%--
  ~ Copyright Siemens AG, 2016-2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@ include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%-- enable requirejs for this page --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<portlet:resourceURL var="downloadLicenseInfoURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_INFO%>'/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${requestScope.project.id}"/>
</portlet:resourceURL>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
    <jsp:useBean id="sw360User" class="org.eclipse.sw360.datahandler.thrift.users.User" scope="request"/>
    <jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"
                 scope="request"/>
    <jsp:useBean id="licenseInfoOutputFormats"
                 type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo>"
                 scope="request"/>
</c:catch>
<core_rt:if test="${empty attributeNotFoundException}">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-treetable/3.2.0/css/jquery.treetable.css"/>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/jquery.treetable.theme.sw360.css"/>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

    <div id="header"></div>
    <p class="pageHeader"><span class="pageHeaderBigSpan">Generate License Information File For Project <sw360:ProjectName project="${project}"/></span>
    </p>

    <div id="content" >
        <form id="downloadLicenseInfoForm" name="downloadLicenseInfoForm" action="<%=downloadLicenseInfoURL%>" method="post">
            <%@include file="/html/projects/includes/attachmentSelectTable.jspf" %>
            <span class="pull-right">
               <select class="toplabelledInput, formatSelect" id="<%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>"
                       name="<portlet:namespace/><%=PortalConstants.LICENSE_INFO_SELECTED_OUTPUT_FORMAT%>">
                                   <sw360:DisplayOutputFormats options='${licenseInfoOutputFormats}'/>
               </select>
               <input type="submit" value="Download File" class="addButton"/>
            </span>
        </form>

    </div>
</core_rt:if>
