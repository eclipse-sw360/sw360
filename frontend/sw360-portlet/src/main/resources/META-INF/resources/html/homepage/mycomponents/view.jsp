<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
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

<jsp:useBean id="components" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Component>"
             class="java.util.ArrayList" scope="request"/>

<h4>My Components</h4>
<div class="row">
    <div class="col">
        <table id="myComponentsTable" class="table table-bordered table-lowspace">
            <colgroup>
                <col style="width: 60%;"/>
                <col style="width: 40%;"/>
            </colgroup>
        </table>
    </div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables' ], function($, datatables) {
        var result = [];

        <core_rt:forEach items="${components}" var="component">
            result.push({
                "DT_RowId": "${component.id}",
                "0": "<sw360:DisplayComponentLink component="${component}"/>",
                "1": '<sw360:out value="${component.description}"/>'
            });
        </core_rt:forEach>

        datatables.create('#myComponentsTable', {
            data: result,
            dom:
				"<'row'<'col-sm-12'tr>>" +
				"<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            columns: [
                {"title": "Component Name"},
                {"title": "Description"}
            ],
            language: {
                emptyTable: 'You do not own any components.'
            }
        });
    });
</script>
