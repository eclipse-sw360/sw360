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

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<div class="container">
    <div class="row">
        <div class="col">
             <div class="row portlet-toolbar">
                 <div class="col-auto">
                     <div class="btn-toolbar" role="toolbar">
                         <div id="topRow" class="btn-group" role="group">
                             <button type="button" id="createNewClientButton" class="btn btn-primary"><liferay-ui:message key="create.new.client" /></button>
                         </div>
                     </div>
                </div>
                <div id="countTitle" class="col portlet-title text-truncate" title="<liferay-ui:message key="oauth.client" /> ()">
                    <liferay-ui:message key="oauth.client" /> ()
                </div>
             </div>

            <div class="row">
                <div class="col">
                    <table id="clientTable" class="table table-bordered">
                        <colgroup>
                            <col style="width: 1.7rem"/>
                            <col style="width: 35%"/>
                            <col style="width: 25%"/>
                            <col style="width: 20%"/>
                            <col style="width: 20%"/>
                            <col style="width: 2.5rem"/>
                        </colgroup>
                    </table>
                </div>
            </div>
       </div>
    </div>
</div>

<div id="clientFormDiv" class="d-none" style="max-height:60vh">
    <form id="clientForm">
        <table class="table edit-table two-columns" id="oauth">
            <tr>
                <td colspan="2">
                    <div class="form-group">
                        <label class="mandatory" for="description"><liferay-ui:message key="description" /></label>
                        <input id="description" name="description" type="text" title="<liferay-ui:message key="enter.description.in.format" />"
                            class="form-control" required pattern=".*\S.*" placeholder="<liferay-ui:message key="enter.description" />"/>
                        <div class="invalid-feedback">
                            <liferay-ui:message key="please.enter.a.description" />
                        </div>
                    </div>
                </td>
            </tr>
            <tr>
               <td>
                    <div class="form-group">
                        <label class="mandatory" for="authorities"><liferay-ui:message key="authorities" /></label>
                        <input id="authorities" name="authorities" type="text" title="<liferay-ui:message key="please.enter.authorities.comma.separated" />"
                            placeholder="<liferay-ui:message key="enter.authorities" />" class="form-control" required
                            pattern=".*\S.*" />
                        <div class="invalid-feedback">
                            <liferay-ui:message key="please.enter.authorities.comma.separated" />
                        </div>
                    </div>
                </td>
                <td>
                    <div class="form-check">
                        <label class="mandatory"><liferay-ui:message key="scope" /></label>
                        <label id="scopeWarning" class="text-danger d-none"> (<liferay-ui:message key="atleast.one.should.be.selected" />)</label><br>
                        <input type="checkbox" name="scope_read" id="scope_read" class="form-check-input" />
                        <label class="form-check-label" for="scope_read">
                            <liferay-ui:message key="read.access" />
                        </label>
                        <br>
                        <input type="checkbox" name="scope_write" id="scope_write" class="form-check-input" />
                        <label class="form-check-label" for="scope_write">
                            <liferay-ui:message key="write.access" />
                        </label>
                    </div>
                </td>
            </tr>
            <tr>
                <td>
                    <div class="form-group">
                        <label class="mandatory d-block" for="access_token_validity">
                            <liferay-ui:message key="access.token.validity" />
                        </label>
                        <input id="access_token_validity" name="access_token_validity" type="text"
                            placeholder="<liferay-ui:message key="enter.access.token.validity" />"
                            class="form-control w-50 d-inline" required pattern="\d+" />
                        <select id="access_token_validity_metric" name="access_token_validity_metric" class="form-control w-25 d-inline">
                            <option value="seconds" selected><liferay-ui:message key="seconds" /></option>
                            <option value="days"><liferay-ui:message key="days" /></option>
                        </select>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <label class="mandatory d-block" for="refresh_token_validity">
                             <liferay-ui:message key="refresh.token.validity" />
                        </label>
                        <input id="refresh_token_validity" name="refresh_token_validity" type="text"
                            placeholder="<liferay-ui:message key="enter.refresh.token.validity" />"
                            class="form-control w-50 d-inline" required pattern="\d+" />
                        <select id="refresh_token_validity_metric" name="refresh_token_validity_metric" class="form-control w-25 d-inline">
                            <option value="seconds" selected><liferay-ui:message key="seconds" /></option>
                            <option value="days"><liferay-ui:message key="days" /></option>
                        </select>
                    </div>
                </td>
            </tr>
        </table>
        <input id="submitBtn" type="submit" class="d-none"/>
    </form>
