<%--
  ~ Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>
<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink>" scope="request"/>
<jsp:useBean id="parentScopeGroupId" class="java.lang.String" scope="request"/>
<jsp:useBean id="parent_branch_id" class="java.lang.String" scope="request"/>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<core_rt:if test="${empty parentScopeGroupId}">
    <core_rt:set var="concludedScopeGroupId" value="${pageContext.getAttribute('scopeGroupId')}"/>
</core_rt:if>
<core_rt:if test="${not empty parentScopeGroupId}">
    <core_rt:set var="concludedScopeGroupId" value="${parentScopeGroupId}"/>
</core_rt:if>

<core_rt:forEach items='${releaseList}' var="releaseLink" varStatus="releaseloop">
    <tr data-tt-id="${releaseLink.nodeId}" data-tt-branch="${releaseLink.hasSubreleases}" data-is-release-row="true"
        <core_rt:if test="${true}">data-tt-parent-id="${parent_branch_id}"</core_rt:if>
        <core_rt:if test="${empty parent_branch_id and not empty releaseLink.parentNodeId}">data-tt-parent-id="${releaseLink.parentNodeId}"</core_rt:if>>
        <td style="white-space: nowrap">
            <a href="<sw360:DisplayReleaseLink releaseId="${releaseLink.id}" bare="true" scopeGroupId="${concludedScopeGroupId}" />">
                <sw360:out value="${releaseLink.name} ${releaseLink.version}" maxChar="60" />
            </a>
        </td>
        <td>
            <sw360:DisplayEnum value="${releaseLink.componentType}" />
        </td>
        <td>
            <sw360:DisplayEnum value="${releaseLink.releaseRelationship}" /></td>
        <td>
            <core_rt:if test="${releaseLink.setLicenseIds}">
                <sw360:DisplayLicenseCollection licenseIds="${releaseLink.licenseIds}" scopeGroupId="${concludedScopeGroupId}" />
            </core_rt:if>
        </td>
        <td data-releaseclearingstate='<sw360:DisplayEnum value="${releaseLink.clearingState}" bare="true"/>' class="releaseClearingState"></td>
        <td>
            ${requestScope["relMainLineState"][releaseLink.id]}
        </td>
        <td>
            <core_rt:if test='${not empty requestScope["projectReleaseRelation"][releaseLink.id]}'>
                <sw360:DisplayEnum value='${requestScope["projectReleaseRelation"][releaseLink.id]["mainlineState"]}' />
            </core_rt:if>
        </td>
        <td>
        </td>
        <td class="editAction" data-releaseid="${releaseLink.id}"></td>
    </tr>
</core_rt:forEach>