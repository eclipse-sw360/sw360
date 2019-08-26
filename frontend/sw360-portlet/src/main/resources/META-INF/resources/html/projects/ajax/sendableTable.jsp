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

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="releasesAndProjects" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData>" scope="request"/>

<core_rt:if test="${releasesAndProjects.size()>0}">
    <table class="table table-striped">
        <thead>
            <tr>
                <th><input type="checkbox" class="form-check-input" data-action="select"  checked="" /></th>
                <th>Release</th>
                <th>Project</th>
            </tr>
        </thead>
        <tbody>
            <core_rt:forEach items="${releasesAndProjects}" var="releaseAndProjectString" varStatus="loop">
                <tr>
                    <td>
                        <input type="checkbox" class="form-check-input" name="<portlet:namespace/><%=PortalConstants.RELEASE_ID%>" id="release_${releaseAndProjectString.release.id}" value="${releaseAndProjectString.release.id}" checked="" />
                    </td>
                    <td>
                        <label class="form-check-label" for="release_${releaseAndProjectString.release.id}">
                            <sw360:ReleaseName release="${releaseAndProjectString.release}"/>
                        </label>
                    </td>
                    <td>
                        <sw360:out value="${releaseAndProjectString.projectNames}"/>
                    </td>
                </tr>
            </core_rt:forEach>
        </tbody>
    </table>
</core_rt:if>

<core_rt:if test="${releasesAndProjects.size()==0}">
    <div class="alert alert-info" role="alert">
        No releases linked to this project or its linked projects
    </div>
</core_rt:if>
