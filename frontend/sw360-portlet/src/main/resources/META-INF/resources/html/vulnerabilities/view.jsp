<%--
  ~ Copyright (c) Bosch Software Innovations GmbH 2016.
  ~ Copyright (c) Siemens AG 2016-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability" %>

<%@ include file="/html/init.jsp" %>

<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="vulnerabilityList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability>"
             scope="request"/>
<jsp:useBean id="totalRows" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="viewSize" type="java.lang.Integer" scope="request"/>

<jsp:useBean id="externalId" class="java.lang.String" scope="request"/>
<jsp:useBean id="vulnerableConfiguration" class="java.lang.String" scope="request"/>

<portlet:actionURL var="applyFiltersURL" name="applyFilters">
</portlet:actionURL>

<portlet:renderURL var="addVulnerabilitiesURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>
</portlet:renderURL>

<portlet:resourceURL var="vulnerabilityListAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.VULNERABILITY_LIST%>'/>
</portlet:resourceURL>

<portlet:renderURL var="editVulnerabilityURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>" />
</portlet:renderURL>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_VULNERABILITY%>'/>
</portlet:resourceURL>

<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck card-deck-vertical">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
                <div class="card">
                    <div class="card-header">
                        <liferay-ui:message key="advanced.filter" />
                    </div>
                    <div class="card-body">
                        <form action="<%=applyFiltersURL%>" method="post">
                            <div class="form-group">
                                <label for="external_id"><liferay-ui:message key="cve.id" /></label>
                                <input type="text" class="form-control" name="<portlet:namespace/><%=Vulnerability._Fields.EXTERNAL_ID%>"
                                    value="${externalId}" id="external_id">
                            </div>
                            <div class="form-group">
                                <label for="vulnerable_config"><liferay-ui:message key="vulnerable.configuration" /></label>
                                <input type="text" class="form-control" name="<portlet:namespace/><%=Vulnerability._Fields.VULNERABLE_CONFIGURATION%>"
                                    value="${vulnerableConfiguration}" id="vulnerable_config">
                            </div>
                            <input id="viewSize" name="<portlet:namespace/><%=PortalConstants.VIEW_SIZE%>" value="${viewSize}" type="hidden">
                            <button type="submit" class="btn btn-primary btn-sm btn-block"><liferay-ui:message key="filter" /></button>
                        </form>
                    </div>
                </div>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
					    <div class="btn-group" role="group">
				<button type="button" class="btn btn-primary" onclick="window.location.href='<%=addVulnerabilitiesURL%>'"><liferay-ui:message key="add.vulnerability" /></button>
                        </div>
						<div class="btn-group" role="group">
								<button id="viewSizeBtn" type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
									<liferay-ui:message key="show" /> <span data-name="count"></span>
									<clay:icon symbol="caret-bottom" />
								</button>
								<div class="dropdown-menu" aria-labelledby="btnExport">
									<a class="dropdown-item" data-type="200">200</a>
									<a class="dropdown-item" data-type="500">500</a>
									<a class="dropdown-item" data-type="1000">1000</a>
									<a class="dropdown-item" data-type="-1"><liferay-ui:message key="all" /></a>
								</div>
							</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="vulnerabilities" /> (<core_rt:if test="${vulnerabilityList.size() == totalRows}">${totalRows}</core_rt:if><core_rt:if test="${vulnerabilityList.size() != totalRows}">${vulnerabilityList.size()} latest of ${totalRows}</core_rt:if>)">
					<liferay-ui:message key="vulnerabilities" /> (<core_rt:if test="${vulnerabilityList.size() == totalRows}">${totalRows}</core_rt:if><core_rt:if test="${vulnerabilityList.size() != totalRows}">${vulnerabilityList.size()} latest of ${totalRows}</core_rt:if>)
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="vulnerabilitiesTable" class="table table-bordered" data-view-size="${viewSize}"></table>
                </div>
            </div>

		</div>
	</div>
