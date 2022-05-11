<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp"%>

<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.LicenseType" %>

<jsp:useBean id="licenseType" class="org.eclipse.sw360.datahandler.thrift.licenses.LicenseType" scope="request" />

<portlet:actionURL var="addURL" name="addLicenseType">
</portlet:actionURL>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <div class="btn-group">
                            <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="create.license.type" /></button>
                        </div>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message key="cancel" /></button>
                        </div>
					</div>
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <form id="addLicenseTypeForm" name="addLicenseTypeForm" action="<%=addURL%>" method="post" class="form needs-validation" novalidate>
                        <table id="todoAddTable" class="table edit-table three-columns">
                            <thead>
                                <tr>
                                    <th colspan="3"><liferay-ui:message key="add.license.type" /></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="licenseTypeTitle"><liferay-ui:message key="title" /></label>
                                            <input id="licenseTypeTitle" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.title" />" name="<portlet:namespace/><%=LicenseType._Fields.LICENSE_TYPE%>"/>
                                            <div class="invalid-feedback" id="error-empty">
                                                <liferay-ui:message key="please.enter.the.title" />
                                            </div>
                                            <div class="invalid-feedback" id="error-invalid-char">
                                                <liferay-ui:message key="please.fill.a.z.A.Z.0.9.-...+.only" />
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

<div class="dialogs auto-dialogs"></div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery'], function($) {
        $("div.alert-container").removeClass("cadmin");

        $('.portlet-toolbar button[data-action="cancel"]').on('click', function() {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
            var portletURL = Liferay.PortletURL.createURL( baseUrl )
                .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
            window.location = portletURL.toString();
        });

        // Check on input type change
        $('#licenseTypeTitle').on('input', function() {
            if ($('#addLicenseTypeForm').hasClass('was-validated')) {
                validateInput();
            }
        });

        // Check on enter key press
        $('#licenseTypeTitle').keypress(function(event) {
            var keycode = (event.keyCode ? event.keyCode : event.which);
            if (keycode == '13' && !validateInput()) {
                event.preventDefault();
            }
        });

        $('.portlet-toolbar button[data-action="save"]').on('click', function(event) {
            event.preventDefault();
            if(validateInput()) {
                $('#addLicenseTypeForm').submit();
            }
        });

        function validateInput() {
            $('#addLicenseTypeForm').removeClass('needs-validation');
            $('#licenseTypeTitle')[0].setCustomValidity('');
            $('.invalid-feedback').css('display', 'none');
            $('.invalid-feedback').removeClass('d-block');

            var licenseType = $('#licenseTypeTitle').val();
            
            if (licenseType.length == 0 || $.trim(licenseType).length == 0) {
                $('#addLicenseTypeForm').addClass('was-validated');
                $('#licenseTypeTitle')[0].setCustomValidity('error');
                $('#error-empty').addClass('d-block');
                return false;
            };

            var valid=/^[a-zA-Z0-9\-.+ ]+$/;
            if(!licenseType.match(valid)){
                $('#addLicenseTypeForm').addClass('was-validated');
                $('#licenseTypeTitle')[0].setCustomValidity('error');
                $('#error-invalid-char').addClass('d-block');
                return false;
            };

            return true;
        }
    });
</script>
