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

<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<jsp:useBean id="projectSearch" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.Project>" class="java.util.ArrayList" scope="request"/>

<core_rt:if test="${projectSearch.size()>0}" >
    <core_rt:forEach items="${projectSearch}" var="entry">
        <tr>
            <td>
                <div class="form-check">
                    <input type="checkbox" class="form-check-input" name="<portlet:namespace/>projectId" value="${entry.id}">
                </div>
            </td>
            <td><sw360:ProjectName project="${entry}"/></td>
            <td><sw360:out value="${entry.version}"/></td>
            <td><sw360:DisplayStateBoxes project="${entry}"/></td>
            <td><sw360:DisplayUserEmail email="${entry.projectResponsible}" bare="true"/></td>
            <td><sw360:out value="${entry.description}"/></td>
        </tr>
    </core_rt:forEach>
</core_rt:if>
