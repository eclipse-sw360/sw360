<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %><%--
  ~ Copyright (c) Bosch Software Innovations GmbH 2016.
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

<portlet:actionURL var="scheduleDeleteAttachmentURL" name="scheduleDeleteAttachment">
</portlet:actionURL>

<portlet:actionURL var="unscheduleDeleteAttachmentURL" name="unscheduleDeleteAttachment">
</portlet:actionURL>

<portlet:actionURL var="triggerDeleteAttachmentURL" name="triggerDeleteAttachment">
</portlet:actionURL>

<portlet:actionURL var="triggerCveSearchURL" name="triggerCveSearch">
</portlet:actionURL>

<div class="container">
    <div class="row portlet-toolbar">
        <div class="col-auto">
            <div class="btn-toolbar" role="toolbar">
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-danger" onclick="window.location.href='<%=unscheduleAllServicesURL%>'" <core_rt:if test="${not anyServiceIsScheduled}">disabled</core_rt:if>><liferay-ui:message key="cancel.all.scheduled.tasks" /></button>
                </div>
            </div>
        </div>
        <div class="col portlet-title text-truncate" title="<liferay-ui:message key="schedule.task.administration" />">
            <liferay-ui:message key="schedule.task.administration" />
        </div>
    </div>

    <div class="row">
        <div class="col-6">
            <h4><liferay-ui:message key="cve.search" /></h4>
            <table class="table bordered-table">
                <tr>
                    <th><liferay-ui:message key="schedule.offset" /></th>
                    <td>${cvesearchOffset} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th><liferay-ui:message key="interval" /></th>
                    <td>${cvesearchInterval} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th><liferay-ui:message key="next.synchronization" /></th>
                    <td>${cvesearchNextSync}</td>
                </tr>
            </table>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=scheduleCvesearchURL%>'" <core_rt:if test="${cveSearchIsScheduled}">disabled</core_rt:if>><liferay-ui:message key="schedule.cve.service" /></button>
                    <button type="button" class="btn btn-light" onclick="window.location.href='<%=unscheduleCvesearchURL%>'" <core_rt:if test="${not cveSearchIsScheduled}">disabled</core_rt:if>><liferay-ui:message key="cancel.cve.service" /></button>
                </div>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-6">
            <h4><liferay-ui:message key="attachment.deletion.from.local.fs" /></h4>
             <table class="table bordered-table">
                <tr>
                    <th><liferay-ui:message key="schedule.offset" /></th>
                     <td><sw360:out value="${deleteAttachmentOffset} (hh:mm:ss)" /></td>
                </tr>
                <tr>
                    <th><liferay-ui:message key="interval" /></th>
                    <td><sw360:out value="${deleteAttachmentInterval} (hh:mm:ss)" /></td>
                </tr>
                <tr>
                    <th><liferay-ui:message key="next.synchronization" /></th>
                     <td><sw360:out value="${deleteAttachmentNextSync}"/></td>
                </tr>
            </table>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=scheduleDeleteAttachmentURL%>'"<core_rt:if test="${deleteAttachmentIsScheduled}">disabled</core_rt:if>><liferay-ui:message key="schedule.attachment.deletion.from.local.fs" /></button>
                    <button type="button" class="btn btn-light" onclick="window.location.href='<%=unscheduleDeleteAttachmentURL%>'"<core_rt:if test="${not deleteAttachmentIsScheduled}">disabled</core_rt:if>><liferay-ui:message key="cancel.scheduled.attachment.deletion.from.local.fs" /></button>
                </div>
            </form>
        </div>
    </div>
    <div class="row">
        <div>
            <h4><liferay-ui:message key="manual.triggering.of.scheduled.services" /></h4>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerDeleteAttachmentURL%>'"><liferay-ui:message key="attachment.deletion.from.local.fs" /></button>
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerCveSearchURL%>'"><liferay-ui:message key="cve.search" /></button>
                </div>
            </form>
        </div>
    </div>
</div>
