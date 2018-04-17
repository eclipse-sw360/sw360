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
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>"
                 scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="selectedTab" class="java.lang.String" scope="request"/>
    <jsp:useBean id="numberOfUncheckedVulnerabilities" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="numberOfVulnerabilities" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="defaultLicenseInfoHeaderText" class="java.lang.String" scope="request" />
</c:catch>
<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">

    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

    <core_rt:set var="dontDisplayDeleteButton" value="true" scope="request"/>
    <div id="header"></div>
    <p class="pageHeader"><span class="pageHeaderBigSpan">Project: <sw360:ProjectName project="${project}"/></span>
        <span class="pull-right">
        <input type="button" id="edit" value="Edit" class="addButton">
    </span>
    </p>

    <core_rt:set var="inProjectDetailsContext" value="true" scope="request"/>
    <%@include file="/html/projects/includes/detailOverview.jspf"%>
</core_rt:if>
<script>
    require(['jquery', 'modules/tabview'], function($, tabview) {
        tabview.create('myTab');
        $('#edit').on('click', editProject);

        function editProject() {
            window.location ='<portlet:renderURL ><portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/><portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/></portlet:renderURL>'
        }
    });
</script>
