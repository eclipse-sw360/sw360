<%--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
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

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_TODO%>'/>
</portlet:resourceURL>

<portlet:renderURL var="addTodoURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_ADD%>" />
</portlet:renderURL>

<jsp:useBean id="todoList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Todo>" scope="request"/>

<div class="container">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" onclick="window.location.href='<%=addTodoURL%>'">Add ToDo</button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="ToDos (${todoList.size()})">
					ToDos (${todoList.size()})
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="todoTable" class="table table-bordered">
                        <colgroup>
                            <col />
                            <col />
                            <col />
                            <col style="width: 1.7rem"/>
                        </colgroup>
                    </table>
                </div>
            </div>

		</div>
	</div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs"></div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'utils/includes/quickfilter'], function($, datatables, dialog, quickfilter) {
        var todoTable = createTodoTable();
        quickfilter.addTable(todoTable);

        // register event handlers
        $('#todoTable').on('click', 'svg.delete', function (event) {
            var data = $(event.currentTarget).data();
            deleteTodo(data.id, data.title);
        });

        function createTodoTable() {
            var todosTbl,
                result = [];

            <core_rt:forEach items="${todoList}" var="todo">
                result.push({
                    DT_RowId: "${todo.id}",
                    id: "${todo.id}",
                    title: "${todo.title}",
                    text: "${todo.text}",
                    projectValidity: ${todo.validForProject},
                });
            </core_rt:forEach>

            todosTbl = datatables.create('#todoTable', {
                searching: true,
                data: result,
                columns: [
                    {"title": "Title", data: 'title' },
                    {"title": "Text", data: 'text' },
                    {"title": "Valid for Projects", data: 'projectValidity', className: 'text-center', render: $.fn.dataTable.render.inputCheckbox('project-validity', '', false, checkboxHook) },
                    {"title": "Actions", data: 'id', render: renderActions }
                ],
                initComplete: datatables.showPageContainer
            }, [0, 1, 2], [3]);

            return todosTbl;
        }

        function checkboxHook(value) {
            var $input = this;

            if(value) {
                $input.attr('checked', 'checked');
            }
            $input.prop('disabled', true);
        }

        function renderActions(value, type, row, meta) {
            if(type === 'display') {
                var $actions = $('<div>', {
                        'class': 'actions'
                    }),
                    $deleteAction = $('<svg>', {
                        'class': 'delete lexicon-icon',
                        title: 'Delete',
                        'data-id': value,
                        'data-title': row.title
                    });
                $deleteAction.append($('<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

                $actions.append($deleteAction);
                return $actions[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return '';
            }
        }

        function deleteTodo(id, title) {
            var $dialog;

            function deleteTodoInternal(callback) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=deleteAjaxURL%>',
                    cache: false,
                    data: {
                        <portlet:namespace/>id: id
                    },
                    success: function (data) {
                        callback();

                        if(data.result == 'SUCCESS') {
                            todoTable.row('#' + id).remove().draw(false);
                            $dialog.close();
                        } else if(data.result == 'ACCESS_DENIED') {
                            $dialog.alert("Only admin users can delete Todos!");
                        } else {
                            $dialog.alert("I could not delete the todo.");
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert("I could not delete the todo.");
                    }
                });
            }

            $dialog = dialog.confirm(
                'danger',
                'question-circle',
                'Delete ToDo?',
                '<p>Do you really want to delete the todo <b data-name="title"></b>?</p>',
                'Delete ToDo',
                {
                    title: title,
                },
                function(submit, callback) {
                    deleteTodoInternal(callback);
                }
            );
        }
    });
</script>
