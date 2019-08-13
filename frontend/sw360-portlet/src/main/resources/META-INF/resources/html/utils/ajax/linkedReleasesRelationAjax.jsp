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
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Release" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ReleaseLink" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ReleaseRelationship" %>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink>"  scope="request"/>

<core_rt:forEach items="${releaseList}" var="releaseLink" varStatus="loop">
    <core_rt:set var="uuid" value="${releaseLink.id}"/>
    <tr id="releaseLinkRow${uuid}" >
        <td>
            <div class="form-group">
                <input id="releaseVendor" type="text" placeholder="Enter vendor" class="form-control"
                    value="<sw360:out value="${releaseLink.vendor}"/>" readonly />
            </div>
        </td>
        <td>
            <div class="form-group">
                <input type="hidden" value="${releaseLink.id}" name="<portlet:namespace/><%=Release._Fields.RELEASE_ID_TO_RELATIONSHIP%><%=ReleaseLink._Fields.ID%>">
                <input id="releaseName" type="text" placeholder="Enter release" class="form-control"
                    value="<sw360:out value="${releaseLink.name}"/>" readonly/>
            </div>
        </td>
        <td>
            <div class="form-group">
                <input id="releaseVersion" class="form-control" type="text" placeholder="Enter version"
                    value="<sw360:out value="${releaseLink.version}"/>" readonly/>
            </div>
        </td>
        <td>
            <div class="form-group">
                <select id="releaseRelation"
                        name="<portlet:namespace/><%=Release._Fields.RELEASE_ID_TO_RELATIONSHIP%><%=ReleaseLink._Fields.RELEASE_RELATIONSHIP%>"
                        class="form-control">
                    <sw360:DisplayEnumOptions type="<%=ReleaseRelationship.class%>" selected="${releaseLink.releaseRelationship}"/>
                </select>
            </div>
        </td>
        <td class="actions">
            <div class="actions">
                <svg class="lexicon-icon" data-action="delete-release" data-uuid="${uuid}"><title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#delete"/></svg>
            </div>
        </td>
    </tr>
</core_rt:forEach>
