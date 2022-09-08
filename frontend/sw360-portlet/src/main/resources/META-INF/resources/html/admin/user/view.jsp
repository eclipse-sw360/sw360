<%--
  ~ Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.UserGroup"%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil"%>
<%@ page import="javax.portlet.PortletRequest"%>

<%@ include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />
<portlet:resourceURL var="editsecgroupurl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EDIT_SECONDARY_GROUP_FOR_USER%>" />
</portlet:resourceURL>

<portlet:actionURL var="updateLifeRayUsers" name="updateUsers">
</portlet:actionURL>

<portlet:renderURL var="addUserURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>" />
</portlet:renderURL>

<portlet:resourceURL var="loadUsersPresentInCouchDBURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.USERS_PRESENT_IN_COUCH_DB%>' />
</portlet:resourceURL>

<portlet:resourceURL var="loadUsersAbsentInCouchDBURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.USERS_ABSENT_IN_COUCH_DB%>' />
</portlet:resourceURL>

<div class="container">
    <div class="row">
        <div class="col-3 sidebar">
            <div class="card-deck" id="adv_search_mod_req">
                <div id="searchInput" class="card">
                    <div class="card-header">
                        <liferay-ui:message key="advanced.search" />
                    </div>
                    <div class="card-body">
                        <form id="searchUserFilter">
                            <div class="form-group">
                                <label for="given_name"><liferay-ui:message key="given.name" /></label> <input type="text" class="form-control form-control-sm"
                                    name="<%=org.eclipse.sw360.datahandler.thrift.users.User._Fields.GIVENNAME%>" id="given_name">
                            </div>
                            <div class="form-group">
                                <label for="last_name"><liferay-ui:message key="last.name" /></label> <input type="text" class="form-control form-control-sm"
                                    name="<%=org.eclipse.sw360.datahandler.thrift.users.User._Fields.LASTNAME%>" id="last_name">
                            </div>
                            <div class="form-group">
                                <label for="user_email"><liferay-ui:message key="email" /></label> <input type="text" class="form-control form-control-sm"
                                    name="<%=org.eclipse.sw360.datahandler.thrift.users.User._Fields.EMAIL%>" id="user_email">
                            </div>
                            <div class="form-group">
                                <label for="user_department"><liferay-ui:message key="primary.department" /></label> <input list="secGrpsKeyListForFilter" class="form-control form-control-sm"
                                    id="user_department" name="<%=org.eclipse.sw360.datahandler.thrift.users.User._Fields.DEPARTMENT%>">
                                <datalist id="secGrpsKeyListForFilter">
                                    <core_rt:forEach items="${secGrpsKeys}" var="secGrpsKey">
                                        <option value="${secGrpsKey}" />
                                    </core_rt:forEach>
                                </datalist>
                            </div>
                            <div class="form-group">
                                <label for="primary_role"><liferay-ui:message key="primary.department.role" /></label> <select class="form-control form-control-sm" id="primary_role"
                                    name="<%=org.eclipse.sw360.datahandler.thrift.users.User._Fields.USER_GROUP%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>" class="textlabel stackedLabel"></option>
                                    <sw360:DisplayEnumOptions type="<%=UserGroup.class%>" useStringValues="true" />
                                </select>
                            </div>
                            <input id="formStrValue" type="hidden" value="[]"/>
                            <button type="submit" class="btn btn-primary btn-sm btn-block">
                                <liferay-ui:message key="search" />
                            </button>
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
                            <button type="button" class="btn btn-primary"
                                onclick="window.location.href='<portlet:resourceURL><portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.USER_LIST%>'/></portlet:resourceURL>'">
                                <liferay-ui:message key="download.liferay.users" />
                            </button>
                        </div>
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-primary" onclick="window.location.href='<%=addUserURL%>'">
                                <liferay-ui:message key="add.user" />
                            </button>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="couchdb.users" /> (${couchDbUserCount})">
                    <liferay-ui:message key="couchdb.users" />
                    (${couchDbUserCount})
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <h4 class="mt-1">
                        <liferay-ui:message key="users.already.in.couchdb" />
                    </h4>
                    <table id="userTable" class="table table-bordered">
                    </table>
                    <div id="userPresentSpinner">
                        <%@ include file="/html/utils/includes/pageSpinner.jspf"%>
                    </div>
                    <h4 class="mt-4">
                        <liferay-ui:message key="users.not.in.couchdb" />
                    </h4>
                    <table id="userMissingTable" class="table table-bordered">
                    </table>
                    <div id="userAbsentSpinner">
                        <%@ include file="/html/utils/includes/pageSpinner.jspf"%>
                    </div>
                    <h4 class="mt-4">
                        <liferay-ui:message key="upload.users" />
                    </h4>
                    <form id="usersForm" class="form needs-validation" name="usersForm" action="<%=updateLifeRayUsers%>" method="POST" enctype="multipart/form-data" novalidate>
                        <div class="form-row">
                            <div class="col">
                                <div class="form-group">
                                    <input type="file" class="form-control-file" id="<portlet:namespace/>userFileUploadInput" name="<portlet:namespace/>file" required>
                                    <div class="invalid-feedback">
                                        <liferay-ui:message key="please.select.a.file" />
                                    </div>
                                </div>
                            </div>
                            <div class="col-2">
                                <button type="submit" class="btn btn-secondary btn-block" id="<portlet:namespace/>userCSV-Submit">
                                    <liferay-ui:message key="upload.users" />
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<div id="secGrpsUserFormDiv" class="d-none">
    <div id="editMessage" class="fade show d-none alert-dismissible">
        <strong id="strongMsg"></strong> <span id="fullMsg"></span><br>
        <button type="button" class="close" data-dismiss="alert">&times;</button>
    </div>
    <form id="secGrpsUserForm" style="max-height: 60vh">
        <table class="table edit-table two-columns-with-actions" id="secGroupsAndRolesTable">
            <thead>
                <tr>
                    <th class="headlabel" style="width: 50%"><liferay-ui:message key="secondary.department" /></th>
                    <th class="headlabel" style="width: 50%"><liferay-ui:message key="secondary.department.role" /></th>
                    <th class="headlabel" style="width: 2.5rem"><liferay-ui:message key="action" /></th>
                </tr>
            </thead>
        </table>
        <button type="button" class="btn btn-primary" id="add-sec-grp" style="margin-left: 10px;">
            <liferay-ui:message key="add.row" />
        </button>
        <input id="submitBtn" type="submit" class="d-none" />
    </form>
