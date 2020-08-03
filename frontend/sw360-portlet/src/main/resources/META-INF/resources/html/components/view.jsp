<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~ With modifications by Bosch Software Innovations GmbH, 2016.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Component" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ComponentType" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.DateRange" %>
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

<div class="container" style="display: none;">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
				<div id="searchInput" class="card">
					<div class="card-header">
						<liferay-ui:message key="advanced.search" />
					</div>
                    <div class="card-body">
                        <form action="<%=applyFiltersURL%>" method="post">
                            <div class="form-group">
                                <label for="component_name"><liferay-ui:message key="component.name" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Component._Fields.NAME%>"
                                    value="<sw360:out value="${name}"/>" id="component_name">
                            </div>
                            <div class="form-group">
                                <label for="categories"><liferay-ui:message key="categories" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Component._Fields.CATEGORIES%>"
                                    value="<sw360:out value="${categories}"/>" id="categories">
                            </div>
                            <div class="form-group">
                                <label for="component_type"><liferay-ui:message key="component.type" /></label>
                                <select class="form-control form-control-sm" id="component_type" name="<portlet:namespace/><%=Component._Fields.COMPONENT_TYPE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>" class="textlabel stackedLabel"></option>
                                    <sw360:DisplayEnumOptions type="<%=ComponentType.class%>" selectedName="${componentType}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="languages"><liferay-ui:message key="languages" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Component._Fields.LANGUAGES%>"
                                    value="<sw360:out value="${languages}"/>" id="languages">
                            </div>
                            <div class="form-group">
                                <label for="software_platforms"><liferay-ui:message key="software.platforms" /></label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.SOFTWARE_PLATFORMS%>"
                                    value="<sw360:out value="${softwarePlatforms}"/>" id="software_platforms">
                            </div>
                            <div class="form-group">
                                <label for="operating_systems"><liferay-ui:message key="operating.systems" /></label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.OPERATING_SYSTEMS%>"
                                    value="<sw360:out value="${operatingSystems}"/>" id="operating_systems">
                            </div>
                            <div class="form-group">
                                <label for="vendor_names"><liferay-ui:message key="vendors" /></label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.VENDOR_NAMES%>"
                                    value="<sw360:out value="${vendorNames}"/>" id="vendor_names">
                            </div>
                            <div class="form-group">
                                <label for="main_licenses"><liferay-ui:message key="main.licenses" /></label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.MAIN_LICENSE_IDS%>"
                                    value="<sw360:out value="${mainLicenseIds}"/>" id="main_licenses">
                            </div>
                            <div class="form-group">
                                <label for="created_by"><liferay-ui:message key="created.by.email" /></label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Component._Fields.CREATED_BY%>"
                                    value="<sw360:out value="${createdBy}"/>" id="created_by">
                            </div>
                            <div class="form-group">
                                <span class="d-flex align-items-center mb-2">
                                    <label class="mb-0 mr-4" for="created_on"><liferay-ui:message key="created.on" /></label>
                                    <select class="form-control form-control-sm w-50" id="dateRange" name="<portlet:namespace/><%=PortalConstants.DATE_RANGE%>">
                                        <option value="<%=PortalConstants.NO_FILTER%>" class="textlabel stackedLabel"></option>
                                        <sw360:DisplayEnumOptions type="<%=DateRange.class%>" selectedName="${dateRange}" useStringValues="true"/>
                                    </select>
                                </span>
                                <input id="created_on" class="datepicker form-control form-control-sm" autocomplete="off"
                                    name="<portlet:namespace/><%=Component._Fields.CREATED_ON%>" <core_rt:if test="${empty createdOn}"> style="display: none;" </core_rt:if>
                                    type="text" pattern="\d{4}-\d{2}-\d{2}" value="<sw360:out value="${createdOn}"/>" />
                                <label id="toLabel" <core_rt:if test="${empty endDate}"> style="display: none;" </core_rt:if> ><liferay-ui:message key="to" /></label>
                                <input type="text" id="endDate" class="datepicker form-control form-control-sm ml-0" autocomplete="off"
                                    name="<portlet:namespace/><%=PortalConstants.END_DATE%>" <core_rt:if test="${empty endDate}"> style="display: none;" </core_rt:if>
                                    value="<sw360:out value="${endDate}"/>" pattern="\d{4}-\d{2}-\d{2}" />
                            </div>
                            <button type="submit" class="btn btn-primary btn-sm btn-block"><liferay-ui:message key="search" /></button>
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
							<button type="button" class="btn btn-primary" onclick="window.location.href='<%=addComponentURL%>'"><liferay-ui:message key="add.component" /></button>
							<button type="button" class="btn btn-secondary" data-action="import-spdx-bom"><liferay-ui:message key="import.spdx.bom" /></button>
						</div>
						<div id="btnExportGroup" class="btn-group" role="group">
							<button id="btnExport" type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
						        <liferay-ui:message key="export.spreadsheet" />
						        <clay:icon symbol="caret-bottom" />
						    </button>
						    <div class="dropdown-menu" aria-labelledby="btnExport">
						      <a class="dropdown-item" href="#" data-type="componentOnly"><liferay-ui:message key="components.only" /></a>
						      <a class="dropdown-item" href="#" data-type="componentWithReleases"><liferay-ui:message key="components.with.releases" /></a>
						    </div>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="components" /> (${totalRows})">
					<liferay-ui:message key="components" />(<span id="componentCounter">${totalRows}</span>)
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
<%@ include file="/html/utils/includes/importBom.jspf" %>
<script>
    var renderCallback = function () {
    };
    var dataGetter = function(field) {
    };
