<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~ With modifications by Bosch Software Innovations GmbH, 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Component" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ComponentType" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="categories" class="java.lang.String" scope="request"/>
<jsp:useBean id="languages" class="java.lang.String" scope="request"/>
<jsp:useBean id="softwarePlatforms" class="java.lang.String" scope="request"/>
<jsp:useBean id="operatingSystems" class="java.lang.String" scope="request"/>
<jsp:useBean id="componentType" class="java.lang.String" scope="request"/>
<jsp:useBean id="vendorList" class="java.lang.String" scope="request"/>
<jsp:useBean id="vendorNames" class="java.lang.String" scope="request"/>
<jsp:useBean id="mainLicenseIds" class="java.lang.String" scope="request"/>
<jsp:useBean id="name" class="java.lang.String" scope="request"/>
<jsp:useBean id="totalRows" type="java.lang.Integer" scope="request"/>

<core_rt:set var="programmingLanguages" value='<%=PortalConstants.PROGRAMMING_LANGUAGES%>'/>
<core_rt:set var="operatingSystemsAutoC" value='<%=PortalConstants.OPERATING_SYSTEMS%>'/>
<core_rt:set var="softwarePlatformsAutoC" value='<%=PortalConstants.SOFTWARE_PLATFORMS%>'/>

<portlet:renderURL var="addComponentURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>
</portlet:renderURL>

<portlet:renderURL var="friendlyComponentURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</portlet:renderURL>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_COMPONENT%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="sw360ComponentsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_COMPONENT_LIST%>'/>
</portlet:resourceURL>

<portlet:actionURL var="applyFiltersURL" name="applyFilters">
</portlet:actionURL>

<liferay-portlet:renderURL var="friendlyLicenseURL" portletName="sw360_portlet_licenses">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<portlet:resourceURL var="sw360CompositeUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.CODESCOOP_ACTION_COMPOSITE%>'/>
</portlet:resourceURL>

<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
				<div id="searchInput" class="card">
					<div class="card-header">
						Advanced Search
					</div>
                    <div class="card-body">
                        <form action="<%=applyFiltersURL%>" method="post">
                            <div class="form-group">
                                <label for="component_name">Component Name</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Component._Fields.NAME%>"
                                    value="<sw360:out value="${name}"/>" id="component_name">
                            </div>
                            <div class="form-group">
                                <label for="categories">Categories</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Component._Fields.CATEGORIES%>"
                                    value="<sw360:out value="${categories}"/>" id="categories">
                            </div>
                            <div class="form-group">
                                <label for="component_type">Component Type</label>
                                <select class="form-control form-control-sm" id="component_type" name="<portlet:namespace/><%=Component._Fields.COMPONENT_TYPE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>" class="textlabel stackedLabel"></option>
                                    <sw360:DisplayEnumOptions type="<%=ComponentType.class%>" selectedName="${componentType}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="languages">Languages</label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Component._Fields.LANGUAGES%>"
                                    value="<sw360:out value="${languages}"/>" id="languages">
                            </div>
                            <div class="form-group">
                                <label for="software_platforms">Software Platforms</label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.SOFTWARE_PLATFORMS%>"
                                    value="<sw360:out value="${softwarePlatforms}"/>" id="software_platforms">
                            </div>
                            <div class="form-group">
                                <label for="operating_systems">Operating Systems</label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.OPERATING_SYSTEMS%>"
                                    value="<sw360:out value="${operatingSystems}"/>" id="operating_systems">
                            </div>
                            <div class="form-group">
                                <label for="vendor_names">Vendors</label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.VENDOR_NAMES%>"
                                    value="<sw360:out value="${vendorNames}"/>" id="vendor_names">
                            </div>
                            <div class="form-group">
                                <label for="main_licenses">Main Licenses</label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.MAIN_LICENSE_IDS%>"
                                    value="<sw360:out value="${mainLicenseIds}"/>" id="main_licenses">
                            </div>
                            <button type="submit" class="btn btn-primary btn-sm btn-block">Search</button>
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
							<button type="button" class="btn btn-primary" onclick="window.location.href='<%=addComponentURL%>'">Add Component</button>
						</div>
						<div id="btnExportGroup" class="btn-group" role="group">
							<button id="btnExport" type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
						        Export Spreadsheet
						        <clay:icon symbol="caret-bottom" />
						    </button>
						    <div class="dropdown-menu" aria-labelledby="btnExport">
						      <a class="dropdown-item" href="#" data-type="componentOnly">Components only</a>
						      <a class="dropdown-item" href="#" data-type="componentWithReleases">Components with releases</a>
						    </div>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="Components (${totalRows})">
					Components (<span id="componentCounter">${totalRows}</span>)
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="componentsTable" class="table table-bordered"></table>
                </div>
            </div>

		</div>
	</div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs"></div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    var renderCallback = function () {
    };
    var dataGetter = function(field) {
    };
