<%--
  ~ Copyright Siemens AG, 2016-2017, 2019. Part of the SW360 Portal Project.
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

<portlet:resourceURL var="downloadSourceBundleURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_SOURCE_CODE_BUNDLE%>'/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${requestScope.project.id}"/>
    <portlet:param name="<%=PortalConstants.CONTEXT_TYPE%>" value="${documentType}"/>
    <portlet:param name="<%=PortalConstants.CONTEXT_ID%>" value="${requestScope.project.id}"/>
</portlet:resourceURL>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
    <jsp:useBean id="sw360User" class="org.eclipse.sw360.datahandler.thrift.users.User" scope="request"/>
    <jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"
                 scope="request"/>
</c:catch>
<core_rt:if test="${empty attributeNotFoundException}">
  <div class="container" style="display: none;">
	<div class="row">
            <div class="col portlet-title left text-truncate" title="Generate Source Code Bundle">
                Generate Source Code Bundle
            </div>
            <div class="col portlet-title text-truncate" title="${sw360:printProjectName(project)}">
                <sw360:ProjectName project="${project}"/>
            </div>
        </div>
        <div class="row">
            <div class="col" >
                <form id="downloadSourceBundleForm" class="form-inline" name="downloadSourceBundleForm" action="<%=downloadSourceBundleURL%>" method="post">
                    <%@include file="/html/projects/includes/attachmentSelectTable.jspf" %>
                    <div class="form-group">
                        <input type="submit" class="btn btn-primary" value="Download File"/>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
</core_rt:if>
