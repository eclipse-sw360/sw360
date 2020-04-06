<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
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
    <h4 class="actions"><liferay-ui:message key="my.task.assignments" /> <span title="<liferay-ui:message key="reload" />"><clay:icon symbol="reload"/></span></h4>
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
                {"title": "<liferay-ui:message key="document.name" />", data: 'name', render: renderModerationRequestLink },
                {"title": "<liferay-ui:message key="status" />", data: 'state' },
            ],
            language: {
                paginate: {
                    previous: "<liferay-ui:message key="previous" />",
                    next: "<liferay-ui:message key="next" />"
                },
                emptyTable: "<liferay-ui:message key="there.are.no.tasks.assigned.to.you" />",
                info: "<liferay-ui:message key="showing" />",
                infoEmpty: "<liferay-ui:message key="infoempty" />",
                processing: "<liferay-ui:message key="processing" />",
                loadingRecords: "<liferay-ui:message key="loading" />"
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
