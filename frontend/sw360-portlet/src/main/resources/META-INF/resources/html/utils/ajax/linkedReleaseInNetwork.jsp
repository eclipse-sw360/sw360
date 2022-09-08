<%--
  ~ Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ReleaseLink" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.MainlineState" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<jsp:useBean id="releasesInNetwork" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink>"  scope="request"/>
<core_rt:set var="mainlineStateEnabledForUserRole" value='<%=PortalConstants.MAINLINE_STATE_ENABLED_FOR_USER%>'/>
<core_rt:forEach items="${releasesInNetwork}" var="releaseLink" varStatus="loop">

    <core_rt:choose>
        <core_rt:when test="${releaseLink.accessible}">

            <core_rt:set var="uuid" value="${releaseLink.id}"/>
            <tr id="releaseLinkRow${uuid}" parent-node="${releaseLink.parentNodeId}" data-layer="${releaseLink.layer}" data-index="${releaseLink.index}">
                <td style="vertical-align:middle;font-size:1rem;display:flex;width:100%" class="content-middle">
                    <div style="display:block;width:max-content;">
                        <core_rt:forEach begin="0" end="${releaseLink.layer}" var="val">
                              &nbsp;&nbsp;&nbsp;&nbsp;
                        </core_rt:forEach>
                        <sw360:out value="${releaseLink.name}"/>
                    </div>
                    <button type="button" class="btn btn-secondary add-child float-right" style="margin-left: auto; margin-right: 0px;">
                        <svg class="action lexicon-icon" style="width:15px">
                            <title><liferay-ui:message key="add.child.releases" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#plus"/>
                        </svg>
                    </button>
                </td>
                <td>
                    <div class="form-group">
                        <select id="projectReleaseVersion" class="form-control releaseVersion" style="display:inline-block"
                             data-old="<sw360:out value="${releaseLink.id}"/>">
                            <core_rt:forEach items="${releaseLink.releaseWithSameComponent}" var="release">
                                <core_rt:if test = "${releaseLink.id == release.id}">
                                    <option value="<sw360:out value="${release.id}"/>" selected >
                                        <sw360:out value="${release.version}"/>
                                    </option>
                                </core_rt:if>
                                <core_rt:if test = "${releaseLink.id != release.id}">
                                    <option value="<sw360:out value="${release.id}"/>">
                                        <sw360:out value="${release.version}"/>
                                    </option>
                                </core_rt:if>
                            </core_rt:forEach>
                        </select>
                    </div>
                </td>
                <td style="width: 2%" class="content-middle">
                    <div class="form-group" style="text-align:center">
                        <svg class="action load-release">
                            <title><liferay-ui:message key="load.default.child.releases" /></title>
                            <use href="<%=request.getContextPath()%>/images/icons.svg#spinner"/>
                        </svg>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <select id="projectReleaseRelation" class="form-control projectReleaseRelation">
                            <sw360:DisplayEnumOptions type="<%=ReleaseRelationship.class%>" useStringValues="true" selected="${releaseLink.releaseRelationship}"/>
                        </select>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <select class="form-control mainlineState" id="mainlineState"
                                <core_rt:if test="${not isUserAtLeastClearingAdmin and not mainlineStateEnabledForUserRole}" >
                                    disabled="disabled"
                                </core_rt:if>
                        >
                            <sw360:DisplayEnumOptions type="<%=MainlineState.class%>" useStringValues="true" selected="${releaseLink.mainlineState}"/>
                        </select>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <input id="releaseComment"
                        type="text" placeholder="<liferay-ui:message key="enter.comment" />" class="form-control releaseComment"
                            value="<sw360:out value="${releaseLink.comment}"/>"/>
                    </div>
                </td>
                <td class="content-middle" style="width: 2%">
                    <svg class="action lexicon-icon" data-row-id="releaseLinkRow${uuid}" data-release-name="<sw360:out value='${releaseLink.longName}' jsQuoting="true"/>">
                        <title><liferay-ui:message key="delete" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                    </svg>
                </td>
            </tr>

        </core_rt:when>
        <core_rt:otherwise>

            <core_rt:set var="uuid" value="${releaseLink.id}"/>
            <tr id="releaseLinkRow${uuid}" parent-node="${releaseLink.parentNodeId}" data-layer="${releaseLink.layer}" data-index="${releaseLink.index}">
                <td style="vertical-align:middle;font-size:1rem;display:flex;width:100%" class="content-middle">
                    <div style="display:block;width:max-content;">
                        <core_rt:forEach begin="0" end="${releaseLink.layer}" var="val">
                              &nbsp;&nbsp;&nbsp;&nbsp;
                        </core_rt:forEach>
                        <liferay-ui:message key="inaccessible.release" />
                    </div>
                <td>
                </td>
                <td>
                    <div class="form-group">
                        <input id="releaseRelation" type="hidden"
                                name="<portlet:namespace/><%=PortalConstants.RELEASE_USAGE%><%=ProjectReleaseRelationship._Fields.RELEASE_RELATION%>"
                                value="${releaseLink.releaseRelationship.getValue()}"
                                >
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <input id="mainlineState" type="hidden"
                                name="<portlet:namespace/><%=PortalConstants.RELEASE_USAGE%><%=ProjectReleaseRelationship._Fields.MAINLINE_STATE%>"
                                value="${releaseLink.mainlineState.getValue()}"
                                >
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <input id="releaseComment" type="hidden"
                                name="<portlet:namespace/><%=PortalConstants.RELEASE_USAGE%><%=ProjectReleaseRelationship._Fields.COMMENT%>"
                                value="<sw360:out value="${releaseLink.comment}"/>"
                                >
                    </div>
                </td>
                <td class="content-middle">
                </td>
            </tr>

        </core_rt:otherwise>
    </core_rt:choose>
</core_rt:forEach>

