<%--
  ~ Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil"%>
<%@ page import="javax.portlet.PortletRequest"%>
<%@ page import="org.eclipse.sw360.datahandler.common.SW360Constants"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.UserGroup"%>

<%@ include file="/html/init.jsp"%>
<%@ include file="/html/utils/includes/logError.jspf"%>
<%@ include file="/html/utils/includes/requirejs.jspf"%>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="userObj" class="org.eclipse.sw360.datahandler.thrift.users.User" scope="request" />
    <core_rt:set var="addMode" value="${empty userObj.email}" />
    <core_rt:set var="ssoLoginEnabled" value='<%=PortalConstants.SSO_LOGIN_ENABLED%>' />
    <core_rt:set var="secondaryDepartmentsAndRolesEntrySet" value="${userObj.secondaryDepartmentsAndRoles.entrySet()}" />
    <core_rt:set var="clientInfosEntrySet" value="${userObj.oidcClientInfos.entrySet()}" />
</c:catch>

<portlet:actionURL var="updateURL" name="update">
    <portlet:param name="<%=PortalConstants.USER_EMAIL%>" value="${userObj.email}" />
</portlet:actionURL>

<portlet:actionURL var="deactivateUserURL" name="deactivate">
    <portlet:param name="<%=PortalConstants.USER_EMAIL%>" value="${userObj.email}" />
