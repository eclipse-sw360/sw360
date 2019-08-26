<%--
  ~ Copyright (c) Bosch Software Innovations GmbH 2015-2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ taglib prefix="min-width" uri="http://liferay.com/tld/aui" %>
<%@ taglib prefix="min-height" uri="http://liferay.com/tld/aui" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.portal.portlets.projectimport.ProjectImportConstants" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="importables" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.Project>"
             scope="request"/>
<jsp:useBean id="idName" type="java.lang.String" scope="request"/>
<jsp:useBean id="loggedIn" type="java.lang.Boolean" scope="request" />
<jsp:useBean id="loggedInServer" type="java.lang.String" scope="request" />

<core_rt:set var="hosts" value="<%= PortalConstants.PROJECTIMPORT_HOSTS %>"/>

<portlet:resourceURL var="ajaxURL" id="import.jsp"></portlet:resourceURL>

<div class="container">
    <div class="row">
        <div class="col-3 sidebar">
            <div class="card-deck card-deck-vertical">
                <div class="card">
                    <div class="card-header">
                        Server
                    </div>
                    <div class="card-body">
                        <form id="remoteLoginForm" class="form needs-validation" novalidate>
                            <div class="alert alert-danger mb-3" style="display: none;"></div>
                            <div class="alert alert-success mb-3" <core_rt:if test="${not loggedIn}">style="display: none;"</core_rt:if>>
                                You are logged in <clay:icon symbol="check"/>
                            </div>
                            <div class="form-group">
                                <label for="project_name">Server URL</label>
                                <core_rt:if test="${empty hosts}">
                                    <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=RemoteCredentials._Fields.SERVER_URL%>"
                                        value="<sw360:out value="${name}"/>" id="input-dataserver-url" autofocus required>
                                    <div class="invalid-feedback">
                                        Please enter the server url!
                                    </div>
                                </core_rt:if>
                                <core_rt:if test="${not empty hosts}">
                                    <select class="form-control form-control-sm" id="input-dataserver-url"
                                        name="<portlet:namespace/><%=RemoteCredentials._Fields.SERVER_URL%>" required>
                                        <core_rt:forEach items="${hosts}" var="host">
                                            <option value="${host}" <core_rt:if
                                                test='${loggedInServer == host}'>selected="selected"</core_rt:if>> ${host} </option>
                                        </core_rt:forEach>
                                    </select>
                                    <div class="invalid-feedback">
                                        Please select the server!
                                    </div>
                                </core_rt:if>
                            </div>
                            <div id="remoteLoginFormHidingPart">
                                <div class="form-group">
                                    <label for="input-dataserver-user">Server User</label>
                                    <input class="form-control form-control-sm" id="input-dataserver-user"
                                        name="<portlet:namespace/><%=RemoteCredentials._Fields.USERNAME%>" />
                                    <div class="invalid-feedback">
                                        Please enter the user name!
                                    </div>
                                </div>
                                <div class='form-group'>
                                    <label for="input-dataserver-pw">Password</label>
                                    <input class="form-control form-control-sm" type="password" id="input-dataserver-pw"
                                        name="<portlet:namespace/><%=RemoteCredentials._Fields.PASSWORD%>" />
                                    <div class="invalid-feedback">
                                        Please enter the password!
                                    </div>
                                </div>
                            </div>
                            <div>
                                <button id="buttonConnect" type="button" class="btn btn-primary btn-sm btn-block" <core_rt:if test="${loggedIn}">disabled</core_rt:if>>Connect</button>
                            </div>
                            <div>
                                <button id="buttonDisconnect" type="button" class="btn btn-secondary btn-sm btn-block mt-2" <core_rt:if test="${not loggedIn}">disabled</core_rt:if>>Disconnect</button>
                            </div>
                        </form>
                    </div>
                </div>
                <div class="card">
                    <div class="card-header">
                        Filter
                    </div>
                    <div class="card-body">
                        <form class="form" id="remoteFilterForm">
                            <div class="form-group">
                                <label for="project_name">Project Name (first letters)</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.NAME%>"
                                    value="<sw360:out value="${name}"/>" <core_rt:if test="${not loggedIn}">disabled</core_rt:if> id="input-project-name">
                            </div>
                            <button id="buttonRefresh" type="button" class="btn btn-primary btn-sm btn-block" <core_rt:if test="${not loggedIn}">disabled</core_rt:if>>Refresh</button>
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
                            <button type="button" class="btn btn-primary" data-action="import-projects" disabled>Import Projects</button>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="Project Import (BDP)">
                    Project Import (BDP)
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <div class="alert alert-info">
                        <p>For performance reasons, only the first 50 results will be shown. Please enter the prefix of the project name to narrow the search.</p>
                        <p class="mb-0">You may use the Shift-Key to select more rows.</p>
                    </div>
                    <table id="dataSourceTable" class="table table-bordered">
                        <colgroup>
                            <col style="width: 3.4rem;"/>
                            <col style="width: 30%;"/>
                            <col style="width: 70%;"/>
                        </colgroup>
                    </table>
                </div>
            </div>

        </div>
    </div>
