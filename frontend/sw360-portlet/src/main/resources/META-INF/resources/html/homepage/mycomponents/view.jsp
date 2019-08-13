<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~ With contributions by Bosch Software Innovations GmbH, 2016.
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

<portlet:resourceURL var="loadComponentsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_COMPONENT_LIST%>'/>
</portlet:resourceURL>

<section id="my-components">
    <h4 class="actions">My Components <span title="Reload"><clay:icon symbol="reload"/></span></h4>
    <div class="row">
        <div class="col">
            <table id="myComponentsTable" class="table table-bordered table-lowspace" data-load-url="<%=loadComponentsURL%>">
                <colgroup>
                    <col style="width: 60%;"/>
                    <col style="width: 40%;"/>
                </colgroup>
            </table>
        </div>
    </div>
</section>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'utils/link' ], function($, datatables, link) {
        var table;

        $('#my-components h4 svg')
            .attr('data-action', 'reload-my-components')
            .addClass('spinning disabled');

        $('#my-components').on('click', 'svg[data-action="reload-my-components"]:not(.disabled)', reloadTable);

        $(document).off('pageshow.my-components');
        $(document).on('pageshow.my-components', function() {
            reloadTable();
        });

        table = datatables.create('#myComponentsTable', {
            // the following parameter must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
            bServerSide: false,
            // the following parameter must not be converted to 'ajax', otherwise it won't work anymore (probably due to datatable plugins)
            sAjaxSource: $('#myComponentsTable').data().loadUrl,

            dom:
				"<'row'<'col-sm-12'tr>>" +
				"<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            columns: [
                {"title": "Component Name", data: 'name', render: renderComponentNameLink},
                {"title": "Description", data: 'description', render: $.fn.dataTable.render.ellipsis }
            ],
            language: {
                emptyTable: 'You do not own any components.'
            },
            initComplete: function() {
                $('#my-components h4 svg').removeClass('spinning disabled');
            }
        });

        function renderComponentNameLink(name, type, row) {
            return $('<a/>', {
                'class': 'text-truncate',
                title: name,
                href: link.to('component', 'show', row.id)
            }).text(name)[0].outerHTML;
        }

        function reloadTable() {
            $('#my-components h4 svg').addClass('spinning disabled');
            table.ajax.reload(function() {
                $('#my-components h4 svg').removeClass('spinning disabled');
            }, false );
        }
    });
</script>
