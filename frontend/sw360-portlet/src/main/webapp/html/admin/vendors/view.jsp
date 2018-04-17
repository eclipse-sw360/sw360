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
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="exportVendorsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EXPORT_TO_EXCEL%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_VENDOR%>'/>
</portlet:resourceURL>

<portlet:renderURL var="addVendorURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>" />
</portlet:renderURL>

<jsp:useBean id="vendorList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>"  scope="request"/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/datatables.net-buttons-dt/1.1.2/css/buttons.dataTables.min.css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader">
    <span class="pageHeaderBigSpan">Vendors</span> <span class="pageHeaderSmallSpan">(${vendorList.size()})</span>
    <span class="pull-right">
        <input type="button" class="addButton" onclick="window.location.href='<%=exportVendorsURL%>'" value="Export Vendors">
        <input type="button" class="addButton" onclick="window.location.href='<%=addVendorURL%>'" value="Add Vendor">
    </span>
</p>

<div id="searchInput" class="content1">
    <%@ include file="/html/utils/includes/quickfilter.jspf" %>
</div>
<div id="vendorsTableDiv" class="content2">
    <table id="vendorsTable" cellpadding="0" cellspacing="0" border="0" class="display">
        <tfoot>
        <tr>
            <th colspan="4"></th>
        </tr>
        </tfoot>
    </table>
</div>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'utils/includes/quickfilter', 'modules/confirm', /* jquery-plugins: */ 'datatables', 'datatables_buttons', 'buttons.print', 'jquery-confirm'], function($, quickfilter, confirm) {
            var vendorsTable,
                vendorIdInURL = '<%=PortalConstants.VENDOR_ID%>',
                pageName = '<%=PortalConstants.PAGENAME%>';
                pageEdit = '<%=PortalConstants.PAGENAME_EDIT%>';
                baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';

            // initializing
            vendorsTable = createVendorsTable();
            quickfilter.addTable(vendorsTable);

            // register event handlers
            $('#vendorsTable').on('click', 'img.delete', function (event) {
                var data = $(event.currentTarget).data();
                deleteVendor(data.vendorId, data.vendorName);
            });

            // helper functions
            function createDetailURLfromVendorId (paramVal) {
                var portletURL = PortletURL.createURL( baseUrl ).setParameter(pageName,pageEdit).setParameter(vendorIdInURL, paramVal);
                return portletURL.toString();
            }

            // catch ctrl+p and print dataTable
            $(document).on('keydown', function(e){
                if(e.ctrlKey && e.which === 80){
                    e.preventDefault();
                    vendorsTable.buttons('.custom-print-button').trigger();
                }
            });

            function createVendorsTable() {
                var vendorsTable,
                    result = [];

                <core_rt:forEach items="${vendorList}" var="vendor">
                    result.push({
                        "DT_RowId": "${vendor.id}",
                        "0": "<a href='"+createDetailURLfromVendorId('${vendor.id}')+"' target='_self'><sw360:out value="${vendor.fullname}"/></a>",
                        "1": "<sw360:out value="${vendor.shortname}"/>",
                        "2": "<sw360:out value="${vendor.url}"/>",
                        "3": "<a href='"+createDetailURLfromVendorId('${vendor.id}')+"' target='_self'><img src='<%=request.getContextPath()%>/images/edit.png' alt='Edit' title='Edit'></a>"
                        +"<img class='delete' src='<%=request.getContextPath()%>/images/Trash.png' data-vendor-id='${vendor.id}' data-vendor-name='<sw360:out value="${vendor.fullname}"/>')\"  alt='Delete' title='Delete'>"
                    });
                </core_rt:forEach>

                vendorsTable = $('#vendorsTable').DataTable({
                    pagingType: "simple_numbers",
                    dom: "lBrtip",
                    buttons: [
                        {
                            extend: 'print',
                            text: 'Print',
                            autoPrint: true,
                            className: 'custom-print-button',
                            exportOptions: {
                                columns: [0, 1, 2]
                            }
                        }
                    ],
                    data: result,
                    columns: [
                        {"title": "Full Name"},
                        {"title": "Short Name"},
                        {"title": "URL"},
                        {"title": "Actions"}
                    ],
                    autoWidth: false
                });

                return vendorsTable;
            }

            function deleteVendor(id, name) {
                function deleteVendorInternal() {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=deleteAjaxURL%>',
                        cache: false,
                        data: {
                            <portlet:namespace/>vendorId: id
                        },
                        success: function (data) {
                            if(data.result == 'SUCCESS')
                                vendorsTable.row('#' + id).remove().draw(false);
                            else {
                                $.alert("I could not delete the vendor!");
                            }
                        },
                        error: function () {
                            $.alert("I could not delete the vendor!");
                        }
                    });
                }

                confirm.confirmDeletion("Do you really want to delete the vendor <b>" + name + "</b> ?", deleteVendorInternal);
            }
        });
    });
</script>
