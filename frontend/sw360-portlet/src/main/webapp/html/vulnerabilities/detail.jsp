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

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/themes/base/jquery-ui.min.css">
<script src="<%=request.getContextPath()%>/webjars/jquery/dist/jquery.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/dist/jquery.validate.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/dist/additional-methods.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/jquery-ui.min.js"></script>


<div id="content" >
    <div class="container-fluid">
        <div id="myTab" class="row-fluid" <core_rt:if test="${not empty selectedTab}"> data-initial-tab="${selectedTab}" </core_rt:if>>
            <ul class="nav nav-tabs span2">
                <li><a href="#tab-Summary">Summary</a></li>
                <li><a href="#tab-metaData">Meta data</a></li>
                <li><a href="#tab-references">References</a></li>
            </ul>
            <div class="tab-content span10">
                <div id="tab-Summary" class="tab-pane" >
                    <%@include file="/html/vulnerabilities/summary.jspf" %>
                </div>
                <div id="tab-metaData" >
                    <%@include file="/html/vulnerabilities/metaData.jspf" %>
                </div>
                <div id="tab-references" >
                    <%@include file="/html/vulnerabilities/references.jspf" %>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'modules/tabview'], function($, tabview) {
        tabview.create('myTab');
    });
</script>
