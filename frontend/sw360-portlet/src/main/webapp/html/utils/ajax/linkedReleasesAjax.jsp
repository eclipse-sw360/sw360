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


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ReleaseLink" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.MainlineState" %>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink>"  scope="request"/>
<core_rt:forEach items="${releaseList}" var="releaseLink" varStatus="loop">
    <core_rt:set var="uuid" value="${releaseLink.id}"/>
    <tr id="releaseLinkRow${uuid}" >
        <td>
            <label class="textlabel stackedLabel" for="releaseVendor">Vendor name</label>
            <input id="releaseVendor" type="text" class="toplabelledInput" placeholder="No vendor"
                   value="<sw360:out value="${releaseLink.vendor}"/>" readonly/>
        </td>
        <td>
            <input type="hidden" value="${releaseLink.id}" name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ReleaseLink._Fields.ID%>">
            <label class="textlabel stackedLabel" for="releaseName">Release name</label>
            <input id="releaseName" type="text" class="toplabelledInput" placeholder="Enter release"
                   value="<sw360:out value="${releaseLink.name}"/>" readonly/>
        </td>
        <td>
            <label class="textlabel stackedLabel" for="releaseVersion">Release version</label>
            <input id="releaseVersion" type="text" class="toplabelledInput" placeholder="Enter version"
                   value="<sw360:out value="${releaseLink.version}"/>" readonly/>
        </td>
        <td>
            <label class="textlabel stackedLabel" for="releaseRelation">Release relation</label>
            <select class="toplabelledInput" id="releaseRelation"
                    name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.RELEASE_RELATION%>"
                    style="min-height: 28px;">

                <sw360:DisplayEnumOptions type="<%=ReleaseRelationship.class%>" selected="${releaseLink.releaseRelationship}"/>
            </select>
            <sw360:DisplayEnumInfo type="<%=ReleaseRelationship.class%>"/>
        </td>
        <td>
            <label class="textlabel stackedLabel" for="mainlineState">Project Mainline state</label>
            <select class="toplabelledInput" id="mainlineState"
                    name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.MAINLINE_STATE%>"
                    style="min-height: 28px;">

                <sw360:DisplayEnumOptions type="<%=MainlineState.class%>" selected="${releaseLink.mainlineState}"/>
            </select>
            <sw360:DisplayEnumInfo type="<%=MainlineState.class%>"/>
        </td>

        <td class="deletor">
            <img src="<%=request.getContextPath()%>/images/Trash.png" onclick="deleteReleaseLink('releaseLinkRow${uuid}','<sw360:out value='${releaseLink.longName}' jsQuoting="true"/>')" alt="Delete">
        </td>

    </tr>
</core_rt:forEach>
