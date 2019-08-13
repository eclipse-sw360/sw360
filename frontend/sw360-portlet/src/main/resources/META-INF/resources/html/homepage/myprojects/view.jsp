<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~ With contributions by Siemens Healthcare Diagnostics Inc, 2018.
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

<jsp:useBean id="projects" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.Project>"
             scope="request"/>

<h4>My Projects</h4>
<div class="row">
    <div class="col">
        <table id="myProjectsTable" class="table table-bordered table-lowspace">
            <colgroup>
                <col style="width: 40%;"/>
                <col style="width: 30%;"/>
                <col style="width: 30%;"/>
            </colgroup>
        </table>
    </div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables' ], function($, datatables) {
        var result = [];

        <core_rt:forEach items="${projects}" var="project">
            result.push({
                "DT_RowId": "${project.id}",
                "0": "<sw360:DisplayProjectLink project="${project}"/>",
                "1": '<sw360:out value="${project.description}"/>',
                "2": '<sw360:DisplayAcceptedReleases releaseClearingStateSummary="${project.releaseClearingStateSummary}"/>'
            });
        </core_rt:forEach>

        datatables.create('#myProjectsTable', {
            data: result,
            dom:
				"<'row'<'col-sm-12'tr>>" +
				"<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            columns: [
                {"title": "Project Name"},
                {"title": "Description"},
                {"title": "Approved Releases"},
            ],
            language: {
                emptyTable: 'You do not own any projects.'
            }
        });
    });
</script>
