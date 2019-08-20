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

<portlet:resourceURL var="viewReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_LINKED_RELEASES%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/>
</portlet:resourceURL>

<div class="dialogs">
	<div id="searchReleasesDialog" data-title="Link Releases" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">

                    <form>
                        <div class="row form-group">
                            <div class="col-6">
                                <input type="text" name="searchrelease" id="searchrelease" placeholder="Enter search text..." class="form-control" autofocus/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="searchbuttonrelease">Search</button>

                                <core_rt:if test="${enableSearchForReleasesFromLinkedProjects}">
                                    <button type="button" class="btn btn-secondary" id="linkedReleasesButton">Releases of linked projects</button>
                                </core_rt:if>
                            </div>
                        </div>

                        <div id="search-release-form">
                            <div class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only">Loading...</span>
                                </div>
                            </div>

                            <table id="releaseSearchResultsTable" class="table table-bordered">
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th>Vendor</th>
                                        <th>Release name</th>
                                        <th>Version</th>
                                        <th>Clearing state</th>
                                        <th>Mainline state</th>
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
			        <button id="selectReleaseButton" type="button" class="btn btn-primary" title="Link releases">Link releases</button>
			    </div>
			</div>
		</div>
	</div>
</div>

<script>
    require(['jquery', 'modules/dialog', 'bridges/datatables', 'utils/keyboard', /* jquery-plugins */ 'jquery-ui' ], function($, dialog, datatables, keyboard) {
       var $dataTable,
            $dialog;

        keyboard.bindkeyPressToClick('searchrelease', 'searchbuttonrelease');

        $('#addLinkedReleasesToReleaseButton').on('click', showReleaseDialog);
        $('#searchbuttonrelease').on('click', function() {
            releaseContentFromAjax('<%=PortalConstants.RELEASE_SEARCH%>', $('#searchrelease').val(), function(data) {
                if($dataTable) {
                    $dataTable.destroy();
                }
                $('#releaseSearchResultsTable tbody').html(data);
                makeReleaseDataTable();
            });
        });
        $('#linkedReleasesButton').on('click', function() {
            releaseContentFromAjax('<%=PortalConstants.RELEASE_LIST_FROM_LINKED_PROJECTS%>', '', function(data) {
                if($dataTable) {
                    $dataTable.destroy();
                }
                $('#releaseSearchResultsTable tbody').html(data);
                makeReleaseDataTable();
            });
        });
        $('#releaseSearchResultsTable').on('change', 'input', function() {
            $dialog.enablePrimaryButtons($('#releaseSearchResultsTable input:checked').length > 0);
        });

        function showReleaseDialog() {
            if($dataTable) {
                $dataTable.destroy();
                $dataTable = undefined;
            }

            $dialog = dialog.open('#searchReleasesDialog', {
            }, function(submit, callback) {
                var releaseIds = [];

                $('#releaseSearchResultsTable').find(':checked').each(function () {
                    releaseIds.push(this.value);
                });

                releaseContentFromAjax('<%=PortalConstants.LIST_NEW_LINKED_RELEASES%>', releaseIds, function(data) {
                    $('#LinkedReleasesInfo tbody').append(data);
                });

                callback(true);
            }, function() {
                this.$.find('.spinner').hide();
                this.$.find('#releaseSearchResultsTable').hide();
                this.$.find('#searchrelease').val('');
                this.enablePrimaryButtons(false);
            });
        }

        function makeReleaseDataTable() {
            $dataTable = datatables.create('#releaseSearchResultsTable', {
                destroy: true,
                paging: false,
                info: false,
                language: {
                    emptyTable: "No releases found.",
                    processing: "Processing..."
                },
                order: [
                    [2, 'asc']
                ],
                select: 'multi+shift'
            }, undefined, [0]);
            datatables.enableCheckboxForSelection($dataTable, 0);
        }

        function releaseContentFromAjax(what, where, callback) {
            $dialog.$.find('.spinner').show();
            $dialog.$.find('#releaseSearchResultsTable').hide();
            $dialog.$.find('#searchbuttonrelease').prop('disabled', true);
            $dialog.$.find('#linkedReleasesButton').prop('disabled', true);
            $dialog.enablePrimaryButtons(false);

            jQuery.ajax({
                type: 'POST',
                url: '<%=viewReleaseURL%>',
                data: {
                    '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                    '<portlet:namespace/><%=PortalConstants.WHERE%>': where
                },
                success: function (data) {
                    callback(data);

                    $dialog.$.find('.spinner').hide();
                    $dialog.$.find('#releaseSearchResultsTable').show();
                    $dialog.$.find('#searchbuttonrelease').prop('disabled', false);
                    $dialog.$.find('#linkedReleasesButton').prop('disabled', false);
                },
                error: function() {
                    $dialog.alert('Cannot link to release.');
                }
            });
        }

    });
</script>