</div>

<div class="dialogs auto-dialogs"></div>


<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var dataSourceTable,
            PortletURL = Liferay.PortletURL;

        require(['jquery', 'bridges/datatables', 'modules/button', 'modules/dialog', 'modules/validation'], function($, datatables, button, dialog, validation) {

            dataSourceTable = createDataSourceTable(getInitialData());
            $('#buttonConnect, #buttonRefresh').on('click', connectAndRefreshDataSource);
            $('#buttonDisconnect').on('click', disconnectDataSource);
            $('.portlet-toolbar button[data-action="import-projects"]').on('click', showImportProjectsPopup);
            $('#dataSourceTable').on('change', 'input[name="project"]', function() {
                $('.portlet-toolbar button[data-action="import-projects"]').prop('disabled',
                    $('#dataSourceTable').find('input[name="project"]:checked').length <= 0);
            });

            function getInitialData() {
                var result = [];

                <core_rt:forEach items="${importables}" var="importable">
                    var checkedProjectId = '<%=ProjectImportConstants.CHECKED_PROJECT%>${importable.externalIds.get(idName)}';
                    result.push({
                        "DT_RowId": "${importable.externalIds.get(idName)}",
                        "0": '',
                        "1": '<span id="' + checkedProjectId + '"><sw360:out value="${importable.externalIds.get(idName)}"/></span>',
                        "2": '<span id="' + checkedProjectId + 'Name"><sw360:out value="${importable.name}"/></span>'
                    });
                </core_rt:forEach>

                return result;
            }

            function createDataSourceTable(data) {
                var $table = datatables.create('#dataSourceTable', {
                    destroy: true,
                    data: data,
                    select: {
                        style: 'multi+shift'
                    },
                    columns: [
                        { title: '', className: 'text-center', render: $.fn.dataTable.render.inputCheckbox('project', ''), orderable: false },
                        { title: "Project id in selected data source"},
                        { title: "Project name"}
                    ],
                    order: [
                        [1, 'asc']
                    ],
                    language: {
                        emptyTable: "No projects available."
                    }
                });
                datatables.enableCheckboxForSelection($table, 0);
                return $table;
            }

            function connectAndRefreshDataSource(event) {
                var data = {},
                    serverUrl = $("#input-dataserver-url").val();

                if(!validation.validate('#remoteLoginForm')) {
                    return false;
                }

                data["<portlet:namespace/><%=ProjectImportConstants.USER_ACTION__IMPORT%>"] = "<%=ProjectImportConstants.USER_ACTION__NEW_IMPORT_SOURCE%>";
                data["<portlet:namespace/><%=ProjectImportConstants.SERVER_URL%>"] = serverUrl;
                data["<portlet:namespace/><%=ProjectImportConstants.USERNAME%>"] = $("#input-dataserver-user").val();
                data["<portlet:namespace/><%=ProjectImportConstants.PASSWORD%>"] = $("#input-dataserver-pw").val();
                data["<portlet:namespace/><%=ProjectImportConstants.PROJECT_NAME%>"] = $("#input-project-name").val();

                $('#remoteLoginForm').find('.alert:first').hide();
                button.wait($(event.currentTarget));
                $.ajax({
                    url: '<%=ajaxURL%>',
                    type: 'POST',
                    cache: false,
                    dataType: 'json',
                    data: data
                }).always(function() {
                    button.finish($(event.currentTarget));
                }).done(function (response) {
                    connectDBRequestSuccess(response, serverUrl);
                }).fail(function(){
                    $('#remoteLoginForm').find('.alert:nth-child(2)').hide();
                    $('#remoteLoginForm').find('.alert:first').text('Could not connect to server.');
                    $('#remoteLoginForm').find('.alert:first').show();
                });
            }

            function connectDBRequestSuccess(response, serverURL) {
                var responseCode = response.<%=ProjectImportConstants.RESPONSE__STATUS%>;

                switch(responseCode) {
                    case '<%=ProjectImportConstants.RESPONSE__DB_CHANGED%>':
                        $('#remoteLoginForm').find('input').prop('disabled', true);
                        $('#remoteFilterForm').find('input').prop('disabled', false);
                        $('#buttonConnect').prop('disabled', true);
                        $('#buttonRefresh').prop('disabled', false);
                        $('#buttonDisconnect').prop('disabled', false);

                        $('#remoteLoginForm').find('.alert:nth-child(2)').show();

                        $.ajax({
                            url: '<%=ajaxURL%>',
                            type: 'POST',
                            cache: false,
                            dataType: 'json',
                            data: {
                                "<portlet:namespace/><%=ProjectImportConstants.USER_ACTION__IMPORT%>":
                                    "<%=ProjectImportConstants.USER_ACTION__UPDATE_IMPORTABLES%>",
                                "<portlet:namespace/><%=ProjectImportConstants.PROJECT_NAME%>":
                                    $("#input-project-name").val()
                            }
                        }).done(function (response) {
                            importUpdateProjectTable(response);
                        }).fail(function(){
                            $('#remoteLoginForm').find('.alert:first').show();
                            $('#remoteLoginForm').find('.alert:first').text('Could not get the projects.');
                        });
                        break;
                    case '<%=ProjectImportConstants.RESPONSE__DB_CONNECT_ERROR%>':
                        $('#remoteLoginForm').find('.alert:nth-child(2)').hide();
                        $('#remoteLoginForm').find('.alert:first').show();
                        $('#remoteLoginForm').find('.alert:first').text('Could not connect to database.');
                        break;
                    case '<%=ProjectImportConstants.RESPONSE__DB_URL_NOT_SET%>':
                        $('#remoteLoginForm').find('.alert:nth-child(2)').hide();
                        $('#remoteLoginForm').find('.alert:first').show();
                        $('#remoteLoginForm').find('.alert:first').text('Please enter a server URL');
                        break;
                    case '<%=ProjectImportConstants.RESPONSE__UNAUTHORIZED%>':
                        $('#remoteLoginForm').find('.alert:nth-child(2)').hide();
                        $('#remoteLoginForm').find('.alert:first').show();
                        $('#remoteLoginForm').find('.alert:first').text('Unable to authenticate with this username/password.');
                        break;
                    default:
                    break;
                }
            }

             function importUpdateProjectTable(response) {
                var importables = response.<%=ProjectImportConstants.RESPONSE__NEW_IMPORTABLES%>;
                var projectList = [];

                importables.forEach(function(el) {
                    el = JSON.parse(el);
                    var checkedProjectId = '<%=ProjectImportConstants.CHECKED_PROJECT%>' + el.externalId;
                    projectList.push({
                        "DT_RowId": el.externalId,
                        "0": '',
                        "1": '<span id="' + checkedProjectId + '">' + el.externalId + '</span>',
                        "2": '<span id="' + checkedProjectId + 'Name">' + el.name + '</span>'
                    });
                });

                dataSourceTable = createDataSourceTable(projectList);
            }

            function showImportProjectsPopup() {
                var $dialog,
                    $projectList = $('<ol></ol>'),
                    selectedProjects = {};

                dataSourceTable.rows( { selected: true }).data().each(function(row) {
                    var id = row.DT_RowId,
                        transferId = $(row[1]).attr('id'),
                        name = $(row[2]).text();

                    selectedProjects[id] = {
                        transferId: transferId,
                        text: name
                    };

                    $projectList.append($('<li></li>').text(name).attr('data-id', id));
                });

                $dialog = dialog.confirm(
                    null,
                    'question-circle',
                    'Import projects?',
                    '<p>The following projects will be imported:</p>' + $projectList[0].outerHTML,
                    'Import projects',
                    {},
                    function(submit, callback) {
                        $.ajax({
                            url: '<%=ajaxURL%>',
                            type: 'POST',
                            cache: false,
                            dataType: 'json',
                            data: {
                                "<portlet:namespace/>checked":
                                    Object.keys(selectedProjects).map( function(key) { return selectedProjects[key].transferId; }),
                                "<portlet:namespace/><%=ProjectImportConstants.USER_ACTION__IMPORT%>":
                                    '<%=ProjectImportConstants.USER_ACTION__IMPORT_DATA%>'
                            }
                        }).done(function (response) {
                            var $check = $('<span><span class="symbol text-success">&nbsp;<clay:icon symbol="check-circle"/></span></span>'),
                                $failed = $('<span><span class="symbol text-danger">&nbsp;<clay:icon symbol="times-circle"/></span></span>');

                            callback();
                            switch(response.<%=ProjectImportConstants.RESPONSE__STATUS%>) {
                                case '<%=ProjectImportConstants.RESPONSE__SUCCESS%>':

                                    $dialog.success('Projects imported successfully.', true);
                                    dataSourceTable.rows( { selected: true }).deselect();

                                    Object.keys(selectedProjects).forEach(function(key) {
                                        $dialog.$.find('li[data-id="' + key + '"] .symbol').remove();
                                        $dialog.$.find('li[data-id="' + key + '"]').append($check.html());
                                    });

                                    selectedProjects = {};
                                    break;
                                case '<%=ProjectImportConstants.RESPONSE__FAILURE%>':
                                    var $failedList = $("<ol></ol>"),
                                        failedKeys = {},
                                        failedIdsList = response.<%=ProjectImportConstants.RESPONSE__FAILED_IDS%>;

                                    $.each(failedIdsList, function (key, value) {
                                        var $key = $('<b></b>').text(selectedProjects[key].text);

                                        failedKeys[key] = true;
                                        $failedList.append($('<li></li>').text(': ' + value).prepend($key));

                                        $dialog.$.find('li[data-id="' + key + '"] .symbol').remove();
                                        $dialog.$.find('li[data-id="' + key + '"]').append($failed.html());
                                    });

                                    Object.keys(selectedProjects).forEach(function(key) {
                                        if(!failedKeys[key]) {
                                            $dialog.$.find('li[data-id="' + key + '"] .symbol').remove();
                                            $dialog.$.find('li[data-id="' + key + '"]').append($check.html());
                                            dataSourceTable.row('#' + key).deselect();
                                            delete selectedProjects[key]
                                        }
                                    });

                                    $dialog.warning('Some projects failed to import: ' + $failedList[0].outerHTML);
                                    break;
                                case '<%=ProjectImportConstants.RESPONSE__GENERAL_FAILURE%>':
                                    $dialog.alert('Could not import the projects.');
                                    break;
                                default:
                                    $dialog.alert('Unknown result from server.');
                            }
                        }).fail(function(){
                            callback();
                            $dialog.alert('Could not import the projects.');
                        });
                    }
                );
            }

            function disconnectDataSource() {
                var data = new Object();
                data["<portlet:namespace/><%=ProjectImportConstants.USER_ACTION__IMPORT%>"] = "<%=ProjectImportConstants.USER_ACTION__DISCONNECT%>";
                $.ajax({
                    url: '<%=ajaxURL%>',
                    type: 'POST',
                    data: data,
                    success: function(response) {
                        dataSourceTable = createDataSourceTable([]);

                        $('#remoteLoginForm').find('input').prop('disabled', false);
                        $('#remoteFilterForm').find('input').prop('disabled', true);
                        $('#buttonConnect').prop('disabled', false);
                        $('#buttonRefresh').prop('disabled', true);
                        $('#buttonDisconnect').prop('disabled', true);

                        $('#remoteLoginForm').find('.alert:nth-child(2)').hide();
                    }
                });
            }
        });
    });
</script>