</portlet:actionURL>
<div class="container">
    <div class="row">
        <div class="col">
            <core_rt:if test="${not addMode}">
                <core_rt:if test="${userMissingCouchdb}">
                    <div class="fade show alert alert-warning alert-dismissible">
                        <strong><liferay-ui:message key="warning" /> !</strong> <span><liferay-ui:message key="user.not.found.in.couch.db" />. <liferay-ui:message
                                key="update.user.with.correct.data.to.synchronize.liferay.and.couch.db" /></span><br>
                        <button type="button" class="close" data-dismiss="alert">&times;</button>
                    </div>
                </core_rt:if>
                <core_rt:if test="${userMissingLiferay}">
                    <div class="fade show alert alert-warning alert-dismissible">
                        <strong><liferay-ui:message key="warning" /> !</strong> <span><liferay-ui:message key="user.not.found.in.liferay.db" />. <liferay-ui:message
                                key="update.user.with.correct.data.to.synchronize.liferay.and.couch.db" /></span><br>
                        <button type="button" class="close" data-dismiss="alert">&times;</button>
                    </div>
                </core_rt:if>
            </core_rt:if>
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <core_rt:if test="${addMode}">
                                <button type="button" id="formSubmit" class="btn btn-primary">
                                    <liferay-ui:message key="create.user" />
                                </button>
                            </core_rt:if>

                            <core_rt:if test="${not addMode}">
                                <button type="button" id="formSubmit" class="btn btn-primary">
                                    <liferay-ui:message key="update.user" />
                                </button>
                            </core_rt:if>
                        </div>

                        <core_rt:if test="${not addMode}">
                            <div class="btn-group" role="group">
                                <button id="deactivateUserButton" type="button" class="btn btn-danger"
                                    <core_rt:if test="${not userActivateDeactivate}">
                                        disabled
                                    </core_rt:if>>
                                    <core_rt:if test="${not userObj.deactivated}">
                                        <liferay-ui:message key="deactivate.user" />
                                    </core_rt:if>
                                    <core_rt:if test="${userObj.deactivated}">
                                        <liferay-ui:message key="activate.user" />
                                    </core_rt:if>
                                </button>
                            </div>
                        </core_rt:if>

                        <div class="btn-group" role="group">
                            <button id="cancelEditButton" type="button" class="btn btn-light">
                                <liferay-ui:message key="cancel" />
                            </button>
                        </div>

                    </div>
                </div>
                <core_rt:if test="${not addMode}">
                    <div class="col portlet-title text-truncate" title="<sw360:UserName user="${userObj}"/>">
                        <sw360:UserName user="${userObj}" />
                    </div>
                </core_rt:if>
            </div>
            <div class="row">
                <div class="col">
                    <form id="userEditForm" name="userEditForm" autocompete="off" action="<%=updateURL%>" class="needs-validation" method="post" novalidate data-deactivate-url="<%=deactivateUserURL%>">
                        <div class="tab-content">
                            <table class="table edit-table two-columns" id="UserBasicInfo">
                                <thead>
                                    <tr>
                                        <th colspan="2"><liferay-ui:message key="general.information" /></th>
                                    </tr>
                                </thead>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="user_name"><liferay-ui:message key="given.name" /></label> <input id="user_name"
                                                name="<portlet:namespace/><%=User._Fields.GIVENNAME%>" type="text" placeholder="<liferay-ui:message key="enter.user.given.name" />" class="form-control"
                                                value="<sw360:out value="${userObj.givenname}"/>" required pattern=".*\S.*" />
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="user_lastname"><liferay-ui:message key="last.name" /></label> <input id="user_lastname" class="form-control"
                                                name="<portlet:namespace/><%=User._Fields.LASTNAME%>" type="text" required pattern=".*\S.*"
                                                placeholder="<liferay-ui:message key="enter.user.lastname" />" value="<sw360:out value="${userObj.lastname}"/>" />
                                        </div>
                                    </td>
                                </tr>

                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="userEmail" class="mandatory"><liferay-ui:message key="email.id" />:</label> <input id="userEmail" type="email" class="form-control"
                                                autocomplete="off" value="<sw360:out value="${userObj.email}"/>" name="<portlet:namespace/><%=User._Fields.EMAIL%>"
                                                placeholder="<liferay-ui:message key='enter.user.email' />" required />
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="user_gid"><liferay-ui:message key="gid" /></label> <input id="user_gid" class="form-control"
                                                name="<portlet:namespace/><%=User._Fields.EXTERNALID%>" type="text" required pattern=".*\S.*" placeholder="<liferay-ui:message key="enter.user.gid" />"
                                                value="<sw360:out value="${userObj.externalid}"/>" />
                                        </div>
                                    </td>
                                </tr>

                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="user_department"><liferay-ui:message key="department" /></label> <input id="user_department" class="form-control"
                                                name="<portlet:namespace/><%=User._Fields.DEPARTMENT%>" type="text" required pattern=".*\S.*"
                                                placeholder="<liferay-ui:message key="enter.user.primary.department" />" value="<sw360:out value="${userObj.department}"/>" />
                                        </div>
                                    </td>

                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="user_primary_roles"><liferay-ui:message key="primary.roles" /></label> <select class="form-control" id="user_primary_roles"
                                                name="<portlet:namespace/><%=User._Fields.USER_GROUP%>" required>
                                                <option value="" class="textlabel stackedLabel"><liferay-ui:message key="select.primary.role" /></option>
                                                <sw360:DisplayEnumOptions type="<%=UserGroup.class%>" useStringValues="true" selected="${userObj.userGroup}" />
                                            </select>
                                        </div>
                                    </td>
                                </tr>

                                <core_rt:if test="${not ssoLoginEnabled}">
                                    <tr>
                                        <td>
                                            <div class="form-group">
                                                <label <core_rt:if test="${addMode or (not isPasswordOptional)}" >class="mandatory"</core_rt:if> for="user_pwd"><liferay-ui:message key="password" />
                                                <core_rt:if test="${not (addMode or (not isPasswordOptional))}" > (<liferay-ui:message key="keep.password.empty.to.reuse.old.password" />)</core_rt:if>
                                                </label> 
                                                <input id="user_pwd" autocomplete="off"
                                                    class="form-control" name="<portlet:namespace/><%=PortalConstants.PASSWORD%>" type="password"
                                                    <core_rt:if test="${addMode or (not isPasswordOptional)}" >required pattern=".*\S.*"</core_rt:if> placeholder="<liferay-ui:message key="enter.user.pwd" />" />
                                            </div>
                                        </td>
                                        <td>
                                        </td>
                                    </tr>

                                </core_rt:if>
                            </table>
                            <core_rt:set var="externalIdsSet" value="${project.externalIds.entrySet()}" />
                            <core_rt:set var="externalIdKeys" value="<%=PortalConstants.PROJECT_EXTERNAL_ID_KEYS%>" />
                            <%@include file="/html/utils/includes/editSecondaryDepartmentAndRoles.jsp"%>
                            <br/>
                            <%@include file="/html/utils/includes/editOauthClientId.jsp"%>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="dialogs auto-dialogs">
    <div id="toggleActivateUserDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <clay:icon symbol="question-circle" />
                        <core_rt:if test="${not userObj.deactivated}">
                            <liferay-ui:message key="deactivate.user" />
                        </core_rt:if>

                        <core_rt:if test="${userObj.deactivated}">
                            <liferay-ui:message key="activate.user" />
                        </core_rt:if>
                        ?
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p>
                        <core_rt:if test="${not userObj.deactivated}">
                            <liferay-ui:message key="do.you.really.want.to.deactivate.the.user.x" />
                        </core_rt:if>
                        <core_rt:if test="${userObj.deactivated}">
                            <liferay-ui:message key="do.you.really.want.to.activate.the.user.x" />
                        </core_rt:if>
                    </p>
                    <hr />
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">
                        <liferay-ui:message key="cancel" />
                    </button>
                    <button type="button" class="btn btn-danger">
                        <core_rt:if test="${not userObj.deactivated}">
                            <liferay-ui:message key="deactivate.user" />
                        </core_rt:if>
                        <core_rt:if test="${userObj.deactivated}">
                            <liferay-ui:message key="activate.user" />
                        </core_rt:if>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
