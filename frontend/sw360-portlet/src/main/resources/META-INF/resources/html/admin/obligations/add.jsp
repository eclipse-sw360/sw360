<%--
  ~ Copyright Siemens AG, 2013-2017.
  ~ Copyright TOSHIBA CORPORATION, 2021.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021.
  ~ Part of the SW360 Portal Project.
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
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode" %>

<jsp:useBean id="todo" class="org.eclipse.sw360.datahandler.thrift.licenses.Obligation" scope="request" />
<jsp:useBean id="obligList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>" scope="request"/>

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
                                    <td style="display: flex; width: 100%;">
                                        <div style="flex: 1; margin-right: 1rem;" class="form-group">
                                            <label for="todoTitle"><liferay-ui:message key="title" /></label>
                                            <input id="todoTitle" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.title" />" name="<portlet:namespace/><%=Obligation._Fields.TITLE%>"/>
                                            <div class="invalid-feedback" id="empty-title">
                                                <liferay-ui:message key="please.enter.a.title" />
                                            </div>
                                            <div class="invalid-feedback" id="duplicate-obl">
                                                <liferay-ui:message key="an.obligation.with.the.same.name.already.exists" />
                                            </div>
                                        </div>
                                        <div class="form-group" style="display: none;">
                                            <label for="obligsText"><liferay-ui:message key="text" /></label>
                                            <input id="obligsText" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.text" />" name="<portlet:namespace/><%=Obligation._Fields.TEXT%>"/>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.text" />
                                            </div>
                                        </div>
                                        <div style="flex: 1; margin-right: 1rem;" class="form-group">
                                            <label for="obligationType"><liferay-ui:message key="obligation.type" /></label>
                                            <select class="form-control" id="obligationType" name="<portlet:namespace/><%=Obligation._Fields.OBLIGATION_TYPE%>">
                                                <option value="">Select Obligation Type</option>
                                                <sw360:DisplayEnumOptions type="<%=ObligationType.class%>" selected="${todo.obligationType}"/>
                                            </select>
                                        </div>
                                        <div style="flex: 1; margin-right: 1rem;" class="form-group">
                                            <label for="obligationLevel"><liferay-ui:message key="obligation.level" /></label>
                                            <select class="form-control" id="obligationLevel" name="<portlet:namespace/><%=Obligation._Fields.OBLIGATION_LEVEL%>">
                                                <sw360:DisplayEnumOptions type="<%=ObligationLevel.class%>" selected="${todo.obligationLevel}"/>
                                            </select>
                                            <small class="form-text">
                                                <sw360:DisplayEnumInfo type="<%=ObligationLevel.class%>"/>
                                                <liferay-ui:message key="learn.more.about.obligation.level"/>
                                            </small>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="3">
                                        <div class="form-group">
                                            <label for="obligsText"><liferay-ui:message key="text"/></label>
                                            <div class="invalid-feedback" id="empty-text">
                                                <liferay-ui:message key="please.enter.a.text" />
                                            </div>
                                            <%@ include file="obligationTextTree.jsp" %>
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
        $('.invalid-feedback').css('display', 'none');
        $('.invalid-feedback').removeClass('d-block');
        validation.enableForm('#todoAddForm');

        $('.portlet-toolbar button[data-action="cancel"]').on('click', function() {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
            var portletURL = Liferay.PortletURL.createURL( baseUrl )
                .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
            window.location = portletURL.toString();
        });

        $('.portlet-toolbar button[data-action="save"]').on('click', function() {
            $('.invalid-feedback').css('display', 'none');
            $('.invalid-feedback').removeClass('d-block');
            if (checkObligation($("#todoTitle").val())) {
                const tree = readNode('#root');
                const jsonTextTree = JSON.stringify(tree);
                document.getElementById("obligsText").value = jsonTextTree;
                $('#todoAddForm').submit();
            }
        });

        function readNode(currentNode) {
            var nodeData = {val:[], children:[]};

            nodeData.val = getNodeValues(currentNode);

            const childNodes = $(currentNode).children('ul');

            $(childNodes).each(function(key, childNode) {
                var tmp = $(childNode).children('.tree-node').first();
                nodeData.children.push(readNode(tmp));
            });

            return nodeData;
        }

        function getNodeValues(node) {
            const children = $(node).children();

            var nodeValues = [];

            $.each(children, function(key, child) {
                if ($(child).is('input') && $(child).css('display') != 'none') {
                    nodeValues.push($(child).val());
                }
            });

            if ($(node).find('.elementType').val() == "Obligation") {
                nodeValues.push("UNDEFINED")
            }

            return nodeValues;
        }

        function checkObligation(title) {
            check = true
            if (title.trim().length == 0){
                $('#empty-title').addClass('d-block')
                check = false
            }
            <core_rt:forEach items="${obligList}" var="oblig">
                var obligationTitle = "<sw360:out value='${oblig.title}'/>"
                if (obligationTitle == title.trim()) {
                    $('#duplicate-obl').addClass('d-block')
                    check = false
                }
            </core_rt:forEach>

            var obligation_text = $('#out').text().replaceAll(" ","").replaceAll("\n","")
            if (obligation_text == "") {
                $('#empty-text').addClass('d-block')
                check = false
            }
            return check
        }
    });
</script>
