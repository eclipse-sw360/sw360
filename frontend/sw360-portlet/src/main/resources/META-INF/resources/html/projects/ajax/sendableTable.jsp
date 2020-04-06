<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
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
                <th><liferay-ui:message key="release" /></th>
                <th><liferay-ui:message key="project" /></th>
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
        <liferay-ui:message key="no.releases.linked.to.this.project.or.its.linked.projects" />
    </div>
</core_rt:if>