AUI().use('liferay-portlet-url', function () {
require(['jquery', 'modules/dialog', 'modules/validation', 'bridges/jquery-ui' ], function($, dialog, validation) {
    $("div.alert-container").removeClass("cadmin");
    document.title = $("<span></span>").html("<sw360:UserName user="${userObj}"/> - " + document.title).text();

    validation.enableForm('#userEditForm');
    validation.jumpToFailedTab('#userEditForm');
    
    $('#formSubmit').click(
        function () {
            $('#userEditForm').submit();
        }
    );
    $('#cancelEditButton').on('click', cancel);
    $('#deactivateUserButton').on('click', activateOrDeactivateUser);

    function cancel() {
        var baseUrl = '<%=PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(),
					PortletRequest.RENDER_PHASE)%>',
        portletURL = Liferay.PortletURL.createURL(baseUrl)
        <core_rt:if test="${not addMode}">
            .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
        </core_rt:if>
        <core_rt:if test="${addMode}">
            .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>')
        </core_rt:if>
            .setParameter('<%=PortalConstants.USER_EMAIL%>', '${userObj.email}');

        window.location = portletURL.toString() + window.location.hash;
    }

    function activateOrDeactivateUser() {
        var $dialog,
            data = $('#userEditForm').data();

        function activateOrDeactivateInternal() {
            let baseUrl = data.deactivateUrl,
                activateOrDeactivateURL = Liferay.PortletURL.createURL(baseUrl);
            window.location.href = activateOrDeactivateURL;
        }

        $dialog = dialog.open('#toggleActivateUserDialog', {
            email: '${userObj.email}',
        }, function(submit, callback) {
            activateOrDeactivateInternal();
        });
    }

    function makeUserUrl(email, page) {
        var portletURLToEditUser = Liferay.PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
            .setParameter('<%=PortalConstants.PAGENAME%>', page)
            .setParameter('<%=PortalConstants.USER_EMAIL%>', "friendlyEmail");
        return portletURLToEditUser.toString().replace("friendlyEmail", email);
    }

    if (window.history.replaceState) {
        window.history.replaceState(null, document.title, makeUserUrl("${userObj.email}", "edit"));
    }
});
});
</script>
