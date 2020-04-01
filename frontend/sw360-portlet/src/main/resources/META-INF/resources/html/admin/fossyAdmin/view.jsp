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
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.portal.portlets.admin.FossologyAdminPortlet" %>
<%@ page import="org.eclipse.sw360.portal.common.FossologyConnectionHelper" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="checkConnectionURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.FOSSOLOGY_CHECK_CONNECTION%>"/>
</portlet:resourceURL>
<portlet:actionURL var="updateConfigURL" name="updateConfig" />

<jsp:useBean id="fossologyConfig" type="java.util.Map<java.lang.String, java.lang.String>" scope="request"/>

<core_rt:set var="fossologyEnabled" value="<%=FossologyConnectionHelper.getInstance().isFossologyConnectionEnabled()%>"/>
<core_rt:choose>
	<core_rt:when test="${fossologyEnabled}">
	    <core_rt:set var="currentConnStatus" value="SUCCESS" />
        <core_rt:set var="currentConnStatusBadge" value="badge-success" />
	</core_rt:when>
	<core_rt:otherwise>
		<core_rt:set var="currentConnStatus" value="FAILURE" />
        <core_rt:set var="currentConnStatusBadge" value="badge-danger" />
	</core_rt:otherwise>
</core_rt:choose>

<div class="container">
	<div class="row">
		<div class="col">

            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" data-action="check"><liferay-ui:message key="re.check.connection" /></button>
						</div>
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="save.configuration" /></button>
						</div>
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message key="cancel" /></button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="fossology.connection.administration" />">
					<liferay-ui:message key="fossology.connection.administration" />
				</div>
            </div>

            <div class="row mt-3 mb-4">
                <div class="col">
                    <table class="table edit-table three-columns">
                        <thead>
                            <tr>
                                <th><liferay-ui:message key="connection.status" /></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>
                                    <liferay-ui:message key="connection.to.fossology.is.currently.in.state" />:
                                    <span id="checkResult" class="badge ${currentConnStatusBadge} mx-3"><liferay-ui:message key="${currentConnStatus}" /></span>
                                    <span class="font-italic"><liferay-ui:message key="checked.on.saved.configuration.which.might.be.different.from.displayed.one.if.you.already.edited.it" /></span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <core_rt:set var="configKeyUrl" value="<%=FossologyAdminPortlet.CONFIG_KEY_URL %>"/>
                    <core_rt:set var="configKeyFolderId" value="<%=FossologyAdminPortlet.CONFIG_KEY_FOLDER_ID %>"/>
                    <core_rt:set var="configKeyToken" value="<%=FossologyAdminPortlet.CONFIG_KEY_TOKEN %>"/>
                    <form id="fossologyConfigurationForm" action="<%=updateConfigURL%>" method="post" class="form">
                        <table id="fossologyConfigEdit" class="table edit-table two-columns">
                            <thead>
                                <tr>
                                    <th colspan="2"><liferay-ui:message key="connection.configuration" /></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="fossologyUrl"><liferay-ui:message key="url" /></label>
                                            <input id="fossologyUrl" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.url.like" />" name="<portlet:namespace/><%=PortalConstants.FOSSOLOGY_CONFIG_KEY_URL%>"
                                                value="<sw360:out value="${fossologyConfig[configKeyUrl]}"/>" />
                                             <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.url" />
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label for="fossologyFolderId"><liferay-ui:message key="folder.id" /></label>
                                            <input id="fossologyFolderId" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.folder.id" />" name="<portlet:namespace/><%=PortalConstants.FOSSOLOGY_CONFIG_KEY_FOLDER_ID%>"
                                                value="<sw360:out value="${fossologyConfig[configKeyFolderId]}"/>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.folder.id" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="fossologyToken"><liferay-ui:message key="access.token" /></label>
                                            <input id="fossologyToken" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.access.token" />" name="<portlet:namespace/><%=PortalConstants.FOSSOLOGY_CONFIG_KEY_TOKEN%>"
                                                value="<sw360:out value="${fossologyConfig[configKeyToken]}"/>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.token" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </form>
                </div>
            </div>
		</div>
	</div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'modules/validation' ], function($, validation) {

        validation.enableForm('#fossologyConfigurationForm');

        $('.portlet-toolbar button[data-action="check"]').on('click', function() {
            doAjax('<%=checkConnectionURL%>', $('#checkResult'));
        });

        $('.portlet-toolbar button[data-action="save"]').on('click', function() {
            $('#fossologyConfigurationForm').submit();
        });

        $('.portlet-toolbar button[data-action="cancel"]').on('click', function() {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
            var portletURL = Liferay.PortletURL.createURL( baseUrl );
            window.location = portletURL.toString();
        });

        function doAjax(url, $resultElement) {
            $resultElement.addClass('badge-light');
            $resultElement.removeClass('badge-danger');
            $resultElement.removeClass('badge-success');
            $resultElement.text("...");

            $.ajax({
                type: 'POST',
                url: url,
                success: function (data) {
                    $resultElement.removeClass('badge-light');
                    if(data.result === 'FAILURE') {
                        $resultElement.addClass('badge-danger');
                        var msg = "<liferay-ui:message key="FAILURE" />"; 
                    } else {
                        $resultElement.addClass('badge-success');
                        var msg = "<liferay-ui:message key="SUCCESS" />";
                    }

                    $resultElement.text(msg);
                },
                error: function () {
                    $resultElement.removeClass('badge-light');
                    $resultElement.addClass('badge-danger');
                    $resultElement.text("Error");
                }
            });
        }
    });
</script>
