<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~ With contributions by Bosch Software Innovations GmbH, 2016.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:resourceURL var="loadComponentsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_COMPONENT_LIST%>'/>
</portlet:resourceURL>

<section id="my-components">
    <h4 class="actions"><liferay-ui:message key="my.components" /> <span title="<liferay-ui:message key="reload" />"><clay:icon symbol="reload"/></span></h4>
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
                {"title": "<liferay-ui:message key="component.name" />", data: 'name', render: renderComponentNameLink},
                {"title": "<liferay-ui:message key="description" />", data: 'description', render: $.fn.dataTable.render.ellipsis }
            ],
            language: {
                paginate: {
                    previous: "<liferay-ui:message key="previous" />",
                    next: "<liferay-ui:message key="next" />"
                },
                emptyTable: "<liferay-ui:message key="you.do.not.own.any.components" />",
                info: "<liferay-ui:message key="showing" />",
                infoEmpty: "<liferay-ui:message key="infoempty" />",
                processing: "<liferay-ui:message key="processing" />",
                loadingRecords: "<liferay-ui:message key="loading" />"
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
