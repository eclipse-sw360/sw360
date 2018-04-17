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
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="userList" type="java.util.List<com.liferay.portal.model.User>" scope="request"/>
<jsp:useBean id="missingUserList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.users.User>"
             scope="request"/>
<portlet:actionURL var="updateLifeRayUsers" name="updateUsers">
</portlet:actionURL>


<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Liferay Users</span> <span
        class="pageHeaderSmallSpan">(${userList.size()}) </span></p>

<div id="searchInput" class="content1">
    <%@ include file="/html/utils/includes/quickfilter.jspf" %>
</div>

<div id="searchTableDiv" class="content2">
    <h4>Users already in liferay</h4>
    <table id="userTable" cellpadding="0" cellspacing="0" border="0" class="display">
        <thead>
        <tr>
            <th>Given name</th>
            <th>Last name</th>
            <th>Department</th>
            <th>User Role</th>
        </tr>
        </thead>
        <tbody>
        <core_rt:forEach var="user" items="${userList}">
            <tr>
                <td><sw360:out value="${user.firstName}"/></td>
                <td><sw360:out value="${user.lastName}"/></td>
                <td><sw360:out value="${user.getOrganizations(false).get(0).getName()}"/></td>
                <td>
                    <core_rt:forEach var="role" items="${user.roles}" varStatus="loop">
                        <sw360:out value="${role.getName()}"/>,
                    </core_rt:forEach>
                </td>
            </tr>

        </core_rt:forEach>
        </tbody>
        <tfoot>
        <tr>
            <th style="width:25%;"></th>
            <th style="width:25%;"></th>
            <th style="width:25%;"></th>
            <th style="width:25%;"></th>
        </tr>
        </tfoot>
    </table>

    <table class="info_table">
        <thead>
        <tr>
            <th colspan="2"> Downloads</th>
        </tr>
        </thead>

        <tbody>
        <tr>
            <td>Download Liferay User CSV</td>
            <td><a href="<portlet:resourceURL>
                               <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.USER_LIST%>'/>
                         </portlet:resourceURL>">
                <img src="<%=request.getContextPath()%>/images/download_enabled.jpg" alt="Download">
            </a>
            </td>
        </tr>
        </tbody>
    </table>
    <br>

    <h4>Users not in liferay</h4>
    <table id="userMissingTable" cellpadding="0" cellspacing="0" border="0" class="display">
        <thead>
        <tr>
            <th>Given name</th>
            <th>Last name</th>
        </tr>
        </thead>
        <tbody>
        <core_rt:forEach var="user" items="${missingUserList}">
            <tr>
                <td><sw360:out value="${user.givenname}"/></td>
                <td><sw360:out value="${user.lastname}"/></td>
            </tr>

        </core_rt:forEach>
        </tbody>
        <tfoot>
        <tr>
            <th style="width:50%;"></th>
            <th style="width:50%;"></th>
        </tr>
        </tfoot>
    </table>

    <form id="usersForm" name="usersForm" action="<%=updateLifeRayUsers%>" method="POST" enctype="multipart/form-data">
        <div class="fileupload-buttons">
            <span class="fileinput-button">
                <span>Upload user CSV</span>
                <input id="<portlet:namespace/>userFileUploadInput" type="file" name="<portlet:namespace/>file">
            </span>
            <input type="submit" value="Update Users" class="addButton" id="<portlet:namespace/>userCSV-Submit" disabled>
        </div>
    </form>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'utils/includes/quickfilter', /* jquery-plugins: */ 'datatables'], function($, quickfilter) {
            var usersTable,
                usersMissingTable;

            // initializing
            load();

            // register event handlers
            $('#<portlet:namespace/>userFileUploadInput').on('change', function (event) {
                if ($(event.currentTarget).val()) {
                    $("#<portlet:namespace/>userCSV-Submit").prop('disabled', false);
                }
            });

            // helper functions
            function load() {
                usersTable = configureUsersTable();
                usersMissingTable = configureMissingUsersTable();

                quickfilter.addTable(usersTable);
                quickfilter.addTable(usersMissingTable);
            }

            function configureUsersTable() {
                return setupPagination('#userTable');
            }

            function configureMissingUsersTable() {
                return setupPagination('#userMissingTable');
            }

            function setupPagination(tableSelector){
                var tbl;
                if ($(tableSelector)){
                    tbl = $(tableSelector).DataTable({
                        "pagingType": "simple_numbers",
                        "dom": "lrtip",
                        "autoWidth": false
                    });
                }
                return tbl;
            }
        });
    });
</script>
