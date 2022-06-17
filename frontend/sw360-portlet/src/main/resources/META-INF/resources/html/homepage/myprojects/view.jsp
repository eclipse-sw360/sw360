<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~ With contributions by Siemens Healthcare Diagnostics Inc, 2018.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:resourceURL var="loadProjectsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_PROJECT_LIST%>'/>
</portlet:resourceURL>

<section id="my-projects">
    <h4 class="actions">
        My Projects
        <div class="dropdown d-inline text-capitalize" id="dropdown">
            <span title="Config" class="dropdown-toggle float-none" data-toggle="dropdown" id="configId">
                <clay:icon symbol="select-from-list" />
            </span>
            <ul class="dropdown-menu" id="dropdownmenu" name="<portlet:namespace/>rolesandclearingstate"
                aria-labelledby="configId">
                <li class="dropdown-header">Role in Project</li>
                <li><hr class="my-2" /></li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="creator"
                    <core_rt:if test="${userRoles==null||userRoles.CREATED_BY}">checked="checked"</core_rt:if> />
                    <label class="form-check-label" for="creator">Creator</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="moderator"
                    <core_rt:if test="${userRoles==null||userRoles.MODERATORS}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="moderator">Moderator</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="contributor"
                    <core_rt:if test="${userRoles==null||userRoles.CONTRIBUTORS}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="contributor">Contributor</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="projectOwner"
                    <core_rt:if test="${userRoles==null||userRoles.PROJECT_OWNER}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="projectOwner">Project Owner</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="leadArchitect"
                    <core_rt:if test="${userRoles==null||userRoles.LEAD_ARCHITECT}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="leadArchitect"">Lead Architect</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="projectResponsible"
                    <core_rt:if test="${userRoles==null||userRoles.PROJECT_RESPONSIBLE}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="projectResponsible">Project Responsible</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="securityResponsible"
                    <core_rt:if test="${userRoles==null||userRoles.SECURITY_RESPONSIBLES}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="securityResponsible">Security Responsible</label>
                </li>
                <li><hr class="my-2" /></li>
                <li class="dropdown-header">Clearing State</li>
                <li><hr class="my-2" /></li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="open"
                    <core_rt:if test="${clearingState==null||clearingState.OPEN}">checked="checked"</core_rt:if> />
                    <label class="form-check-label" for="open">Open</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="closed"
                    <core_rt:if test="${clearingState==null||clearingState.CLOSED}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="closed">Closed</label>
                </li>
                <li>
                    <input type="checkbox" class="form-check-input ml-4" id="inProgress"
                    <core_rt:if test="${clearingState==null||clearingState.IN_PROGRESS}">checked="checked"</core_rt:if>>
                    <label class="form-check-label" for="inProgress">In Progress</label>
                </li>
                <li><hr class="my-2" /></li>
                <li>
                    <center>
                        <button class="btn btn-primary  btn-sm" type="button" name="<portlet:namespace/>userChoice"
                            data-toggle="dropdown" id="search">Search</button>
                    </center>
                </li>
            </ul>
        </div>
        <span title="Reload" style="float: right"><clay:icon symbol="reload" /></span>
    </h4>
    <div class="row">
        <div class="col">
            <table id="myProjectsTable" class="table table-bordered table-lowspace" data-load-url="<%=loadProjectsURL%>">
                <colgroup>
                    <col style="width: 40%;"/>
                    <col style="width: 30%;"/>
                    <col style="width: 30%;"/>
                </colgroup>
            </table>
        </div>
    </div>
</section>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    var data;
    var userChoice = false;
    function assignSelectedValues() {
        data = [];
        $("#dropdownmenu input:checkbox").each(function() {
            data.push($(this).prop('checked'));
        });
    }

    require(['jquery', 'bridges/datatables', 'utils/link' ], function($, datatables, link, event) {
        var table;

        $('#my-projects h4 svg').not($("#configId svg"))
            .attr('data-action', 'reload-my-projects')
            .addClass('spinning disabled');

        $('#my-projects').on('click', 'svg[data-action="reload-my-projects"]:not(.disabled)', reloadTable);

        $(document).off('pageshow.my-projects');
        $(document).on('pageshow.my-projects', function() {
            reloadTable();
        });
        assignSelectedValues();
        table = datatables.create('#myProjectsTable', {
            // the following parameter must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
            bServerSide: true,
            // the following parameter must not be converted to 'ajax', otherwise it won't work anymore (probably due to datatable plugins)
            sAjaxSource: $('#myProjectsTable').data().loadUrl,

            dom:
				"<'row'<'col-sm-12'tr>>" +
				"<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
            columns: [
                {"title": "<liferay-ui:message key="project.name" />", data: 'name', render: renderProjectNameLink },
                {"title": "<liferay-ui:message key="description" />", data: 'description', render: $.fn.dataTable.render.ellipsis },
                {"title": "<liferay-ui:message key="approved.releases" />", data: 'releaseClearingState', render: renderReleaseClearingState},
            ],
            language: {
                paginate: {
                    previous: "<liferay-ui:message key="previous" />",
                    next: "<liferay-ui:message key="next" />"
                },
                emptyTable: '<liferay-ui:message key="there.are.no.projects.found.with.your.selection" />',
                info: "<liferay-ui:message key="showing" />",
                infoEmpty: "<liferay-ui:message key="infoempty" />",
                processing: "<liferay-ui:message key="processing" />",
                loadingRecords: "<liferay-ui:message key="loading" />"
            },
            initComplete: function() {
                $('#my-projects h4 svg').removeClass('spinning disabled');
            },
            fnServerParams: function(aoData) {
                aoData.push({
                    "name" : $("#dropdownmenu").attr("name"),
                    "value" : data.join()
                });

                aoData.push({
                    "name" : $("#search").prop("name"),
                    "value" : userChoice
                });
            }
        });

        function renderProjectNameLink(name, type, row) {
            return $('<a/>', {
                'class': 'text-truncate',
                title: name,
                href: link.to('project', 'show', row.id)
            }).text(name)[0].outerHTML;
        }

        function renderReleaseClearingState(state) {
            return $('<span/>', {
                title: '<liferay-ui:message key="approved.releases.total.number.of.releases" />'
            }).text(state)[0].outerHTML;
        }

        function reloadTable() {
            $('#my-projects h4 svg').addClass('spinning disabled');
            table.ajax.reload(function() {
                $('#my-projects h4 svg').removeClass('spinning disabled');
            }, false );
        }

        $('#search').on('click', function(event) {
            assignSelectedValues();
            userChoice = true;
            $("#configId").dropdown('toggle')
            reloadTable();
            userChoice = false;
        });
    });

    $('#dropdownmenu').on('click', function(event) {
        event.stopPropagation();
    });

    $('#dropdown').on('hide.bs.dropdown', function() {
        $("#my-projects input:checkbox").each(function(index) {
            $(this).prop("checked", data[index]);
        });
    });
</script>
