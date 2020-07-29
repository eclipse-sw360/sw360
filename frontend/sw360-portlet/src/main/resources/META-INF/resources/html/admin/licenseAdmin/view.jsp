<%--
  ~ Copyright Siemens AG, 2013-2017, 2019.
  ~ Copyright Bosch Software Innovations GmbH, 2016.
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
});
</script>
