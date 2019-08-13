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
<%@include file="/html/init.jsp"%>

<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />


<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.Todo" %>

<jsp:useBean id="todo" class="org.eclipse.sw360.datahandler.thrift.licenses.Todo" scope="request" />

<portlet:actionURL var="addURL" name="addTodo">
</portlet:actionURL>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <div class="btn-group">
                            <button type="button" class="btn btn-primary" data-action="save">Create ToDo</button>
                        </div>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel">Cancel</button>
                        </div>
					</div>
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <form id="todoAddForm" name="todoAddForm" action="<%=addURL%>" method="post" class="form needs-validation" novalidate>
                        <table id="todoAddTable" class="table edit-table three-columns">
                            <thead>
                                <tr>
                                    <th colspan="3">Add ToDo</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="todoTitle">Title</label>
                                            <input id="todoTitle" type="text" required class="form-control" placeholder="Enter title..." name="<portlet:namespace/><%=Todo._Fields.TITLE%>"/>
                                            <div class="invalid-feedback">
                                                Please enter a title!
                                            </div>
                                        </div>
                                    </td>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="todoText">Text</label>
                                            <input id="todoText" type="text" required class="form-control" placeholder="Enter text..." name="<portlet:namespace/><%=Todo._Fields.TEXT%>"/>
                                            <div class="invalid-feedback">
                                                Please enter a text!
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <div class="form-check">
                                            <input id="todoValidForProject" type="checkbox" class="form-check-input" name="<portlet:namespace/><%=Todo._Fields.VALID_FOR_PROJECT%>"/>
                                            <label for="todoValidForProject" class="form-check-label">Valid for Projects</label>
                                        </div>
                                    </td>
                                    <td></td>
                                    <td></td>
                                </tr>
                            </tbody>
                        </table>
                    </form>
                </div>
            </div>
		</div>
	</div>
</div>

<div class="dialogs auto-dialogs"></div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'modules/dialog', 'modules/validation' ], function($, dialog, validation) {
        validation.enableForm('#todoAddForm');

        $('.portlet-toolbar button[data-action="cancel"]').on('click', function() {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
            var portletURL = Liferay.PortletURL.createURL( baseUrl )
                .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
            window.location = portletURL.toString();
        });

        $('.portlet-toolbar button[data-action="save"]').on('click', function() {
            $('#todoAddForm').submit();
        });
    });
</script>
