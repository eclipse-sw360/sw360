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
<jsp:useBean id='svmSyncIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='svmSyncOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmSyncInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmSyncNextSync' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmMatchIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='svmMatchOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmMatchInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmMatchNextSync' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmListUpdateIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='svmListUpdateOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmListUpdateInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='svmListUpdateNextSync' type="java.lang.String" scope="request"/>
<jsp:useBean id='trackingFeedbackIsScheduled' type="java.lang.Boolean" scope="request"/>
<jsp:useBean id='trackingFeedbackOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='trackingFeedbackInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='trackingFeedbackNextSync' type="java.lang.String" scope="request"/>

<core_rt:set var="isSvmEnabled" value='<%=PortalConstants.IS_SVM_ENABLED%>' />
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="scheduleCvesearchURL" name="scheduleCveSearch">
</portlet:actionURL>

<portlet:actionURL var="unscheduleCvesearchURL" name="unscheduleCveSearch">
</portlet:actionURL>
<portlet:actionURL var="scheduleSvmSyncURL" name="scheduleSvmSync">
</portlet:actionURL>

<portlet:actionURL var="unscheduleSvmSyncURL" name="unscheduleSvmSync">
</portlet:actionURL>

<portlet:actionURL var="triggerSvmSyncURL" name="triggerSvmSync">
</portlet:actionURL>

<portlet:actionURL var="scheduleSvmMatchURL" name="scheduleSvmMatch">
</portlet:actionURL>

<portlet:actionURL var="unscheduleSvmMatchURL" name="unscheduleSvmMatch">
</portlet:actionURL>

<portlet:actionURL var="triggerSvmMatchURL" name="triggerSvmMatch">
</portlet:actionURL>

<portlet:actionURL var="scheduleSvmListUpdateURL" name="scheduleSvmListUpdate">
</portlet:actionURL>

<portlet:actionURL var="unscheduleSvmListUpdateURL" name="unscheduleSvmListUpdate">
</portlet:actionURL>

<portlet:actionURL var="triggerSvmListUpdateURL" name="triggerSvmListUpdate">
</portlet:actionURL>

<portlet:actionURL var="scheduleTrackingFeedbackURL" name="scheduleTrackingFeedback">
</portlet:actionURL>

<portlet:actionURL var="unscheduleTrackingFeedbackURL" name="unscheduleTrackingFeedback">
</portlet:actionURL>

<portlet:actionURL var="triggerTrackingFeedbackURL" name="triggerTrackingFeedback">
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
    <core_rt:if test="${isSvmEnabled == true}">
    <div class="row">
        <div class="col-6">
            <h4>SVM Vulnerabilities Sync</h4>
            <table class="table bordered-table">
                <tr>
                    <th>Schedule Offset</th>
                    <td>${svmSyncOffset} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Interval</th>
                    <td>${svmSyncInterval} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Next Synchronization</th>
                    <td>${svmSyncNextSync}</td>
                </tr>
            </table>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=scheduleSvmSyncURL%>'" <core_rt:if test="${svmSyncIsScheduled}">disabled</core_rt:if>>Schedule SVM Sync</button>
                    <button type="button" class="btn btn-light" onclick="window.location.href='<%=unscheduleSvmSyncURL%>'" <core_rt:if test="${not svmSyncIsScheduled}">disabled</core_rt:if>>Cancel Scheduled SVM Sync</button>
                </div>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-6">
            <h4>SVM Vulnerabilities Reverse Match</h4>
            <table class="table bordered-table">
                <tr>
                    <th>Schedule Offset</th>
                    <td>${svmMatchOffset} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Interval</th>
                    <td>${svmMatchInterval} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Next Synchronization</th>
                    <td>${svmMatchNextSync}</td>
                </tr>
            </table>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=scheduleSvmMatchURL%>'" <core_rt:if test="${svmMatchIsScheduled}">disabled</core_rt:if>>Schedule SVM Reverse Match</button>
                    <button type="button" class="btn btn-light" onclick="window.location.href='<%=unscheduleSvmMatchURL%>'" <core_rt:if test="${not svmMatchIsScheduled}">disabled</core_rt:if>>Cancel Scheduled SVM Reverse Match</button>
                </div>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-6">
            <h4>SVM Monitoring List Update</h4>
            <table class="table bordered-table">
                <tr>
                    <th>Schedule Offset</th>
                    <td>${svmListUpdateOffset} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Interval</th>
                    <td>${svmListUpdateInterval} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Next Synchronization</th>
                    <td>${svmListUpdateNextSync}</td>
                </tr>
            </table>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=scheduleSvmListUpdateURL%>'" <core_rt:if test="${svmListUpdateIsScheduled}">disabled</core_rt:if>>Schedule SVM Monitoring List Update</button>
                    <button type="button" class="btn btn-light" onclick="window.location.href='<%=unscheduleSvmListUpdateURL%>'" <core_rt:if test="${not svmListUpdateIsScheduled}">disabled</core_rt:if>>Cancel Scheduled SVM Monitoring List Update</button>
                </div>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-6">
            <h4>SVM Release Tracking Feedback</h4>
            <table class="table bordered-table">
                <tr>
                    <th>Schedule Offset</th>
                    <td>${trackingFeedbackOffset} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Interval</th>
                    <td>${trackingFeedbackInterval} (hh:mm:ss)</td>
                </tr>
                <tr>
                    <th>Next Synchronization</th>
                    <td>${trackingFeedbackNextSync}</td>
                </tr>
            </table>
            <form class="form mt-3">
                <div class="form-group">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=scheduleTrackingFeedbackURL%>'" <core_rt:if test="${trackingFeedbackIsScheduled}">disabled</core_rt:if>>Schedule SVM Release Tracking Feedback</button>
                    <button type="button" class="btn btn-light" onclick="window.location.href='<%=unscheduleTrackingFeedbackURL%>'" <core_rt:if test="${not trackingFeedbackIsScheduled}">disabled</core_rt:if>>Cancel Scheduled SVM Release Tracking Feedback</button>
                </div>
            </form>
        </div>
    </div>
    </core_rt:if>
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
                <core_rt:if test="${isSvmEnabled == true}">
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerSvmSyncURL%>'">SVM Vulnerabilities Sync</button>
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerSvmMatchURL%>'">SVM Vulnerabilities Reverse Match</button>
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerSvmListUpdateURL%>'">SVM Monitoring List Update</button>
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerTrackingFeedbackURL%>'">SVM Release Tracking Feedback</button>
                </core_rt:if>
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerDeleteAttachmentURL%>'"><liferay-ui:message key="attachment.deletion.from.local.fs" /></button>
                    <button type="button" class="btn btn-primary" onclick="window.location.href='<%=triggerCveSearchURL%>'"><liferay-ui:message key="cve.search" /></button>
                </div>
            </form>
        </div>
    </div>
</div>
