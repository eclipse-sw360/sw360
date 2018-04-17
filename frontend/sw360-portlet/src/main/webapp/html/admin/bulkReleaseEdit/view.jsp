<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>"
             scope="request"/>

<portlet:resourceURL var="updateReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.RELEASE%>"/>
</portlet:resourceURL>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Release Bulk Edit</span>
</p>
<div id="searchInput" class="content1">
    <%@ include file="/html/utils/includes/quickfilter.jspf" %>
</div>

<div id="content" class="content2">
    <table class="table info_table" id="ComponentBasicInfo">
        <thead>
        <tr>
            <th width="13%">Status</th>
            <th width="20%">CPE id</th>
            <th width="20%">Vendor</th>
            <th width="20%">Release name</th>
            <th width="20%">Release version</th>
            <th width="7%">Submit</th>
        </tr>
        </thead>
        <core_rt:forEach items="${releaseList}" var="release">
            <tr id="TableRow${release.id}">
                <td width="13%" id="Status${release.id}"></td>
                <td width="20%">
                    <label>
                    <input id='cpeid${release.id}'  type="text"
                           class="toplabelledInput"
                           placeholder="Enter CPE ID" required="" value="${release.cpeid}"/>
                    </label>
                    <%-- this and following hidden spans are added to make display filter and sorting using dataTables work--%>
                    <span style="display:none" id='plaincpeid${release.id}'>${release.cpeid}</span>
                </td>
                <td width="20%">
                    <sw360:DisplayVendorEdit id='vendorId${release.id}' displayLabel="false"
                                             vendor="${release.vendor}" releaseId="${release.id}"/>
                    <span style="display:none" id='plainvendor${release.id}'>${release.vendor.fullname}</span>
                </td>
                <td width="20%">
                    <label>
                    <input id='name${release.id}' type="text"
                           placeholder="Enter Name"
                                value="<sw360:out value="${release.name}"/>"
                            />
                    </label>
                    <span style="display:none" id='plainname${release.id}'>${release.name}</span>
                </td>
                <td width="20%">
                    <label>
                    <input id='version${release.id}'  type="text"
                           placeholder="Enter Version"
                           value="<sw360:out value="${release.version}"/>"/>
                    </label>
                    <span style="display:none" id='plainversion${release.id}'>${release.version}</span>
                </td>
                <td width="7%">
                    <input type="button" name="submit" data-release-id="${release.id}"  value="OK" />
                </td>
            </tr>
        </core_rt:forEach>
    </table>
</div>

<%@include file="/html/components/includes/vendors/searchVendor.jspf" %>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'utils/includes/quickfilter', 'modules/autocomplete', 'components/includes/vendors/searchVendor', /* jquery-plugins: */ 'datatables', 'jquery-confirm'], function($, quickfilter, autocomplete, vendorsearch) {
            var componentsInfoTable;

            // initializing
            load();

            // register event handlers
            $('#ComponentBasicInfo').on('click', 'input.edit-vendor', function(event) {
                var data = $(event.currentTarget).data();
                displayVendors(data.releaseId);
            });
            $('#ComponentBasicInfo').on('click', 'input[name=submit]', function(event) {
                var data = $(event.currentTarget).data();
                submitRow(data.releaseId);
            });

            // helper functions
            function load() {
                componentsInfoTable = configureComponentBasicInfoTable();
                quickfilter.addTable(componentsInfoTable);
            }

            function configureComponentBasicInfoTable(){
                var tbl;

                tbl = $('#ComponentBasicInfo').DataTable({
                    "pagingType": "simple_numbers",
                    "dom": "lrtip",
                    "autoWidth": false,
                    "columnDefs": [
                        { "width": "13%", "targets": [ 0 ] },
                        { "width": "20%", "targets": [ 1 ] },
                        { "width": "20%", "targets": [ 2 ] },
                        { "width": "20%", "targets": [ 3 ] },
                        { "width": "20%", "targets": [ 4 ] },
                        { "width": "7%", "targets": [ 5 ] }
                    ]
                });

                return tbl;
            }

            function submitRow(id) {
                var resultElement  = $('#Status'+id);
                resultElement.text("...");
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=updateReleaseURL%>',
                    data: {
                        <portlet:namespace/>releaseId: id,
                        <portlet:namespace/>VENDOR_ID: $('#vendorId'+id).val(),
                        <portlet:namespace/>CPEID:$('#cpeid'+id).val(),
                        <portlet:namespace/>NAME:$('#name'+id).val(),
                        <portlet:namespace/>VERSION:$('#version'+id).val()
                    },
                    success: function (data) {
                        resultElement.text(data.result);
                    },
                    error: function () {
                        resultElement.text("error");
                    }
                });
            }

            function displayVendors(releaseId) {
                vendorsearch.openSearchDialog('<portlet:namespace/>what', '<portlet:namespace/>where',
                  '<portlet:namespace/>FULLNAME', '<portlet:namespace/>SHORTNAME', '<portlet:namespace/>URL', function(vendorInfo) {
                    fillVendorInfo(releaseId, vendorInfo);
                });
            }

            function fillVendorInfo(releaseId, vendorInfo) {
                var beforeComma = vendorInfo.substr(0, vendorInfo.indexOf(","));
                var afterComma = vendorInfo.substr(vendorInfo.indexOf(",") + 1);

                $('#vendorId'+ releaseId).val(beforeComma.trim());
                $('#vendorId'+ releaseId+'Display').val(afterComma.trim());
            }
        });
    });
</script>

