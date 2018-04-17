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


<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request" />
<div id="search-release-form" title="Search Release" style="display: none; background-color: #ffffff;">
    <form>
        <div style="display: inline-block">
            <input type="text" name="searchrelease" id="searchrelease" placeholder="search" class="searchbar"/>&nbsp;
            <input type="button" value="Search" class="searchbutton" id="releaseSearchButton"/>
            <core_rt:if test="${enableSearchForReleasesFromLinkedProjects}">
                <input type="button" value="Releases of linked projects" class="searchbutton" id="linkedReleasesButton" />
            </core_rt:if>
            <span style="display: none; color: red" id="loadingReleasesTableNotifier" >Loading table...</span>
        </div>

        <div id="Releasesearchresults">
            <table width="100%" id="releaseSearchResultsTable">
                <thead style="border-bottom: 2px solid #66c1c2;" >
                <tr class="trheader" style="height: 30px;">
                    <th width="4%">&nbsp;</th>
                    <th width="19.2%" align="left">Vendor</th>
                    <th width="19.2%" align="left">Release name</th>
                    <th width="19.2%" align="left">Version</th>
                    <th width="19.2%" align="left">Clearing state</th>
                    <th width="19.2%" align="left">Mainline state</th>
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
                <input type="button" value="Select" class="addButton" id="selectReleaseButton"/>
            </div>
        </div>
    </form>
</div>

<portlet:resourceURL var="viewReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_LINKED_RELEASES%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/>
</portlet:resourceURL>

<script>

    require(['jquery', /* jquery-plugins */ 'datatables', 'jquery-ui', 'jquery-confirm'], function($) {

        $("#addLinkedReleasesToReleaseButton").on('click', showReleaseDialog);
        $('#releaseSearchButton').on('click', function() { ReleaseContentFromAjax('releaseSearchResultsTable', '<%=PortalConstants.RELEASE_SEARCH%>', $('#searchrelease').val(), true); });
        <core_rt:if test="${enableSearchForReleasesFromLinkedProjects}">
            $('#linkedReleasesButton').on('click', function() { ReleaseContentFromAjax('releaseSearchResultsTable', '<%=PortalConstants.RELEASE_LIST_FROM_LINKED_PROJECTS%>', '', true); });
        </core_rt:if>
        $('#selectReleaseButton').on('click', selectRelease);


        var firstRunForReleasesTable = true;

        Liferay.on('allPortletsReady', function() {
                bindkeyPressToClick('searchrelease', 'searchbuttonrelease');}
        );

        function destroyReleaseDataTable() {
            $('#releaseSearchResultsTable').DataTable().destroy();
        }

        function toggleReleasesSearchNotification() {
            $('#releaseSearchButton').prop('disabled', !$('#releaseSearchButton').prop('disabled'));
            $('#linkedReleasesButton').prop('disabled', !$('#linkedReleasesButton').prop('disabled'));
            $('#loadingReleasesTableNotifier').toggle();
        }

        function makeReleaseDataTable() {
            $('#releaseSearchResultsTable').DataTable(
                {
                    "sPaginationType": "full_numbers",
                    "paging": false,
                    "scrollY": "220",
                    "info": false,
                    "bFilter": false,
                    "language": {"processing":     "Processing..."},
                    "processing": true,
                    "initComplete": toggleReleasesSearchNotification
                });
        }

        function showReleaseDialog() {
            openDialog('search-release-form', 'searchrelease');
            if (firstRunForReleasesTable) {
                makeReleaseDataTable();
                firstRunForReleasesTable = false;
            }
            if ($('#releaseSearchButton').attr('disabled') == 'disabled') {
                toggleReleasesSearchNotification();
            }
        }

        function selectRelease() {
            var releaseIds = [];

            $('#releaseSearchResultsTable').find(':checked').each(
                function () {
                    releaseIds.push(this.value);
                }
            );
            addReleaseInfo(releaseIds);

            closeOpenDialogs();
            return false;
        }


        function addReleaseInfo(linkedReleases) {
            ReleaseContentFromAjax('LinkedReleasesInfo', '<%=PortalConstants.LIST_NEW_LINKED_RELEASES%>', linkedReleases);
        }

        function setDataToReleaseTableAndRefresh(id, data) {
            if (data.indexOf("No releases found") == -1) {
                destroyReleaseDataTable();
                $('#' + id + " tbody").html(data);
                makeReleaseDataTable();
            } else {
                $('#' + id + " tbody").html(data);
                toggleReleasesSearchNotification();
            }
        }

        function ReleaseContentFromAjax(id, what, where, replace) {
            toggleReleasesSearchNotification()
            jQuery.ajax({
                type: 'POST',
                url: '<%=viewReleaseURL%>',
                data: {
                    '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                    '<portlet:namespace/><%=PortalConstants.WHERE%>': where
                },
                success: function (data) {
                    if (replace) {
                        setDataToReleaseTableAndRefresh(id, data);
                    } else {
                        $('#' + id + " tbody").append(data);
                    }
                }
            });
        }

    });
</script>

<%@include file="/html/utils/includes/linkedReleaseDelete.jspf" %>
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">