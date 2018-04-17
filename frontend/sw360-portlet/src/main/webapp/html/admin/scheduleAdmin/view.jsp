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
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
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

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Schedule Task Administration</span> </p>

<h4 class="withTopMargin">CVE Search: </h4>
<br/>
<b>Settings for scheduling the CVE search service:</b><br/>
Offset: ${cvesearchOffset} (hh:mm:ss)<br/>
Interval: ${cvesearchInterval} (hh:mm:ss)<br/>
Next Synchronization: ${cvesearchNextSync}<br/>
<br/>
<input type="button"
       <core_rt:if test="${cveSearchIsScheduled}">class="notApplicableButton"</core_rt:if>
       <core_rt:if test="${not cveSearchIsScheduled}">class="addButton" onclick="window.location.href='<%=scheduleCvesearchURL%>'"</core_rt:if>
       value="Schedule CveSearch Updates">

<input type="button"
       <core_rt:if test="${cveSearchIsScheduled}">class="addButton"  onclick="window.location.href='<%=unscheduleCvesearchURL%>'"</core_rt:if>
       <core_rt:if test="${not cveSearchIsScheduled}">class="notApplicableButton"</core_rt:if>
       value="Cancel Scheduled CveSearch Updates">

<h4 class="withTopMargin">All Services:</h4>

<input type="button"
       <core_rt:if test="${anyServiceIsScheduled}">class="addButton" onclick="window.location.href='<%=unscheduleAllServicesURL%>'" </core_rt:if>
       <core_rt:if test="${not anyServiceIsScheduled}">class="notApplicableButton" </core_rt:if>
       value="Cancel All Scheduled Tasks">

