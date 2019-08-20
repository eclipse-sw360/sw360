<%--
  ~ Copyright (c) Bosch Software Innovations GmbH 2016.
  ~ Copyright (c) Siemens AG 2016-2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability" %>

<%@ include file="/html/init.jsp" %>

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

<portlet:resourceURL var="vulnerabilityListAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.VULNERABILITY_LIST%>'/>
</portlet:resourceURL>

<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck card-deck-vertical">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
                <div class="card">
                    <div class="card-header">
                        Advanced Filter
                    </div>
                    <div class="card-body">
                        <form action="<%=applyFiltersURL%>" method="post">
                            <div class="form-group">
                                <label for="external_id">CVE ID</label>
                                <input type="text" class="form-control" name="<portlet:namespace/><%=Vulnerability._Fields.EXTERNAL_ID%>"
                                    value="${externalId}" id="external_id">
                            </div>
                            <div class="form-group">
                                <label for="vulnerable_config">Vulnerable Configuration</label>
                                <input type="text" class="form-control" name="<portlet:namespace/><%=Vulnerability._Fields.VULNERABLE_CONFIGURATION%>"
                                    value="${vulnerableConfiguration}" id="vulnerable_config">
                            </div>
                            <input id="viewSize" name="<portlet:namespace/><%=PortalConstants.VIEW_SIZE%>" value="${viewSize}" type="hidden">
                            <button type="submit" class="btn btn-primary btn-sm btn-block">Filter</button>
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
								<button id="viewSizeBtn" type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
									Show <span data-name="count"></span>
									<clay:icon symbol="caret-bottom" />
								</button>
								<div class="dropdown-menu" aria-labelledby="btnExport">
									<a class="dropdown-item" data-type="200">200</a>
									<a class="dropdown-item" data-type="500">500</a>
									<a class="dropdown-item" data-type="1000">1000</a>
									<a class="dropdown-item" data-type="-1">All</a>
								</div>
							</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="Vulnerabilities (<core_rt:if test="${vulnerabilityList.size() == totalRows}">${totalRows}</core_rt:if><core_rt:if test="${vulnerabilityList.size() != totalRows}">${vulnerabilityList.size()} latest of ${totalRows}</core_rt:if>)">
					Vulnerabilities (<core_rt:if test="${vulnerabilityList.size() == totalRows}">${totalRows}</core_rt:if><core_rt:if test="${vulnerabilityList.size() != totalRows}">${vulnerabilityList.size()} latest of ${totalRows}</core_rt:if>)
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
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'bridges/datatables', 'utils/includes/quickfilter', 'bridges/jquery-ui'], function($, datatables, quickfilter) {
            var vulnerabilityTable;

            // initializing
            vulnerabilityTable = createVulnerabilityTable();
            quickfilter.addTable(vulnerabilityTable);

            var viewSize = $('#vulnerabilitiesTable').data().viewSize;
            $('#viewSizeBtn [data-name="count"]').text(viewSize > 0 ? 'latest ' + viewSize : 'all');
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
                        externalId: "${vulnerability.externalId}",
                        title: {
                            text: "<sw360:out value='${vulnerability.title}'/>",
                            tooltip: "<sw360:out value='${vulnerability.description}'/>"
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
                        lastExternalUpdate: "<sw360:out value='${vulnerability.lastExternalUpdate}' default='not set'/>"
                    });
                </core_rt:forEach>

                function renderDetailURL(data, type, row, meta) {
                    var $link;

                    if(type === 'display') {
                        $link = $('<a/>', {
                            href: createDetailURLFromVulnerabilityId(data),
                            _target: '_self'
                        }).text(data);
                        return $link[0].outerHTML;
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
                        { title: "External Id", data: 'externalId', render: renderDetailURL },
                        { title: "Title", data: 'title', render: $.fn.dataTable.render.infoText() },
                        { title: "Weighting", data: 'cvss', render: renderCvss },
                        { title: "Publish Date", data: 'publishDate', default: '' },
                        { title: "Last Update", data: 'lastExternalUpdate', default: '' }
                    ],
                    order: [[4, 'desc'],[3, 'desc']],
                    initComplete: datatables.showPageContainer
                }, [0, 1, 2, 3, 4]);

                $('#vulnerabilitiesTable .info-text').tooltip({
                    delay: 0,
                    track: true,
                    fade: 250,
                    classes: {
                        "ui-tooltip": "ui-corner-all ui-widget-shadow info-text"
                    },
                    content: function () {
                        return $('<textarea/>').html($(this).prop('title')).val();
                    }
                });

                return table;
            }

            function createDetailURLFromVulnerabilityId(paramVal) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                        .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>').setParameter('<%=PortalConstants.VULNERABILITY_ID%>', paramVal);
                return portletURL.toString();
            }
        });
    });
 </script>
