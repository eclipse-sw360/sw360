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
<%@include file="/html/init.jsp" %>


<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>


<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="releasesAndProjects" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData>" scope="request"/>

<core_rt:if test="${releasesAndProjects.size()>0}">
    <core_rt:forEach items="${releasesAndProjects}" var="releaseAndProjectString" varStatus="loop">
        <tr id="release${loop.count}" class="tr_clone">
            <td >
                <label ><input type="checkbox" name="<portlet:namespace/><%=PortalConstants.RELEASE_ID%>" value="${releaseAndProjectString.release.id}" checked=""><sw360:ReleaseName release="${releaseAndProjectString.release}"/></label>
            </td>
            <td>
                    <sw360:out value="${releaseAndProjectString.projectNames}"/>
            </td>
        </tr>
    </core_rt:forEach>
</core_rt:if>

<core_rt:if test="${releasesAndProjects.size()==0}">
    <tr class="tr_clone">
        <td >
            No releases linked to this project or its linked projects
        </td>

    </tr>
</core_rt:if>