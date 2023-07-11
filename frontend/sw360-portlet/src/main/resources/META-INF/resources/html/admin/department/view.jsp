<%--
  ~ Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ include file="/html/init.jsp" %>
<%--&lt;%&ndash; the following is needed by liferay to display error messages&ndash;%&gt;--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf" %>
<jsp:useBean id='departmentOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='departmentInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='departmentNextSync' type="java.lang.String" scope="request"/>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="scheduleDepartmentURL" name="scheduleImportDepartment">
</portlet:actionURL>
<portlet:actionURL var="unscheduleDepartmentURL" name="unScheduleImportDepartment">
</portlet:actionURL>
<portlet:actionURL var="scheduleDepartmentManuallyURL" name="importDepartmentManually">
</portlet:actionURL>
<portlet:actionURL var="editPathFolder" name="writePathFolder">
</portlet:actionURL>
<portlet:resourceURL var="importDepartmentManually">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.IMPORT_DEPARTMENT_MANUALLY%>'/>
</portlet:resourceURL>

<style>
    .log-none {
        display: none;
    }

    #content-${lastFileName} {
        display: block;
    }
</style>

<div class="container" style="display: none;" id="loading-page">
    <div class="row">
        <div class="col">
            <div class="row">
                <div class="col-6 portlet-toolbar">
                    <table class="table bordered-table">
                        <tr>
                            <th style="line-height: 40px"><liferay-ui:message key="registration.folder.path"/></th>
                            <td>
                                <form id="editPathFolder" name="editPathFolder" class="needs-validation"
                                      action="<%=editPathFolder%>" method="post" novalidate>
                                    <input id="pathFolderDepartment" style="margin-top: 0" required type="text"
                                           class="form-control"
                                           name="<portlet:namespace/><%=PortalConstants.DEPARTMENT_URL%>"
                                           value="<sw360:out value="${pathConfigFolderDepartment}"/>"
                                           placeholder=" <liferay-ui:message key="enter.the.directory.path"/>"/>
                                </form>
                            </td>
                            <td width="3%">
                                <button type="button" class="btn btn-primary" id="updatePathFolder" data-action="save" disabled>
                                    <liferay-ui:message key="update"/></button>
                            </td>

                        </tr>
                        <tr>
                            <th><liferay-ui:message key="interval"/></th>
                            <td>${departmentInterval} (hh:mm:ss)</td>
                            <td></td>
                        </tr>
                        <tr>
                            <th><liferay-ui:message key="last.running.time.department"/></th>
                            <td>${lastRunningTime}</td>
                            <td></td>
                        </tr>
                        <tr>
                            <th><liferay-ui:message key="next.running.time.department"/></th>
                            <td>${departmentNextSync}</td>
                            <td></td>
                        </tr>
                    </table>
                    <form class="form mt-3">
                        <div class="form-group">
                            <button type="button" class="btn btn-primary" id="departmentIsScheduled"
                                    onclick="window.location.href='<%=scheduleDepartmentURL%>'"
                                    <core_rt:if test="${departmentIsScheduled}">disabled</core_rt:if> >
                                <liferay-ui:message key="schedule.department.service"/>
                            </button>
                            <button type="button" class="btn btn-light"
                                    onclick="window.location.href='<%=unscheduleDepartmentURL%>'"
                                    <core_rt:if test="${not departmentIsScheduled}">disabled</core_rt:if> >
                                <liferay-ui:message key="cancel.department.service"/>
                            </button>
                            <button type="button" class="btn btn-info" id="manually"
                                    data-action="import-department-manually">
                                <liferay-ui:message key="manually"/>
                            </button>
                            <button type="button" class="btn btn-secondary" id="view-log"><liferay-ui:message
                                    key="view.log"/>
                            </button>
                        </div>
                    </form>
                </div>
            </div>
            <br>
            <br>
            <div class="row">
                <div class="col">
                    <h4 class="mt-1"><liferay-ui:message key="department"/></h4>
                    <table id="userTable" class="table table-bordered">
                        <thead>
                        <tr>
                            <th width="30%"><liferay-ui:message key="department"/></th>
                            <th><liferay-ui:message key="member.emails"/></th>
                            <th width="5%"><liferay-ui:message key="actions"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <core_rt:forEach var="department" items="${departmentList}">
                            <tr>
                                <td><sw360:out value="${department.key}"/></td>
                                <td>
                                    <div style="width:100%; max-height:210px; overflow:auto">
                                        <core_rt:forEach var="secondDepartment" items="${department.value}"
                                                         varStatus="loop">
                                            <span>${loop.index + 1}.</span> <span><sw360:out
                                                value="${secondDepartment.email}"/></span>
                                            <br/>
                                        </core_rt:forEach>
                                        <br/>
                                    </div>
                                </td>
                                <td>
                                    <div class="actions" style="justify-content: center;">
                                        <svg class="editDepartment lexicon-icon"
                                             data-map="<sw360:out value="${department.key}"/>">
                                            <title><liferay-ui:message key="edit"/></title>
                                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/>
                                        </svg>
                                    </div>
                                </td>
                            </tr>
                        </core_rt:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="dialogs auto-dialogs">
    <div id="viewLogDepartmentDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-xl modal-info" role="document">
            <div class="modal-content" style="width:100%; max-height:800px; overflow:auto">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <liferay-ui:message key="view.log"/>
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div id="header-log-error">
                        <label for="file-log">Search</label>
                        <input list="file-logs" type="date" name="file-log" id="file-log" value="${lastFileName}"
                               class="col-sm-12"/>
                        <datalist id="file-logs">
                            <core_rt:forEach var="contentFileLog" items="${listContentFileLog}">
                                <option value="${contentFileLog.key}" }>${contentFileLog.key}</option>
                            </core_rt:forEach>
                        </datalist>
                    </div>
                    <br/>
                    <div style="text-align: center" class="title-log-file"><h4>Log File On: ${lastFileName}</h4></div>
                    <br/>
                    <div id="content-log-error" style="width:100%; max-height:500px; overflow:auto">
                        <core_rt:forEach var="contentLog" items="${listContentFileLog}">
                            <div id="content-${contentLog.key}" class="content-log log-none">
                                <core_rt:forEach var="content" items="${contentLog.value}">
                                    <p>${content}</p>
                                </core_rt:forEach>
                            </div>
                        </core_rt:forEach>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/html/utils/includes/pageSpinner.jspf" %>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        require(['jquery', 'bridges/datatables', 'utils/includes/quickfilter', 'modules/dialog'], function ($, datatables, quickfilter, dialog) {
            let PortletURL = Liferay.PortletURL;
            departmentKeyInURL = '<%=PortalConstants.DEPARTMENT_KEY%>',
                pageName = '<%=PortalConstants.PAGENAME%>';
            pageEdit = '<%=PortalConstants.PAGENAME_EDIT%>';
            baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';

            createExistingUserTable('#userTable');

            $('#userTable').on('click', 'svg.editDepartment', function (event) {
                let data = $(event.currentTarget).data();
                window.location.href = createDetailURLfromDepartmentKey(data.map);
            });

            function createDetailURLfromDepartmentKey(paramVal) {
                var portletURL = PortletURL.createURL(baseUrl).setParameter(pageName, pageEdit).setParameter(departmentKeyInURL, paramVal);
                return portletURL.toString();
            }

            function createExistingUserTable(tableSelector) {
                return datatables.create(tableSelector, {
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    columnDefs: [
                        {
                            'targets': [1, 2],
                            'orderable': false,
                        }
                    ],
                });
            }

            $('#view-log').on('click', () => $dialog = dialog.open('#viewLogDepartmentDialog'));

            let progress = null;
            $('.portlet-toolbar button[data-action="import-department-manually"]').on('click', function () {
                let $dialog;
                if (progress != null) {
                    progress.abort();
                }

                function importDepartmentManually(callback) {
                    progress = $.ajax({
                        type: 'POST',
                        url: '<%=importDepartmentManually%>',
                        cache: false,
                        dataType: 'json'
                    }).always(function () {
                        callback();
                    }).done(function (data) {
                        $('.alert.alert-dialog').hide();
                        if (data.result === 'SUCCESS') {
                            $dialog.success(`<liferay-ui:message key="i.imported.x.out.of.y.department" />`);
                            $('button:contains("Close")').on('click', () => location.reload());
                            $('.close').last().on('click', () => location.reload());
                        } else if (data.result === 'PROCESSING') {
                            $dialog.info('<liferay-ui:message key="importing.process.is.already.running.please.try.again.later" />');
                        } else {
                            $dialog.alert('<liferay-ui:message key="error.happened.during.importing.some.department.may.not.be.imported" />');
                        }
                    }).fail(function () {
                        $('.alert.alert-dialog').hide();
                        $dialog.alert('<liferay-ui:message key="something.went.wrong" />');
                    });
                }

                $dialog = dialog.confirm(
                    null,
                    'question-circle',
                    '<liferay-ui:message key="import.department" />?',
                    '<p id="departmentConfirmMessage"><liferay-ui:message key="do.you.really.want.to.import.department" />',
                    '<liferay-ui:message key="import.department" />',
                    {},
                    function (submit, callback) {
                        $('#departmentConfirmMessage').hide();
                        $dialog.info('<liferay-ui:message key="importing.process.is.running.it.may.takes.a.few.minutes" />', true);
                        $('.modal-header > button').prop('disabled', false);
                        importDepartmentManually(callback);
                    }
                );
            });

            $('.portlet-toolbar button[data-action="save"]').on('click', function (event) {
                $('#editPathFolder').submit();
            });

            let pathFolderDepartment = $('#pathFolderDepartment').val();
            if (pathFolderDepartment === "") {
                $('#departmentIsScheduled').prop('disabled', true);
                $('#manually').prop('disabled', true);
                $('#view-log').prop('disabled', true);
            }

            $('#loading-page').css('display', '');
            $('.container-spinner').css('display', 'none');

            $('#pathFolderDepartment').on('input change', function () {
                $('#editPathFolder').removeClass('needs-validation');
                $('#pathFolderDepartment')[0].setCustomValidity('');
                if ($(this).val() === '' || $.trim($(this).val()).length === 0) {
                    $('#editPathFolder').addClass('was-validated');
                    $('#pathFolderDepartment')[0].setCustomValidity('error');
                    $('#updatePathFolder').prop('disabled', true);
                    return false;
                }
                const valid = /(\/.*|[a-zA-Z]:\\(?:([^<>:"\/\\|?*]*[^<>:"\/\\|?*.]\\|..\\)*([^<>:"\/\\|?*]*[^<>:"\/\\|?*.]\\?|..\\))?)/;
                if (!$(this).val().match(valid)) {
                    $('#editPathFolder').addClass('was-validated');
                    $('#pathFolderDepartment')[0].setCustomValidity('error');
                    $('#updatePathFolder').prop('disabled', true);
                    return false;
                }
                $('#updatePathFolder').prop('disabled', false)
                if ($(this).val() === '${pathConfigFolderDepartment}'){
                    $('#updatePathFolder').prop('disabled', true)
                }
            });
            $('#file-log').on('change', function () {
                $('.content-log').hide();
                $('#content-' + this.value).show();
                let fileName = $('#file-log').val();
                $(".title-log-file").html("<h4>Log File On: " + fileName + "</h4>");
            });

        });
    });
</script>
