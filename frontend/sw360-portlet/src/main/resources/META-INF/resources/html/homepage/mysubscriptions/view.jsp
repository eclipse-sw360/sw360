<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.portal.portlets.Sw360Portlet" %>
<%@ page import="org.eclipse.sw360.portal.portlets.components.ComponentPortlet" %>

<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="componentList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Component>"
             class="java.util.ArrayList" scope="request"/>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>"
             class="java.util.ArrayList" scope="request"/>

<h4>My Subscriptions</h4>
<core_rt:if test="${componentList.size() > 0}">
    <div class="row">
        <div class="col">
            <h6>Components</h6>
            <core_rt:forEach var="component" items="${componentList}">
                <li>
                    <sw360:DisplayComponentLink component="${component}"/><br>
                </li>
            </core_rt:forEach>
        </div>
    </div>
</core_rt:if>

<core_rt:if test="${releaseList.size() > 0}">
    <div class="row">
        <div class="col">
            <h6>Releases</h6>
            <ul>
                <core_rt:forEach var="release" items="${releaseList}">
                    <li>
                        <sw360:DisplayReleaseLink release="${release}"/><br>
                    </li>
                </core_rt:forEach>
            </ul>
        </div>
    </div>
</core_rt:if>

<core_rt:if test="${componentList.size() == 0  and releaseList.size() == 0}">
    <div class="alert alert-info">
        No subscriptions available.
    </div>
</core_rt:if>
