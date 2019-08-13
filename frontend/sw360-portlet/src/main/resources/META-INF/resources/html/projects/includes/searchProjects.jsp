<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request" />

<portlet:resourceURL var="viewProjectURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_LINKED_PROJECTS%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/>
</portlet:resourceURL>

<div class="dialogs">
	<div id="searchProjectsDialog" data-title="Link Projects" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">

                    <form>
                        <div class="row form-group">
                            <div class="col">
                                <input type="text" name="searchproject" id="searchproject" placeholder="Enter search text..." class="form-control"/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="searchbuttonproject">Search</button>
                            </div>
                        </div>

                        <div id="Projectsearchresults">
                            <div class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only">Loading...</span>
                                </div>
                            </div>

                            <table id="projectSearchResultstable" class="table table-bordered">
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th>Project name</th>
                                        <th>Version</th>
                                        <th>State</th>
                                        <th>Responsible</th>
                                        <th>Description</th>
                                    </tr>
                                </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                    </form>
				</div>
			    <div class="modal-footer">
		        <button type="button" class="btn btn-light" data-dismiss="modal">Close</button>
			        <button id="linkProjectsButton" type="button" class="btn btn-primary" title="Link projects">Link projects</button>
			    </div>
			</div>
		</div>
	</div>
</div>

<script>
    require(['jquery', 'modules/dialog', 'bridges/datatables', 'utils/keyboard', /* jquery-plugins */ 'jquery-ui' ], function($, dialog, datatables, keyboard) {
       var $dataTable,
            $dialog;

        keyboard.bindkeyPressToClick('searchproject', 'searchbuttonproject');

        $('#addLinkedProjectButton').on('click', showProjectDialog);
        $('#searchbuttonproject').on('click', function() {
            projectContentFromAjax('<%=PortalConstants.PROJECT_SEARCH%>', $('#searchproject').val(), function(data) {
                if($dataTable) {
                    $dataTable.destroy();
                }
                $('#projectSearchResultstable tbody').html(data);
                makeProjectsDataTable();
            });
        });
        $('#projectSearchResultstable').on('change', 'input', function() {
            $dialog.enablePrimaryButtons($('#projectSearchResultstable input:checked').length > 0);
        });

        function showProjectDialog() {
            if($dataTable) {
                $dataTable.destroy();
                $dataTable = undefined;
            }

            $dialog = dialog.open('#searchProjectsDialog', {
            }, function(submit, callback) {
                var projectIds = [];

                $('#projectSearchResultstable').find(':checked').each(function () {
                    projectIds.push(this.value);
                });

                projectContentFromAjax('<%=PortalConstants.LIST_NEW_LINKED_PROJECTS%>', projectIds, function(data) {
                    $('#LinkedProjectsInfo tbody').append(data);
                });

                callback(true);
            }, function() {
                this.$.find('.spinner').hide();
                this.$.find('#projectSearchResultstable').hide();
                this.$.find('#searchproject').val('');
                this.enablePrimaryButtons(false);
            });
        }

        function makeProjectsDataTable() {
            $dataTable = datatables.create('#projectSearchResultstable', {
                destroy: true,
                paging: false,
                info: false,
                language: {
                    emptyTable: "No projects found.",
                    processing: "Processing..."
                },
                select: 'multi+shift'
            }, undefined, [0]);
            datatables.enableCheckboxForSelection($dataTable, 0);
        }

        function projectContentFromAjax(what, where, callback) {
            $dialog.$.find('.spinner').show();
            $dialog.$.find('#projectSearchResultstable').hide();
            $dialog.$.find('#searchbuttonproject').prop('disabled', true);
            $dialog.enablePrimaryButtons(false);

            jQuery.ajax({
                type: 'POST',
                url: '<%=viewProjectURL%>',
                data: {
                    '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                    '<portlet:namespace/><%=PortalConstants.WHERE%>': where
                },
                success: function (data) {
                    callback(data);

                    $dialog.$.find('.spinner').hide();
                    $dialog.$.find('#projectSearchResultstable').show();
                    $dialog.$.find('#searchbuttonproject').prop('disabled', false);
                },
                error: function() {
                    $dialog.alert('Cannot link to project.');
                }
            });
        }

    });
</script>
