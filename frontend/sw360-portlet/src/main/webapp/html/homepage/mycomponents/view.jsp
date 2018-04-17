<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~ With contributions by Bosch Software Innovations GmbH, 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%-- Note that the necessary includes are in life-ray-portlet.xml --%>

<jsp:useBean id="components" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Component>"
             class="java.util.ArrayList" scope="request"/>

<div class="homepageheading">
    My Components
</div>

<div id="myComponentsDiv" class="homepageListingTable">
    <table id="myComponentsTable" cellpadding="0" cellspacing="0" border="0" class="display">
    </table>
</div>

<script>

    Liferay.on('allPortletsReady', function() {
        var result = [];

        <core_rt:forEach items="${components}" var="component">

        result.push({
            "DT_RowId": "${component.id}",
            "0": "<sw360:DisplayComponentLink component="${component}"/>",
            "1": '<sw360:out value="${component.description}" maxChar="30"/>'
        });

        </core_rt:forEach>

        $('#myComponentsTable').dataTable({
            pagingType: "simple_numbers",
            dom: "rtip",
            data: result,
            pageLength: 10,
            columns: [
                {"title": "Component Name"},
                {"title": "Description"}
            ],
            autoWidth: false
        });
    });

</script>
