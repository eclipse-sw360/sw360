<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
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
	<div id="searchReleasesDialog" data-title="<liferay-ui:message key="link.releases" />" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">

                    <form>
                        <div class="row form-group">
                            <div class="col-6">
                                <input type="text" name="searchrelease" id="searchrelease" placeholder="<liferay-ui:message key="enter.search.text" />" class="form-control" autofocus/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="searchbuttonrelease"><liferay-ui:message key="search" /></button>

                                <core_rt:if test="${enableSearchForReleasesFromLinkedProjects}">
                                    <button type="button" class="btn btn-secondary" id="linkedReleasesButton"><liferay-ui:message key="releases.of.linked.projects" /></button>
                                </core_rt:if>
                            </div>
                        </div>

                        <div id="search-release-form">
                            <div class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only"><liferay-ui:message key="loading" /></span>
                                </div>
                            </div>

                            <table id="releaseSearchResultsTable" class="table table-bordered">
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th><liferay-ui:message key="vendor" /></th>
                                        <th><liferay-ui:message key="component.name" /></th>
                                        <th><liferay-ui:message key="release.version" /></th>
                                        <th><liferay-ui:message key="clearing.state" /></th>
                                        <th><liferay-ui:message key="mainline.state" /></th>
                                    </tr>
                                </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                    </form>
				</div>
			    <div class="modal-footer">
		        <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="close" /></button>
			        <button id="selectReleaseButton" type="button" class="btn btn-primary" title="<liferay-ui:message key="link.releases" />"><liferay-ui:message key="link.releases" /></button>
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

        var homeUrl = themeDisplay.getURLHome().replace(/\/web\//, '/group/');

        $('#addLinkedReleasesToReleaseButton').on('click', showReleaseDialog);
        $('#searchbuttonrelease').on('click', function() {
            releaseContentFromAjax('<%=PortalConstants.RELEASE_SEARCH%>', $('#searchrelease').val(), function(data) {
                if($dataTable) {
                    $dataTable.destroy();
                }
                $('#releaseSearchResultsTable tbody').html(data);
                addLinkToReleaseNameAndVersion();
                makeReleaseDataTable();
            });
        });
        $('#linkedReleasesButton').on('click', function() {
            releaseContentFromAjax('<%=PortalConstants.RELEASE_LIST_FROM_LINKED_PROJECTS%>', '', function(data) {
                if($dataTable) {
                    $dataTable.destroy();
                }
                $('#releaseSearchResultsTable tbody').html(data);
                addLinkToReleaseNameAndVersion();
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
                    emptyTable: "<liferay-ui:message key="no.releases.found" />",
                    processing: "<liferay-ui:message key="processing" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
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
                    $dialog.alert('<liferay-ui:message key="cannot.link.to.release" />');
                }
            });
        }

        function makeReleaseViewUrl(releaseId) {
            return homeUrl + '/components/-/component/release/detailRelease/' + releaseId;
        }

        function makeComponentViewUrl(componentId) {
            return homeUrl + '/components/-/component/detail/' + componentId;
        }

        function detailUrl(name, url)
        {
            let viewUrl = $("<a></a>").attr("href",url).attr("target","_blank").css("word-break","break-word").text(name);
            return viewUrl[0].outerHTML;
        }

        function addLinkToReleaseNameAndVersion() {
            $('#releaseSearchResultsTable > tbody  > tr').each(function() {
                let $releasId = $('td:eq(0)', this).find("input[type='checkbox']").val();
                let $componentId = $('td:eq(0)', this).find("input[type='hidden']").val();
                let $relName = $('td:eq(2)', this);
                let $relVersion = $('td:eq(3)', this);
                let linkOnRelName = detailUrl($relName.text(), makeComponentViewUrl($componentId));
                let linkOnRelVersion = detailUrl($relVersion.text(), makeReleaseViewUrl($releasId));
                $relName.html(linkOnRelName);
                $relVersion.html(linkOnRelVersion);
         });
        }
    });
</script>
