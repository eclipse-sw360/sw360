<%--
  ~ Copyright TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>
<%@ taglib prefix="liferay-ui" uri="http://liferay.com/tld/ui" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.MainlineState" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ReleaseLink" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>
<%@ page import="org.eclipse.sw360.datahandler.common.SW360Utils" %>

<portlet:resourceURL var="viewReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_LINKED_RELEASES%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${project.id}"/>
</portlet:resourceURL>

<h4 class="mt-4"><liferay-ui:message key="linked.releases" /></h4>
<div class="alert alert-danger show-alert" role="alert" style="display:none; position:sticky; top:0; z-index:999">
    <span class="alert-indicator">
        <svg class="lexicon-icon lexicon-icon-exclamation-full" focusable="false" role="presentation">
            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#exclamation-full"></use>
        </svg>
    </span>
    <strong class="lead">Warning: </strong><span class='alert-message'></span>
    <button aria-label="Close" class="close" type="button" style="float:right;margin-top: -5px;">
        <svg class="lexicon-icon lexicon-icon-times" focusable="false" role="presentation">
            <use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#times'/>
        </svg>
    </button>
</div>
<table class="table edit-table five-columns-with-actions" id="LinkedReleasesNetwork">
    <thead>
        <tr>
            <th><liferay-ui:message key="release.name" /></th>
            <th><liferay-ui:message key="release.version" /></th>
            <th><liferay-ui:message key="reload.info" />
                <span class="sw360-tt sw360-tt-ReloadInfo" data-content="<liferay-ui:message key='load.default.child.releases' />">
                    <svg class='lexicon-icon'> <use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open' /></svg>
                </span>
            </th>
            <th><liferay-ui:message key="release.relation" /> <sw360:DisplayEnumInfo type="<%=ReleaseRelationship.class%>"/></th>
            <th><liferay-ui:message key="project.mainline.state" /> <sw360:DisplayEnumInfo type="<%=MainlineState.class%>"/></th>
            <th><liferay-ui:message key="comments" /></th>
            <th></th>
        </tr>
    </thead>
    <tbody id="tableBody">
    </tbody>
</table>
<br/>
<div class="spinner text-center" id="spinner" style="display: none;">
    <div class="spinner-border" role="status">
        <span class="sr-only"><liferay-ui:message key="loading" /></span>
    </div>
</div>
<input type="hidden" id="releaseRelationTree" name="<portlet:namespace/><%=Project._Fields.RELEASE_RELATION_NETWORK%>" />
<button type="button" class="btn btn-secondary" id="addReleaseToNetWork"><liferay-ui:message key="add.releases" /></button>

<div class="dialogs">
    <div id="deleteReleaseInNetworkDialog" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
		    <div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title"><clay:icon symbol="question-circle" />
					<liferay-ui:message key="delete.link.to.release" />
				</h5>
				<button type="button" class="close" data-dismiss="modal" aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
			</div>
				<div class="modal-body">
			        <p><liferay-ui:message key="do.you.really.want.to.remove.the.link.to.release.x" /></p>
				</div>
			    <div class="modal-footer">
			        <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="cancel" /></button>
			        <button type="button" class="btn btn-danger"><liferay-ui:message key="delete.link" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>
<div class="dialogs">
	<div id="searchRootRelease" data-title="<liferay-ui:message key="link.releases" />" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">
                <form>
                    <div class="row form-group">
                        <div class="col-6">
                            <input type="text" name="searchRelease" id="searchRelease" placeholder="<liferay-ui:message key="enter.search.text" />" class="form-control" autofocus/>
                        </div>
                        <div class="col">
                            <button type="button" class="btn btn-secondary" id="searchReleaseBtn"><liferay-ui:message key="search" /></button>
                            <core_rt:if test="${enableSearchForReleasesFromLinkedProjects}">
                                <button type="button" class="btn btn-secondary" id="getLinkedReleasesButton"><liferay-ui:message key="releases.of.linked.projects" /></button>
                            </core_rt:if>
                        </div>
                    </div>
                    <div class="form-check pt-2">
                        <input class="form-check-input" type="checkbox" value="On" id="exactMatchRelease">
                        <label class="form-check-label" for="exactMatchRelease"><liferay-ui:message key="exact.match" /></label>
                        <sup title="<liferay-ui:message key="the.search.result.will.display.elements.exactly.matching.the.input.equivalent.to.using.x.around.the.search.keyword" />" >
                                    <liferay-ui:icon icon="info-sign"/>
                        </sup>
                    </div>
                    <div id="search-release-form">
                        <div class="spinner text-center" style="display: none;">
                            <div class="spinner-border" role="status">
                                <span class="sr-only"><liferay-ui:message key="loading" /></span>
                            </div>
                        </div>
                        <table id="releaseResultsTable" class="table table-bordered">
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
                            <tbody></tbody>
                        </table>
                    </div>
                </form>
				</div>
			    <div class="modal-footer">
		            <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="close" /></button>
			        <button id="selectRootReleaseBtn" type="button" class="btn btn-primary" title="<liferay-ui:message key="link.releases" />"><liferay-ui:message key="link.releases" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>
