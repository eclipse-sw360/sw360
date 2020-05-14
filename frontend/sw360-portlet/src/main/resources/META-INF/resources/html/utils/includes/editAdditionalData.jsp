<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0" %>
<%--
  ~ Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>


<table class="table edit-table two-columns-with-actions" id="additionalDataTable">
    <thead>
        <tr>
            <th colspan="3" class="headlabel"><liferay-ui:message key="additional.data" /></th>
        </tr>
    </thead>
	<c:forEach var="customField" items="${customFields}">
		<tr <c:if test="${customField.hidden}">style="display:none"</c:if> class="bodyRow">
			<td>
				<div>
					<input class="form-control" id="${customField.fieldKey}" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_KEY%>${customField.fieldKey}"
						   type="text" value="<sw360:out value="${customField.fieldLabel}"/>" readonly/>
				</div>
			</td>
			<td>
				<div>
					<c:if test="${customField.fieldType == 'TEXTFIELD'}">
						<input class="form-control" id="${customField.fieldLabel}" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_VALUE%>${customField.fieldKey}"
							   type="text" placeholder="Enter ${customField.fieldLabel}"
							   <c:if test="${not empty customField.fieldPattern}">pattern="${customField.fieldPattern}" title="${customField.fieldPattern}"</c:if>
							   value="<sw360:out value="${customField.value}"/>"/>
					</c:if>
					<c:if test="${customField.fieldType == 'TEXTAREA'}">
						<textarea class="form-control" id="${customField.fieldLabel}" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_VALUE%>${customField.fieldKey}"
								  rows="4" placeholder="Enter ${customField.fieldLabel}">${customField.value}</textarea>
					</c:if>
					<c:if test="${customField.fieldType == 'DATE'}">
						<input id="${customField.fieldLabel}" class="datepicker form-control" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_VALUE%>${customField.fieldKey}" type="text"
							   placeholder="Enter ${customField.fieldLabel}" pattern="\d{4}-\d{2}-\d{2}"
							   value="<sw360:out value="${customField.value}"/>"/>
					</c:if>
					<c:if test="${customField.fieldType == 'DROPDOWN'}">
						<select class="form-control" id="${customField.fieldLabel}" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_VALUE%>${customField.fieldKey}">
							<option value="">Select:</option>
							<c:forEach var="option" items="${customField.options}">
								<option value="${option}" <c:if test="${option eq customField.value}">selected</c:if>>${option}</option>
							</c:forEach>
						</select>
					</c:if>
					<c:if test="${customField.fieldType == 'CHECKBOX'}">
						<input id="${customField.fieldLabel}" type="checkbox" class="form-check-input" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_VALUE%>${customField.fieldKey}"
						value="${customField.options[0]}" <c:if test="${not empty customField.value}">checked</c:if>/>
						<label class="form-check-label" for="${customField.fieldKey}">${customField.options[0]}</label>
					</c:if>

				</div>
			</td>
		</tr>
	</c:forEach>
</table>

<button type="button" class="btn btn-secondary" id="add-additional-data"><liferay-ui:message key="click.to.add.row.to.additional.data" /></button>

<div class="dialogs">
    <div id="deleteAdditionalDataDialog" class="modal fade" tabindex="-1" role="dialog">
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
	require(['jquery', /* jquery-plugins */ 'jquery-ui'], function($) {
		$(".datepicker").datepicker({changeMonth:true,changeYear:true,dateFormat: "yy-mm-dd"});
	});
    require(['jquery', 'modules/dialog'], function($, dialog) {

        createAdditionalDataTable();
        $('#add-additional-data').on('click', function() {
            addRowToAdditionalDataTable();
        });
        $('#additionalDataTable').on('click', 'svg[data-row-id]', function(event) {
            var rowId = $(event.currentTarget).data().rowId;

            dialog.open('#deleteAdditionalDataDialog', {
                text: "<liferay-ui:message key="do.you.really.want.to.remove.this.item" />",
            }, function(submit, callback) {
                $('#' + rowId).remove();
                callback(true);
            });
        });

        function addRowToAdditionalDataTable(key, value, rowId) {
            if (!rowId) {
                rowId = "additionalDataTableRow" + Date.now();
            }
            if ((!key) && (!value)) {
                key = "";
                value = "";
            }

            var newRowAsString =
                '<tr id="' + rowId + '" class="bodyRow">' +
                '<td>' +
                '<input list="additionalDataKeyList" class="form-control" id="<%=PortalConstants.ADDITIONAL_DATA_KEY%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_KEY%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.additional.data.key" />" title="<liferay-ui:message key="additional.data.name" />" value="' + key + '"/>' +
                '</td>' +
                '<td>' +
                '<input class="form-control" id="<%=PortalConstants.ADDITIONAL_DATA_VALUE%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_VALUE%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.additional.data.value" />" title="<liferay-ui:message key="additional.data.value" />" value="' + value + '"/>' +
                '</td>' +
                '<td class="content-middle">' +
                '<svg class="action lexicon-icon" data-row-id="' + rowId + '">' +
                '<title><liferay-ui:message key="delete" /></title>' +
                '<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>' +
                '</svg>' +
                '</td>' +
                '</tr>';

            $('#additionalDataTable tr:last').after(newRowAsString);
        }

        function createAdditionalDataTable() {
            <core_rt:forEach items="${additionalDataSet}" var="tableEntry" varStatus="loop">
                addRowToAdditionalDataTable('<sw360:out value="${tableEntry.key}"/>', '<sw360:out value="${tableEntry.value}"/>', 'additionalDataTableRow${loop.count}');
            </core_rt:forEach>
        }
    });

</script>
