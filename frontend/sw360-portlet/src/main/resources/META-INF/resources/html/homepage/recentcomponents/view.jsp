<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.portal.portlets.Sw360Portlet" %>
<%@ page import="org.eclipse.sw360.portal.portlets.components.ComponentPortlet" %>
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
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="components" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Component>"
             class="java.util.ArrayList" scope="request"/>


<h4><liferay-ui:message key="recent.components" /></h4>
<div class="row">
    <div class="col">
        <core_rt:if test="${components.size() > 0 }">
            <ul>
                <core_rt:forEach var="component" items="${components}">
                    <li>
                        <sw360:DisplayComponentLink component="${component}"/><br>
                    </li>
                </core_rt:forEach>
            </ul>
        </core_rt:if>
        <core_rt:if test="${components.size() == 0}">
            <div class="alert alert-info"><liferay-ui:message key="no.recent.components" /></div>
        </core_rt:if>
    </div>
</div>