<div class="dialogs">
	<div id="searchChildReleaseDialog" data-title="<liferay-ui:message key="link.releases" />" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">
                <form>
                    <div class="row form-group">
                        <div class="col-6">
                            <input type="text" name="searchRelease" id="searchRelease" placeholder="<liferay-ui:message key="enter.search.text" />" class="form-control" autofocus/>
                        </div>
                        <div class="col">
                            <button type="button" class="btn btn-secondary" id="searchReleaseBtn"><liferay-ui:message key="search" /></button>
                            <core_rt:if test="${enableSearchForReleasesFromLinkedProjects}">
                                <button type="button" class="btn btn-secondary" id="getLinkedReleasesButton"><liferay-ui:message key="releases.of.linked.projects" /></button>
                            </core_rt:if>
                        </div>
                    </div>
                    <div class="form-check pt-2">
                        <input class="form-check-input" type="checkbox" value="On" id="exactMatchRelease">
                        <label class="form-check-label" for="exactMatchRelease"><liferay-ui:message key="exact.match" /></label>
                        <sup title="<liferay-ui:message key="the.search.result.will.display.elements.exactly.matching.the.input.equivalent.to.using.x.around.the.search.keyword" />" >
                                    <liferay-ui:icon icon="info-sign"/>
                        </sup>
                    </div>
                    <div id="child-release-search-form">
                        <div class="spinner text-center" style="display: none;">
                            <div class="spinner-border" role="status">
                                <span class="sr-only"><liferay-ui:message key="loading" /></span>
                            </div>
                        </div>
                        <table id="searchChildResultsTable" class="table table-bordered">
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
                            <tbody></tbody>
                        </table>
                    </div>
                </form>
				</div>
			    <div class="modal-footer">
		            <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="close" /></button>
			        <button id="selectChildReleaseBtn" type="button" class="btn btn-primary" title="<liferay-ui:message key="link.releases" />"><liferay-ui:message key="link.releases" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;
        require(['jquery', 'modules/dialog', 'bridges/datatables', 'utils/keyboard', 'modules/alert', /* jquery-plugins */ 'jquery-ui' ], function($, dialog, datatables, keyboard, alert) {
            var $dataTable, $releaseDialog;
            let releaseId = "";
            var releaseWithRelations = ${project.releaseRelationNetwork};
            let projectId = "${project.id}";
            let releaseWithRelationsString = "[]";

            $( document ).ready(async function(event){
                if (releaseWithRelations == undefined || releaseWithRelations.length == 0) {
                    releaseWithRelations = [];
                } else {
                    if (typeof releaseWithRelations == "string") {
                        releaseWithRelations = JSON.parse(releaseWithRelations);
                    }
                }
                releaseWithRelationsString = JSON.stringify(releaseWithRelations);
                $('#releaseRelationTree').val(releaseWithRelationsString);
                displayReleaseRelationShip();
            });

            async function displayReleaseRelationShip() {
                $('#spinner').show();
                let newRow = await getHtmlRowsFromDependencyNetwork('<%=PortalConstants.GET_HTML_RELEASE_ROWS%>');
                newRow = newRow.replace(/\s+/g,' ').trim();
                document.getElementById("tableBody").innerHTML = newRow;
                $('#spinner').hide();
            }

            async function displayRelationOfNode(){
               let newSelect = $(this).closest('tr').find('select').eq(0).val().trim();
               let oldSelect = $(this).closest('tr').find('select').eq(0).attr('data-old');
               let currentLayer =  $(this).closest('tr').attr('data-layer');
               let parentNode = $(this).closest('tr').attr('parent-node');
               let currentIndex = $(this).closest('tr').attr('data-index');
               let stringNode = await getLinkedReleaseOfNode('<%=PortalConstants.FIND_LINKED_RELEASE_OF_NODE%>',newSelect);
               let newNode = JSON.parse(stringNode);
               let i=0;
               let trace = [];
               trace.unshift(currentIndex);
               for (let layer = currentLayer - 1; layer >= 0; layer-- ) {
                    trace.unshift($(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('data-index'));
                    parentNode =  $(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('parent-node');
               }
               replaceNode(releaseWithRelations, i, trace, newNode[0], newSelect, oldSelect);
               $('#tableBody').empty();
               $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
               displayReleaseRelationShip();
            }

            function replaceNode(releasesLinked, i, trace, newNode, newSelect, oldSelect){
                let index = trace[i];
                if (i === (trace.length - 1)) {
                     releasesLinked[index].releaseLink = newNode.releaseLink;
                     return;
                }
                else{
                    i++;
                    replaceNode(releasesLinked[index].releaseLink, i, trace, newNode, newSelect, oldSelect);
                }
            }

            async function changeReleaseIdInNode() {
               let idValue = $(this).closest('tr').find('select').eq(0).val();
               let oldSelect = $(this).closest('tr').find('select').eq(0).attr('data-old');
               let currentLayer =  $(this).closest('tr').attr('data-layer');
               let parentNode = $(this).closest('tr').attr('parent-node');
               let currentIndex = $(this).closest('tr').attr('data-index');
               let version = $(this).closest('tr').find('select').eq(0).find('option:selected').text();
               let componentName = $(this).closest('tr').find('td').eq(0).find('div').eq(0).find('div').eq(0).text().trim();
               let i=0;
               let trace = [];
               trace.unshift(currentIndex);
               let releaseIdTrace = [];
               releaseIdTrace.push(parentNode);
               $("#formSubmit").prop('disabled', true);
               let messages = [];
               for (let layer = currentLayer - 1; layer >= 0; layer-- ) {
                    trace.unshift($(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('data-index'));
                    parentNode =  $(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('parent-node');
                    releaseIdTrace.push(parentNode);
               }
               // Remove empty releaseId
               releaseIdTrace.pop();
               if (checkDuplicateReleaseWithSameLevel(trace, idValue)) {
                   $(this).closest('tr').find('select').eq(0).val(oldSelect);
                   messages.push('<p>Release <b>' + componentName + '(' + version + ')</b>' + ' duplicated!!!</p>');
               }
               let cyclicLinkedReleasePaths = await getCyclicLinkedReleasePath('<%=PortalConstants.CYCLIC_LINKED_RELEASE_PATH%>', releaseIdTrace, [idValue]);
               for (let cyclicLinkedReleasePath of cyclicLinkedReleasePaths) {
                   if (cyclicLinkedReleasePath !== "") {
                       $(this).closest('tr').find('select').eq(0).val(oldSelect);
                       messages.push('<p>Cyclic Hierarchy: <b>' + cyclicLinkedReleasePath + '</b></p>');
                   }
               }
               let replacingNode = findChildNodeByIndexTrace(releaseWithRelations, trace, i);
               let subNodeIds = getSubNodeReleaseIds(replacingNode);
               cyclicLinkedReleasePaths = await getCyclicLinkedReleasePath('<%=PortalConstants.CYCLIC_LINKED_RELEASE_PATH%>', [idValue], subNodeIds);
               for (let cyclicLinkedReleasePath of cyclicLinkedReleasePaths) {
                   if (cyclicLinkedReleasePath !== "") {
                       $(this).closest('tr').find('select').eq(0).val(oldSelect);
                       messages.push('<p>Cyclic Hierarchy: <b>' + cyclicLinkedReleasePath + '</b></p>');
                   }
               }
               if (messages.length != 0) {
                   displayAlert(messages);
                   $("#formSubmit").prop('disabled', false);
                   return;
               }
               replaceValueOfNode(releaseWithRelations, i, trace, idValue, "", "releaseId");
               $(this).closest('tr').attr('id', 'releaseLinkRow' + idValue);
               setSubNodesParent($(this).closest('tr'), oldSelect, idValue);
               $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
               $("#formSubmit").prop('disabled', false);
               $(this).closest('tr').find('select').eq(0).attr('data-old', idValue);
            }

            function changeReleaseRelation() {
               let newSelect = $(this).closest('tr').find('select').eq(0).val();
               let currentLayer =  $(this).closest('tr').attr('data-layer');
               let parentNode = $(this).closest('tr').attr('parent-node');
               let idValue = $(this).closest('tr').find('select').eq(1).val();
               let textValue = $(this).closest('tr').find('select').eq(1).find('option:selected').text();
               let currentIndex = $(this).closest('tr').attr('data-index');
               let i=0;
               let trace = [];
               trace.unshift(currentIndex);
               for (let layer = currentLayer - 1; layer >= 0; layer-- ) {
                    trace.unshift($(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('data-index'));
                    parentNode =  $(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('parent-node');
               }
               replaceValueOfNode(releaseWithRelations, i, trace, idValue, textValue, "releaseRelation");
               $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
            }

            function changeMainLineState() {
               let newSelect = $(this).closest('tr').find('select').eq(0).val();
               let currentLayer =  $(this).closest('tr').attr('data-layer');
               let parentNode = $(this).closest('tr').attr('parent-node');
               let idValue = $(this).closest('tr').find('select').eq(2).val();
               let textValue = $(this).closest('tr').find('select').eq(2).find('option:selected').text();
               let currentIndex = $(this).closest('tr').attr('data-index');
               let i=0;
               let trace = [];
               trace.unshift(currentIndex);
               for (let layer = currentLayer - 1; layer >= 0; layer-- ) {
                    trace.unshift($(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('data-index'));
                    parentNode =  $(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('parent-node');
               }
               replaceValueOfNode(releaseWithRelations, i, trace, idValue, textValue, "mainLineState");
               $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
            }

            function replaceValueOfNode(releasesLinked, i, trace, idValue, textValue, type) {
                let index = trace[i];
                if(i === (trace.length - 1)) {
                    if (type == "releaseId") {
                        releasesLinked[index].releaseId = idValue;
                    } else if (type == "releaseRelation") {
                        releasesLinked[index].releaseRelationship = idValue;
                    } else if (type == "mainLineState") {
                        releasesLinked[index].mainlineState = idValue;
                    } else if (type == "comment") {
                        releasesLinked[index].comment = textValue.trim();
                    }
                    return;
                }
                else {
                    i++;
                    replaceValueOfNode(releasesLinked[index].releaseLink, i, trace, idValue, textValue, type);
                }
            }

            function changeComment() {
               let newSelect = $(this).closest('tr').find('select').eq(0).val();
               let currentLayer =  $(this).closest('tr').attr('data-layer');
               let parentNode = $(this).closest('tr').attr('parent-node');
               let value = $(this).val();
               let currentIndex = $(this).closest('tr').attr('data-index');
               let i=0;
               let trace = [];
               trace.unshift(currentIndex);
               for (let layer = currentLayer - 1; layer >= 0; layer-- ) {
                    trace.unshift($(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('data-index'));
                    parentNode = $(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('parent-node');
               }
               replaceValueOfNode(releaseWithRelations, i, trace, "", value, "comment");
               $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
            }

            function removeNodeFromNetwork(releasesLinked, i, trace) {
                let index = trace[i];
                if (i === (trace.length - 1)) {
                    releasesLinked.splice(index,1);
                    return;
                } else {
                    i++;
                    removeNodeFromNetwork(releasesLinked[index].releaseLink, i, trace);
                }
            }

            function addChildNode(releasesLinked, i, trace, newNodes) {
                let index = trace[i];
                if (i === (trace.length - 1)) {
                    releasesLinked[index].releaseLink.push(...newNodes);
                    return;
                } else {
                    i++;
                    addChildNode(releasesLinked[index].releaseLink, i, trace, newNodes);
                }
            }

            function getLinkedReleaseOfNode(what, releaseId){
                return new Promise(function(resolve, reject) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=viewReleaseURL%>',
                        data: {
                            '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                            '<portlet:namespace/><%=PortalConstants.RELEASE_ID%>': releaseId
                        },
                        success: function (body) {
                            resolve(body.result);
                        },
                        error: function() {
                            reject("");
                        }
                    });
                });
            }

            $('#addReleaseToNetWork').on('click', showSearchReleaseDialog);
            $('#LinkedReleasesNetwork').on('click', '.add-child', function(event) {
                let selectedElement = $(this).closest('tr');
                showChildSearchReleaseDialog(selectedElement);
            });

            $('#searchChildReleaseDialog').on('click', '#searchReleaseBtn', function(event) {
                var searchRelease = $('#searchChildReleaseDialog #searchRelease').val();
                if ($('#searchChildReleaseDialog #exactMatchRelease').is(':checked') && !(searchRelease.startsWith("\"") && searchRelease.endsWith("\""))) {
                   searchRelease = '"' + searchRelease + '"';
                }
                releaseFromAjax('<%=PortalConstants.RELEASE_SEARCH%>', searchRelease, '#searchChildResultsTable', function(data) {
                    if($dataTable) {
                        $dataTable.destroy();
                    }
                    $('#searchChildResultsTable tbody').html(data);
                    addLinkToReleaseNameAndVersion('searchChildResultsTable');
                    makeSearchChildReleaseDataTable('#searchChildResultsTable');
                });
            });

            function makeSearchChildReleaseDataTable() {
                $dataTable = datatables.create('#searchChildResultsTable', {
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

            function showChildSearchReleaseDialog(selectedElement) {
                let parentId = selectedElement.attr('id').substring(14);
                let parentLayer = parseInt(selectedElement.attr('data-layer'));
                let currentIndex = selectedElement.attr('data-index');
                let releaseName = selectedElement.find('td').eq(0).find('div').text().trim();
                let releaseVersion = selectedElement.find('td').eq(1).text().trim();
                if($dataTable) {
                    $dataTable.destroy();
                    $dataTable = undefined;
                }
                $releaseDialog = dialog.open('#searchChildReleaseDialog', {
                }, async function(submit, callback) {
                    var releaseIds = [];
                    var releaseNames = [];
                    let parentNode = selectedElement.attr('parent-node');
                    let indexTrace = [];
                    let releaseTrace = [];
                    let hierarchyName = [];
                    indexTrace.unshift(currentIndex);
                    releaseTrace.unshift(parentId);
                    hierarchyName.unshift(releaseName + ' (' + releaseVersion + ')');
                    for (let layer = parentLayer - 1; layer >= 0; layer-- ) {
                        indexTrace.unshift(selectedElement.prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('data-index'));
                        releaseName = selectedElement.prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').find('td').eq(0).find('div').text().trim();
                        releaseVersion = selectedElement.prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').find('td').eq(1).text().trim();
                        releaseTrace.unshift(parentNode);
                        hierarchyName.unshift(releaseName + ' (' + releaseVersion + ')');
                        parentNode = selectedElement.prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('parent-node');
                    }
                    let selectedNode = findChildNodeByIndexTrace(releaseWithRelations, indexTrace, 0);
                    let indexCurrentRow = $('#LinkedReleasesNetwork tbody tr').index(selectedElement);
                    let subNodeIds = getSubNodeReleaseIds(selectedNode);
                    let appendIndex = indexCurrentRow + subNodeIds.length;

                    $('#searchChildResultsTable').find(':checked').each(function () {
                        releaseIds.push(this.value);
                        releaseNames.push($(this).closest('tr').find('td').eq(2).find('a').html() + ' (' + $(this).closest('tr').find('td').eq(3).find('a').html() + ')');
                    });

                    let numberOfNodeSameLevel = $('tr[parent-node="'+parentId+'"][data-layer="'+(parentLayer+1)+'"]').length;
                    let canLinkReleases = [];
                    let parentIds = [];
                    let levels = [];
                    let mainlineStates = [];
                    let releaseRelationships = [];
                    let indexOfNodes = [];
                    let comments = [];
                    let selectedReleases = [];
                    let releaseDuplicates = [];
                    let messages = [];
                    let indexOfNode = numberOfNodeSameLevel;
                    for (let [index, releaseId] of releaseIds.entries()) {
                        let cyclicLink = false;
                        let cyclicLinkedReleasePaths = await getCyclicLinkedReleasePath('<%=PortalConstants.CYCLIC_LINKED_RELEASE_PATH%>', releaseTrace.reverse(), [releaseId]);
                        if (cyclicLinkedReleasePaths.length > 0) {
                            for (let cyclicLinkedReleasePath of cyclicLinkedReleasePaths) {
                                if (cyclicLinkedReleasePath !== "") {
                                    messages.push('<p>Cyclic Hierarchy: <b>' + cyclicLinkedReleasePath + '</b></p>');
                                    cyclicLink = true;
                                }
                            }
                        }
                        if (cyclicLink == true) {
                            continue;
                        }
                        if (checkDuplicateSubRelease(indexTrace, releaseId)) {
                            releaseDuplicates.push(releaseNames[index]);
                            continue;
                        }
                        let releaseWithIdExisted = await checkReleaseExist('<%=PortalConstants.CHECK_RELEASE_EXIST%>', releaseId);
                        if (releaseWithIdExisted != null && releaseWithIdExisted == true) {
                            canLinkReleases.push(releaseId);
                            parentIds.push(parentId);
                            levels.push((parentLayer+1));
                            mainlineStates.push("OPEN");
                            releaseRelationships.push("CONTAINED");
                            indexOfNodes.push(indexOfNode);
                            comments.push("");
                            let selectedRelease = {
                                  "releaseLink": [],
                                  "releaseId": releaseId,
                                  "releaseRelationship": "CONTAINED",
                                  "mainlineState": "OPEN",
                                  "comment": "",
                                  "createOn": "<%=SW360Utils.getCreatedOn()%>",
                                  "createBy": "${loginUser.email}"
                            }
                            selectedReleases.push(selectedRelease);
                        }
                        indexOfNode += 1;
                    }
                    addChildNode(releaseWithRelations, 0, indexTrace, selectedReleases);
                    let releaseRows = await addReleaseToTable('<%=PortalConstants.CREATE_LINKED_RELEASE_ROW%>', canLinkReleases, parentIds, levels, mainlineStates, releaseRelationships, indexOfNodes, comments);
                    $("#LinkedReleasesNetwork tbody tr:eq(" + appendIndex + ")").after(releaseRows);
                    $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
                    if (releaseDuplicates.length > 0) {
                        messages.push('<p>Releases duplicated: <b>' + releaseDuplicates.join(', ') + '</b></p>');
                    }
                    if (messages.length > 0) {
                        displayAlert(messages);
                    }
                    callback(true);
                }, function() {
                    $('#searchChildResultsTable').hide();
                    $('#searchChildReleaseDialog #searchRelease').val('');
                    $("#selectChildReleaseBtn").attr("disabled", true);
                });
            }

            $('#searchChildResultsTable').on('change', 'input', function() {
                $("#selectChildReleaseBtn").attr("disabled", false);
            });
            $('#releaseResultsTable').on('change', 'input', function() {
                $("#selectRootReleaseBtn").attr("disabled", false);
            });

            keyboard.bindkeyPressToClick('searchRootRelease #searchRelease', 'searchRootRelease #searchReleaseBtn');
            keyboard.bindkeyPressToClick('searchChildReleaseDialog #searchRelease', 'searchChildReleaseDialog #searchReleaseBtn');
            var homeUrl = themeDisplay.getURLHome().replace(/\/web\//, '/group/');

            $('#searchRootRelease').on('click','#searchReleaseBtn', function() {
                var searchRelease = $('#searchRootRelease #searchRelease').val();
                if ($('#searchRootRelease #exactMatchRelease').is(':checked') && !(searchRelease.startsWith("\"") && searchRelease.endsWith("\""))) {
                   searchRelease = '"' + searchRelease + '"';
                }
                releaseFromAjax('<%=PortalConstants.RELEASE_SEARCH%>', searchRelease, '#releaseResultsTable', function(data) {
                    if($dataTable) {
                        $dataTable.destroy();
                    }
                    $('#releaseResultsTable tbody').html(data);
                    addLinkToReleaseNameAndVersion('releaseResultsTable');
                    makeReleaseDataTable('#releaseResultsTable');
                });
            });

            $('#searchRootRelease').on('click', '#getLinkedReleasesButton', function() {
                releaseFromAjax('<%=PortalConstants.RELEASE_LIST_FROM_LINKED_PROJECTS%>', '', '#releaseResultsTable', function(data) {
                    if ($dataTable) {
                        $dataTable.destroy();
                    }
                    $('#releaseResultsTable tbody').html(data);
                    addLinkToReleaseNameAndVersion('releaseResultsTable');
                    makeReleaseDataTable('#releaseResultsTable');
                });
            });

            $('#searchChildReleaseDialog').on('click', '#getLinkedReleasesButton', function() {
                releaseFromAjax('<%=PortalConstants.RELEASE_LIST_FROM_LINKED_PROJECTS%>', '', '#searchChildResultsTable', function(data) {
                    if($dataTable) {
                        $dataTable.destroy();
                    }
                    $('#searchChildResultsTable tbody').html(data);
                    addLinkToReleaseNameAndVersion('searchChildResultsTable');
                    makeReleaseDataTable('#searchChildResultsTable');
                });
            });

            $('#releaseResultsTable').on('change', 'input', function() {
                $releaseDialog.enablePrimaryButtons($('#releaseResultsTable input:checked').length > 0);
            });

            $('#searchChildResultsTable').on('change', 'input', function() {
                $releaseDialog.enablePrimaryButtons($('#searchChildResultsTable input:checked').length > 0);
            });

            function showSearchReleaseDialog() {
                if($dataTable) {
                    $dataTable.destroy();
                    $dataTable = undefined;
                }
                $releaseDialog = dialog.open('#searchRootRelease', {
                }, async function(submit, callback) {
                    var releaseIds = [];
                    var releaseNames = [];
                    $('#releaseResultsTable').find(':checked').each(function () {
                        releaseIds.push(this.value);
                        releaseNames.push($(this).closest('tr').find('td').eq(2).find('a').html() + ' (' + $(this).closest('tr').find('td').eq(3).find('a').html() + ')');
                    });
                    let numberOfRootExisted = $('tr[parent-node=""]').length;
                    let canLinkReleases = [];
                    let parentIds = [];
                    let levels = [];
                    let mainlineStates = [];
                    let releaseRelationships = [];
                    let indexOfNodes = [];
                    let comments = [];
                    let releaseDuplicates = [];
                    let messages = [];
                    for (let [index, releaseId] of releaseIds.entries()) {
                        let indexOfNode = numberOfRootExisted + index;
                        let numberOfReleaseWithSameId = $('tr[parent-node=""][id="releaseLinkRow' + releaseId + '"]').length;
                        if (numberOfReleaseWithSameId > 0) {
                            releaseDuplicates.push(releaseNames[index]);
                            continue;
                        }
                        let releaseWithIdExisted = await checkReleaseExist('<%=PortalConstants.CHECK_RELEASE_EXIST%>', releaseId);
                        if (releaseWithIdExisted != null && releaseWithIdExisted == true) {
                            canLinkReleases.push(releaseId);
                            parentIds.push("");
                            levels.push(0);
                            mainlineStates.push("OPEN");
                            releaseRelationships.push("CONTAINED");
                            indexOfNodes.push(indexOfNode);
                            comments.push("");
                            let selectedRelease = {
                                  "releaseLink": [],
                                  "releaseId": releaseId,
                                  "releaseRelationship": "CONTAINED",
                                  "mainlineState": "OPEN",
                                  "comment": "",
                                  "createOn": "<%=SW360Utils.getCreatedOn()%>",
                                  "createBy": "${loginUser.email}"
                            }
                            releaseWithRelations.push(selectedRelease);
                        }
                    }
                    let releaseRows = await addReleaseToTable('<%=PortalConstants.CREATE_LINKED_RELEASE_ROW%>', canLinkReleases, parentIds, levels, mainlineStates, releaseRelationships, indexOfNodes, comments);
                    $('#LinkedReleasesNetwork tbody').append(releaseRows);
                    $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
                    if (releaseDuplicates.length > 0) {
                        messages.push('<p>Releases duplicated: <b>' + releaseDuplicates.join(', ') + '</b></p>');
                        displayAlert(messages);
                    }
                    callback(true);
                }, function() {
                    this.$.find('#releaseResultsTable').hide();
                    this.$.find('#searchRelease').val('');
                    $("#selectRootReleaseBtn").attr("disabled", true);
                });
            }

            function makeReleaseDataTable(tableId) {
                $dataTable = datatables.create(tableId, {
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

            function releaseFromAjax(what, where, tableId, callback) {
                $(tableId).hide();
                $(tableId).closest('.modal-body').find('.spinner').show();
                $(tableId).closest('.modal-body').find('#searchReleaseBtn').prop('disabled', true);
                $(tableId).closest('.modal-body').find('#getLinkedReleasesButton').prop('disabled', true);

                jQuery.ajax({
                    type: 'POST',
                    url: '<%=viewReleaseURL%>',
                    data: {
                        '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                        '<portlet:namespace/><%=PortalConstants.WHERE%>': where
                    },
                    success: function (data) {
                        $(tableId + ' tbody').empty();
                        callback(data);
                        $(tableId).show();
                        $(tableId).closest('.modal-body').find('.spinner').hide();
                        $(tableId).closest('.modal-body').find('#searchReleaseBtn').prop('disabled', false);
                        $(tableId).closest('.modal-body').find('#getLinkedReleasesButton').prop('disabled', false);
                    },
                    error: function() {
                        $releaseDialog.alert('<liferay-ui:message key="cannot.link.to.release" />');
                    }
                });
             }

            function addReleaseToTable(what, where, parentIds, layers, mainLineState, releaseRelation, indexes, comments){
                return new Promise(function(resolve, reject) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=viewReleaseURL%>',
                        data: {
                            '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                            '<portlet:namespace/><%=PortalConstants.WHERE%>': where,
                            '<portlet:namespace/><%=PortalConstants.PARENT_NODE_ID%>': parentIds,
                            '<portlet:namespace/><%=PortalConstants.LAYER%>': layers,
                            '<portlet:namespace/><%=PortalConstants.MAINLINE_STATE%>': mainLineState,
                            '<portlet:namespace/><%=PortalConstants.RELEASE_RELATION_SHIP%>': releaseRelation,
                            '<portlet:namespace/><%=PortalConstants.INDEXES%>': indexes,
                            '<portlet:namespace/><%=PortalConstants.COMMENTS%>': comments
                        },
                        success: function (data) {
                            resolve(data);
                        },
                        error: function() {
                            reject("");
                            $releaseDialog.alert('<liferay-ui:message key="cannot.link.to.release" />');
                        }
                    });
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

            function addLinkToReleaseNameAndVersion(tableId) {
                $('#'+tableId+' > tbody  > tr').each(function() {
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

            function checkReleaseExist(what, releaseId){
                return new Promise(function(resolve, reject) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=viewReleaseURL%>',
                        data: {
                            '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                            '<portlet:namespace/><%=PortalConstants.RELEASE_ID%>': releaseId
                        },
                        success: function (data) {
                            resolve(data.result);
                        },
                        error: function() {
                            reject(null);
                        }
                    });
                });
            }

            function checkDuplicateSubRelease(indexTrace, releaseId) {
                let node = releaseWithRelations;
                for (let i = 0; i < indexTrace.length; i++) {
                    node = node[indexTrace[i]];
                    if (i == (indexTrace.length - 1)) {
                        let releasesWithSameRoot = node.releaseLink.map((subNode) => subNode.releaseId);
                        if (releasesWithSameRoot.includes(releaseId)) {
                            return true;
                        }
                    }
                    node = node.releaseLink;
                }
                return false;
            }

            function checkDuplicateReleaseWithSameLevel(trace, releaseId) {
                let node = releaseWithRelations;
                if (trace.length == 1) {
                    let releasesWithSameRoot = node.map((subNode) => subNode.releaseId);
                    if (releasesWithSameRoot.includes(releaseId)) {
                        return true;
                    }
                } else {
                    for (let i = 0; i < trace.length - 1; i++) {
                        node = node[trace[i]];
                        if (i == (trace.length - 2)) {
                            let releasesWithSameRoot = node.releaseLink.map((subNode) => subNode.releaseId);
                            if (releasesWithSameRoot.includes(releaseId)) {
                                return true;
                            }
                        }
                        node = node.releaseLink;
                    }
                }
                return false;
            }

            async function loadReleasesWithSameComponent() {
                let options = $(this).find('option');
                let selectedRelease = $(this).val().trim();
                let loadedRelease = [];
                for (let option of options) {
                    loadedRelease.push($(option).val().trim());
                }
                let releasesWithSameComponent = await getReleasesWithSameComponent('<%=PortalConstants.RELEASES_WITH_SAME_COMPONENT_ID%>', selectedRelease);
                releasesWithSameComponent = new Map(Object.entries(releasesWithSameComponent));
                releasesWithSameComponent.forEach((version, releaseId) => {
                    if (!loadedRelease.includes(releaseId)) {
                        $(this).append($('<option>', {
                            value: releaseId,
                            text: version
                        }));
                    }
                })
            }

            function getCyclicLinkedReleasePath(what, parentIds, childReleaseIds) {
                return new Promise(function(resolve, reject) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=viewReleaseURL%>',
                        data: {
                            '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                            '<portlet:namespace/><%=PortalConstants.RELEASE_ID_ARRAY%>': parentIds,
                            '<portlet:namespace/><%=PortalConstants.CHILD_RELEASE_ID_ARRAY%>': childReleaseIds
                        },
                        success: function (data) {
                            resolve(data.data);
                        },
                        error: function() {
                            reject([]);
                        }
                    });
                });
            }

            function getReleasesWithSameComponent(what, releaseId){
                return new Promise(function(resolve, reject) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=viewReleaseURL%>',
                        data: {
                            '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                            '<portlet:namespace/><%=PortalConstants.RELEASE_ID%>': releaseId
                        },
                        success: function (data) {
                            resolve(data.result);
                        },
                        error: function() {
                            reject({});
                        }
                    });
                });
            }

            $("#checkDependency").click(async function() {
                $("#checkDependency .spinner").show();
                let currentNetwork = $('#releaseRelationTree').val();
                let rowsWithDiff = await checkDiffDependencyNetworkAndReleaseRelationship('<%=PortalConstants.CHECK_DIFF_DEPENDENCY_NETWORK_WITH_RELEASES_RELATIONSHIP%>', currentNetwork);
                resetBackgroundAndTextColor();
                let rows = $("#LinkedReleasesNetwork").find('tr');
                for (let indexOfRow of rowsWithDiff) {
                    changeColorOfRow(rows[indexOfRow]);
                }
                $("#checkDependency .spinner").hide();
            });

            function resetBackgroundAndTextColor() {
                let columns = $("#LinkedReleasesNetwork tbody").find('td');
                for (let col of columns) {
                    $(col).css('background-color', 'white');
                    $(col).css('color', 'black');
                }
            }

            function changeColorOfRow(row) {
                let columns = $(row).find('td');
                for (let col of columns) {
                    $(col).css('background-color', '#ffd4aa');
                    $(col).css('color', 'red');
                }
            }

            function checkDiffDependencyNetworkAndReleaseRelationship(what, currentNetwork) {
                return new Promise(function(resolve, reject) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=viewReleaseURL%>',
                        data: {
                            '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                            '<portlet:namespace/><%=PortalConstants.CURRENT_NETWORK%>': currentNetwork
                        },
                        success: function (data) {
                            resolve(data.result);
                        },
                        error: function() {
                            reject([]);
                        }
                    });
                });
            }

            function findChildNodeByIndexTrace(releasesRelationship, trace, i) {
                let index = trace[i];
                if (i === (trace.length - 1)) {
                    return releasesRelationship[index];
                }
                i++;
                return findChildNodeByIndexTrace(releasesRelationship[index].releaseLink, trace, i);
            }

            function getSubNodeReleaseIds(node) {
                let releaseIds = [];
                for (let subNode of node.releaseLink) {
                    releaseIds.push(subNode.releaseId);
                    releaseIds.push(...getSubNodeReleaseIds(subNode));
                }
                return releaseIds;
            }

            function setSubNodesParent(currentRow, oldSelect, newValue) {
                let rows = $('#LinkedReleasesNetwork tbody').find('tr');
                let numberOfCurrentRow = $(currentRow).index();
                for (let index = (numberOfCurrentRow+1); index < rows.length; index++) {
                    if ($(rows[index]).attr('parent-node') !== oldSelect) {
                        break;
                    }
                    $(rows[index]).attr('parent-node', newValue);
                }
            }

            function displayAlert(messages) {
                for(let message of messages) {
                    $('.show-alert .alert-message').append(message);
                }
                $('.show-alert').css('display','block');
                setTimeout(() => {
                    $('.show-alert .alert-message').empty();
                    $('.show-alert').css('display','none');
                }, 7000);
            }

            function getHtmlRowsFromDependencyNetwork(what){
                return new Promise(function(resolve, reject) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=viewReleaseURL%>',
                        data: {
                            '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                            '<portlet:namespace/><%=PortalConstants.WHERE%>': JSON.stringify(releaseWithRelations)
                        },
                        success: function (data) {
                            resolve(data);
                        },
                        error: function() {
                            reject("");
                            $releaseDialog.alert('<liferay-ui:message key="cannot.link.to.release" />');
                        }
                    });
                });
            }

            $('.show-alert .close').on('click', function(event) {
                $('.show-alert').css('display', 'none');
            });

            $('#tableBody').on('click', 'svg[data-row-id]', function(event) {
                let currentLayer =  $(this).closest('tr').attr('data-layer');
                let parentNode = $(this).closest('tr').attr('parent-node');
                let currentIndex = $(this).closest('tr').attr('data-index');
                let version = $(this).closest('tr').find('select').eq(0).find('option:selected').text().trim();
                let componentName = $(this).closest('tr').find('td').eq(0).find('div').eq(0).text().trim();
                let trace = [];
                trace.unshift(currentIndex);
                for (let layer = currentLayer - 1; layer >= 0; layer-- ) {
                      trace.unshift($(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('data-index'));
                      parentNode =  $(this).closest('tr').prevAll('#releaseLinkRow'+parentNode+'[data-layer="'+layer+'"]').first().attr('parent-node');
                }
                dialog.open('#deleteReleaseInNetworkDialog', {
                    release: componentName + '(' + version + ')'
                }, function(submit, callback) {
                     removeNodeFromNetwork(releaseWithRelations, 0, trace);
                     $('#tableBody').empty();
                     $('#releaseRelationTree').val(JSON.stringify(releaseWithRelations));
                     displayReleaseRelationShip();
                     callback(true);
                });
            });

            $('#tableBody').on('click', '.load-release', displayRelationOfNode);
            $('#tableBody').on('change', '.releaseVersion', changeReleaseIdInNode);
            $('#tableBody').on('change', '.projectReleaseRelation', changeReleaseRelation);
            $('#tableBody').on('change', '.mainlineState', changeMainLineState);
            $('#tableBody').on('focusout', '.releaseComment', changeComment);
            $('#tableBody').on('focus', '.releaseVersion', loadReleasesWithSameComponent);
        });
    });
</script>