</div>

<div class="dialogs auto-dialogs"></div>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf"%>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;
        require(['jquery', 'bridges/datatables', 'utils/includes/quickfilter', 'modules/dialog', 'utils/render', 'modules/bannerMessage'], function($, datatables, quickfilter, dialog, render, bannerMessage) {
            bannerMessage.portletLoad();
            var usersTable,
                usersMissingTable,
                rowIDCounter = 0,
                updateTD,
                primaryDepartment;

            $("#searchUserFilter").on("submit", function( event ) {
                event.preventDefault();
                $("#formStrValue").val(JSON.stringify($(this).serializeArray()));
                $('#userTable').DataTable().ajax.reload(null, true);
            });

            $('#add-sec-grp').on('click', function() {
                addRowToSecGrpsAndRolesTable();
            });
            $('#secGroupsAndRolesTable').on('click', 'svg[data-row-id]', function(event) {
                var rowId = $(event.currentTarget).data().rowId;
                $('#' + rowId).remove();
            });

            $("#secGrpsUserForm").submit(function(event) {
                event.preventDefault();
                $("#confirmDialog div.modal-footer button.btn-info").prop("disabled", true);
                let formData = {}, formDataText = {};
                $("#secGroupsAndRolesTable tr").each(function() {
                    let groupElement = $(this).find("td:first input");
                    if(!groupElement.length) {
                        return;
                    }
                    let group = groupElement.val();
                    let role = $(this).find("td:eq(1) select").val(),
                        roles = formData[group],
                        roleText = $(this).find("td:eq(1) select option:selected").text(),
                        rolesText = formDataText[group];
                    if(!roles) {
                        roles = new Array();
                        rolesText = new Array();
                    }
                    if(roles.indexOf(role) == -1) {
                        roles.push(role);
                        rolesText.push(roleText);
                    }

                    formData[group] = roles;
                    formDataText[group] = rolesText;
                });
                let emailOfUser = $("#emailOfUser").val();
                $.ajax({
                    type: 'POST',
                    url : '<%= editsecgroupurl %>',
                    data: JSON.stringify({"formData" : formData, "email" : emailOfUser}),
                    accept: 'application/json',
                    contentType: 'application/json',
                    cache: false,
                    success: function(data) {
                        updateAlertMsg('<liferay-ui:message key="SUCCESS" /> !', '<liferay-ui:message key="secondary.departments.and.roles.edited.successfully.for.user" /> : ' + emailOfUser, "alert alert-success")
                        updateSecGrpRolesTD(formDataText);
                        $("#confirmDialog div.modal-footer button.btn-info").remove();
                        $("#confirmDialog div.modal-footer button.btn-light").html('<liferay-ui:message key="ok" />').removeClass("btn-light").addClass("btn-info");
                    },
                    error: function(data) {
                        $("#confirmDialog div.modal-footer button.btn-info").removeAttr("disabled");
                        updateAlertMsg('<liferay-ui:message key="error" /> !', '<liferay-ui:message key="failed.to.edit.secondary.departments.and.roles.for.user" /> : ' + emailOfUser, "alert alert-warning")
                    }
                });
            });

            function makeUserEditUrl(email, page) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                    .setParameter('<%=PortalConstants.PAGENAME%>', page)
                    .setParameter('<%=PortalConstants.USER_EMAIL%>', "friendlyEmail");
                return portletURL.toString().replace("friendlyEmail", email);
            }

            var secGrpsUserFormDiv = $("#secGrpsUserFormDiv").clone(true, true);
            secGrpsUserFormDiv.removeClass("d-none");
            $("#secGrpsUserFormDiv").remove();
            function editSecondaryGroup(){
                dialog.confirm('info', 'users', '<liferay-ui:message key="edit.users.secondary.departments.and.role" />', secGrpsUserFormDiv.clone(true, true), '<liferay-ui:message key="save" />', {
                }, function(submit, callback) {
                    $("#submitBtn").trigger("click");
                    callback(false);
                });
                $('#secGrpsUserForm').after('<input id="emailOfUser" type="hidden"/>');
                $("#emailOfUser").val($(this).parents("tr").find("td.email").text());
                updateTD = $(this).parents("tr").find("td.secondaryGrpRoles");
                primaryDepartment = $(this).parents("tr").find("td:eq(3)").text();
                let group = $(this).parents("tr").find("td ul.mapDisplayRootItem li").each(function(){
                    let group = $(this).find("span.mapDisplayChildItemLeft").text();
                    let roles = $(this).find("span.mapDisplayChildItemRight").text().split(",");
                    for(let role of roles){
                        addRowToSecGrpsAndRolesTable(group, role.trim());
                    }
                });

                if($(this).parents("tr").find("td ul.mapDisplayRootItem li").length == 0) {
                    addRowToSecGrpsAndRolesTable();
                }
            };
            // initializing
            usersTable = createUsersPresentInCouchDBTable();

            $(document).ready(function(){
                $.ajax({
                    url: '<%=loadUsersAbsentInCouchDBURL%>',
                    type: "GET",
                    success: function(result){
                        usersMissingTable = createUsersAbsentInCouchDBTable(result);
                        $("#userAbsentSpinner").remove();
                  }});
            });

            // register event handlers
            $('#<portlet:namespace/>userFileUploadInput').on('change', function (event) {
                if ($(event.currentTarget).val()) {
                    $("#<portlet:namespace/>userCSV-Submit").prop('disabled', false);
                }
            });

            function updateAlertMsg(strongMsg, fullMsg, classes) {
                $("#editMessage").addClass(classes).removeClass("d-none");
                $("#strongMsg").text(strongMsg);
                $("#fullMsg").text(fullMsg);
            }

            function updateSecGrpRolesTD(formData) {
                let str = `<ul class="mapDisplayRootItem">`;
                for (var grp in formData) {
                    if(grp === primaryDepartment) {
                        continue;
                    }
                    let roles = formData[grp].join(", ");
                    str +=`<li><span class=\"mapDisplayChildItemLeft\">` +
                          grp +
                          `</span><span> -> </span><span class=\"mapDisplayChildItemRight\"> ` +
                          roles +
                          `</span></li>`;
                }
                str += `</ul>`;
                let tableid = updateTD.parents("table").attr("id");
                if(tableid === 'userTable') {
                    usersTable.cell(updateTD).data(str);
                } else {
                    usersMissingTable.cell(updateTD).data(str);
                }
            }

            function addRowToSecGrpsAndRolesTable(key, value, rowId) {
                if (!rowId) {
                    rowId = "secGrpsAndRolesTableRow" + rowIDCounter++;
                }
                if ((!key) && (!value)) {
                    key = "";
                    value = "";
                }

                var newRowAsString =
                    '<tr id="' + rowId + '" class="bodyRow">' +
                    '<td>' +
                    '<input list="secGrpsKeyList" class="form-control" id="secGrp' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="enter.secondary.department" />" title="<liferay-ui:message key="enter.secondary.department" />" value="' + key + '"/>' +
                    prepareKeyDatalist() + 
                    '</td>' +
                    '<td>' +
                    '<select class="form-control" id="secGrpRole' + rowId + '" required="" minlength="1" placeholder="<liferay-ui:message key="select.secondary.department.role" />" title="<liferay-ui:message key="select.secondary.department.role" />" value="' + value + '">' +
                    '<option value="" class="textlabel stackedLabel" ><liferay-ui:message key="select.secondary.department.role" /></option>' +
                    '<sw360:DisplayEnumOptions type="<%=UserGroup.class%>" useStringValues="true" options="${secondaryRolesOptions}"/>'+
                    '</select>' +
                    '</td>' +
                    '<td class="content-middle">' +
                    '<svg class="action lexicon-icon" data-row-id="' + rowId + '">' +
                    '<title>Delete</title>' +
                    '<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>' +
                    '</svg>' +
                    '</td>' +
                    '</tr>';
                $('#secGroupsAndRolesTable tr:last').after(newRowAsString);
                $("#secGrpRole" + rowId + " option").each(function() {
                    if($(this).text() === value) {
                      $(this).attr('selected', 'selected'); 
                      return false;
                    }                        
                });
            }

            function prepareKeyDatalist() {
                var datalist = '<datalist id="secGrpsKeyList">';
                <core_rt:forEach items="${secGrpsKeys}" var="secGrpsKey">
                    datalist += '<option value="' + "${secGrpsKey}" + '">';
               </core_rt:forEach>
                return datalist + '</datalist>';
            }

            // create and render datatable
            function createUsersPresentInCouchDBTable() {
                return datatables.create('#userTable', {
                    bServerSide: true,
                    sAjaxSource: '<%=loadUsersPresentInCouchDBURL%>',
                    fnServerParams: function (aoData) {
                        let searchFormDataArr = JSON.parse($("#formStrValue").val());
                        if (searchFormDataArr) {
                            for(data of searchFormDataArr) {
                                aoData.push(data);
                            }
                        }
                    },
                    columns: [
                        {title: "<liferay-ui:message key="given.name" />", data: "givenname", "defaultContent": ""},
                        {title: "<liferay-ui:message key="last.name" />", data: "lastname", "defaultContent": ""},
                        {title: "<liferay-ui:message key="email" />", data: "email", "defaultContent": "", render: {display: displayUserLink}, className: "email"},
                        {title: "<liferay-ui:message key="active.status" />", data: "deactivated", "defaultContent": ""},
                        {title: "<liferay-ui:message key="primary.department" />", data: "primaryDepartment", "defaultContent": ""},
                        {title: "<liferay-ui:message key="primary.department.role" />", data: "primaryDepartmentRole", "defaultContent": ""},
                        {title: "<liferay-ui:message key="secondary.departments.and.roles" />", data: "secondaryDepartmentsAndRoles", "defaultContent": "", className: "secondaryGrpRoles"},
                        {title: "<liferay-ui:message key="actions" />", data: "email", "defaultContent": "", render: {display: renderUserActions}}
                    ],
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                        order: [[2, 'asc']],
                        columnDefs: [
                            {
                                'targets': [7],
                                'orderable': false,
                            }
                        ],
                        fnDrawCallback: function() {
                            $(".editSecondaryGrp").click(editSecondaryGroup);
                            $("#userPresentSpinner").remove();
                        }
                    }, [0, 1, 2, 3, 4, 5, 6], undefined);
            }
        
            function createUsersAbsentInCouchDBTable(usersAbsentInCouchDBJsonData) {
                return datatables.create('#userMissingTable', {
                    data: usersAbsentInCouchDBJsonData.aaData,
                    columns: [
                        {title: "<liferay-ui:message key="given.name" />", data: "givenname", "defaultContent": ""},
                        {title: "<liferay-ui:message key="last.name" />", data: "lastname", "defaultContent": ""},
                        {title: "<liferay-ui:message key="email" />", data: "email", "defaultContent": "", render: {display: displayUserLink}, className: "email"},
                        {title: "<liferay-ui:message key="primary.department" />", data: "primaryDepartment", "defaultContent": ""},
                        {title: "<liferay-ui:message key="primary.department.role" />", data: "primaryDepartmentRole", "defaultContent": ""},
                        {title: "<liferay-ui:message key="actions" />", data: "email", "defaultContent": "", render: {display: renderUserActions}}
                    ],
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                        order: [[2, 'asc']],
                        columnDefs: [
                            {
                                'targets': [5],
                                'orderable': false,
                            }
                        ],
                    }, [0, 1, 2, 3, 4], undefined, true);
            }
            
            function displayUserLink(email) {
                return render.linkTo(makeUserEditUrl(email, "detail"), "", email);
            }

            function renderUserActions(email, type, row) {
                let $actions = $('<div>', {
                    'class': 'actions'
                }),
                editAction,
                secondaryGrpAction;
            
                editAction = render.linkTo(
                    makeUserEditUrl(email, "edit"),
                    "",
                    `<svg class="lexicon-icon">
                    <title><liferay-ui:message key="edit" /></title>
                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil" />
                    </svg>`
                );

                if (row.id) {
                    secondaryGrpAction = `<svg class="editSecondaryGrp lexicon-icon">
                        <title><liferay-ui:message key="edit.users.secondary.departments.and.role" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#documents-and-media" />
                        </svg>`;
                }
        
                $actions.append(editAction, secondaryGrpAction);
                return $actions[0].outerHTML;
            }
        });
    });
</script>
