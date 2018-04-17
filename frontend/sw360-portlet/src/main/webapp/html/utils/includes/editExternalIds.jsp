<%--
  ~ Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<table class="table info_table" id="externalIdsTable">
    <thead>
    <tr>
        <th colspan="3" class="headlabel">External Ids</th>
    </tr>
    </thead>
</table>

<input type="button" class="addButton" id="add-external-id" value="Click to add row to External Ids "/>
<br/>
<br/>

<script>
    require(['jquery', 'modules/confirm'], function($, confirm) {

        Liferay.on('allPortletsReady', function() {
            createExternalIdsTable();

            $('#add-external-id').on('click', function() {
                addRowToExternalIdsTable();
            });
        });

        function deleteIDItem(rowIdOne) {
            function deleteMapItemInternal() {
                $('#' + rowIdOne).remove();
            };

            deleteConfirmed("Do you really want to remove this item?", deleteMapItemInternal);
        }

        function addRowToExternalIdsTable(key, value, rowId) {
            if (!rowId) {
                var rowId = "externalIdsTableRow" + Date.now();
            }
            if ((!key) && (!value)) {
                    var key = "", value = "";
                }
            var newRowAsString =
                '<tr id="' + rowId + '" class="bodyRow">' +
                '<td width="46%">' +
                '<input class="keyClass" id="<%=PortalConstants.EXTERNAL_ID_KEY%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_KEY%>' + rowId + '" required="" minlength="1" class="toplabelledInput" placeholder="Enter external id key" title="Input name" value="' + key + '"/>' +
                '</td>' +
                '<td width="46%">' +
                '<input class="valueClass" id="<%=PortalConstants.EXTERNAL_ID_VALUE%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_VALUE%>' + rowId + '" required="" minlength="1" class="toplabelledInput" placeholder="Enter external id value" title="Input id" value="' + value + '"/>' +
                '</td>' +
                '<td class="deletor" width="8%">' +
                '<img src="<%=request.getContextPath()%>/images/Trash.png" onclick="deleteMapItem(\'' + rowId + '\')" alt="Delete">' +
                '</td>' +
                '</tr>';
            $('#externalIdsTable tr:last').after(newRowAsString);
        }

        function createExternalIdsTable() {
            <core_rt:forEach items="${externalIdsSet}" var="tableEntry" varStatus="loop">
            addRowToExternalIdsTable('<sw360:out value="${tableEntry.key}"/>', '<sw360:out value="${tableEntry.value}"/>', 'externalIdsTableRow${loop.count}');
            </core_rt:forEach>
        }
    });

</script>
