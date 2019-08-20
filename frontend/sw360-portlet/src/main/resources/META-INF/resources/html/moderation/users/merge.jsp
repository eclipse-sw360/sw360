<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal User.
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

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.moderation.DocumentType" %>

<jsp:useBean id="newuser" type="org.eclipse.sw360.datahandler.thrift.users.User" scope="request"/>
<jsp:useBean id="moderationRequest" class="org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest" scope="request"/>


<div class="container" id="moderation-request-merge" data-document-type="<%=DocumentType.USER%>">

    <core_rt:set var="moderationTitle" value="Add ${sw360:printUserName(newuser)}" scope="request" />
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
                        <div id="moderation-changes" class="collapse" aria-labelledby="moderation-changes-heading" data-parent="#moderation-wizard">
                            <div class="card-body">
                                <h4 class="mt-2">Proposed User Attributes</h4>
                                <table class="table label-value-table" id="userOverview">
                                    <thead>
                                        <tr>
                                            <th colspan="2"><sw360:out value="User Details: ${newuser.fullname}"/></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <td>First Name:</td>
                                            <td><sw360:out value="${newuser.givenname}"/></td>
                                        </tr>
                                        <tr>
                                            <td>Last Name:</td>
                                            <td><sw360:out value="${newuser.lastname}"/></td>
                                        </tr>
                                        <tr>
                                            <td>Email:</td>
                                            <td><sw360:out value="${newuser.email}"/></td>
                                        </tr>
                                        <tr>
                                            <td>Group:</td>
                                            <td><sw360:out value="${newuser.department}"/></td>
                                        </tr>
                                        <tr>
                                            <td>Role:</td>
                                            <td><sw360:DisplayEnum value="${newuser.userGroup}"/></td>
                                        </tr>
                                        <tr>
                                            <td>External Id:</td>
                                            <td><sw360:out value="${newuser.externalid}"/></td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </core_rt:if>
            </div>
        </div>
    </div>
</div>