</div>
<div class="dialogs auto-dialogs"></div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'bridges/datatables', 'modules/dialog', 'modules/alert', 'utils/link', 'utils/includes/quickfilter', 'bridges/jquery-ui'], function($, datatables, dialog, alert, linkutil, quickfilter) {
            var vulnerabilityTable;
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
            var pageName = '<%=PortalConstants.PAGENAME%>';
            var pageEdit = '<%=PortalConstants.PAGENAME_EDIT%>';
            var vulnerabilityIdInURL = '<%=PortalConstants.VULNERABILITY_ID%>',

            // initializing
            vulnerabilityTable = createVulnerabilityTable();
            quickfilter.addTable(vulnerabilityTable);

            vulnerabilityTable.on('preDraw', function() {
                $('[role="tooltip"]').css("display", "none");
                $('#vulnerabilitiesTable .info-text').tooltip("close");
            });

            vulnerabilityTable.on('draw', function() {
                $('[role="tooltip"]').css("display", "none");
                $('#vulnerabilitiesTable .info-text').tooltip("close");
            });

            var viewSize = $('#vulnerabilitiesTable').data().viewSize;
            $('#viewSizeBtn [data-name="count"]').text(viewSize > 0 ? '<liferay-ui:message key="latest" /> ' + viewSize : '<liferay-ui:message key="all" />');
            $('#viewSizeBtn + div > a').on('click', function(event) {
                var viewSize = $(event.currentTarget).data().type;

                var PortletURL = Liferay.PortletURL;
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>');
                portletURL.setParameter('<%=PortalConstants.VIEW_SIZE%>', viewSize);

                window.location.href = portletURL.toString() + window.location.hash;
            });

            // catch ctrl+p and print dataTable
            $(document).on('keydown', function(e){
                if(e.ctrlKey && e.which === 80){
                    e.preventDefault();
                    vulnerabilityTable.buttons('.custom-print-button').trigger();
                }
            });

            function createVulnerabilityTable() {
                var table,
                    result = [];

                <core_rt:forEach items="${vulnerabilityList}" var="vulnerability">
                    result.push({
                        DT_RowId: "${vulnerability.id}",
                        externalId: "<sw360:out value='${vulnerability.externalId}'  jsQuoting='true'/>",
                        title: {
                            text: "<sw360:out value='${vulnerability.title}'  jsQuoting='true'/>",
                            tooltip: "<sw360:out value='${vulnerability.description}' jsQuoting='true'/>"
                        },
                        cvss: {
                            weighting: "${vulnerability.cvss}",
                            time: "${vulnerability.cvssTime}",
                        },
                        priority: {
                            text: "<sw360:out value='${vulnerability.priority}'/>",
                            tooltip: "<sw360:out value='${vulnerability.priorityText}'/>"
                        },
                        publishDate: "<sw360:out value='${vulnerability.publishDate}'/>",
                        lastExternalUpdate: "<sw360:out value='${vulnerability.lastExternalUpdate}' default='not set'/>",
                        action: '<div class="actions">'
                                    +   '<svg class="edit lexicon-icon" data-vulnerability-id="${vulnerability.id}"><title><liferay-ui:message key="edit" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>'
                                    +   '<svg class="delete lexicon-icon" data-vulnerability-id="${vulnerability.id}" data-external-id="<sw360:out value="${vulnerability.externalId}"/>"><title><liferay-ui:message key="delete" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/></svg>'
                                + '</div>'
                    });
                </core_rt:forEach>

                function renderDetailURL(data, type, row, meta) {
                    var $link;
                    data = data.toString().replace(/&gt;/g, '>').replace(/&lt;/g, '<').replace(/&#039;/g, "'").replace(/&#034;/g, '"').replace(/&amp;/,'&');

                    if(type === 'display') {
                        $link = $('<a/>', {
                            href: createDetailURLFromVulnerabilityId(data),
                            _target: '_self'
                        }).text(data);
                        return $link[0].outerHTML;
                    } else if(type === 'print') {
                        return data;
                    } else if(type === 'type') {
                        return 'string';
                    } else {
                        return data;
                    }
                }

                function renderCvss(data, type, row, meta) {
                    var $cvss = $('<span/>', {
                        'class': 'text-danger'
                    });

                    if(type === 'display') {
                        if(data.weighting) {
                            $cvss.text(data.weighting);
                            if(data.time) {
                                $cvss.append(" (as of: ").append($('<span/>').text(data.time)).append(")");
                            }
                        }
                        $cvss.append($.fn.dataTable.render.infoText('/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open'));
                        return $cvss[0].outerHTML;
                    } else if(type === 'print') {
                        if(data.weighting) {
                            $cvss.text(data.weighting);
                            if(data.time) {
                                $cvss.append(" (as of: ").append($('<span/>').text(data.time)).append(")");
                            }
                        }
                    } else if(type === 'type') {
                        return 'number';
                    } else {
                        return data.weighting;
                    }
                }

                table = datatables.create('#vulnerabilitiesTable', {
                    data:result,
                    searching: true,
                    columns: [
                        { title: "<liferay-ui:message key="external.id" />", data: 'externalId', render: renderDetailURL },
                        { title: "<liferay-ui:message key="title" />", data: 'title', render: $.fn.dataTable.render.infoText() },
                        { title: "<liferay-ui:message key="weighting" />", data: 'cvss', render: renderCvss },
                        { title: "<liferay-ui:message key="publish.date" />", data: 'publishDate', default: '' },
                        { title: "<liferay-ui:message key="last.update" />", data: 'lastExternalUpdate', default: '' },
                        { title: "<liferay-ui:message key="actions" />", data: 'action', default: '', orderable: false}
                    ],
                    order: [[4, 'desc'],[3, 'desc']],
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    initComplete: datatables.showPageContainer
                }, [0, 1, 2, 3, 4]);

                return table;
            }

            function createDetailURLFromVulnerabilityId(paramVal) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                        .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>').setParameter('<%=PortalConstants.VULNERABILITY_ID%>', paramVal);
                return portletURL.toString();
            }

            $('#vulnerabilitiesTable').on('click', 'svg.edit', function (event) {
                var data = $(event.currentTarget).data();
                window.location.href = createEditURLFromVulnerabilityId(data.vulnerabilityId);
            });

            $('#vulnerabilitiesTable').on('click', 'svg.delete', function (event) {
                var data = $(event.currentTarget).data();
                deleteVulnerability(data.vulnerabilityId, data.externalId);
            });

            function createEditURLFromVulnerabilityId (paramVal) {
                 var portletURL = PortletURL.createURL( baseUrl ).setParameter(pageName, pageEdit).setParameter(vulnerabilityIdInURL, paramVal);
                 return portletURL.toString();
            }


            function deleteVulnerability(id, externalId) {
                var $dialog;
                var $container = $("#container");
                function deleteVulnerabilityInternal(callback) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=deleteAjaxURL%>',
                        cache: false,
                        data: {
                            <portlet:namespace/>vulnerabilityId: id
                        },
                        success: function (data) {
                            callback();
                            console.log(data);
                            if (data.result == 'SUCCESS') {
                                vulnerabilityTable.row('#' + id).remove().draw(false);
                                $('.modal-dialog p').remove();
                                $dialog.success('<liferay-ui:message key="vulnerability.deleted" />', true);
                            } else if (data.result == 'IN_USE') {
                                $('.modal-dialog p').remove();
                                $dialog.alert('<liferay-ui:message key="can.not.remove.vulnerability.because.used.by.release" />');
                            } else {
                                $('.modal-dialog p').remove();
                                $dialog.alert('<liferay-ui:message key="error.when.remove.vulnerability" />');
                            }
                        },
                        error: function () {
                            callback();
                            $dialog.alert('<liferay-ui:message key="error.when.remove.vulnerability" />');
                        }
                    });
                }

                $dialog = dialog.confirm(
                      'danger',
                      'question-circle',
                      '<liferay-ui:message key="delete.vulnerability" />?',
                      '<p><liferay-ui:message key="do.you.really.want.to.delete.the.vulnerability.x" />?</p>',
                      '<liferay-ui:message key="delete.vulnerability" />',
                      {
                            name: externalId,
                      },
                      function(submit, callback) {
                            deleteVulnerabilityInternal(callback);
                      }
                );
            }
        });
    });
 </script>
