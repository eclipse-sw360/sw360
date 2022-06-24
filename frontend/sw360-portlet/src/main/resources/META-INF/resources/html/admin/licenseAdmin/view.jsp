<%--
  ~ Copyright Siemens AG, 2013-2017, 2019.
  ~ Copyright Bosch Software Innovations GmbH, 2016.
  ~ Copyright TOSHIBA CORPORATION, 2021.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021.
  ~ Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
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

<portlet:resourceURL var="importOSADLLicenseInformationURL">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.ACTION_IMPORT_OSADL_LICENSE_INFORMATION%>'/>
</portlet:resourceURL>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DOWNLOAD_LICENSE_BACKUP%>'/></portlet:resourceURL>'">
                                <liferay-ui:message key="download.license.archive" />
                            </button>
						</div>
                        <div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" data-action="import-spdx">
                                <liferay-ui:message key="import.spdx.information" />
                            </button>
						</div>
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-primary" data-action="import-OSADL">
                                <liferay-ui:message key="import.osadl.information" />
                            </button>
                        </div>
                        <div class="btn-group" role="group">
							<button type="button" class="btn btn-danger" data-action="delete-licenses">
                                <liferay-ui:message key="delete.all.license.information" />
                            </button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="license.administration" />">
					<liferay-ui:message key="license.administration" />
				</div>
            </div>
            <div class="row">
                <div class="col">
                    <h4><liferay-ui:message key="upload.license.archive" /></h4>
                    <form id="uploadLicenseArchiveForm" name="uploadLicenseArchiveForm" action="<%=updateLicenseArchiveURL%>" method="post" class="form needs-validation" novalidate>
                        <div class="form-group">
                            <input id="<portlet:namespace/>LicenseArchivefileuploadInput" type="file" class="form-control-file" name="<portlet:namespace/>file" required>
                            <div class="invalid-feedback">
                                <liferay-ui:message key="please.select.a.file" />
                            </div>
                        </div>
                        <div class="form-check">
                            <input type="checkbox" id="overwriteIfExternalIdMatches" class="form-check-input"
                                name="<portlet:namespace/>overwriteIfExternalIdMatches"
                                value="true">
                            <label for="overwriteIfExternalIdMatches" class="form-check-label">
                                <liferay-ui:message key="overwrite.if.externals.ids.match" />
                            </label>
                        </div>
                        <div class="form-check">
                            <input type="checkbox" id="overwriteIfIdMatchesEvenWithoutExternalIdMatch" class="form-check-input"
                                name="<portlet:namespace/>overwriteIfIdMatchesEvenWithoutExternalIdMatch"
                                value="true">
                            <label for="overwriteIfIdMatchesEvenWithoutExternalIdMatch" class="form-check-label">
                                <liferay-ui:message key="overwrite.if.ids.match" />
                            </label>
                        </div>

                        <button type="submit" class="btn btn-secondary"><liferay-ui:message key="upload.licenses" /></button>
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
    var progress = null;

    $('.portlet-toolbar button[data-action="import-spdx"]').on('click', function() {
        var $dialog;
        if (progress != null) {
            progress.abort();
        }

        function importSpdxLicenseInformationInternal(callback) {
            progress = $.ajax({
                type: 'POST',
                url: '<%=importSpdxLicenseInformationURL%>',
                cache: false,
                dataType: 'json'
            }).always(function() {
                callback();
            }).done(function (data) {
                if (data.result == 'SUCCESS') {
                    $dialog.success(`<liferay-ui:message key="i.imported.x.out.of.y.spdx.licenses.z" />`, true);
                }else {
                    $dialog.alert('<liferay-ui:message key="i.could.not.import.all.spdx.license.information" />');
                }
            }).fail(function(){
                $dialog.alert('<liferay-ui:message key="something.went.wrong" />');
            });
        }

        $dialog = dialog.confirm(
            null,
            'question-circle',
            '<liferay-ui:message key="import.spdx.licenses" />?',
            '<p><liferay-ui:message key="do.you.really.want.to.import.all.spdx.all.licenses" />',
            '<liferay-ui:message key="import.spdx.licenses" />',
            {},
            function(submit, callback) {
                importSpdxLicenseInformationInternal(callback);
            }
        );
    });

    $('.portlet-toolbar button[data-action="import-OSADL"]').on('click', function() {
        var $dialog;
        if (progress != null) {
            progress.abort();
        }

        function importOSADLLicenseInformationInternal(callback) {
            progress = $.ajax({
                type: 'POST',
                url: '<%=importOSADLLicenseInformationURL%>',
                cache: false,
                dataType: 'json'
            }).always(function() {
                callback();
            }).done(function (data) {
                $('.alert.alert-dialog').hide();
                if (data.result == 'SUCCESS') {
                    $dialog.success(`<liferay-ui:message key="i.imported.x.out.of.y.osadl.license.obliations" />`);
                    if (data.totalAffectedObjects != 0 ) {
                        $('.modal-body').append(`<div id="listLicenseSuccess">
                                        <p style="margin-top: 1rem; margin-bottom: 0.5rem;"><liferay-ui:message key="list.of.licenses.were.imported"/></p>
                                    <div id="licenseSuccessTable">
                                    </div>`);
                        var licensesSuccessTable = createLicenseTable(data.message, 'licensesSuccess');
                        $('#licenseSuccessTable').append(licensesSuccessTable);
                    }
                    if (data.totalObjects != data.totalAffectedObjects) {
                        $('.modal-body').append(`<div id="listLicenseMissing">
                                        <p style="margin-top: 1rem; margin-bottom: 0.5rem;"><liferay-ui:message key="list.of.licenses.could.not.be.imported"/></p>
                                    <div id="licenseMissingTable">
                                    </div>`);
                        var licensesMissingTable = createLicenseTable(data.message, 'licensesMissing');
                        $('#licenseMissingTable').append(licensesMissingTable);
                    }
                } else if (data.result == 'PROCESSING') {
                    $dialog.info('<liferay-ui:message key="importing.process.is.already.running.please.try.again.later" />');
                } else {
                    $dialog.alert('<liferay-ui:message key="error.happened.during.license.obligation.importing.some.license.obliations.may.not.be.imported" />');
                }
            }).fail(function(){
                $('.alert.alert-dialog').hide();
                $dialog.alert('<liferay-ui:message key="something.went.wrong" />');
            });
        }

        $dialog = dialog.confirm(
            null,
            'question-circle',
            '<liferay-ui:message key="import.osadl.license.obligations" />?',
            '<p id="OSADLConfirmMessage"><liferay-ui:message key="do.you.really.want.to.import.all.osadl.license.obligations" />',
            '<liferay-ui:message key="import.osadl.license.obligations" />',
            {},
            function(submit, callback) {
                $('#OSADLConfirmMessage').hide();
                $dialog.info('<liferay-ui:message key="importing.process.is.running.it.may.takes.a.few.minutes" />', true);
                $('.modal-header > button').prop('disabled', false);
                importOSADLLicenseInformationInternal(callback);
            }
        );
    });

    $('.portlet-toolbar button[data-action="delete-licenses"]').on('click', function() {
        var $dialog;
        if (progress != null) {
            progress.abort();
        }

        function deleteAllLicenseInformationInternal(callback) {
            progress = $.ajax({
                type: 'POST',
                url: '<%=deleteAllLicenseInformationURL%>',
                cache: false,
                dataType: 'json'
            }).always(function() {
               callback();
            }).done(function (data) {
                if (data.result == 'SUCCESS') {
                    $dialog.success(`<liferay-ui:message key="i.deleted.x.out.of.y.documents.in.the.database" />`, true);
                }else {
                    $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.license.information" />');
                }
            }).fail(function(){
                $dialog.alert('<liferay-ui:message key="something.went.wrong" />');
            });
        }

        $dialog = dialog.confirm(
            'danger',
            'question-circle',
            '<liferay-ui:message key="delete.all.licenses" />?',
            '<div class="alert alert-warning"><liferay-ui:message key="note.other.documents.might.use.the.licenses" /></div>' +
            '<p><liferay-ui:message key="do.you.really.want.to.delete.all.licenses.license.types.obligations.obligations.risks.risk.categories.and.oblig.custom.properties.from.the.database" />' +
            '<div class="alert alert-info"><liferay-ui:message key="this.function.is.meant.to.be.followed.by.a.new.license.import" /></div>',
            '<liferay-ui:message key="delete.all.license.information" />',
            {},
            function(submit, callback) {
                deleteAllLicenseInformationInternal(callback);
            }
        );
    });

    function createLicenseTable(dataMessage, status) {
        const jsonData = JSON.parse(dataMessage);
        const licensesJson = jsonData[status];

        var licenseTable = '<div class="table-license-list dataTable">';
        licenseTable += '<table>';
        licenseTable += '<thead>';
        licenseTable += '<tr>';
        licenseTable += '<th><liferay-ui:message key="index" /></th>';
        licenseTable += '<th><liferay-ui:message key="license.shortname" /></th>';
        licenseTable += '<th><liferay-ui:message key="license.fullname" /></th>';
        licenseTable += '</tr>';
        licenseTable += '</thead>';
        licenseTable += '<tbody>';

        var i = 1;
        for (var key in licensesJson) {
            licenseTable += createRowTable(i, key, licensesJson[key]);
            i++;
        }

        licenseTable += '</tbody>';
        licenseTable += '</table>';
        licenseTable += '</div>';
        return licenseTable;
    }

    function createRowTable(index, shortname, fullname){
        var row = '<tr>';
        row += '<td>' + index + '</td>';
        row += '<td>' + shortname + '</td>';
        row += '<td>' + fullname + '</td>';
        row += '</tr>';
        return row;
    }

});
</script>
