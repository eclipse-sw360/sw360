<%--
  ~ Copyright Siemens AG, 2017-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.UserGroup"%>

<table class="table edit-table two-columns-with-actions" id="secDepartmentRolesTable">
    <thead>
        <tr>
            <th colspan="3" class="headlabel"><liferay-ui:message key="secondary.departments.and.roles" /></th>
        </tr>
    </thead>
</table>

<button type="button" class="btn btn-secondary" id="add-sec-grp-roles-id">
    <liferay-ui:message key="click.to.add.secondary.department.and.roles" />
</button>

<div class="dialogs">
    <div id="deleteSecGrpRolesDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <clay:icon symbol="question-circle" />
                        <liferay-ui:message key="delete.item" />
                        ?
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p data-name="text"></p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">
                        <liferay-ui:message key="cancel" />
                    </button>
                    <button type="button" class="btn btn-danger">
                        <liferay-ui:message key="delete.item" />
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    require(['jquery', 'modules/dialog'], function($, dialog) {

        createSecDepartmentRolesTable();
        $('#add-sec-grp-roles-id').on('click', function() {
                addRowToSecDepartmentRolesTable();
        });
        $('#secDepartmentRolesTable').on('click', 'svg[data-row-id]', function(event) {
            var rowId = $(event.currentTarget).data().rowId;

            dialog.open('#deleteSecGrpRolesDialog', {
                text: "<liferay-ui:message key="do.you.really.want.to.remove.this.item" />",
            }, function(submit, callback) {
                $('#' + rowId).remove();
                callback(true);
            });
        });

        function addRowsToSecDepartmentRolesTable(key, values, rowId) {
            try {
                var valueArray = JSON.parse($('<div />').html(values).text()).sort()
                for (var i = 0, length = valueArray.length; i < length; i++) {
                    var value = valueArray[i];
                    addRowToSecDepartmentRolesTable(key, value, rowId + i)
                }
            } catch(error) {
                addRowToSecDepartmentRolesTable(key, values, rowId)
			}
        }

        function addRowToSecDepartmentRolesTable(key, value, rowId) {
            if (!rowId) {
                rowId = "secDepartmentRolesTableRow" + Date.now();
            }
            if ((!key) && (!value)) {
                key = "";
                value = "";
            }

            var newRowAsString =
                '<tr id="' + rowId + '" class="bodyRow">' +
                '<td>' +
                '<input list="grpsKeyList" class="form-control" id="secGrp' + rowId + '" name="<portlet:namespace/><%=PortalConstants.USER_SECONDARY_GROUP_KEY%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.secondary.department" />" title="<liferay-ui:message key="enter.secondary.department" />" value="' + key + '"/>' +
                prepareKeyDatalist() + 
                '</td>' +
                '<td>' +
                '<select class="form-control" id="secGrpRole' + rowId + '" name="<portlet:namespace/><%=PortalConstants.USER_SECONDARY_GROUP_VALUES%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="select.secondary.department.role" />" title="<liferay-ui:message key="select.secondary.department.role" />" >' +
                '<option value="" class="textlabel stackedLabel" ><liferay-ui:message key="select.secondary.department.role" /></option>' +
                '<sw360:DisplayEnumOptions type="<%=UserGroup.class%>" useStringValues="true" options="${secondaryRolesOptions}"/>'+
                '</select>' +
                '</td>' +
                '<td class="content-middle">' +
                '<svg class="action lexicon-icon" data-row-id="' + rowId + '">' +
                '<title>Delete</title>' +
                '<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>' +
                '</svg>' +
                '</td>' +
                '</tr>';
            $('#secDepartmentRolesTable tr:last').after(newRowAsString);
            $("#secGrpRole" + rowId + " option").each(function() {
                if ($(this).val() === value) {
                  $(this).attr('selected', 'selected'); 
                  return false;
                }                        
            });
        }

        function prepareKeyDatalist() {
            var datalist = '<datalist id="grpsKeyList">';
            <core_rt:forEach items="${grpsKeys}" var="grpsKey">
                datalist += '<option value="' + "${grpsKey}" + '">';
            </core_rt:forEach>
            return datalist + '</datalist>';
        }

        function createSecDepartmentRolesTable() {
            <core_rt:forEach items="${secondaryDepartmentsAndRolesEntrySet}" var="tableEntry" varStatus="loop">
            <core_rt:forEach items="${tableEntry.value}" var="group" varStatus="innerloop">
			addRowsToSecDepartmentRolesTable('<sw360:out value="${tableEntry.key}"/>', '<sw360:out value="${group}"/>', 'secDepartmentRolesTableRow${loop.count}${innerloop.count}');
            </core_rt:forEach>
            </core_rt:forEach>
        }

        function decodeHTMLentities(str) {
            return str.replace(/&#(\d+);/g, function(match, dec) {
                 return String.fromCharCode(dec);
            });
        }

		$(document).ready(function(){
		    $("#user_department").after(prepareKeyDatalist()).attr("list","grpsKeyList")
		})
    });

</script>
