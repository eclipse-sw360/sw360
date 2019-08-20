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
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_TASK_SUBMISSION_LIST%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_MODERATION_REQUEST%>'/>
</portlet:resourceURL>

<section id="my-task-submissions">
    <h4 class="actions">My Task Submissions <span title="Reload"><clay:icon symbol="reload"/></span></h4>
    <div class="row">
        <div class="col">
            <table id="tasksubmissionTable" class="table table-bordered table-lowspace" data-load-url="<%=loadTasksURL%>">
                <colgroup>
                    <col style="width: 60%;"/>
                    <col style="width: 40%;"/>
                    <col style="width: 1.7rem"/>
                </colgroup>
            </table>
        </div>
    </div>
</section>

<div class="dialogs auto-dialogs"></div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'utils/link', 'utils/render' ], function($, datatables, dialog, link, render) {
        var $datatable;

        $datatable = createTable();

        $('#tasksubmissionTable').on('click', 'svg.delete', function(event) {
            var data = $(event.currentTarget).data();
            deleteModerationRequest(data.moderationId, data.documentName);
        });

        $('#my-task-submissions h4 svg')
            .attr('data-action', 'reload-my-task-submissions')
            .addClass('spinning disabled');

        $('#my-task-submissions').on('click', 'svg[data-action="reload-my-task-submissions"]:not(.disabled)', reloadTable);

        $(document).off('pageshow.my-task-submissions');
        $(document).on('pageshow.my-task-submissions', function() {
            reloadTable();
        });

        function createTable() {
            return datatables.create('#tasksubmissionTable', {
                // the following parameter must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
                bServerSide: false,
                // the following parameter must not be converted to 'ajax', otherwise it won't work anymore (probably due to datatable plugins)
                sAjaxSource: $('#tasksubmissionTable').data().loadUrl,

                dom:
                    "<'row'<'col-sm-12'tr>>" +
                    "<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
                columns: [
                    {"title": "Document Name", data: 'name', render: renderModerationRequestLink },
                    {"title": "Status", data: 'state' },
                    {"title": "Actions", data: 'id', className: "one action", orderable: false, render: renderDeleteAction }
                ],
                language: {
                    emptyTable: 'You do not have any open moderation requests.'
                },
                initComplete: function() {
                    $('#my-task-submissions h4 svg').removeClass('spinning disabled');
                }
            });
        }

        function renderModerationRequestLink(name, type, row) {
            return $('<a/>', {
                'class': 'text-truncate',
                title: name,
                href: link.to('moderationRequest', 'edit', row.id)
            }).text(name)[0].outerHTML;
        }

        function reloadTable() {
            $('#my-task-submissions h4 svg').addClass('spinning disabled');
            $datatable.ajax.reload(function() {
                $('#my-task-submissions h4 svg').removeClass('spinning disabled');
            }, false );
        }

        function renderDeleteAction(id, type, row) {
            var $actions = $('<div/>', {
                'class': 'actions'
            });
            $actions.append(render.trashIcon());
            $actions.find('.delete').attr({
                'data-moderation-id': id,
                'data-document-name': row.name
            });

            return $actions[0].outerHTML;
        }

        function deleteModerationRequest(id, docName) {
            var $dialog;

            function deleteModerationRequestInternal(callback) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=deleteAjaxURL%>',
                    cache: false,
                    data: {
                        <portlet:namespace/>moderationId: id
                    },
                    success: function (data) {
                        if (data.result == 'SUCCESS') {
                            callback(true);
                            $datatable.row('#' + id).remove().draw(false);
                        }
                        else {
                            callback();
                            $dialog.alert("I could not delete the moderation request!");
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert("I could not delete the moderation request!");
                    }
                });
            }

            $dialog = dialog.confirm(
                'danger',
                'question-circle',
                'Delete Moderation Request?',
                '<p>Do you really want to delete the moderation request for <b data-name="name"></b>?</p>',
                'Delete Moderation Request',
                {
                    name: docName,
                },
                function(submit, callback) {
                    deleteModerationRequestInternal(callback);
                }
            );
        }
    });
</script>