</script>

<core_rt:if test="${codescoopActive}">
    <script>
        window.codescoopEnabled = true;
        document.addEventListener("DOMContentLoaded", function() {
            require(['modules/codeScoop' ], function(codeScoop) {
                var api = new codeScoop();
                api.activateIndexes("componentsTable", "<%=sw360ComponentsURL%>", "<%=sw360CompositeUrl%>");
                renderCallback = api._update_indexes;
                dataGetter = api._get_composite_data_item;
            });
            document
                .getElementById("componentsTable")
                .getElementsByTagName("tfoot")[0]
                .getElementsByTagName("th")[0]
                .setAttribute("colspan", 10)
        });
    </script>
</core_rt:if>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'modules/autocomplete', 'modules/dialog', 'bridges/datatables', 'utils/render' ], function($, autocomplete, dialog, datatables, render) {
            var componentsTable;

            // initializing
            autocomplete.prepareForMultipleHits('languages', ${programmingLanguages});
            autocomplete.prepareForMultipleHits('software_platforms', ${softwarePlatformsAutoC});
            autocomplete.prepareForMultipleHits('operating_systems', ${operatingSystemsAutoC});
            autocomplete.prepareForMultipleHits('vendor_names', ${vendorList});
            componentsTable = createComponentsTable();

            // catch ctrl+p and print dataTable
            $(document).on('keydown', function(e){
                if(e.ctrlKey && e.which === 80){
                    e.preventDefault();
                    componentsTable.buttons('.custom-print-button').trigger();
                }
            });

            // register event handlers
            $('.filterInput').on('input', function() {
                $('#exportSpreadsheetButton').prop('disabled', true);
                <%--when filters are actually applied, page is refreshed and exportSpreadsheetButton enabled automatically--%>
            });
            $('#componentsTable').on('click', 'svg.delete', function(event) {
                var data = $(event.currentTarget).data();
                deleteComponent(data.componentId, data.componentName, data.componentReleaseCount, data.componentAttachmentCount);
            });
            $('#btnExportGroup a.dropdown-item').on('click', function(event) {
                exportSpreadsheet($(event.currentTarget).data('type'));
            });

            // helper functions
            function makeComponentUrl(componentId, page) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                    .setParameter('<%=PortalConstants.PAGENAME%>', page)
                    .setParameter('<%=PortalConstants.COMPONENT_ID%>', componentId);
                return portletURL.toString();
            }

            function replaceFriendlyUrlParameter(portletUrl, id, page) {
                return portletUrl
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', id);
            }

            // create and render data table
            function createComponentsTable() {
                var columnDefs =  [];
                var columns = [
                    {"title": "Vendor", data: "vndrs"},
                    {"title": "Component Name", data: "name", render: {display: renderComponentNameLink}},
                    {"title": "Main Licenses", data: "lics", render: {display: renderLicenseLink}},
                    {"title": "Component Type", data: "cType"},
                    {"title": "Actions", data: "id", render: {display: renderComponentActions}, className: 'two actions', orderable: false }
                ];
                var printColumns = [0, 1, 2, 3];

                if (window.codescoopEnabled) {
                    columns = [
                        {"title": "Logo", data: function(row, type, val, meta){
                                return dataGetter(row.DT_RowId, 'logo');
                            }},
                        {"title": "Vendor", data: "vndrs"},
                        {"title": "Component Name", data: "name", render: {display: renderComponentNameLink}},
                        {"title": "Main Licenses", data: "lics"},
                        {"title": "Rate", data: function(row, type, val, meta){
                                return dataGetter(row.DT_RowId, 'rate');
                            }},
                        {"title": "Interest", data: function(row, type, val, meta){
                                return dataGetter(row.DT_RowId, 'index', 'interest');
                            }},
                        {"title": "Activity", data: function(row, type, val, meta){
                                return dataGetter(row.DT_RowId, 'index', 'activity');
                            }},
                        {"title": "Health", data: function(row, type, val, meta){
                                return dataGetter(row.DT_RowId, 'index', 'health');
                            }},
                        {"title": "Component Type", data: "cType"},
                        {"title": "Actions", data: "id", render: {display: renderComponentActions}, className: 'two actions', orderable: false }
                    ];
                    columnDefs = [{ "orderable": false, "targets": [0, 4, 5, 6 ,7] }];
                    printColumns = [0, 1, 2, 3, 4, 5, 6, 7, 8];
                }

                var componentsTable = datatables.create('#componentsTable', {
                    bServerSide: true,
                    sAjaxSource: '<%=sw360ComponentsURL%>',

                    columns: columns,
                    columnDefs: columnDefs,
                    drawCallback: renderCallback,
                    initComplete: datatables.showPageContainer,
                    order: [
                        [1, 'asc']
                    ]
                }, printColumns);

                return componentsTable;
            }

            function renderComponentActions(id, type, row) {
                 var $actions = $('<div>', {
				'class': 'actions'
			}),
			$editAction = render.linkTo(
			makeComponentUrl(id, '<%=PortalConstants.PAGENAME_EDIT%>'),
                        "",
                        '<svg class="lexicon-icon" title="Edit"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>'
                    ),
                    $deleteAction = $('<svg>', {
			'class': 'delete lexicon-icon',
			title: 'Delete',
			'data-component-id': id,
			'data-component-name': row.name,
			'data-component-release-count': row.lRelsSize,
			'data-component-attachment-count': row.attsSize,
                    });
			$deleteAction.append($('<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

                $actions.append($editAction, $deleteAction);
                return $actions[0].outerHTML;
            }

            function renderComponentNameLink(name, type, row) {
                return render.linkTo(replaceFriendlyUrlParameter('<%=friendlyComponentURL%>', row.id, '<%=PortalConstants.PAGENAME_DETAIL%>'), name);
            }

            function renderLicenseLink(lics, type, row) {
                var licensePortletURL = '<%=friendlyLicenseURL%>'
                    .replace(/components/g, "licenses");// DIRTY WORKAROUND

                for (var i = 0; i < lics.length; i++) {
                    lics[i] = render.linkTo(replaceFriendlyUrlParameter(licensePortletURL.toString(), lics[i], '<%=PortalConstants.PAGENAME_DETAIL%>'), lics[i]);
                }

                return lics;
            }

            // Export Spreadsheet action
            function exportSpreadsheet(type){
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RESOURCE_PHASE) %>')
                        .setParameter('<%=PortalConstants.ACTION%>', '<%=PortalConstants.EXPORT_TO_EXCEL%>');
                portletURL.setParameter('<%=Component._Fields.NAME%>', $('#component_name').val());
                portletURL.setParameter('<%=Component._Fields.CATEGORIES%>', $('#categories').val());
                portletURL.setParameter('<%=Component._Fields.LANGUAGES%>', $('#languages').val());
                portletURL.setParameter('<%=Component._Fields.SOFTWARE_PLATFORMS%>', $('#software_platforms').val());
                portletURL.setParameter('<%=Component._Fields.OPERATING_SYSTEMS%>', $('#operating_systems').val());
                portletURL.setParameter('<%=Component._Fields.VENDOR_NAMES%>', $('#vendor_names').val());
                portletURL.setParameter('<%=Component._Fields.COMPONENT_TYPE%>', $('#component_type').val());
                portletURL.setParameter('<%=Component._Fields.MAIN_LICENSE_IDS%>', $('#main_licenses').val());
                portletURL.setParameter('<%=PortalConstants.EXTENDED_EXCEL_EXPORT%>', type === 'componentWithReleases' ? 'true' : 'false');

                window.location.href = portletURL.toString();
            }

            // Delete component action
            function deleteComponent(id, name, numberOfReleases, attachmentsSize) {
                var $dialog;

                function deleteComponentInternal(callback) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=deleteAjaxURL%>',
                        cache: false,
                        data: {
                            <portlet:namespace/>componentid: id,
                            "<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>": btoa($("#moderationDeleteCommentField").val())
                        },
                        success: function (data) {
                            callback();

                            if (data.result == 'SUCCESS') {
                                componentsTable.row('#' + id).remove().draw(false);
                                $('#componentCounter').text(parseInt($('#componentCounter').text()) - 1);
                                $('#componentCounter').parent().attr('title', $('#componentCounter').parent().text());
                                $dialog.close();
                            }
                            else if (data.result == 'SENT_TO_MODERATOR') {
                                $dialog.info("You may not delete the component, but a request was sent to a moderator!", true);
                            }
                            else if (data.result == 'IN_USE') {
                                $dialog.warning("I could not delete the component, since it is in use.");
                            } else {
                                $dialog.alert("I could not delete the component.");
                            }
                        },
                        error: function () {
                            callback();
                            $dialog.alert("I could not delete the component!");
                        }
                    });
                }

                if (numberOfReleases > 0) {
                    dialog.warn(
                        'The component <b data-name="name"></b> cannot be deleted, since it contains <b data-name="releaseCount"></b> releases. Please delete the releases first.',
                        {
                            name: name,
                            releaseCount: numberOfReleases
                        }
                    );

                } else {
                    $dialog = dialog.confirm(
                        'danger',
                        'question-circle',
                        'Delete Component?',
                            '<p>Do you really want to delete the component <b data-name="name"></b>?</p>' +
                            '<div data-hide="hasNoDependencies">' +
                                '<p>' +
                                    'This component <b data-name="name"></b> contains:' +
                                '</p>' +
                                '<ul>' +
                                    '<li data-hide="hasNoReleases"><span data-name="releases"></span> releases</li>' +
                                    '<li data-hide="hasNoAttachments"><span data-name="attachments"></span> attachments</li>' +
                                '</ul>' +
                            '</div>' +
                            '<hr/>' +
                            '<form>' +
                                '<div class="form-group">' +
                                    '<label for="moderationDeleteCommentField">Please comment your changes</label>' +
                                    '<textarea id="moderationDeleteCommentField" class="form-control" data-name="comment" rows="4" placeholder="Comment your request..."></textarea>' +
                                '</div>' +
                            '</form>',
                        'Delete Component',
                        {
                            name: name,
                            releases: numberOfReleases,
                            hasNoReleases: numberOfReleases == 0,
                            attachments: attachmentsSize,
                            hasNoAttachments: attachmentsSize == 0,
                            hasNoDependencies: numberOfReleases == 0 && attachmentsSize == 0
                        }, function(submit, callback) {
                            deleteComponentInternal(callback);
                        }
                    );
                }
            }
        });
    });
</script>