</script>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'modules/autocomplete', 'modules/dialog', 'bridges/datatables', 'utils/render' ], function($, autocomplete, dialog, datatables, render) {
            var componentsTable,
                vendorNames = new Array();
            <core_rt:forEach items="${vendorList}" var="vendorName">
                vendorNames.push("<sw360:out value='${vendorName}'/>");
             </core_rt:forEach >
            // initializing
            autocomplete.prepareForMultipleHits('languages', ${programmingLanguages});
            autocomplete.prepareForMultipleHits('software_platforms', ${softwarePlatformsAutoC});
            autocomplete.prepareForMultipleHits('operating_systems', ${operatingSystemsAutoC});
            autocomplete.prepareForMultipleHits('vendor_names', vendorNames);
            componentsTable = createComponentsTable();

            $('.datepicker').datepicker({changeMonth:true,changeYear:true,dateFormat: "yy-mm-dd", maxDate: new Date()}).change(dateChanged).on('changeDate', dateChanged);

            function dateChanged(ev) {
                let id = $(this).attr("id"),
                    dt = $(this).val();
                if (id === "created_on") {
                    $('#endDate').datepicker('option', 'minDate', dt);
                } else if (id === "endDate") {
                    $('#created_on').datepicker('option', 'maxDate', dt ? dt : new Date());
                }
            }

            $('#dateRange').on('change', function (e) {
                let selected = $("#dateRange option:selected").text(),
                    $datePkr = $(".datepicker"),
                    $toLabel = $("#toLabel");

                if (!selected) {
                    $datePkr.hide().val("");
                    $toLabel.hide();
                    return;
                }

                if (selected === 'Between') {
                    $datePkr.show();
                    $toLabel.show();
                } else {
                    $("#created_on").show();
                    $toLabel.hide();
                    $("#endDate").hide().val("");
                }
            });

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
                let columns = [
                    {"title": "<liferay-ui:message key="vendor" />", data: "vndrs", render: {display: renderVendorNames}},
                    {"title": "<liferay-ui:message key="component.name" />", data: "name", render: {display: renderComponentNameLink}},
                    {"title": "<liferay-ui:message key="main.licenses" />", data: "lics", render: {display: renderLicenseLink}},
                    {"title": "<liferay-ui:message key="component.type" />", data: "cType"},
                    {"title": "<liferay-ui:message key="actions" />", data: "id", render: {display: renderComponentActions}, className: 'two actions', orderable: false }
                ];
                let printColumns = [0, 1, 2, 3];
                var componentsTable = datatables.create('#componentsTable', {
                    bServerSide: true,
                    sAjaxSource: '<%=sw360ComponentsURL%>',

                    columns: columns,
                    columnDefs: [],
                    drawCallback: renderCallback,
                    initComplete: datatables.showPageContainer,
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    order: [
                        [1, 'asc']
                    ]
                }, printColumns);

                return componentsTable;
            }

            function renderVendorNames(name) {
                    return $("<span></span>").text(name)[0].outerHTML;
            }

            function renderComponentActions(id, type, row) {
                var $actions = $('<div>', {
				    'class': 'actions'
                    }),
                    $editAction = render.linkTo(
                        makeComponentUrl(id, '<%=PortalConstants.PAGENAME_EDIT%>'),
                        "",
                        '<svg class="lexicon-icon" title="<liferay-ui:message key="edit" />"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>'
                    ),
                    $deleteAction = $('<svg>', {
                        'class': 'delete lexicon-icon',
                        'data-component-id': id,
                        'data-component-name': row.name,
                        'data-component-release-count': row.lRelsSize,
                        'data-component-attachment-count': row.attsSize,
                    });
            
                $deleteAction.append($('<title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

                $actions.append($editAction, $deleteAction);
                return $actions[0].outerHTML;
            }

            function renderComponentNameLink(name, type, row) {
                return render.linkTo(replaceFriendlyUrlParameter('<%=friendlyComponentURL%>', row.id, '<%=PortalConstants.PAGENAME_DETAIL%>'), name);
            }

            function renderLicenseLink(lics, type, row) {
                var links = [],
                    licensePortletURL = '<%=friendlyLicenseURL%>'
                    .replace(/components/g, "licenses");// DIRTY WORKAROUND

                for (var i = 0; i < lics.length; i++) {
                    links[i] = render.linkTo(replaceFriendlyUrlParameter(licensePortletURL.toString(), lics[i], '<%=PortalConstants.PAGENAME_DETAIL%>'), lics[i]);
                }

                if(type == 'display') {
                    return links.join(', ');
                } else if(type == 'print') {
                    return lics.join(', ');
                } else if(type == 'type') {
                    return 'string';
                } else {
                    return lics.join(', ');
                }
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
                                $dialog.info('<liferay-ui:message key="you.may.not.delete.the.component.but.a.request.was.sent.to.a.moderator" />', true);
                            }
                            else if (data.result == 'IN_USE') {
                                $dialog.warning('<liferay-ui:message key="i.could.not.delete.the.component.since.it.is.in.use" />');
                            } else {
                                $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.component" />');
                            }
                        },
                        error: function () {
                            callback();
                            $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.component" />');
                        }
                    });
                }

                if (numberOfReleases > 0) {
                    dialog.warn(
						'<liferay-ui:message key="the.component.x.cannot.be.deleted.since.it.contains.y.releases.please.delete.the.releases.first" />',
                        {
                            name: name,
                            releaseCount: numberOfReleases
                        }
                    );

                } else {
                    $dialog = dialog.confirm(
                        'danger',
                        'question-circle',
                        '<liferay-ui:message key="delete.component" />?',
                            '<p><liferay-ui:message key="do.you.really.want.to.delete.the.component.x" /></p>' +
                            '<div data-hide="hasNoDependencies">' +
                                '<p>' +
                                    '<liferay-ui:message key="this.component.x.contains" />' +
                                '</p>' +
                                '<ul>' +
                                    '<li data-hide="hasNoReleases"><span data-name="releases"></span> <liferay-ui:message key="releases" /></li>' +
                                    '<li data-hide="hasNoAttachments"><span data-name="attachments"></span> <liferay-ui:message key="attachments" /></li>' +
                                '</ul>' +
                            '</div>' +
                            '<hr/>' +
                            '<form>' +
                                '<div class="form-group">' +
                                    '<label for="moderationDeleteCommentField"><liferay-ui:message key="please.comment.your.changes" /></label>' +
                                    '<textarea id="moderationDeleteCommentField" class="form-control" data-name="comment" rows="4" placeholder="<liferay-ui:message key="comment.your.request" />"></textarea>' +
                                '</div>' +
                            '</form>',
                        '<liferay-ui:message key="delete.component" />',
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