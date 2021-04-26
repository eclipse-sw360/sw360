<%--
  ~ Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.UserGroup" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>
<portlet:resourceURL var="editsecgroupurl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EDIT_SECONDARY_GROUP_FOR_USER%>"/>
</portlet:resourceURL>
<jsp:useBean id="userList" type="java.util.List<com.liferay.portal.kernel.model.User>" scope="request"/>
<jsp:useBean id="missingUserList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.users.User>"
             scope="request"/>
<portlet:actionURL var="updateLifeRayUsers" name="updateUsers">
</portlet:actionURL>

<div class="container">
	<div class="row">
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
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="liferay.users" /> (${userList.size()})">
					<liferay-ui:message key="liferay.users" /> (${userList.size()})
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <h4 class="mt-1"><liferay-ui:message key="users.already.in.liferay" /></h4>
			        <table id="userTable" class="table table-bordered">
                        <thead>
                            <tr>
                                <th><liferay-ui:message key="given.name" /></th>
                                <th><liferay-ui:message key="last.name" /></th>
                                <th><liferay-ui:message key="email" /></th>
                                <th><liferay-ui:message key="primary.department" /></th>
                                <th><liferay-ui:message key="primary.department.role" /></th>
                                <th><liferay-ui:message key="secondary.departments.and.roles" /></th>
                                <th><liferay-ui:message key="actions" /></th>
                            </tr>
                        </thead>
                        <tbody>
                            <core_rt:forEach var="user" items="${userList}">
                                <tr>
                                    <td><sw360:out value="${user.givenname}"/></td>
                                    <td><sw360:out value="${user.lastname}"/></td>
                                    <td class="email"><sw360:out value="${user.email}"/></td>
                                    <td><sw360:out value="${user.department}"/></td>
                                    <td>
                                        <core_rt:forEach var="role" items="${user.primaryRoles}" varStatus="loop">
                                            <sw360:out value="${role}"/>
                                            <core_rt:if test="${!loop.last}">,</core_rt:if>
                                        </core_rt:forEach>
                                    </td>
                                    <td class="secondaryGrpRoles"><sw360:DisplayMapOfSecondaryGroupAndRoles value="${user.secondaryDepartmentsAndRoles}"/></td>
                                    <td>
                                        <div class="actions">
                                            <core_rt:if test="${empty user.id}">
                                                <svg class="infoUser lexicon-icon" disabled>
                                                    <title><liferay-ui:message key="user.record.not.found.in.couch.db" /></title>
                                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle" />
                                                </svg>
                                            </core_rt:if>
                                            <core_rt:if test="${not empty user.id}">
                                                <svg class="editUser lexicon-icon" data-email="${user.email}">
                                                    <title><liferay-ui:message key="delete" /></title>
                                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil" />
                                                </svg>
                                            </core_rt:if>
                                        </div>
                                    </td>
                                </tr>
                            </core_rt:forEach>
                        </tbody>
                    </table>

                    <h4 class="mt-4"><liferay-ui:message key="users.not.in.liferay" /></h4>
                    <table id="userMissingTable" class="table table-bordered">
                        <thead>
                            <tr>
                                <th><liferay-ui:message key="given.name" /></th>
                                <th><liferay-ui:message key="last.name" /></th>
                                <th><liferay-ui:message key="email" /></th>
                                <th><liferay-ui:message key="primary.department" /></th>
                                <th><liferay-ui:message key="primary.department.role" /></th>
                                <th><liferay-ui:message key="secondary.departments.and.roles" /></th>
                                <th><liferay-ui:message key="actions" /></th>
                            </tr>
                        </thead>
                        <tbody>
                            <core_rt:forEach var="user" items="${missingUserList}">
                                <tr>
                                    <td><sw360:out value="${user.givenname}"/></td>
                                    <td><sw360:out value="${user.lastname}"/></td>
                                    <td class="email"><sw360:out value="${user.email}"/></td>
                                    <td><sw360:out value="${user.department}"/></td>
                                    <td><sw360:DisplayEnum value="${user.userGroup}"/></td>
                                    <td class="secondaryGrpRoles"><sw360:DisplayMapOfSecondaryGroupAndRoles value="${user.secondaryDepartmentsAndRoles}"/></td>
                                    <td>
                                        <div class="actions">
                                            <core_rt:if test="${empty user.id}">
                                                <svg class="infoUser lexicon-icon">
                                                    <title><liferay-ui:message key="user.record.not.found.in.couch.db" /></title>
                                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle" />
                                                </svg>
                                            </core_rt:if>
                                            <core_rt:if test="${not empty user.id}">
                                                <svg class="editUser lexicon-icon" data-email="${user.email}">
                                                    <title><liferay-ui:message key="delete" /></title>
                                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil" />
                                                </svg>
                                            </core_rt:if>
                                        </div>
                                    </td>
                                </tr>
                            </core_rt:forEach>
                        </tbody>
                    </table>

                    <h4 class="mt-4"><liferay-ui:message key="upload.users" /></h4>
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
                                <button type="submit" class="btn btn-secondary btn-block" id="<portlet:namespace/>userCSV-Submit"><liferay-ui:message key="upload.users" /></button>
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
    <form id="secGrpsUserForm" style="max-height:60vh">
        <table class="table edit-table two-columns-with-actions" id="secGroupsAndRolesTable">
            <thead>
                <tr>
                    <th class="headlabel" style="width: 50%"><liferay-ui:message key="secondary.department" /></th>
                    <th class="headlabel" style="width: 50%"><liferay-ui:message key="secondary.department.role" /></th>
                    <th class="headlabel" style="width: 2.5rem"><liferay-ui:message key="action" /></th>
                </tr>
           </thead>
       </table>
       <button type="button" class="btn btn-primary" id="add-sec-grp" style="margin-left: 10px;"><liferay-ui:message key="add.row" /></button>
       <input id="submitBtn" type="submit" class="d-none"/>
    </form>
