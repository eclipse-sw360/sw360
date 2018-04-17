<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request" />
<div id="search-project-form" title="Search Project" style="display: none; background-color: #ffffff;">
    <form>
        <div style="display: inline-block">
            <input type="text" name="searchproject" id="searchproject" placeholder="search" class="searchbar"/>&nbsp;
            <input type="button" value="Search" class="searchbutton" id="searchbuttonproject"/>
            <span style="display: none; color: red" id="loadingProjectsTableNotifier" >Loading table...</span>
        </div>

        <div id="Projectsearchresults">
            <table width="100%" id="projectSearchResultstable">
                <thead style="border-bottom: 2px solid #66c1c2;" >
                <tr class="trheader" style="height: 30px;">
                    <th width="4%">&nbsp;</th>
                    <th class="textlabel" align="left">Project name</th>
                    <th class="textlabel" align="left">Version</th>
                    <th class="textlabel" align="left">State</th>
                    <th class="textlabel" align="left">Responsible</th>
                    <th class="textlabel" align="left">Description</th>
                </tr>
                </thead>
                <tbody>
                    <tr class="trbodyClass">
                        <td></td><td></td><td></td><td></td><td></td><td></td>
                    </tr>
                </tbody>
            </table>
            <hr noshade size="1" style="background-color: #66c1c2; border-color: #59D1C4;"/>
            <br/>

            <div>
                <input type="button" value="Select" class="addButton" id="selectProjectButton"/>
            </div>
        </div>
    </form>
</div>

<portlet:resourceURL var="viewProjectURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_LINKED_PROJECTS%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/>
</portlet:resourceURL>


<script>
    require(['jquery', /* jquery-plugins */ 'datatables', 'jquery-ui', 'jquery-confirm'], function($) {

        Liferay.on('allPortletsReady', function() {
            bindkeyPressToClick('searchproject', 'searchbuttonproject');
        });

        $('#addLinkedProjectButton').on('click', showProjectDialog);
        $('#searchbuttonproject').on('click', function() { ProjectContentFromAjax('projectSearchResultstable', '<%=PortalConstants.PROJECT_SEARCH%>', $('#searchproject').val(), true); });
        $('#selectProjectButton').on('click', selectProject);

        var firstRunForProjectsTable = true;

        function destroyProjectsDataTable() {
            $('#projectSearchResultstable').DataTable().destroy();
        }

        function toggleProjectSearchNotification() {
            $('#searchbuttonproject').prop('disabled', !$('#searchbuttonproject').prop('disabled'));
            $('#loadingProjectsTableNotifier').toggle();
        }

        function makeProjectsDataTable() {
            $('#projectSearchResultstable').DataTable(
                {
                    "sPaginationType": "full_numbers",
                    "paging": false,
                    "scrollY": "220",
                    "info": false,
                    "bFilter": false,
                    "language": {"processing":     "Processing..."},
                    "processing": true,
                    "initComplete": toggleProjectSearchNotification
                });
        }

        function showProjectDialog() {
            openDialog('search-project-form', 'searchproject');
            if (firstRunForProjectsTable) {
                makeProjectsDataTable();
                firstRunForProjectsTable = false;
            }
            if ($('#searchbuttonproject').attr('disabled') == 'disabled') {
                toggleProjectSearchNotification();
            }
        }

        function selectProject() {

            var projectIds = [];

            $('#projectSearchResultstable').find(':checked').each(
                function () {
                    projectIds.push(this.value);
                }
            );
            addProjectInfo(projectIds);

            closeOpenDialogs();
            return false;
        }


        function addProjectInfo(linkedProjects) {
            ProjectContentFromAjax('LinkedProjectsInfo', '<%=PortalConstants.LIST_NEW_LINKED_PROJECTS%>', linkedProjects);
        }

        function setDataToProjectsTableAndRefresh(id, data) {
            if (data.indexOf("No project found") == -1) {
                destroyProjectsDataTable();
                $('#' + id + " tbody").html(data);
                makeProjectsDataTable();
            } else {
                $('#' + id + " tbody").html(data);
                toggleProjectSearchNotification();
            }
        }

        function ProjectContentFromAjax(id, what, where, replace) {
            toggleProjectSearchNotification();
            jQuery.ajax({
                type: 'POST',
                url: '<%=viewProjectURL%>',
                data: {
                    '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                    '<portlet:namespace/><%=PortalConstants.WHERE%>': where
                },
                success: function (data) {
                    if (replace) {
                        setDataToProjectsTableAndRefresh(id, data);
                    } else {
                        $('#' + id + " tbody").append(data);
                    }
                }
            });
        }
    });

</script>


<%@include file="/html/projects/includes/linkedProjectDelete.jspf" %>
