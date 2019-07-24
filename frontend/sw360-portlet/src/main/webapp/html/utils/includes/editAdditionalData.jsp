<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0" %>
<%--
  ~ Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<table class="table info_table" id="additionalDataTable">
    <thead>
    <tr>
        <th colspan="3" class="headlabel">Additional Data</th>
    </tr>
    </thead>
</table>

<input type="button" class="addButton" id="add-additional-data" value="Click to add row of Additional Data"/>
<br/>
<br/>

<script>
    require(['jquery', 'modules/confirm'], function($, confirm) {

        Liferay.on('allPortletsReady', function() {
            createAdditionalDataTable();

            $('#add-additional-data').on('click', function() {
                addRowToAdditionalDataTable();
            });
        });

        function deleteIDItem(rowIdOne) {
            function deleteMapItemInternal() {
                $('#' + rowIdOne).remove();
            }
            deleteConfirmed("Do you really want to remove this item?", deleteMapItemInternal);
        }

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
                '<td width="46%">' +
                '<input list="additionalDataKeyList" class="keyClass" id="<%=PortalConstants.ADDITIONAL_DATA_KEY%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_KEY%>' + rowId + '" required="" minlength="1" class="toplabelledInput" placeholder="Enter additional data key" title="additional data name" value="' + key + '"/>' +
                '</td>' +
                '<td width="46%">' +
                '<input class="valueClass" id="<%=PortalConstants.ADDITIONAL_DATA_VALUE%>' + rowId + '" name="<portlet:namespace/><%=PortalConstants.ADDITIONAL_DATA_VALUE%>' + rowId + '" required="" minlength="1" class="toplabelledInput" placeholder="Enter additional data value" title="additional data value" value="' + value + '"/>' +
                '</td>' +
                '<td class="deletor" width="8%">' +
                '<img src="<%=request.getContextPath()%>/images/Trash.png" onclick="deleteMapItem(\'' + rowId + '\')" alt="Delete">' +
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
