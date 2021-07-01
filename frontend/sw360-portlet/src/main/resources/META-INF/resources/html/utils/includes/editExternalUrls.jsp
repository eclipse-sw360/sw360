<%--
  ~ Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<table class="table edit-table two-columns-with-actions" id="externalUrlsTable">
    <thead>
        <tr>
            <th colspan="3" class="headlabel"><liferay-ui:message key="external.urls" /></th>
        </tr>
    </thead>
</table>

<button type="button" class="btn btn-secondary" id="add-external-url"><liferay-ui:message key="click.to.add.row.to.external.urls" /></button>

<div class="dialogs">
    <div id="deleteExternalUrlDialog" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
		    <div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title">
					<clay:icon symbol="question-circle" />
					<liferay-ui:message key="delete.item" />?
				</h5>
				<button type="button" class="close" data-dismiss="modal" aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
			</div>
				<div class="modal-body">
			        <p data-name="text"></p>
				</div>
			    <div class="modal-footer">
			        <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="cancel" /></button>
			        <button type="button" class="btn btn-danger"><liferay-ui:message key="delete.item" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>

<script>
    require(['jquery', 'modules/dialog'], function($, dialog) {

        createExternalUrlsTable();
        $('#add-external-url').on('click', function() {
                addRowToExternalUrlsTable();
        });
        $('#externalUrlsTable').on('click', 'svg[data-row-id]', function(event) {
            var rowId = $(event.currentTarget).data().rowId;

            dialog.open('#deleteExternalUrlDialog', {
                text: "<liferay-ui:message key="do.you.really.want.to.remove.this.item" />",
            }, function(submit, callback) {
                $('#' + rowId).remove();
                callback(true);
            });
        });

        function addRowsToExternalUrlsTable(key, values, rowId) {
           try {
                var valueArray = JSON.parse($('<div />').html(values).text()).sort()
                for (var i = 0, length = valueArray.length; i < length; i++) {
                    var value = valueArray[i];
                    addRowToExternalUrlsTable(key, value, rowId + i)
               }
            } catch(error) {
                addRowToExternalUrlsTable(key, values, rowId)
            }
        }

        function addRowToExternalUrlsTable(key, value, rowId) {
            if (!rowId) {
                rowId = "externalUrlsTableRow" + Date.now();
            }
            if ((!key) && (!value)) {
                key = "";
                value = "";
            }

            var newRowAsString =
                '<tr id="' + rowId + '" class="bodyRow">' +
                '<td>' +
                '<input list="externalUrlList" pattern=[a-z0-9\-.+]* class="form-control" id="<%=PortalConstants.EXTERNAL_URL_KEY%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_URL_KEY%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.external.url.key" />" title="<liferay-ui:message key="external.url.name" />" value="' + key + '"/>' +
                prepareExternalUrlDatalist() +
                '</td>' +
                '<td>' +
                '<input class="form-control" type="URL" id="<%=PortalConstants.EXTERNAL_URL_VALUE%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_URL_VALUE%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.external.url.value" />" title="<liferay-ui:message key="external.url.value" />" value="' + value + '"/>' +
                '<div class="invalid-feedback">' +
                '<liferay-ui:message key="prease.enter.a.valid.url" />' +
                '</div>' +
                '</td>' +
                '<td class="content-middle">' +
                '<svg class="action lexicon-icon" data-row-id="' + rowId + '">' +
                '<title>Delete</title>' +
                '<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>' +
                '</svg>' +
                '</td>' +
                '</tr>';
            $('#externalUrlsTable tr:last').after(newRowAsString);
        }

        function prepareExternalUrlDatalist() {
            var datalist = '<datalist id="externalUrlList">';
            <core_rt:forEach items="${externalUrlKeys}" var="externalUrlKey">
                datalist += '<option value="' + "${externalUrlKey}" + '">';
            </core_rt:forEach>
            return datalist + '</datalist>';
        }

        function createExternalUrlsTable() {
            <core_rt:forEach items="${externalUrlsSet}" var="tableEntry" varStatus="loop">
			addRowsToExternalUrlsTable('<sw360:out value="${tableEntry.key}"/>', '<sw360:out value="${tableEntry.value}"/>', 'externalUrlsTableRow${loop.count}');
            </core_rt:forEach>
        }

    });

</script>
