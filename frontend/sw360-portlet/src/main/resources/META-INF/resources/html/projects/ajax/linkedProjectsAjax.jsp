<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectLink" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship" %>
<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"  scope="request"/>

<core_rt:forEach items="${projectList}" var="projectLink" varStatus="loop">
    <core_rt:set var="uuid" value="${projectLink.id}"/>
    <tr id="projectLinkRow${uuid}" >
        <td>
            <div class="form-group">
                <input type="hidden" value="${projectLink.id}" name="<portlet:namespace/><%=Project._Fields.LINKED_PROJECTS%><%=ProjectLink._Fields.ID%>">
                <input type="text" placeholder="Enter project" class="form-control" data-content="projectName"
                    value="<sw360:out value="${projectLink.name}"/>" readonly onclick="window.location='<sw360:DisplayProjectLink projectId="${projectLink.id}" bare="true"/>'"/>
            </div>
        </td>
        <td>
            <div class="form-group">
                <input type="hidden" value="${projectLink.version}" name="<portlet:namespace/><%=ProjectLink._Fields.VERSION%>">
                <input type="text" placeholder="No project version"  class="form-control"
                    value="<sw360:out value="${projectLink.version}"/>" readonly/>
            </div>
        </td>
        <td>
            <div class="form-group">
                <select name="<portlet:namespace/><%=Project._Fields.LINKED_PROJECTS%><%=ProjectLink._Fields.RELATION%>" class="form-control">
                    <sw360:DisplayEnumOptions type="<%=ProjectRelationship.class%>" selected="${projectLink.relation}"/>
                </select>
            </div>
        </td>
        <td class="content-middle">
            <svg title="Delete" class="action lexicon-icon" data-row-id="projectLinkRow${uuid}">
                <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
            </svg>
        </td>
    </tr>
</core_rt:forEach>
