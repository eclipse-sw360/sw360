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

<portlet:resourceURL var="viewProjectURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_LINKED_PROJECTS%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/>
</portlet:resourceURL>

<div class="dialogs">
	<div id="searchProjectsDialog" data-title="<liferay-ui:message key="link.projects" />" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">

                    <form>
                        <div class="row form-group">
                            <div class="col">
                                <input type="text" name="searchproject" id="searchproject" placeholder="<liferay-ui:message key="enter.search.text" />" class="form-control"/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="searchbuttonproject"><liferay-ui:message key="search" /></button>
                            </div>
                        </div>

                        <div id="Projectsearchresults">
                            <div class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only"><liferay-ui:message key="loading" /></span>
                                </div>
                            </div>

                            <table id="projectSearchResultstable" class="table table-bordered">
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th><liferay-ui:message key="project.name" /></th>
                                        <th><liferay-ui:message key="version" /></th>
                                        <th><liferay-ui:message key="state" /></th>
                                        <th><liferay-ui:message key="responsible" /></th>
                                        <th><liferay-ui:message key="description" /></th>
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
			        <button id="linkProjectsButton" type="button" class="btn btn-primary" title="<liferay-ui:message key="link.projects" />"><liferay-ui:message key="link.projects" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>

<div class="dialogs">
    <div id="commentsmodDialog" data-title="<liferay-ui:message key="create.moderation.request" />" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
            <div class="modal-content">
            <div class="modal-body container">

                    <form>
                        <div class="form-group">
                            <label for="moderationComment"><liferay-ui:message key="please.comment.your.changes" /></label>
                            <textarea name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="form-control" placeholder="<liferay-ui:message key="leave.a.comment.on.your.request" />" data-name="comment"></textarea>'
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="close" /></button>
                    <button id="linkProjectsButton" type="button" class="btn btn-primary" title="<liferay-ui:message key="send.moderation.request" />"><liferay-ui:message key="send.moderation.request" /></button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    require(['jquery', 'modules/dialog', 'bridges/datatables', 'utils/keyboard', /* jquery-plugins,  'jquery-ui',*/ 'utils/link', 'utils/includes/clipboard'], function($, dialog, datatables, keyboard, link, clipboard) {
       var $dataTable,
            $dialog;

        keyboard.bindkeyPressToClick('searchproject', 'searchbuttonproject');

        $('[data-dismiss=modal]').on('click', function (e) {
            var $t = $(this),
            target = $t[0].href || $t.data("target") || $t.parents('.modal') || [];

        $(target)
           .find("input,textarea,select")
               .val('')
               .end()
           .find("input[type=checkbox], input[type=radio]")
               .prop("checked", "")
               .end();
        })
        $('#addLinkedProjectButton').on('click', showProjectDialog);
        $('#linkToProjectButton').on('click', showProjectDialogForLinkToProj);
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

        $('#copyToClipboard').on('click', function(event) {
            let textSelector = "table tr td#documentId",
            textToCopy = $(textSelector).clone().children().remove().end().text().trim();
            clipboard.copyToClipboard(textToCopy, textSelector);
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

        function showProjectDialogForLinkToProj() {
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

                projectContentFromAjax('<%=PortalConstants.PROJECT_LINK_TO_PROJECT%>', projectIds, function(data) {
                    callback();
                    $.each(data, function(index, res) {
                        if(res.success) {
                            alertSuccessMessage(res);
                        } else if(typeof res.writeAccess !== 'undefined' && !res.writeAccess) {
                            alertModerationRequest(res);
                        } else {
                            alertFailureMessage(res);
                        }
                    });
                });
            }, function() {
                this.$.find('.spinner').hide();
                this.$.find('#projectSearchResultstable').hide();
                this.$.find('#searchproject').val('');
                this.enablePrimaryButtons(false);
            });

        }

        function alertModerationRequest(data){
            var $result = $('<div id= '+data.destnProjectName+'></div>');
            var $p2 = $('<p/>');
            $p2.append('Click ');
            $('<a/>', {
              style: 'text-decoration: underline; font-weight: bold;',
              id: 'openModDialog'
            }).on('click', function(e) {
                e.preventDefault();
                openModerationRequestDialog(data);
                $('#'+data.destnProjectName+'').parent("div").hide();
            }).text('here').appendTo($p2);
            $p2.append(' to raise moderation request to update project '+ data.destnProjectName);
            $p2.appendTo($result);
            $p2.addClass('mb-0');
            $dialog.alert($result, false);
        }

        function openModerationRequestDialog(data) {
                let $dialog1 = dialog.open('#commentsmodDialog', {
                }, function(submit, callback) {
                    var comment = $('#moderationRequestCommentField').val();
                    var projToComment =  data.destProjectId+":"+comment;
                    projectContentFromAjax('<%=PortalConstants.PROECT_MODERATION_REQUEST%>', projToComment, function(data) {
                        if(data.success) {
                            $dialog1.success(data.message, true);
                        } else {
                            $dialog1.alert(data.message, true);
                        }
                    });
                });
        }

        function alertSuccessMessage(data) {
            var $result = $('<div></div>');
            var $p1 = $('<p/>');
                $p1.append('The project ');
            $('<b/>').text(data.srcProjectName).appendTo($p1);
            $p1.append(' has been successfully linked to project ');
            $('<b/>').text(data.destnProjectName).appendTo($p1);
            $p1.appendTo($result);

            var $p2 = $('<p/>');
            $p2.append('Click ');
            $('<a/>', {
              href: link.to('project', 'edit', data.destProjectId) + '#/tab-linkedProjects',
              style: 'text-decoration: underline;'
            }).on('click', function(event) {
                $dialog.close();
                window.location.href = $(event.currentTarget).attr('href');
            }).text('here').appendTo($p2);
            $p2.append(' to edit the project relation.');
            $p2.appendTo($result);
            $p2.addClass('mb-0');
            $dialog.success($result);
        }

        function alertFailureMessage(data) {
            var $result = $('<div></div>').text(data.errorMsg);
            $dialog.alert($result);
        }

        function makeProjectsDataTable() {
            $dataTable = datatables.create('#projectSearchResultstable', {
                destroy: true,
                paging: false,
                info: false,
                language: {
                    emptyTable: "<liferay-ui:message key="no.projects.found" />",
                    processing: "<liferay-ui:message key="processing" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
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
                    $dialog.alert('<liferay-ui:message key="cannot.link.to.project" />');
                }
            });
        }

    });
</script>