</div>

<div class="dialogs auto-dialogs"></div>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;
        require(['jquery', 'bridges/datatables', 'utils/includes/quickfilter', 'modules/dialog'], function($, datatables, quickfilter, dialog) {
            var usersTable,
                usersMissingTable,
                rowIDCounter = 0,
                updateTD,
                primaryDepartment;
            $('#add-sec-grp').on('click', function() {
                addRowToSecGrpsAndRolesTable();
            });
            $('#secGroupsAndRolesTable').on('click', 'svg[data-row-id]', function(event) {
                var rowId = $(event.currentTarget).data().rowId;
                $('#' + rowId).remove();
            });

            $("#secGrpsUserForm").submit(function(event) {
                event.preventDefault();
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
                    },
                    error: function(data) {
                        updateAlertMsg('<liferay-ui:message key="error" /> !', '<liferay-ui:message key="failed.to.edit.secondary.departments.and.roles.for.user" /> : ' + emailOfUser, "alert alert-warning")
                    }
                });
            });

            var secGrpsUserFormDiv = $("#secGrpsUserFormDiv").clone(true, true);
            secGrpsUserFormDiv.removeClass("d-none");
            $("#secGrpsUserFormDiv").remove();
            $(".editUser").click(function(){
                dialog.confirm('info', 'users', '<liferay-ui:message key="edit.users.secondary.departments.and.role" />', secGrpsUserFormDiv.clone(true, true), '<liferay-ui:message key="save" />', {
                }, function(submit, callback) {
                    $("#submitBtn").trigger("click");
                    callback(false);
                    $("#confirmDialog div.modal-footer button.btn-info").remove();
                    $("#confirmDialog div.modal-footer button.btn-light").html('<liferay-ui:message key="ok" />').removeClass("btn-light").addClass("btn-info");
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
                })
            });
            // initializing
            usersTable = createExistingUserTable('#userTable');
            usersMissingTable = createExistingUserTable('#userMissingTable');

            // register event handlers
            $('#<portlet:namespace/>userFileUploadInput').on('change', function (event) {
                if ($(event.currentTarget).val()) {
                    $("#<portlet:namespace/>userCSV-Submit").prop('disabled', false);
                }
            });

            function createExistingUserTable(tableSelector){
                return datatables.create(tableSelector, {
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    order: [[2, 'asc']],
                    columnDefs: [
                        {
                            "targets": 0,
                            "createdCell": function (td, cellData, rowData, row, col) {
                                $(td).attr('title', 'click the icon to toggle obligation text');
                            }
                        },
                        {
                            'targets': [6],
                            'orderable': false,
                        }
                    ],
                }, [0, 1, 2, 3, 4, 5], undefined, true);
            }
 
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
        });
    });
</script>
