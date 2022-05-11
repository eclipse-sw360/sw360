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
<jsp:useBean id="obligationId" type="java.lang.String" scope="request"/>
<jsp:useBean id="obligationAction" class="java.lang.String" scope="request"/>

<portlet:actionURL var="addURL" name="addObligations">
    <portlet:param name="<%=PortalConstants.OBLIGATION_ID%>" value="${obligationId}"/>
    <portlet:param name="<%=PortalConstants.OBLIGATION_ACTION%>" value="${obligationAction}"/>
</portlet:actionURL>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <div class="btn-group">
                            <core_rt:if test="${obligationAction == 'edit'}">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="update.obligation" /></button>
                            </core_rt:if>
                            <core_rt:if test="${obligationAction != 'edit'}">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="create.obligation" /></button>
                            </core_rt:if>
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
                                    <th colspan="3"></th>
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
                                <%@ include file="obligationTextTree.jsp" %>
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
    function buildNode(node, nodeTag) {
        if (typeof node.children == 'undefined') {
            return;
        }

        for (let i = 0; i < node.children.length; i++) {
            nodeTag.find('[data-func=add-child]').first().click();

            let child = node.children[i];
            let childTag = nodeTag.find('li').last();

            if (child.type == 'obligationElement') {
                childTag.find('.elementType').first().val('<Obligation>');
                childTag.find('.elementType').first().change();
                childTag.find('.obLangElement').first().val(child.langElement);
                childTag.find('.obAction').first().val(child.action);
                childTag.find('.obObject').first().val(child.object);
            } else {
                childTag.find('.elementType').first().val(child.type);
                childTag.find('.other').first().val(child.text);
            }

            buildNode(child, childTag);
        }
    }

    require(['jquery', 'modules/dialog', 'modules/validation' ], function($, dialog, validation) {
        $("div.alert-container").removeClass("cadmin");
        var action = '${obligationAction}';

        let obligationObj = jQuery.parseJSON(JSON.stringify(${ obligationJson }));
        let obligationListObj = jQuery.parseJSON(JSON.stringify(${ obligationListJson }));
        let obligationTextObj = jQuery.parseJSON(JSON.stringify(${ obligationTextJson }));

        if (action == 'edit') {
            $('[data-action="save"]').text("Update Obligation");
        }

        $(function () {
            if (action != '') {
                var oblType = obligationObj.obligationType;

                switch (oblType) {
                    case "PERMISSION":
                        $('#obligationType').val("0");
                        break
                    case "RISK":
                        $('#obligationType').val("1");
                        break
                    case "EXCEPTION":
                        $('#obligationType').val("2");
                        break
                    case "RESTRICTION":
                        $('#obligationType').val("3");
                        break
                    case "OBLIGATION":
                        $('#obligationType').val("4");
                        break
                    default:
                        $('#obligationType').val($("#obligationType option:first").val());
                }

                var oblLevel = obligationObj.obligationLevel;

                switch (oblLevel) {
                    case "ORGANISATION_OBLIGATION":
                        $('#obligationLevel').val("0");
                        break;
                    case "PROJECT_OBLIGATION":
                        $('#obligationLevel').val("1");
                        break;
                    case "COMPONENT_OBLIGATION":
                        $('#obligationLevel').val("2");
                        break;
                    case "LICENSE_OBLIGATION":
                        $('#obligationLevel').val("3");
                        break;
                    default:
                        $('#obligationLevel').val("0");
                }
            }

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

                if (checkObligation()) {
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
                        nodeValues.push($(child).val().trim());
                    }
                });

                if ($(node).find('.elementType').val() == '<Obligation>') {
                    nodeValues.push("UNDEFINED");
                }

                if (nodeValues.length > 0 && nodeValues[0] == '<Obligation>') {
                    nodeValues[0] = 'Obligation';
                }

                return nodeValues;
            }

            function checkObligation() {
                let errorList = [];
                let title = $("#todoTitle").val();

                if (title.trim().length == 0) {
                    errorList.push('empty-title');
                }

                for (let i = 0; i < obligationListObj.length; i++) {
                    let obligationTitle = obligationListObj[i].title;

                    if (obligationTitle == title.trim() &&
                        (obligationTitle != obligationObj.title || action != 'edit')) {
                        errorList.push('duplicate-obl');
                        break;
                    }
                }

                var obligationText = $('#out').text().substring(title.length).replaceAll(" ","").replaceAll("\n","");

                if (obligationText == '') {
                    errorList.push('empty-text');
                }

                if (errorList.length === 0) {
                    return true;
                } else {
                    showError(errorList);
                    return false;
                }
            }

            function showError(errorList) {
                errorList.forEach(e => {
                    $('#' + e).addClass('d-block');
                });
            }
        });
    });
</script>
