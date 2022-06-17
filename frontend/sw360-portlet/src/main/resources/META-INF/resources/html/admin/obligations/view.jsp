<%--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_TODO%>'/>
</portlet:resourceURL>

<portlet:renderURL var="addObligationsURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_ADD%>" />
</portlet:renderURL>

<jsp:useBean id="obligList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>" scope="request"/>

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
							<button type="button" class="btn btn-primary" onclick="window.location.href='<%=addObligationsURL%>'"><liferay-ui:message key="add.obligation" /></button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="obligations" /> (${obligList.size()})">
					<liferay-ui:message key="obligations" /> (${obligList.size()})
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
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;
        require(['jquery', 'bridges/datatables', 'modules/dialog', 'utils/includes/quickfilter', 'utils/render'], function($, datatables, dialog, quickfilter, render) {
            var todoTable = createTodoTable();
            quickfilter.addTable(todoTable);

            // register event handlers
            $('#todoTable').on('click', 'svg.delete', function (event) {
                var data = $(event.currentTarget).data();
                deleteObligations(data.id, data.title);
            });

            function createTodoTable() {
                var todosTbl,
                    result = [];

                <core_rt:forEach items="${obligList}" var="oblig">
                    var obligText = "<sw360:out value='${oblig.text}' stripNewlines='false' jsQuoting='true'/>";
                    obligText = obligText.replace(/[\r\n]/g, '<br>');
                    obligText = obligText.replace(/\t/g, '&ensp;&ensp;&ensp;&ensp;');
                    result.push({
                        DT_RowId: "${oblig.id}",
                        id: "${oblig.id}",
                        title: "<sw360:out value='${oblig.title}'/>",
                        text: '<p style="overflow: auto; max-height: 10rem;">'+obligText+'</p>',
                        obligationLevel: "<sw360:DisplayEnum value="${oblig.obligationLevel}"/>"
                    });
                </core_rt:forEach>

                todosTbl = datatables.create('#todoTable', {
                    searching: true,
                    data: result,
                    columns: [
                        {"title": "<liferay-ui:message key="title" />", data: 'title' },
                        {"title": "<liferay-ui:message key="text" />", data: 'text' },
                        {"title": "<liferay-ui:message key="obligation.level" />", data: 'obligationLevel'},
                        {"title": "<liferay-ui:message key="actions" />", data: 'id', render: renderActions, className: 'four actions', orderable: false}
                    ],
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    initComplete: datatables.showPageContainer
                }, [0, 1], [3]);

                return todosTbl;
            }

            function renderActions(value, type, row, meta) {
                if(type === 'display') {
                    var id = value
                    var $actions = $('<div>', {
                            'class': 'actions'
                        }),

                        $deleteAction = '<a><svg class="delete lexicon-icon" name="delete-annotation" data-id=' + value + ' data-title="' + row.title + '" viewBox="0 0 512 512"><title><liferay-ui:message key="delete" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/></svg></a>',

                        $duplicateAction = render.linkTo(
                            makeComponentUrl(id, '<%=PortalConstants.PAGENAME_DUPLICATE%>'),
                            "",
                            '<svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#paste"/><title><liferay-ui:message key="duplicate" /></title></svg>'
                        ),

                        $editAction = render.linkTo(
                            makeComponentUrl(id, '<%=PortalConstants.PAGENAME_EDIT%>'),
                            "",
                            '<svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/><title><liferay-ui:message key="edit" /></title></svg>'
                        ),

                        $viewChangeLogAction = render.linkTo(
                            makeComponentUrl(id, 'obligation_changelog'),
                            "",
                            '<svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#document"/><title><liferay-ui:message key="change.log" /></title></svg>'
                        );

                    $actions.append($editAction, $duplicateAction, $viewChangeLogAction, $deleteAction);
                    return $actions[0].outerHTML;
                } else if(type === 'type') {
                    return 'string';
                } else {
                    return '';
                }
            }

            // helper functions
            function makeComponentUrl(obligationId, page) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                        .setParameter('<%=PortalConstants.PAGENAME%>', page)
                        // .setParameter('<%=PortalConstants.OBLIGATION_ID%>', obligationId)
                        .setParameter('<%=PortalConstants.DOCUMENT_ID%>', obligationId)
                return portletURL.toString();
            }

            function deleteObligations(id, title) {
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
                                $dialog.alert('<liferay-ui:message key="do.you.really.want.to.delete.the.obligation.x" />');
                            } else {
                                $dialog.alert("<liferay-ui:message key="i.could.not.delete.the.obligation" />");
                            }
                        },
                        error: function () {
                            callback();
                            $dialog.alert("<liferay-ui:message key="i.could.not.delete.the.obligation" />");
                        }
                    });
                }

                $dialog = dialog.confirm(
                    'danger',
                    'question-circle',
                    '<liferay-ui:message key="delete.obligation" />?',
                    '<p><liferay-ui:message key="do.you.really.want.to.delete.the.obligation.x" />?</p>',
                    '<liferay-ui:message key="delete.obligation" />',
                    {
                        title: title,
                    },
                    function(submit, callback) {
                        deleteTodoInternal(callback);
                    }
                );
            }
        });
    });
</script>
