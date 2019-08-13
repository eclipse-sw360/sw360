<%--
  ~ Copyright Siemens AG, 2013-2016, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.moderation.DocumentType" %>
<%@ page import="javax.portlet.PortletRequest" %>

<jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>" scope="request"/>
<jsp:useBean id="moderationRequest" class="org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest" scope="request"/>
<jsp:useBean id="actual_project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request" />

<core_rt:set var="project" value="${actual_project}" scope="request"/>
<div class="container" id="moderation-request-merge" data-document-type="<%=DocumentType.PROJECT%>">

    <core_rt:set var="moderationTitle" value="Delete ${sw360:printProjectName(project)}" scope="request" />
    <%@include file="/html/moderation/includes/moderationHeader.jspf"%>

    <div class="row">
        <div class="col">
            <div id="moderation-wizard" class="accordion">
                <div class="card">
                    <div id="moderation-header-heading" class="card-header">
                        <h2 class="mb-0">
                            <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#moderation-header" aria-expanded="true" aria-controls="moderation-header">
                                Moderation Request Information
                            </button>
                        </h2>
                    </div>
                    <div id="moderation-header" class="collapse show" aria-labelledby="moderation-header-heading" data-parent="#moderation-wizard">
                        <div class="card-body">
                            <%@include file="/html/moderation/includes/moderationInfo.jspf"%>
                        </div>
                    </div>
                </div>
                <core_rt:if test="${sw360:isOpenModerationRequest(moderationRequest)}">
                    <div class="card">
                        <div id="moderation-changes-heading" class="card-header">
                            <h2 class="mb-0">
                                <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#moderation-changes" aria-expanded="false" aria-controls="moderation-changes">
                                    Proposed Changes
                                </button>
                            </h2>
                        </div>
                        <div id="moderation-changes" class="collapse p-3" aria-labelledby="moderation-changes-heading" data-parent="#moderation-wizard">
                            <div class="alert alert-danger mb-0">
                                The project ${sw360:printProjectName(project)} is requested to be deleted.
                            </div>
                        </div>
                    </div>
                </core_rt:if>
                <div class="card">
                    <div id="current-document-heading" class="card-header">
                        <h2 class="mb-0">
                            <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#current-document" aria-expanded="false" aria-controls="current-document">
                                Current Project
                            </button>
                        </h2>
                    </div>
                    <div id="current-document" class="collapse" aria-labelledby="current-document-heading" data-parent="#moderation-wizard">
                        <div class="card-body">
                            <core_rt:set var="inProjectDetailsContext" value="false" scope="request"/>
                            <%@include file="/html/utils/includes/requirejs.jspf" %>
                            <%@include file="/html/projects/includes/detailOverview.jspf"%>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

