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



<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_TODO%>'/>
</portlet:resourceURL>

<portlet:renderURL var="addTodoURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_ADD%>" />
</portlet:renderURL>

<jsp:useBean id="todoList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Todo>" scope="request"/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/datatables.net-buttons-bs/css/buttons.bootstrap.min.css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader">
    <span class="pageHeaderBigSpan">TODOs</span> <span class="pageHeaderSmallSpan">(${todoList.size()})</span>
    <span class="pull-right">
        <input type="button" class="addButton" onclick="window.location.href='<%=addTodoURL%>'" value="Add Todo">
    </span>
</p>

<div id="searchInput" class="content1">
    <%@ include file="/html/utils/includes/quickfilter.jspf" %>
</div>
<div id="todoTableDiv" class="content2">
    <table id="todoTable" cellpadding="0" cellspacing="0" border="0" class="display">
        <tfoot>
        <tr>
            <th colspan="4"></th>
        </tr>
        </tfoot>
    </table>
</div>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-confirm2/dist/jquery-confirm.min.css">

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {

        require(['jquery', 'utils/includes/quickfilter', 'modules/confirm', /* jquery-plugins: */ 'datatables.net', 'datatables.net-buttons', 'datatables.net-buttons.print', 'jquery-confirm'], function($, quickfilter, confirm) {

            var todoTable = createTodoTable();
            quickfilter.addTable(todoTable);

            // register event handlers
            $('#todoTable').on('click', 'img.delete', function (event) {
                var data = $(event.currentTarget).data();
                deleteTodo(data.id, data.title);
            });


            function createTodoTable() {
                var todosTbl,
                    result = [];

                <core_rt:forEach items="${todoList}" var="todo">
                result.push({
                    "DT_RowId": "${todo.id}",
                    "0": "${todo.title}",
                    "1": "${todo.text}",
                    "2": "<input type='checkbox' disabled='true' <core_rt:if test="${todo.validForProject}">checked</core_rt:if>>",
                    "3": "<img class='delete' src='<%=request.getContextPath()%>/images/Trash.png' data-id='${todo.id}'  data-title='${todo.title}' alt='Delete' title='Delete'>"
                });
                </core_rt:forEach>

                todosTbl = $('#todoTable').DataTable({
                    pagingType: "simple_numbers",
                    dom: "lBrtip",
                    data: result,
                    columns: [
                        {"title": "Title"},
                        {"title": "Text"},
                        {"title": "Valid For Projects"},
                        {"title": "Actions"}
                    ],
                    autoWidth: false
                });

                return todosTbl;
            }

            function deleteTodo(id, title) {
                function deleteTodoInternal() {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=deleteAjaxURL%>',
                        cache: false,
                        data: {
                            <portlet:namespace/>id: id
                        },
                        success: function (data) {
                            if(data.result == 'SUCCESS') {
                                todoTable.row('#' + id).remove().draw(false);
                            } else if(data.result == 'ACCESS_DENIED') {
                                $.alert("Only admin users can delete TODOs!");
                            } else {
                                $.alert("Deleting TODO failed!");
                            }
                        },
                        error: function () {
                            $.alert("Deleting TODO failed");
                        }
                    });
                }

                confirm.confirmDeletion("Do you really want to delete TODO  <b>" + title + "</b> ?", deleteTodoInternal);
            }
        });
    });
</script>
