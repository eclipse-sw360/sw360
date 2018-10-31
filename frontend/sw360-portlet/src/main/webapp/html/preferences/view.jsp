<%--
  ~ Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
--%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.RestApiToken" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="generateTokenURL" name="createToken"></portlet:actionURL>
<portlet:actionURL var="saveUserPreferencesURL" name="savePreferences"></portlet:actionURL>

<portlet:actionURL var="deleteTokenURL" name="deleteToken">
    <portlet:param name="<%=PortalConstants.API_TOKEN_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</portlet:actionURL>

<jsp:useBean id="sw360User" type="org.eclipse.sw360.datahandler.thrift.users.User" scope="request"/>
<jsp:useBean id="eventsConfig" type="java.util.Map<java.lang.String, java.util.List<java.util.Map.Entry<java.lang.String, java.lang.String>>>" scope="request"/>
<jsp:useBean id="accessToken" class="java.lang.String" scope="request"/>
<jsp:useBean id="accessTokenList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.users.RestApiToken>" scope="request"/>

<core_rt:set var="enableTokenGenerator" value='<%=PortalConstants.API_TOKEN_ENABLE_GENERATOR%>'/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
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
            <td>E-mail:</td>
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

    <core_rt:if test="${enableTokenGenerator}">
        <hr>
        <form action="<%=generateTokenURL%>" method="post">
            <table class="table info_table" id="restInfoTable">
                <thead>
                <tr>
                    <th colspan="2">REST API Token</th>
                </tr>
                </thead>
                <tr>
                    <td>Name:</td>
                    <td>
                        <input class="toplabelledInput" id="rest_token" required=""
                               name="<portlet:namespace/><%=RestApiToken._Fields.NAME%>"
                               type="text" placeholder="Enter token name" value=""/>
                    </td>
                </tr>
                <tr>
                    <td>Authorities:</td>
                    <td>
                        <input type="checkbox" name="<portlet:namespace/><%=RestApiToken._Fields.AUTHORITIES%>READ"
                               id="authorities_read"/>
                        <label class="checkboxlabel inlinelabel" for="authorities_read">Read Access</label>
                        <br>
                        <input type="checkbox" name="<portlet:namespace/><%=RestApiToken._Fields.AUTHORITIES%>WRITE"
                               id="authorities_write"/>
                        <label class="checkboxlabel inlinelabel" for="authorities_write">Write Access</label>
                    </td>
                </tr>
                <tr>
                    <td>Expiration Date:</td>
                    <td>
                        <input id="expirationDate" class="datepicker" required="" autocomplete="off"
                               name="<portlet:namespace/>expirationDate"
                               type="text" placeholder="Enter expiration date" pattern="\d{4}-\d{2}-\d{2}" value=""/>
                    </td>
                </tr>
                <tr>
                    <td>Token:
                        <img class="infopic" src="<%=request.getContextPath()%>/images/ic_info.png"
                             title="Authorization Header (Authorization: Token <API-Token>)"/>
                    </td>
                    <td>
                        <label id="accesstoken" class="inlinelabel"><b><sw360:out value="${accessToken}"/></b></label>
                    </td>
                </tr>
            </table>
            <input type="submit" class="addButton" value="Generate Token">
        </form>

        <core_rt:if test="${accessTokenList.size()>0}">
            <h5>Active REST API Tokens</h5>
            <table class="table info_table">
                <thead>
                <tr>
                    <th>Token Name</th>
                    <th>Created On</th>
                    <th>Expiration Date</th>
                    <th>Authorities</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <core_rt:forEach var="tokenFromTokenList" items="${accessTokenList}">
                    <tr>
                        <td><sw360:out value="${tokenFromTokenList.name}"/></td>
                        <td><sw360:out value="${tokenFromTokenList.createdOn}"/></td>
                        <td><sw360:DisplayApiTokenExpireDate token="${tokenFromTokenList}"/></td>
                        <td><sw360:out value="${tokenFromTokenList.authorities}"/></td>
                        <td><input type="button" class="addButton" value="Revoke Token"
                                   onclick="window.location=generateDeleteTokenUrl('${tokenFromTokenList.name}')">
                        </td>
                    </tr>
                </core_rt:forEach>
                </tbody>
            </table>
        </core_rt:if>
    </core_rt:if>

    <hr>
    <form action="<%=saveUserPreferencesURL%>" id="preferences_form" method="post">
        <table class="table info_table" id="readOnlyUserData-rest">
            <thead>
            <tr>
                <th colspan="1">E-Mail Notification Preferences</th>
            </tr>
            </thead>
            <tr>
                <td>
                    <p>
                        <input type="checkbox" name="<portlet:namespace/><%=User._Fields.WANTS_MAIL_NOTIFICATION%>" id="wants_mail_notification"
                                <core_rt:if test="${sw360User.wantsMailNotification == 'true'}"> checked="checked" </core_rt:if>/>
                        <label class="checkboxlabel inlinelabel" for="wants_mail_notification">Enable E-Mail Notifications</label>
                    </p>
                </td>
            </tr>
            <core_rt:forEach items="${eventsConfig}" var="config">
                <tr class="notifiable_events">
                    <td>
                        <h6>Receive notifications for changes in ${config.key} where your role is:</h6>
                        <core_rt:forEach items="${config.value}" var="fieldEntry">
                            <input type="checkbox" class="indented"
                                   name="<portlet:namespace/><%=User._Fields.NOTIFICATION_PREFERENCES%>${config.key}${fieldEntry.key}" id="${config.key}${fieldEntry.key}"
                                    <core_rt:if test="${'true' == sw360User.notificationPreferences[config.key.concat(fieldEntry.key)]}"> checked="checked" </core_rt:if>/>
                            <label class="checkboxlabel inlinelabel" for="${config.key}${fieldEntry.key}">${fieldEntry.value}</label><br>
                        </core_rt:forEach>
                    </td>
                </tr>
            </core_rt:forEach>
        </table>
        <input type="submit" class="addButton" value="Save">
    </form>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>

    // Generate delete token URL for revoke function
    function generateDeleteTokenUrl(name) {
        return '<%=deleteTokenURL%>'
            .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', name);
    }

    AUI().use('liferay-portlet-url', function () {

        require(['jquery', /* jquery-plugins */ 'jquery-ui'], function ($) {

            var $wantsMailNotification = $('#wants_mail_notification');
            setDisabledStateOfPreferenceCheckboxes(!$wantsMailNotification.is(":checked"));

            // register event handlers
            $wantsMailNotification.on('change', function() {
                setDisabledStateOfPreferenceCheckboxes(!this.checked);
            });

            function setDisabledStateOfPreferenceCheckboxes(disabled){
                $(".notifiable_events").find(":checkbox").each(function () {
                    this.disabled = disabled;
                })
            }

            $('.datepicker').datepicker({dateFormat: "yy-mm-dd"});
            $('.datepicker').datepicker('option', 'minDate', new Date());

            $('#authorities_read').change(function () {
                calcMaxDate();
            });

            $('#authorities_write').change(function () {
                calcMaxDate();
            });

            function calcMaxDate() {
                var expireDate = new Date();
                var maxNumberOfValidDays = 0;

                if ($('#authorities_write').is(":checked")) {
                    maxNumberOfValidDays = <%=PortalConstants.API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS%>;
                } else {
                    maxNumberOfValidDays = <%=PortalConstants.API_TOKEN_MAX_VALIDITY_READ_IN_DAYS%>;
                }

                expireDate.setDate(expireDate.getDate() + maxNumberOfValidDays);
                $('.datepicker').datepicker('option', 'maxDate', expireDate);
            }

        });
    });
</script>
