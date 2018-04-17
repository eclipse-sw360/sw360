<%--
  ~ Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
--%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="sw360User" type="org.eclipse.sw360.datahandler.thrift.users.User" scope="request"/>
<jsp:useBean id="eventsConfig" type="java.util.Map<java.lang.String, java.util.List<java.util.Map.Entry<java.lang.String, java.lang.String>>>" scope="request"/>

<portlet:actionURL var="saveUserPreferencesURL" name="savePreferences"/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">User Preferences</span></p>
<div id="content">
    <table class="table info_table" id="readOnlyUserData">
        <thead>
            <tr>
                <th colspan="2">SW360 User</th>
            </tr>
        </thead>
        <tr>
            <td>Name:</td>
            <td><sw360:out value="${sw360User.fullname}"/></td>
        </tr>
        <tr>
            <td>Id (E-Mail):</td>
            <td><sw360:DisplayUserEmail email="${sw360User.email}" bare="true"/></td>
        </tr>
        <tr>
            <td>Group:</td>
            <td><sw360:out value="${sw360User.department}"/></td>
        </tr>
        <tr>
            <td>External Id:</td>
            <td><sw360:out value="${sw360User.externalid}"/></td>
        </tr>
        <tr>
            <td>Role:</td>
            <td><sw360:DisplayEnum value="${sw360User.userGroup}"/></td>
        </tr>
    </table>
    <form action="<%=saveUserPreferencesURL%>" id="preferences_form" method="post">
        <h4>E-Mail Notification Preferences</h4>
        <p>
            <input type="checkbox" name="<portlet:namespace/><%=User._Fields.WANTS_MAIL_NOTIFICATION%>" id="wants_mail_notification"
            <core_rt:if test="${sw360User.wantsMailNotification == 'true'}"> checked="checked" </core_rt:if>/>
            <label class="checkboxlabel inlinelabel" for="wants_mail_notification">Enable E-Mail Notifications</label>
        </p>
        <div id="notifiable_events" class="indented">
        <core_rt:forEach items="${eventsConfig}" var="config">
            <h5>Receive notifications for changes in ${config.key} where your role is :</h5>
            <core_rt:forEach items="${config.value}" var="fieldEntry">
                <div class="indented">
                <input type="checkbox" name="<portlet:namespace/><%=User._Fields.NOTIFICATION_PREFERENCES%>${config.key}${fieldEntry.key}" id="${config.key}${fieldEntry.key}"
                <core_rt:if test="${'true' == sw360User.notificationPreferences[config.key.concat(fieldEntry.key)]}"> checked="checked" </core_rt:if>/>
                <label class="checkboxlabel inlinelabel" for="${config.key}${fieldEntry.key}">${fieldEntry.value}</label>
                </div>
            </core_rt:forEach>
        </core_rt:forEach>
        </div>
        <br/>
        <input type="submit" class="addButton" value="Save">
    </form>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery'], function($) {

            var $wantsMailNotification = $('#wants_mail_notification');
            setDisabledStateOfPreferenceCheckboxes(!$wantsMailNotification.is(":checked"));

            // register event handlers
            $wantsMailNotification.on('change', function() {
                setDisabledStateOfPreferenceCheckboxes(!this.checked);
            });

            function setDisabledStateOfPreferenceCheckboxes(disabled){
                $("#notifiable_events").find(":checkbox").each(function () {
                    this.disabled = disabled;
                })
            }
        });
    });
</script>
