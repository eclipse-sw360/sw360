<%--
  ~ Copyright Siemens AG, 2013-2018. Part of the SW360 Portal User.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@include file="/html/init.jsp" %>

<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:resourceURL var="licenseSearchURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.LICENSE_SEARCH%>"/>
</portlet:resourceURL>

<script>
    require(['jquery', 'utils/includes/searchAndSelectIds'], function ($, idSearch) {

        Liferay.on('allPortletsReady', function() {
            $('.licenseSearchDialogInteractive').on('click', function() {
                showSetLicensesDialog( $(this).data('id') );
            })
        });

        function searchLicenseAjax(what, how) {
            return jQuery.ajax({
                type: 'POST',
                url: '<%=licenseSearchURL%>',
                data: {
                    '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                    '<portlet:namespace/><%=PortalConstants.HOW%>': how
                }
            });
        }

        firstRun = true;

        function addAlreadySelectedLicensesToTable(data, currentState) {
            var tableData = "";

             if (currentState.resultFullData.length > 0 && currentState.resultFullData[0] != "") {
                for (var i=0; i<currentState.resultFullData.length; i++) {
                    var entry = currentState.resultFullData[i].split(",");
                    tableData += '<tr>' +
                        '<td><input type="checkbox" checked="checked" value="' + entry.join(",") + '" name="id"/>' +'</td>' +
                        '<td>' + entry[1] + '</td>' +
                        '</tr>';
                }
            }
            tableData += data;
            return tableData;
        }

        function getIdsFromSelectedLicensesData(currentState) {
            var ids = [];
            var displayIds = [];
            for (var i=0; i<currentState.resultFullData.length; i++) {
                var tmp = currentState.resultFullData[i].split(",");
                ids.push(tmp[0].trim());
                displayIds.push(tmp[1].trim());
            }

            return {'ids': ids, 'displayIds': displayIds};
        }

        function renderLicenseInputToFullData(entry) {
            return entry + "," + entry;
        }

        function showSetLicensesDialog(resultInputId) {
            var htmlElements = { 'addButton'       : $('#search-add-licenses-button'),
                                 'searchButton'    : $('#search-licenses-button'),
                                 'searchInput'     : $('#search-licenses-text'),
                                 'resetButton'     : $('#reset-licenses-button'),
                                 'resultTableBody' : $('#search-licenses-result-table-body'),
                                 'resultTable'     : $('#search-licenses-result-table'),
                                 'searchDiv'       : 'search-licenses-div',
                                 'emptyRow'        : "<tr class=\"trbodyClass\"><td></td><td></td></tr>"
            };

            var functions =    { 'ajaxSearch'  : searchLicenseAjax,
                                 'prepareData' : addAlreadySelectedLicensesToTable,
                                 'extractIds'  : getIdsFromSelectedLicensesData,
                                 'renderInput' : renderLicenseInputToFullData,
            };

            idSearch.openSearchDialog(true, resultInputId, htmlElements, functions, firstRun);
            firstRun = false;
        }
    });
</script>

