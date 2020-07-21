<%--
  ~ Copyright Siemens AG, 2017-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<table class="table edit-table two-columns-with-actions" id="externalIdsTable">
    <thead>
        <tr>
            <th colspan="3" class="headlabel"><liferay-ui:message key="external.ids" /></th>
        </tr>
    </thead>
</table>

<button type="button" class="btn btn-secondary" id="add-external-id"><liferay-ui:message key="click.to.add.row.to.external.ids" /></button>

<div class="dialogs">
    <div id="deleteExternalIdDialog" class="modal fade" tabindex="-1" role="dialog">
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

        createExternalIdsTable();
        $('#add-external-id').on('click', function() {
                addRowToExternalIdsTable();
        });
        $('#externalIdsTable').on('click', 'svg[data-row-id]', function(event) {
            var rowId = $(event.currentTarget).data().rowId;

            dialog.open('#deleteExternalIdDialog', {
                text: "<liferay-ui:message key="do.you.really.want.to.remove.this.item" />",
            }, function(submit, callback) {
                $('#' + rowId).remove();
                callback(true);
            });
        });

        function addRowsToExternalIdsTable(key, values, rowId) {
			try {
        		var valueArray = JSON.parse($('<div />').html(values).text()).sort()
				for (var i = 0, length = valueArray.length; i < length; i++) {
					var value = valueArray[i];
					addRowToExternalIdsTable(key, value, rowId + i)
				}
			} catch(error) {
				addRowToExternalIdsTable(key, values, rowId)
			}
		}

        function addRowToExternalIdsTable(key, value, rowId) {
            if (!rowId) {
                rowId = "externalIdsTableRow" + Date.now();
            }
            if ((!key) && (!value)) {
                key = "";
                value = "";
            }

            var newRowAsString =
                '<tr id="' + rowId + '" class="bodyRow">' +
                '<td>' +
                '<input list="externalKeyList" class="form-control" id="<%=PortalConstants.EXTERNAL_ID_KEY%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_KEY%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.external.id.key" />" title="<liferay-ui:message key="external.id.name" />" value="' + key + '"/>' +
                prepareKeyDatalist() + // creates a datalist with preferred key names
                '</td>' +
                '<td>' +
                '<input class="form-control" id="<%=PortalConstants.EXTERNAL_ID_VALUE%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_VALUE%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.external.id.value" />" title="<liferay-ui:message key="external.id.value" />" value="' + value + '"/>' +
                '</td>' +
                '<td class="content-middle">' +
                '<svg class="action lexicon-icon" data-row-id="' + rowId + '">' +
                '<title>Delete</title>' +
                '<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>' +
                '</svg>' +
                '</td>' +
                '</tr>';
            $('#externalIdsTable tr:last').after(newRowAsString);
        }

        function prepareKeyDatalist() {
            var datalist = '<datalist id="externalKeyList">';
            <core_rt:forEach items="${externalIdKeys}" var="externalIdKey">
                datalist += '<option value="' + "${externalIdKey}" + '">';
            </core_rt:forEach>
            return datalist + '</datalist>';
        }

        function createExternalIdsTable() {
            <core_rt:forEach items="${externalIdsSet}" var="tableEntry" varStatus="loop">
			addRowsToExternalIdsTable('<sw360:out value="${tableEntry.key}"/>', '<sw360:out value="${tableEntry.value}"/>', 'externalIdsTableRow${loop.count}');
            </core_rt:forEach>
        }

		function decodeHTMLentities(str) {
			return str.replace(/&#(\d+);/g, function(match, dec) {
				return String.fromCharCode(dec);
			});
		}
    });

</script>
