<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal User.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@include file="/html/init.jsp" %>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:resourceURL var="userSearchURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.USER_SEARCH%>"/>
</portlet:resourceURL>

<script>
    require(['jquery', 'utils/includes/searchAndSelectIds'], function($, idSearch) {

        $('.userSearchDialogInteractive').on('click', function() {
            showUserDialog( $(this).data('multiUser'), $(this).data('id') );
        });

        function searchUserAjax(what, how) {
            return jQuery.ajax({
                type: 'POST',
                url: '<%=userSearchURL%>',
                data: {
                    '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                    '<portlet:namespace/><%=PortalConstants.HOW%>': how
                }
            });
        }

        function addAlreadySelectedUsersToTable(data, currentState) {
            var tableData = "";

            if (currentState.resultFullData.length > 0 && currentState.resultFullData[0] != "") {
                for (var i=0; i<currentState.resultFullData.length; i++) {
                    var entry = currentState.resultFullData[i].split(",");
                    tableData += '<tr>' +
                        '<td><input type="' + ( currentState.multi ? 'checkbox' : 'radio' ) + '" checked="checked" value="' + entry.join(",") + '" name="id"/>' +'</td>' +
                        '<td>' + entry[0] + '</td>' +
                        '<td>' + entry[1] + '</td>' +
                        '<td>' + entry[2] + '</td>' +
                        '<td>' + entry[3] + '</td>' +
                        '</tr>';
                }
            }
            tableData += data;
            return tableData;
        }

        function renderUserInputToFullData(entry) {
            return " , ," + entry + ", ," + entry;
        }

        const indexMail = 2;
        const indexFullname = 4;

        function getIdsFromSelectedUsersData(currentState) {
            var ids = [];
            var displayIds = [];
            for (var i=0; i<currentState.resultFullData.length; i++) {
                var tmp = currentState.resultFullData[i].split(",");
                ids.push(tmp[indexMail].trim());
                displayIds.push(tmp[indexFullname].trim());
            }

            return {'ids': ids, 'displayIds': displayIds};
        }

        function showUserDialog(multiUser, resultInputId) {
            var htmlElements = {
                'addButton': $('#search-add-button'),
                'searchButton': $('#search-button'),
                'resetButton': $('#reset-button'),
                'searchInput': 'search-text',
                'resultTableBody': $('#search-result-table-body'),
                'resultTable': $('#search-result-table'),
                'searchDiv': 'search-users-div'
            };
            var functions = {
                'ajaxSearch': searchUserAjax,
                'prepareData': addAlreadySelectedUsersToTable,
                'extractIds': getIdsFromSelectedUsersData,
                'renderInput': renderUserInputToFullData,
            };

            idSearch.openSearchDialog(multiUser, resultInputId, htmlElements, functions);
        }
    });
</script>
