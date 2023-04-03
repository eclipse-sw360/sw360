<%--
  ~ Copyright TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>
<jsp:useBean id="networkReleaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink>" scope="request"/>
<jsp:useBean id="parentScopeGroupId" class="java.lang.String" scope="request"/>
<jsp:useBean id="networkParentBranchId" class="java.lang.String" scope="request"/>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<core_rt:if test="${empty parentScopeGroupId}">
    <core_rt:set var="concludedScopeGroupId" value="${pageContext.getAttribute('scopeGroupId')}"/>
</core_rt:if>
<core_rt:if test="${not empty parentScopeGroupId}">
    <core_rt:set var="concludedScopeGroupId" value="${parentScopeGroupId}"/>
</core_rt:if>

<core_rt:forEach items='${networkReleaseList}' var="releaseLink" varStatus="releaseloop">
    <core_rt:choose>
        <core_rt:when test="${releaseLink.accessible}">
            <tr data-tt-id="${releaseLink.nodeId}" data-tt-branch="${releaseLink.hasSubreleases}" data-is-release-row="true"
                <core_rt:if test="${true}">data-tt-parent-id="${networkParentBranchId}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.parentNodeId}">data-parent-release="${releaseLink.parentNodeId}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.layer}">data-layer="${releaseLink.layer}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.projectId}">data-project-id="${releaseLink.projectId}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.index}">data-index="${releaseLink.index}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.id}">data-release-id="${releaseLink.id}"</core_rt:if>
                >
                <td style="white-space: nowrap">
                    <a href="<sw360:DisplayReleaseLink releaseId="${releaseLink.id}" bare="true" scopeGroupId="${concludedScopeGroupId}" />">
                        <sw360:out value="${releaseLink.name} ${releaseLink.version}" maxChar="60" />
                    </a>
                    <core_rt:if test="${releaseLink.clearingState eq 'SCAN_AVAILABLE'}">
                        <span class="actions" >
                            <svg class="cursor lexicon-icon m-2 isr" data-doc-id="${releaseLink.id}"><title><liferay-ui:message key='view.scanner.findings.license' /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle"/></svg>
                        </span>
                    </core_rt:if>
                </td>
                <td>
                    <sw360:DisplayEnum value="${releaseLink.componentType}" />
                </td>
                <td>
                    <sw360:DisplayEnum value="${releaseLink.releaseRelationship}" /></td>
                <td class="actions">
                    <core_rt:if test="${releaseLink.setLicenseIds}">
                        <sw360:DisplayLicenseCollection licenseIds="${releaseLink.licenseIds}" releaseId="${releaseLink.id}" scopeGroupId="${pageContext.getAttribute('scopeGroupId')}" icon="info-circle"/>
                    </core_rt:if>
                </td>
                <td class="actions">
                    <core_rt:if test="${releaseLink.setOtherLicenseIds}">
                        <sw360:DisplayLicenseCollection licenseIds="${releaseLink.otherLicenseIds}" main="false" releaseId="${releaseLink.id}" scopeGroupId="${pageContext.getAttribute('scopeGroupId')}" icon="info-circle"/>
                    </core_rt:if>
                </td>
                <td data-releaseclearingstate='<sw360:DisplayEnum value="${releaseLink.clearingState}" bare="true"/>' class="releaseClearingState"></td>
                <td>
                    <sw360:DisplayEnum value='${releaseLink.releaseMainLineState}'/>
                </td>
                <td>
                    <sw360:DisplayEnum value='${releaseLink.mainlineState}'/>
                </td>
                <td>
                    <sw360:out value='${releaseLink.comment}'/>
                </td>
                <td class="editAction" data-releaseid="${releaseLink.id}"></td>
            </tr>
        </core_rt:when>
        <core_rt:otherwise>
            <tr data-tt-id="${releaseLink.nodeId}" data-tt-branch="${releaseLink.hasSubreleases}" data-is-release-row="true"
                <core_rt:if test="${true}">data-tt-parent-id="${networkParentBranchId}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.parentNodeId}">data-parent-release="${releaseLink.parentNodeId}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.layer}">data-layer="${releaseLink.layer}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.projectId}">data-project-id="${releaseLink.projectId}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.index}">data-index="${releaseLink.index}"</core_rt:if>
                <core_rt:if test="${not empty releaseLink.id}">data-release-id="${releaseLink.id}"</core_rt:if>
                >
                <td style="white-space: nowrap">
                    <liferay-ui:message key="inaccessible.release" />
                </td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
            </tr>
        </core_rt:otherwise>
    </core_rt:choose>
</core_rt:forEach>

