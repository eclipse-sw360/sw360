<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp"%>

<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />


<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.Obligation" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationType" %>

<jsp:useBean id="todo" class="org.eclipse.sw360.datahandler.thrift.licenses.Obligation" scope="request" />

<portlet:actionURL var="addURL" name="addObligations">
</portlet:actionURL>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <div class="btn-group">
                            <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="create.obligation" /></button>
                        </div>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message key="cancel" /></button>
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
                                    <th colspan="3"><liferay-ui:message key="add.obligation" /></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="todoTitle"><liferay-ui:message key="title" /></label>
                                            <input id="todoTitle" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.title" />" name="<portlet:namespace/><%=Obligation._Fields.TITLE%>"/>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.title" />
                                            </div>
                                        </div>
                                    </td>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="obligsText"><liferay-ui:message key="text" /></label>
                                            <input id="obligsText" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.text" />" name="<portlet:namespace/><%=Obligation._Fields.TEXT%>"/>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.text" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="obligationType"><liferay-ui:message key="obligation.type" /></label>
                                            <select class="form-control" id="obligationType" name="<portlet:namespace/><%=Obligation._Fields.OBLIGATION_TYPE%>">
                                                <option value="">Select Obligation Type</option>
                                                <sw360:DisplayEnumOptions type="<%=ObligationType.class%>" selected="${todo.obligationType}"/>
                                            </select>
                                        </div>
                                    </td>
                                    <td>
                                    <div class="form-group">
                                        <label for="obligationLevel"><liferay-ui:message key="obligation.level" /></label>
                                        <select class="form-control" id="obligationLevel" name="<portlet:namespace/><%=Obligation._Fields.OBLIGATION_LEVEL%>">
                                            <sw360:DisplayEnumOptions type="<%=ObligationLevel.class%>" selected="${todo.obligationLevel}"/>
                                        </select>
                                     </div>
                                    </td>
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