</div>
<div id="pageSpinner">
    <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
</div>

<div class="dialogs auto-dialogs"></div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'utils/render', 'utils/includes/clipboard'], function($, datatables, dialog, render, clipboard) {
         function loadListOfClient() {
            $.ajax({
                url: '/authorization/client-management',
                type: "GET",
                success: function(result){
                    createClientTable(result);
                    let text = '<liferay-ui:message key="oauth.client" /> (' + result.length + ')';
                    $("#countTitle").attr("title" ,text).html(text);
                },
                error: function(result) {
                    if(result.status == 401) {
                        $("#pageSpinner").remove();
                        let timer = 30,
                            sessionReloadCount = sessionStorage.getItem("oauthPageReloadCount");

                        if(sessionReloadCount === null || sessionReloadCount === undefined) {
                            sessionReloadCount = 1;
                        } else {
                            sessionReloadCount = Number(sessionReloadCount);
                            sessionReloadCount += 1;
                        }
                        sessionStorage.setItem("oauthPageReloadCount", sessionReloadCount);
                        if(sessionReloadCount <= 3) {
                            window.open("/authorization/client-management");
                            setInterval(function () {
                                $("#topRow").html('<liferay-ui:message key="reload.page.after.login.to.authorization.server.auto.reloading.page.in" /> '
                                 + timer + ' <liferay-ui:message key="seconds" />');
                                if (--timer <= 0) {
                                    location.reload();
                                }
                            }, 1000);
                        } else {
                            $("#topRow").html('<liferay-ui:message key="reload.page.after.login.to.authorization.server" />');
                        }
                    }
                }
            });
         }
         loadListOfClient();

         $("#clientForm").submit(function(event) {
             event.preventDefault();
             let formDataArr = $("#clientForm").serializeArray();
             let scope = [], authorities= [], formData = {}, refresh_token_validity_metric_is_days = false,
             access_token_validity_metric_is_days = false;
             for(let data of formDataArr) {
                 if(data.name ===  "scope_read" && data.value === "on") {
                     scope.push("READ");
                 } else if(data.name ==  "scope_write" && data.value === "on") {
                     scope.push("WRITE");
                 } else if(data.name ==  "authorities") {
                     for(let auth of data.value.split(",")) {
                         let authTrimmed = auth.trim();
                         if(authTrimmed.length > 0) {
                             authorities.push(authTrimmed);
                         }
                     }
                 } else if(data.name ===  "access_token_validity_metric" && data.value === "days") {
                     access_token_validity_metric_is_days = true;
                 } else if(data.name ===  "refresh_token_validity_metric" && data.value === "days") {
                     refresh_token_validity_metric_is_days = true;
                 } else {
                     formData[data.name] = data.value;
                 }
             }
             if(scope.length == 0) {
                 $("#scopeWarning").removeClass("d-none");
                 return false;
             } else {
                 $("#scopeWarning").addClass("d-none");
             }
             formData["authorities"] = authorities;
             formData["scope"] = scope;
             formData["access_token_validity"] = Number(formData["access_token_validity"]);
             formData["refresh_token_validity"] = Number(formData["refresh_token_validity"]);
             if(access_token_validity_metric_is_days) {
                 formData["access_token_validity"] *= 24*60*60;
             }
             
             if(refresh_token_validity_metric_is_days) {
                 formData["refresh_token_validity"] *= 24*60*60;
             }
             createOrUpdateClientAjax(formData);
             return false;
        });
        let clientFormDiv = $("#clientFormDiv").clone(true, true);
        clientFormDiv.removeClass("d-none");
        $("#clientFormDiv").remove();

        $("#createNewClientButton").click(function(){
            dialog.confirm('info', 'check-square', '<liferay-ui:message key="create.new.client" />', clientFormDiv.clone(true, true), '<liferay-ui:message key="create" />', {
            }, function(submit, callback) {
                $("#submitBtn").trigger("click");
                callback(false);
                $("#confirmDialog div.modal-footer button.btn-info").remove();
                $("#confirmDialog div.modal-footer button.btn-light").html('<liferay-ui:message key="ok" />').removeClass("btn-light").addClass("btn-info");
            });

            $("#authorities").val("BASIC");
            $("#access_token_validity_metric").prop("selectedIndex", 0);
            $("#refresh_token_validity_metric").prop("selectedIndex", 0);
        });

        // register event handlers
        $('#clientTable').on('click', 'svg.delete', function (event) {
            var data = $(event.currentTarget).data();
            deleteClient(data.id, data.description, $(event.currentTarget).parents("tr"));
        });

        $('#clientTable').on('click', 'svg.edit', function (event) {
            var data = $(event.currentTarget).data();
            editClient(data.id, data.description, $(event.currentTarget).parents("tr"));
        });

        var clientsTbl = null;
        function createClientTable(result) {
            clientsTbl = datatables.create('#clientTable', {
                data: result,
                columns: [
                    {
                        "title": "<span class='client-more-info' title='<liferay-ui:message key="expand.all" />' data-show='false'>&#x25BA</span>",
                        "className": 'details-control',
                        "data": null,
                        "defaultContent": '&#x25BA',
                        "orderable": false
                    },
                    {"title": "<liferay-ui:message key="description" />", data: 'description' },
                    {"title": "<liferay-ui:message key="client.id" />", data: 'client_id' },
                    {"title": "<liferay-ui:message key="authorities" />", data: 'authorities'},
                    {"title": "<liferay-ui:message key="scope" />", data: 'scope'},
                    {"title": "<liferay-ui:message key="actions" />","orderable": false , data: 'client_id', render: renderActions }
                ],
                columnDefs: [
                    {
                        "targets": 0,
                        "createdCell": function (td, cellData, rowData, row, col) {
                            $(td).attr('title', 'click the icon to toggle obligation text');
                        }
                    }
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                createdRow: function (row, data) {
                    data.text = "Client Secret : " + data.client_secret + "<br>" + "Access Token Validity : " + displayValidity(data.access_token_validity)
                               + "<br>" + "Refresh Token Validity : " + displayValidity(data.refresh_token_validity);
                },
                order: [[1, 'asc']],
                initComplete: function() { 
                    datatables.showPageContainer;
                    $("#pageSpinner").remove();
                    /* Add event listener for opening and closing individual child row */
                    $('#clientTable tbody').on('click', 'td.details-control', function () {
                        render.toggleChildRow($(this), clientsTbl);
                    });

                    /* Add event listener for opening and closing all the child rows */
                    $('#clientTable thead').on('click', 'span.client-more-info', function() {
                        render.toggleAllChildRows($(this), clientsTbl);
                    });
                    }
            }, [0, 1, 2, 3, 4, 5], undefined, true);
        }

        function convertSecondsToDaysHoursminutesSeconds(seconds) {
            seconds = Number(seconds);
            let day = Math.floor(seconds / (3600*24));
            let hour = Math.floor(seconds % (3600*24) / 3600);
            let min = Math.floor(seconds % 3600 / 60);
            let sec = Math.floor(seconds % 3600 % 60);

            let dayStr = day > 0 ? day + (day == 1 ? " day, " : " days ") : "";
            let hourStr = hour > 0 ? hour + (hour == 1 ? " hour, " : " hours ") : "";
            let minStr = min > 0 ? min + (min == 1 ? " minute, " : " minutes ") : "";
            let secStr = sec > 0 ? sec + (sec == 1 ? " second" : " seconds") : "";
            return dayStr + hourStr + minStr + secStr; 
        }

        function displayValidity(sec) {
            return convertSecondsToDaysHoursminutesSeconds(sec) + " (" + sec + " seconds)"
        }
        function renderActions(value, type, row, meta) {
            if(type === 'display') {
                var $actions = $('<div>', {
                        'class': 'actions'
                    }),
                    $deleteAction = $('<svg>', {
                        'class': 'delete lexicon-icon',
                        'data-id': value,
                        'data-description': row.description
                    }),
                    $editAction = $('<svg>', {
                        'class': 'edit lexicon-icon',
                        'data-id': value,
                        'data-description': row.description
                    });
                $deleteAction.append($('<title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));
                $editAction.append($('<title><liferay-ui:message key="edit" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/>'));
                $actions.append($editAction);
                $actions.append($deleteAction);
                return $actions[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return '';
            }
        }

        function createOrUpdateClientAjax(formData) {
            jQuery.ajax({
                type: 'POST',
                url: '/authorization/client-management/',
                contentType: "application/json",
                accept: "application/json",
                data: JSON.stringify(formData),
                cache: false,
                success: function (data) {
                    let output = '<table class="table two-columns"><tr>' +
                    "<td><pre id='token' style='white-space: pre-wrap;word-break: break-all;'>" + JSON.stringify(data, null, 4) + "</pre></td>" +
                    '<td style="vertical-align: top;width: 9%;text-align: end;"><button id="copyToClipboard" type="button" class="btn btn-sm" data-toggle="tooltip" title="<liferay-ui:message key="copy.to.clipboard" />">' + 
                    '<clay:icon symbol="paste" />' +
                    '</button></td>' +
                    "</tr></table>"

                    $("#clientFormDiv").html(output);
                    $('#clientTable tbody').unbind();
                    $('#clientTable thead').unbind();
                    $('#copyToClipboard').on('click', function(event) {
                        let textSelector = "#confirmDialog table tr td:eq(1)",
                            textToCopy = $("#confirmDialog table tr td pre#token").text();
                        clipboard.copyToClipboard(textToCopy, textSelector, true);
                    });
                    clientsTbl.destroy();
                    loadListOfClient();
                },
                error: function (data) {
                    $("#clientFormDiv").html("<pre style='white-space: pre-wrap;word-break: break-all;'>" + JSON.stringify(data, null, 4) + "</pre>");
                }
            });
        }

        function deleteClient(id, description, row) {
            var $dialog;

            function deleteClientInternal(callback) {
                jQuery.ajax({
                    type: 'DELETE',
                    url: '/authorization/client-management/' + id,
                    contentType: "application/json",
                    cache: false,
                    success: function (data) {
                        callback();
                        if(data.client_id == id) {
                            clientsTbl.row(row).remove().draw(false);
                            $dialog.close();
                        } else {
                            $dialog.alert("<liferay-ui:message key="i.could.not.delete.the.client" />");
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert("<liferay-ui:message key="i.could.not.delete.the.client" />");
                    }
                });
            }

            $dialog = dialog.confirm(
                'danger',
                'question-circle',
                '<liferay-ui:message key="delete.client" />?',
                '<p><liferay-ui:message key="do.you.really.want.to.delete.the.client.x" />?</p>',
                '<liferay-ui:message key="delete.client" />',
                {
                    description: description,
                },
                function(submit, callback) {
                    deleteClientInternal(callback);
                }
            );
        }

        function editClient(id, description, row) {
            let editData = clientsTbl.row(row).data();
            dialog.confirm('info', 'check-square', '<liferay-ui:message key="edit.client" />', clientFormDiv.clone(true, true), '<liferay-ui:message key="edit" />', {
            }, function(submit, callback) {
                $("#submitBtn").trigger("click");
                callback(false);
                $("#confirmDialog div.modal-footer button.btn-info").remove();
                $("#confirmDialog div.modal-footer button.btn-light").html('<liferay-ui:message key="ok" />').removeClass("btn-light").addClass("btn-info");
            });
 
            $("#authorities").val(editData["authorities"].join(","));
            $("#description").val(editData["description"]);
            $("#access_token_validity").val(editData["access_token_validity"]);
            $("#refresh_token_validity").val(editData["refresh_token_validity"]);
            for(let scp of editData["scope"]) {
                if(scp === "READ") {
                    $("#scope_read").prop("checked", true);
                }
                
                if(scp === "WRITE") {
                    $("#scope_write").prop("checked", true);
                }
            }
            let clientIdField = $('<input type="hidden" name="client_id"/>').val(editData["client_id"]);
            $("#clientForm").append(clientIdField);
            $("#access_token_validity_metric").prop("selectedIndex", 0);
            $("#refresh_token_validity_metric").prop("selectedIndex", 0);
        }
    });
</script>
