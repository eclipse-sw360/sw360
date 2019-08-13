<%--
  ~ Copyright Siemens AG, 2013-2017, 2019.
  ~ Copyright Bosch Software Innovations GmbH, 2016.
  ~ Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="updateLicenseArchiveURL" name="updateLicenses">
</portlet:actionURL>

<portlet:resourceURL var="deleteAllLicenseInformationURL">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.ACTION_DELETE_ALL_LICENSE_INFORMATION%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="importSpdxLicenseInformationURL">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.ACTION_IMPORT_SPDX_LICENSE_INFORMATION%>'/>
</portlet:resourceURL>


<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_BACKUP%>'/></portlet:resourceURL>'">
                                Download License Archive
                            </button>
						</div>
                        <div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" data-action="import-spdx">
                                Import SPDX Information
                            </button>
						</div>
                        <div class="btn-group" role="group">
							<button type="button" class="btn btn-danger" data-action="delete-licenses">
                                Delete all License information
                            </button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="License Administration">
					License Administration
				</div>
            </div>
            <div class="row">
                <div class="col">
                    <h4>Upload License Archive</h4>
                    <form id="uploadLicenseArchiveForm" name="uploadLicenseArchiveForm" action="<%=updateLicenseArchiveURL%>" method="post" class="form needs-validation" novalidate>
                        <div class="form-group">
                            <input id="<portlet:namespace/>LicenseArchivefileuploadInput" type="file" class="form-control-file" name="<portlet:namespace/>file" required>
                            <div class="invalid-feedback">
                                Please select a file!
                            </div>
                        </div>
                        <div class="form-check">
                            <input type="checkbox" id="overwriteIfExternalIdMatches" class="form-check-input"
                                name="<portlet:namespace/>overwriteIfExternalIdMatches"
                                value="true">
                            <label for="overwriteIfExternalIdMatches" class="form-check-label">
                                Overwrite if externals IDs match
                            </label>
                        </div>
                        <div class="form-check">
                            <input type="checkbox" id="overwriteIfIdMatchesEvenWithoutExternalIdMatch" class="form-check-input"
                                name="<portlet:namespace/>overwriteIfIdMatchesEvenWithoutExternalIdMatch"
                                value="true">
                            <label for="overwriteIfIdMatchesEvenWithoutExternalIdMatch" class="form-check-label">
                                Overwrite if IDs match
                            </label>
                        </div>

                        <button type="submit" class="btn btn-secondary">Upload Licenses</button>
                    </form>
                </div>
            </div>
		</div>
	</div>
</div>

<div class="dialogs auto-dialogs"></div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
require(['jquery', 'modules/dialog', 'modules/validation'], function($, dialog, validation) {

    validation.enableForm('#uploadLicenseArchiveForm');

    $('.portlet-toolbar button[data-action="import-spdx"]').on('click', function() {
        var $dialog;

        function importSpdxLicenseInformationInternal(callback) {
            $.ajax({
                type: 'POST',
                url: '<%=importSpdxLicenseInformationURL%>',
                cache: false,
                dataType: 'json'
            }).always(function() {
                callback();
            }).done(function (data) {
                if (data.result == 'SUCCESS') {
                    $dialog.success("I imported " + data.totalAffectedObjects + " out of " + data.totalObjects + " SPDX licenses. " + data.message, true);
                }else {
                    $dialog.alert("I could not import all SPDX license information!");
                }
            }).fail(function(){
                $dialog.alert('Something went wrong.');
            });
        }

        $dialog = dialog.confirm(
            null,
            'question-circle',
            'Import SPDX licenses?',
            '<p>Do you really want to import all SPDX all licenses?',
            'Import SPDX licenses',
            {},
            function(submit, callback) {
                importSpdxLicenseInformationInternal(callback);
            }
        );
    });

    $('.portlet-toolbar button[data-action="delete-licenses"]').on('click', function() {
        var $dialog;

        function deleteAllLicenseInformationInternal(callback) {
            $.ajax({
                type: 'POST',
                url: '<%=deleteAllLicenseInformationURL%>',
                cache: false,
                dataType: 'json'
            }).always(function() {
               callback();
            }).done(function (data) {
                if (data.result == 'SUCCESS') {
                    $dialog.success("I deleted " + data.totalAffectedObjects + " out of " + data.totalObjects + " documents in the database.", true);
                }else {
                    $dialog.alert("I could not delete the license information!");
                }
            }).fail(function(){
                $dialog.alert('Something went wrong.');
            });
        }

        $dialog = dialog.confirm(
            'danger',
            'question-circle',
            'Delete all Licenses?',
            '<div class="alert alert-warning">Note: other documents might use the licenses.</div>' +
            '<p>Do you really want to delete all licenses, license types, todos, obligations, risks, risk categories and todo custom properties from the database?' +
            '<div class="alert alert-info">This function is meant to be followed by a new license import.</div>',
            'Delete all License information',
            {},
            function(submit, callback) {
                deleteAllLicenseInformationInternal(callback);
            }
        );
    });
});
</script>
