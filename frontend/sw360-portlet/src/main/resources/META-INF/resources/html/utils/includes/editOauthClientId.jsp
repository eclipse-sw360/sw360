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
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.UserAccess"%>

<table class="table edit-table three-columns-with-actions" id="oidcClientsTable">
    <thead>
        <tr>
            <th colspan="4" class="headlabel"><liferay-ui:message key="oidc.client" /></th>
        </tr>
    </thead>
</table>

<button type="button" class="btn btn-secondary" id="add-client-id">
    <liferay-ui:message key="click.to.add.oidc.client" />
</button>

<div class="dialogs">
    <div id="deleteOidcClientDialog" class="modal fade" tabindex="-1" role="dialog">
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

        createOidcClientTable();
        $('#add-client-id').on('click', function() {
                addRowToClientIdAccessTable();
        });
        $('#oidcClientsTable').on('click', 'svg[data-row-id]', function(event) {
            var rowId = $(event.currentTarget).data().rowId;

            dialog.open('#deleteOidcClientDialog', {
                text: "<liferay-ui:message key="do.you.really.want.to.remove.this.item" />",
            }, function(submit, callback) {
                $('#' + rowId).remove();
                callback(true);
            });
        });

        function addRowsToOidcClientsTable(key, value, name, rowId) {
            addRowToClientIdAccessTable(key, value, name, rowId);
        }

        function addRowToClientIdAccessTable(key, value, name, rowId) {
            if (!rowId) {
                rowId = "oidcClientsTableRow" + Date.now();
            }
            if ((!key) && (!value)) {
                key = "";
                value = "";
            }

            if (!name) {
                name = "";
            }
            var newRowAsString =
                '<tr id="' + rowId + '" class="bodyRow">' +
                '<td>' +
                '<input class="form-control" id="clientIdName' + rowId + '" name="<portlet:namespace/><%=PortalConstants.USER_CLIENT_ID_NAME_VALUE%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.oidc.client.name" />" title="<liferay-ui:message key="enter.oidc.client.name" />" value="' + name + '"/>' +
                '</td>' +
                '<td>' +
                '<input class="form-control" id="clientId' + rowId + '" name="<portlet:namespace/><%=PortalConstants.USER_CLIENT_ID_KEY%>' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.oidc.client.id" />" title="<liferay-ui:message key="enter.oidc.client.id" />" value="' + key + '"/>' +
                '</td>' +
                '<td>' +
                '<div class="form-check pl-5">' +
                '<input type="radio" checked="checked" id="clientReadAccessId' + rowId + '" name="<portlet:namespace/><%=PortalConstants.USER_CLIENT_ID_ACCESS_VALUE%>' + rowId + '" class="form-check-input" value="<%=UserAccess.READ%>" />' +
                '<label class="form-check-label" for="clientReadAccessId' + rowId + '">' +
                '<liferay-ui:message key="read.access" />' +
                '</label><br>' +
                '<input type="radio" id="clientReadWriteAccessId' + rowId + '" name="<portlet:namespace/><%=PortalConstants.USER_CLIENT_ID_ACCESS_VALUE%>' + rowId + '" class="form-check-input"  value="<%=UserAccess.READ_WRITE%>"/>' +
                '<label class="form-check-label" for="clientReadWriteAccessId' + rowId + '">' +
                '<liferay-ui:message key="read.and.write.access" />' +
                '</label> </div>' +
                '</td>' +
                '<td class="content-middle">' +
                '<svg class="action lexicon-icon" data-row-id="' + rowId + '">' +
                '<title>Delete</title>' +
                '<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>' +
                '</svg>' +
                '</td>' +
                '</tr>';
            $('#oidcClientsTable tr:last').after(newRowAsString);
            $("#clientReadAccessId" + rowId).each(function() {
                if ("<%=UserAccess.READ%>" === value) {
                  $(this).attr('checked', 'checked'); 
                  return false;
                }                        
            });
            
            $("#clientReadWriteAccessId" + rowId).each(function() {
                if ("<%=UserAccess.READ_WRITE%>" === value) {
                    $(this).attr('checked', 'checked'); 
                    return false;
                }
            });
        }

        function createOidcClientTable() {
            <core_rt:forEach items="${clientInfosEntrySet}" var="tableEntry" varStatus="loop">
			addRowsToOidcClientsTable('<sw360:out value="${tableEntry.key}"/>', '<sw360:out value="${tableEntry.value.access}"/>', '<sw360:out value="${tableEntry.value.name}"/>', 'oidcClientsTableRow${loop.count}');
            </core_rt:forEach>
        }

        function decodeHTMLentities(str) {
            return str.replace(/&#(\d+);/g, function(match, dec) {
                 return String.fromCharCode(dec);
            });
        }
    });

</script>
