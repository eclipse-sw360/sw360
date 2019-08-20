<%--
  ~ Copyright (c) Bosch Software Innovations GmbH 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<jsp:useBean id="vulnerability" class="org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability" scope="request" />


<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
			<a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab">Summary</a>
			<a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-metaData'}">active</core_rt:if>" href="#tab-metaData" data-toggle="list" role="tab">Metadata</a>
			<a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-references'}">active</core_rt:if>" href="#tab-references" data-toggle="list" role="tab">References</a>
		    </div>
	    </div>
	    <div class="col">
		<div class="row portlet-toolbar">
				<div class="col portlet-title text-truncate" title="<sw360:out value="${vulnerability.title}"/>">
					<sw360:out value="${vulnerability.title}"/>
				</div>
			</div>
			<div class="row">
				<div class="col">
		            <div class="tab-content">
		                <div id="tab-Summary" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Summary'}">active show</core_rt:if>" >
                            <%@include file="/html/vulnerabilities/summary.jspf" %>
                        </div>
		                <div id="tab-metaData" class="tab-pane <core_rt:if test="${selectedTab == 'tab-metaData'}">active show</core_rt:if>">
		                    <%@include file="/html/vulnerabilities/metaData.jspf" %>
		                </div>
                        <div id="tab-references" class="tab-pane <core_rt:if test="${selectedTab == 'tab-references'}">active show</core_rt:if>">
		                    <%@include file="/html/vulnerabilities/references.jspf" %>
		                </div>
		            </div>
		        </div>
		    </div>
        </div>
    </div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'modules/listgroup'], function($, listgroup) {
        listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');
    });
</script>
