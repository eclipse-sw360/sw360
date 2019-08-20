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
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<table class="table edit-table two-columns-with-actions" id="externalIdsTable">
    <thead>
        <tr>
            <th colspan="3" class="headlabel">External Ids</th>
        </tr>
    </thead>
</table>

<button type="button" class="btn btn-secondary" id="add-external-id">Click to add row to External Ids</button>

<div class="dialogs">
    <div id="deleteExternalIdDialog" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
		    <div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title">
					<clay:icon symbol="question-circle" />
					Delete Item?
				</h5>
				<button type="button" class="close" data-dismiss="modal" aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
			</div>
				<div class="modal-body">
			        <p data-name="text"></p>
				</div>
			    <div class="modal-footer">
			        <button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
			        <button type="button" class="btn btn-danger">Delete Item</button>
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
                text: "Do you really want to remove this item?",
            }, function(submit, callback) {
                $('#' + rowId).remove();
                callback(true);
            });
        });

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
                '<input list="externalKeyList" class="form-control" id="<%=PortalConstants.EXTERNAL_ID_KEY%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_KEY%>' + rowId + '" required="" minlength="1" placeholder="Enter external id key" title="external id name" value="' + key + '"/>' +
                prepareKeyDatalist() + // creates a datalist with preferred key names
                '</td>' +
                '<td>' +
                '<input class="form-control" id="<%=PortalConstants.EXTERNAL_ID_VALUE%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_VALUE%>' + rowId + '" required="" minlength="1" placeholder="Enter external id value" title="external id value" value="' + value + '"/>' +
                '</td>' +
                '<td class="content-middle">' +
                '<svg title="Delete" class="action lexicon-icon" data-row-id="' + rowId + '">' +
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
            addRowToExternalIdsTable('<sw360:out value="${tableEntry.key}"/>', '<sw360:out value="${tableEntry.value}"/>', 'externalIdsTableRow${loop.count}');
            </core_rt:forEach>
        }
    });

</script>
