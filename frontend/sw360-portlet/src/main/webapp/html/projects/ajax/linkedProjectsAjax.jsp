<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>


<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectLink" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship" %>
<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>"  scope="request"/>


<core_rt:forEach items="${projectList}" var="projectLink" varStatus="loop">
    <core_rt:set var="uuid" value="${projectLink.id}"/>
    <tr id="projectLinkRow${uuid}" >
        <td width="32%">
            <input type="hidden" value="${projectLink.id}" name="<portlet:namespace/><%=Project._Fields.LINKED_PROJECTS%><%=ProjectLink._Fields.ID%>">
            <label class="textlabel stackedLabel" for="projectName">Project name</label>
            <input id="projectName" type="text" class="toplabelledInput" placeholder="Enter project"
                   value="<sw360:out value="${projectLink.name}"/>" readonly onclick="window.location='<sw360:DisplayProjectLink projectId="${projectLink.id}" bare="true"/>'"/>
        </td>
        <td width="32%">
            <input type="hidden" value="${projectLink.version}" name="<portlet:namespace/><%=ProjectLink._Fields.VERSION%>">
            <label class="textlabel stackedLabel" for="projectVersion">Project version</label>
            <input id="projectVersion" type="text" class="toplabelledInput" placeholder="No project version"
                   value="<sw360:out value="${projectLink.version}"/>" readonly/>
        </td>
        <td width="32%">
            <label class="textlabel stackedLabel" for="projectRelation">Project relation</label>
            <select class="toplabelledInput" id="projectRelation"
                    name="<portlet:namespace/><%=Project._Fields.LINKED_PROJECTS%><%=ProjectLink._Fields.RELATION%>"
                    style="min-width: 162px; min-height: 28px;">

                <sw360:DisplayEnumOptions type="<%=ProjectRelationship.class%>" selected="${projectLink.relation}"/>
            </select>
            <sw360:DisplayEnumInfo type="<%=ProjectRelationship.class%>"/>
        </td>

        <td class="deletor">
            <img src="<%=request.getContextPath()%>/images/Trash.png" onclick="deleteProjectLink('projectLinkRow${uuid}')" alt="Delete">
        </td>

    </tr>
</core_rt:forEach>
