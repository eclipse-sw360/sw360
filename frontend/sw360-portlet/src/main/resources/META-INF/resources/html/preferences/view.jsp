<%--
  ~ Copyright Siemens AG, 2017-2019. Part of the SW360 Portal Project.
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

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <button type="button" id="updateSettingsBtn" class="btn btn-primary">Update Settings</button>
                        </div>
                    </div>
                </div>
				<div class="col portlet-title text-truncate" title="User Preferences">
					User Preferences
				</div>
            </div>

            <div class="row">
                <div class="col-6">
                    <h5>E-Mail Notification Preferences</h5>
                    <form action="<%=saveUserPreferencesURL%>" id="preferencesForm" method="post">
                        <div class="form-group">
                            <div class="form-check">
                                <input type="checkbox" class="form-check-input" name="<portlet:namespace/><%=User._Fields.WANTS_MAIL_NOTIFICATION%>" id="wants_mail_notification"
                                                    <core_rt:if test="${sw360User.wantsMailNotification == 'true'}"> checked="checked" </core_rt:if> />
                                <label class="form-check-label" for="wants_mail_notification">Enable E-Mail Notifications</label>
                            </div>
                        </div>

                        <div class="alert alert-info">
                            You will be notified on changes of an item if you have the selected role in the changed item.
                        </div>

                        <div class="accordion" id="notificationSettings">
                            <core_rt:forEach items="${eventsConfig}" var="config" varStatus="status">
                                <div class="card">
                                    <div class="card-header" id="${config.key}Heading">
                                        <h2 class="mb-0">
                                            <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#${config.key}" aria-expanded="${status.first ? 'true' : 'false'}" aria-controls="${config.key}">
                                                ${config.key}
                                            </button>
                                        </h2>
                                    </div>
                                    <div id="${config.key}" class="collapse ${status.first ? 'show' : ''}" aria-labelledby="${config.key}Heading" data-parent="#notificationSettings">
                                        <div class="card-body">
                                             <core_rt:forEach items="${config.value}" var="fieldEntry">
                                                <div class="form-group">
                                                    <div class="form-check">
                                                        <input id="${config.key}${fieldEntry.key}" type="checkbox" class="form-check-input" name="<portlet:namespace/><%=User._Fields.NOTIFICATION_PREFERENCES%>${config.key}${fieldEntry.key}"
                                                                <core_rt:if test="${'true' == sw360User.notificationPreferences[config.key.concat(fieldEntry.key)]}"> checked="checked" </core_rt:if>/>
                                                        <label class="form-check-label" for="${config.key}${fieldEntry.key}">${fieldEntry.value}</label>
                                                    </div>
                                                </div>
                                            </core_rt:forEach>
                                        </div>
                                    </div>
                                </div>
                            </core_rt:forEach>
                        </div>
                    </form>
                </div>
                <div class="col-6">
                    <table id="readOnlyUserData" class="table label-value-table">
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
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <core_rt:if test="${enableTokenGenerator}">
                        <h4>REST API Tokens</h4>
                        <form id="generateTokenForm" action="<%=generateTokenURL%>" method="post" novalidate class="needs-validation">
                            <table class="table label-value-table" id="restInfoTable">
                                <thead>
                                <tr>
                                    <th colspan="2">REST API Token</th>
                                </tr>
                                </thead>
                                <tr>
                                    <td>Name:</td>
                                    <td>
                                        <div class="form-group">
                                            <input class="form-control" id="rest_token" required=""
                                                name="<portlet:namespace/><%=RestApiToken._Fields.NAME%>"
                                                type="text" placeholder="Enter token name" value="" />
                                            <div class="invalid-feedback">
                                                Please enter a token name!
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Authorities:</td>
                                    <td>
                                        <div class="form-check">
                                            <input type="checkbox" name="<portlet:namespace/><%=RestApiToken._Fields.AUTHORITIES%>READ"
                                                id="authorities_read" class="form-check-input" />
                                            <label class="form-check-label" for="authorities_read">Read Access</label>
                                            <br>
                                            <input type="checkbox" name="<portlet:namespace/><%=RestApiToken._Fields.AUTHORITIES%>WRITE"
                                                id="authorities_write" class="form-check-input" />
                                            <label class="form-check-label" for="authorities_write">Write Access</label>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Expiration Date:</td>
                                    <td>
                                        <input id="expirationDate" class="datepicker form-control" required="" autocomplete="off"
                                            name="<portlet:namespace/>expirationDate"
                                            type="text" placeholder="Enter expiration date" pattern="\d{4}-\d{2}-\d{2}" value="" />
                                        <div class="invalid-feedback">
                                            Please enter an expiration date!
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Token:
                                        <span title="Authorization Header (Authorization: Token <API-Token>)">
                                            <clay:icon symbol="info-circle-open" />
                                        </span>
                                    </td>
                                    <td>
                                        <label id="accesstoken" class="inlinelabel"><b><sw360:out value="${accessToken}"/></b></label>
                                    </td>
                                </tr>
                            </table>
                            <button type="submit" class="btn btn-secondary">Generate Token</button>
                        </form>

                        <core_rt:if test="${accessTokenList.size()>0}">
                            <table class="table edit-table four-columns-with-actions mt-4">
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
                                        <td><button type="button" class="btn btn-sm btn-danger"
                                                onclick="window.location=generateDeleteTokenUrl('${tokenFromTokenList.name}')">Revoke Token</button>
                                        </td>
                                    </tr>
                                </core_rt:forEach>
                                </tbody>
                            </table>
                        </core_rt:if>
                    </core_rt:if>
                </div>
            </div>
		</div>
	</div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    function generateDeleteTokenUrl(name) {
        return '<%=deleteTokenURL%>'
            .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', name);
    }

    require(['jquery', 'modules/validation', 'bridges/jquery-ui'], function ($, validation) {

        var $wantsMailNotification = $('#wants_mail_notification');
        setDisabledStateOfPreferenceCheckboxes(!$wantsMailNotification.is(":checked"));

        if($('#generateTokenForm').length > 0) {
            validation.enableForm('#generateTokenForm');
        }

        // register event handlers
        $wantsMailNotification.on('change', function() {
            setDisabledStateOfPreferenceCheckboxes(!this.checked);
        });

        $('#updateSettingsBtn').on('click', function() {
            $('#preferencesForm').submit();
        });

        function setDisabledStateOfPreferenceCheckboxes(disabled){
            $("#notificationSettings").find(":checkbox").each(function () {
                this.disabled = disabled;
            });
        }

        $('.datepicker').datepicker({changeMonth:true,changeYear:true,dateFormat: "yy-mm-dd"});
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
</script>
