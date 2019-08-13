<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:resourceURL var="loadTasksURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_TASK_ASSIGNMENT_LIST%>'/>
</portlet:resourceURL>

<section id="my-task-assignments">
    <h4 class="actions">My Task Assignments <span title="Reload"><clay:icon symbol="reload"/></span></h4>
    <div class="row">
        <div class="col">
            <table id="taskassignmentTable" class="table table-bordered table-lowspace"  data-load-url="<%=loadTasksURL%>">
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

        $('#my-task-assignments h4 svg')
            .attr('data-action', 'reload-my-task-assignments')
            .addClass('spinning disabled');

        $('#my-task-assignments').on('click', 'svg[data-action="reload-my-task-assignments"]:not(.disabled)', reloadTable);

        $(document).off('pageshow.my-task-assignments');
        $(document).on('pageshow.my-task-assignments', function() {
            reloadTable();
        });

        table = datatables.create('#taskassignmentTable', {
            // the following parameter must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
            bServerSide: false,
            // the following parameter must not be converted to 'ajax', otherwise it won't work anymore (probably due to datatable plugins)
            sAjaxSource: $('#taskassignmentTable').data().loadUrl,

            dom:
				"<'row'<'col-sm-12'tr>>" +
				"<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            columns: [
                {"title": "Document Name", data: 'name', render: renderModerationRequestLink },
                {"title": "Status", data: 'state' },
            ],
            language: {
                emptyTable: 'There are no tasks assigned to you.'
            },
            initComplete: function() {
                $('#my-task-assignments h4 svg').removeClass('spinning disabled');
            }
        });

        function renderModerationRequestLink(name, type, row) {
            return $('<a/>', {
                'class': 'text-truncate',
                title: name,
                href: link.to('moderationRequest', 'edit', row.id)
            }).text(name)[0].outerHTML;
        }

        function reloadTable() {
            $('#my-task-assignments h4 svg').addClass('spinning disabled');
            table.ajax.reload(function() {
                $('#my-task-assignments h4 svg').removeClass('spinning disabled');
            }, false );
        }
    });
</script>
