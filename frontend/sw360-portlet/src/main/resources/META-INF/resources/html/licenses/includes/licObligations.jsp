<%--
  ~ Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<div id="license-obligation-view">
    <table id="licenseTodoTableDialog" class="table table-bordered">
        <colgroup>
            <col style="width: 4rem;" />
            <col />
        </colgroup>
        <thead>
            <tr>
                <th class="license-more-info">
                    <span title="<liferay-ui:message key="expand.all" />" data-show="false">&#x25BA</span>
                 </th>
                 <th id="select-all">
                     <input title='<liferay-ui:message key="select.all" />' type="checkbox" id="selectAllLicenseObligation" class="form-check-input"/>
                 </th>
                 <th><liferay-ui:message key="obligation.title" /></th>
                 <th><liferay-ui:message key="obligation.type" /></th>
            </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
</div>
<script>
    require(['jquery', 'modules/dialog', 'bridges/datatables', 'modules/validation', 'utils/render'], function($, dialog, datatables, validation, render) {
    	
    var licenseObligationJSON = [];
    <core_rt:forEach var="ob" varStatus="status" items="${obligationList}">
    	licenseObligationJSON.push({
    		"DT_RowId": "oblLinkRow${ob.id}",
            "obligationId": "<sw360:out value='${ob.id}'/>",
            "obligationType": "<sw360:DisplayEnum value='${ob.obligationType}'/>", 
            "obligationTitle": "<sw360:out value='${ob.title}'/>",
            "text": "<sw360:out value='${ob.text}'/>",
        });
    </core_rt:forEach>
    
    var licensetable = datatables.create('#licenseTodoTableDialog', {
        "data": licenseObligationJSON,
        "deferRender": false,
        "columns": [
            {
                "className": 'license-details-control',
                "data": null,
                "defaultContent": '&#x25BA'
            },
            {
                "data": null,
                'className': 'dt-body-center',
                'render': function (data, type, full, meta){
                    return '<input type="checkbox" class="checkbox-control" >';
                }
            },
            { "data": "obligationTitle", className: 'text-center' },
            { "data": "obligationType", className: 'text-center' },

        ],
        "columnDefs": [
            {
                "targets": 2,
                "createdCell": function (td, cellData, rowData, row, col) {
                    $(td).attr('title', 'click the icon to toggle obligation text');
                }
            },
        ],
        "order": [[2, 'asc']],
        "initComplete": datatables.showPageContainer
    }, [2], [0, 1], true);
    
    $('#licenseTodoTableDialog tbody').on('click', 'td.license-details-control', function () {
        render.toggleChildRow($(this), licensetable);
    });

    $('#licenseTodoTableDialog thead').on('click', 'th.license-more-info', function() {
        render.toggleAllChildRows($(this), licensetable);
    });
    
    $('#selectAllLicenseObligation').on('click', function() {
       let selectAll = $('#selectAllLicenseObligation').prop("checked");
       licensetable.rows().every(function (rowIdx, tableLoop, rowLoop) {
           $node = $(this.node()),
           $node.find("input.checkbox-control").prop('checked', selectAll);
       });
    });

    $('#licenseTodoTableDialog tbody').on('change', 'input[type="checkbox"]', function() {
       if (!this.checked) {
          var all = $('#selectAllLicenseObligation').get(0);
          if (all && all.checked && ('indeterminate' in all)) {
             all.indeterminate = true;
          }
       }
    });
});
</script>