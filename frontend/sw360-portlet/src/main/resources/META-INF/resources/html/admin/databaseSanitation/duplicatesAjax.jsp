<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="duplicateReleases" type="java.util.Map<java.lang.String,  java.util.List<java.lang.String>>"
             scope="request"/>
<jsp:useBean id="duplicateReleaseSources" type="java.util.Map<java.lang.String,  java.util.List<java.lang.String>>"
             scope="request"/>
<jsp:useBean id="duplicateComponents" type="java.util.Map<java.lang.String,  java.util.List<java.lang.String>>"
             scope="request"/>

<jsp:useBean id="duplicateProjects" type="java.util.Map<java.lang.String,  java.util.List<java.lang.String>>"
             scope="request"/>

<core_rt:if test="${duplicateReleases.size()>0}">
    <h4><liferay-ui:message key="releases.with.the.same.identifier.name.version" /></h4>
    <table id="duplicateReleasesTable" class="table table-bordered">
        <thead>
        <tr>
            <th><liferay-ui:message key="release.name" /></th><th><liferay-ui:message key="links" /></th>
        </tr>
        </thead>
        <tbody>
        <core_rt:forEach items="${duplicateReleases.entrySet()}" var="duplicate">
            <tr>
                <td>${duplicate.key}</td>
                <td>
                    <core_rt:forEach items="${duplicate.value}" var="id" varStatus="loop">
                        <sw360:DisplayReleaseLink releaseId="${id}"
                                                  showName="false">${loop.count}</sw360:DisplayReleaseLink>&nbsp;
                    </core_rt:forEach>
                </td>
            </tr>
        </core_rt:forEach>
        </tbody>
    </table>
</core_rt:if>

<core_rt:if test="${duplicateReleaseSources.size()>0}">
    <h4><liferay-ui:message key="releases.with.more.than.one.source.attachment" /></h4>
    <table id="duplicateReleaseSourcesTable" class="table table-bordered">
        <thead>
        <tr>
            <th><liferay-ui:message key="release.name" /></th><th><liferay-ui:message key="source.attachments.count" /></th>
        </tr>
        </thead>
        <tbody>
        <core_rt:forEach items="${duplicateReleaseSources.entrySet()}" var="duplicate">
            <tr>
                <td>   <sw360:DisplayReleaseLink releaseId="${duplicate.value.get(0)}"
                                                 showName="false">${duplicate.key}</sw360:DisplayReleaseLink></td>
                <td>
                    <sw360:out value="${duplicate.value.size()}"/>
                </td>
            </tr>
        </core_rt:forEach>
        </tbody>
    </table>
</core_rt:if>

<core_rt:if test="${duplicateComponents.size()>0}">
    <h4><liferay-ui:message key="components.with.the.same.identifier.name" /></h4>
    <table id="duplicateComponentsTable" class="table table-bordered">
        <thead>
        <tr>
            <th><liferay-ui:message key="component.name" /></th><th><liferay-ui:message key="links" /></th>
        </tr>
        </thead>
        <tbody>
        <core_rt:forEach items="${duplicateComponents.entrySet()}" var="duplicate">
            <tr>
                <td>${duplicate.key}</td>
                <td>
                    <core_rt:forEach items="${duplicate.value}" var="id" varStatus="loop">
                        <sw360:DisplayComponentLink componentId="${id}"
                                                  showName="false">${loop.count}</sw360:DisplayComponentLink>&nbsp;
                    </core_rt:forEach>
                </td>
            </tr>
        </core_rt:forEach>
        </tbody>
    </table>
</core_rt:if>

<core_rt:if test="${duplicateProjects.size()>0}">
    <h4><liferay-ui:message key="projects.with.the.same.identifier.name.version" /></h4>
    <table id="duplicateProjectsTable" class="table table-bordered">
        <thead>
        <tr>
            <th><liferay-ui:message key="project.name" /></th><th><liferay-ui:message key="links" /></th>
        </tr>
        </thead>
        <tbody>
        <core_rt:forEach items="${duplicateProjects.entrySet()}" var="duplicate">
            <tr>
                <td>${duplicate.key}</td>
                <td>
                    <core_rt:forEach items="${duplicate.value}" var="id" varStatus="loop">
                        <sw360:DisplayProjectLink projectId="${id}"
                                                    showName="false">${loop.count}</sw360:DisplayProjectLink>&nbsp;
                    </core_rt:forEach>
                </td>
            </tr>
        </core_rt:forEach>
        </tbody>
    </table>
</core_rt:if>
