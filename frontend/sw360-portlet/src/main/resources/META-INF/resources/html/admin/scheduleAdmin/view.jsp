<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %><%--
  ~ Copyright (c) Bosch Software Innovations GmbH 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<jsp:useBean id='cveSearchIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='anyServiceIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='cvesearchOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='cvesearchInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='cvesearchNextSync' type="java.lang.String" scope="request"/>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="scheduleCvesearchURL" name="scheduleCveSearch">
</portlet:actionURL>

<portlet:actionURL var="unscheduleCvesearchURL" name="unscheduleCveSearch">
</portlet:actionURL>

<portlet:actionURL var="unscheduleAllServicesURL" name="unscheduleAllServices">
</portlet:actionURL>

<div class="container">
    <div class="row portlet-toolbar">
        <div class="col-auto">
            <div class="btn-toolbar" role="toolbar">
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-danger" onclick="window.location.href='<%=unscheduleAllServicesURL%>'" <core_rt:if test="${not anyServiceIsScheduled}">disabled</core_rt:if>>Cancel all Scheduled Tasks</button>
                </div>
            </div>
        </div>
        <div class="col portlet-title text-truncate" title="Schedule Task Administration">
            Schedule Task Administration
        </div>
    </div>

    <div class="row">
        <div class="col-6">
            <h4>CVE Search</h4>
            <table class="table bordered-table">
                <tr>
                    <th>Schedule Offset</th>
                    <td>${cvesearchOffset} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Interval</th>
                    <td>${cvesearchInterval} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Next Synchronization</th>
                    <td>${cvesearchNextSync}</td>
                </tr>
            </table>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=scheduleCvesearchURL%>'" <core_rt:if test="${cveSearchIsScheduled}">disabled</core_rt:if>>Schedule CVE service</button>
                    <button type="button" class="btn btn-light" onclick="window.location.href='<%=unscheduleCvesearchURL%>'" <core_rt:if test="${not cveSearchIsScheduled}">disabled</core_rt:if>>Cancel CVE service</button>
                </div>
            </form>
        </div>
    </div>
</div>
