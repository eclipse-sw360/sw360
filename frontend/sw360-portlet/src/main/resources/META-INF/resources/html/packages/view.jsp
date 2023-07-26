<%--
  ~ Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.DateRange" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.Package" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.PackageManager" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<liferay-portlet:renderURL var="friendlyLicenseURL" portletName="sw360_portlet_licenses">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<liferay-portlet:renderURL var="friendlyReleaseURL" portletName="sw360_portlet_components">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.RELEASE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<portlet:resourceURL var="loadPackagesURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_PACKAGE_LIST%>'/>
</portlet:resourceURL>

<portlet:renderURL var="addPackageURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>
</portlet:renderURL>

<portlet:resourceURL var="deletePackageURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_PACKAGE%>'/>
</portlet:resourceURL>

<portlet:actionURL var="applyFiltersURL" name="applyFilters">
</portlet:actionURL>

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
                                <label for="package_name"><liferay-ui:message key='package.name' /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Package._Fields.NAME%>"
                                    value="<sw360:out value="${name}"/>" id="package_name">
                            </div>
                            <div class="form-group">
                                <label for="package_version"><liferay-ui:message key='package.version' /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Package._Fields.VERSION%>"
                                    value="<sw360:out value="${version}"/>" id="package_version">
                            </div>
                            <div class="form-group">
                                <label for="package_type"><liferay-ui:message key='package.manager' /></label>
                                <select class="form-control form-control-sm" id="package_type" name="<portlet:namespace/><%=Package._Fields.PACKAGE_MANAGER%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>" class="textlabel stackedLabel"></option>
                                    <sw360:DisplayEnumOptions type="<%=PackageManager.class%>" selectedName="${packageManager}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="license_ids"><liferay-ui:message key='license' /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Package._Fields.LICENSE_IDS%>"
                                    value="<sw360:out value="${licenseIds}"/>" id="license_ids">
                            </div>
                            <div class="form-group">
                                <label for="created_by"><liferay-ui:message key="created.by.email" /></label>
                                <input type="text" class="form-control form-control-sm"
                                    name="<portlet:namespace/><%=Package._Fields.CREATED_BY%>"
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
                                    name="<portlet:namespace/><%=Package._Fields.CREATED_ON%>" <core_rt:if test="${empty createdOn}"> style="display: none;" </core_rt:if>
                                    type="text" pattern="\d{4}-\d{2}-\d{2}" value="<sw360:out value="${createdOn}"/>" />
                                <label id="toLabel" <core_rt:if test="${empty endDate}"> style="display: none;" </core_rt:if> ><liferay-ui:message key="to" /></label>
                                <input type="text" id="endDate" class="datepicker form-control form-control-sm ml-0" autocomplete="off"
                                    name="<portlet:namespace/><%=PortalConstants.END_DATE%>" <core_rt:if test="${empty endDate}"> style="display: none;" </core_rt:if>
                                    value="<sw360:out value="${endDate}"/>" pattern="\d{4}-\d{2}-\d{2}" />
                            </div>
                            <div class="form-group">
                                <input class="form-check-input" type="checkbox" value="On" name="<portlet:namespace/><%=PortalConstants.ORPHAN_PACKAGE_CHECKBOX%>"
                                      <core_rt:if test="${orphanPackageCheckBox != ''}"> checked="checked"</core_rt:if> />
                                <label class="form-check-label" for="orphanPackage"><liferay-ui:message key="orphan.package" /></label>
                                <sup title="<liferay-ui:message key="packages.not.linked.to.any.release" />" type="button">
                                    <liferay-ui:icon icon="info-sign" />
                                </sup>
                            </div>
                            <div class="form-group">
                                <input class="form-check-input" type="checkbox" value="On" name="<portlet:namespace/><%=PortalConstants.EXACT_MATCH_CHECKBOX%>"
                                      <core_rt:if test="${exactMatchCheckBox != ''}"> checked="checked"</core_rt:if> />
                                <label class="form-check-label" for="exactMatch"><liferay-ui:message key="exact.match" /></label>
                                <sup title="<liferay-ui:message key="the.search.result.will.display.elements.exactly.matching.the.input.equivalent.to.using.x.around.the.search.keyword" /> <liferay-ui:message key="applied.on.package.name.and.version" />" type="button">
                                    <liferay-ui:icon icon="info-sign" />
                                </sup>
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
                            <button type="button" class="btn btn-primary" onclick="window.location.href='<%=addPackageURL%>'"><liferay-ui:message key="add.package" /></button>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="packages" />">
                    <liferay-ui:message key="packages" />
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <table id="packagesTable" class="table table-bordered"></table>
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
AUI().use('liferay-portlet-url', function () {
    var PortletURL = Liferay.PortletURL;

    require(['jquery', 'modules/autocomplete', 'modules/dialog', 'bridges/datatables', 'utils/render' ], function($, autocomplete, dialog, datatables, render) {

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

            if (selected === "<liferay-ui:message key="between" />" ) {
                $datePkr.show();
                $toLabel.show();
            } else {
                $("#created_on").show();
                $toLabel.hide();
                $("#endDate").hide().val("");
            }
        });

        var packagesTable = createPackagesTable();
        // create and render data table
        function createPackagesTable() {
            let columns = [
                {"title": "<liferay-ui:message key='package.name.with.version' />", data: "name", render: {display: renderPackageNameLink}, width: "30%"},
                {"title": "<liferay-ui:message key='release.name.with.version' />", data: "relName", defaultContent: "", render: {display: renderReleaseNameLink}, width: "25%"},
                {"title": "<liferay-ui:message key='release.clearing.state' />", data: "relCS", defaultContent: "", render: {display: renderReleaseCS}, width: "10%"},
                {"title": "<liferay-ui:message key='licenses' />", data: "lics", render: {display: renderLicenseLink}, width: "15%"},
                {"title": "<liferay-ui:message key='package.manager' />", data: "pkgMgr", width: "20%"},
                {"title": "<liferay-ui:message key='actions' />", data: 'DT_RowId', render: {display: renderPackageActions}, className: 'two actions', width: "5%"}
            ];
            let printColumns = [0, 1, 2, 3, 4],
                noSortColumns = [1, 2, 5];
            var packagesTable = datatables.create('#packagesTable', {
                // the following two parameters must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
                bServerSide: true,
                sAjaxSource: '<%=loadPackagesURL%>',
                columns: columns,
                columnDefs: [],
                initComplete: datatables.showPageContainer,
                language: {
                    url: "<liferay-ui:message key='datatables.lang' />",
                    loadingRecords: "<liferay-ui:message key='loading' />"
                },
                order: [[0, 'asc']]
            }, printColumns, noSortColumns);

            return packagesTable;
        }

        function renderReleaseCS(relCS, type, row) {
            var $state = $('<div>', {
                'class': 'content-center'
            });
            if (relCS) {
                let backgroundColour = getReleaseClearingStateBackgroundColour(relCS);
                var $csBox = $('<div>', {
                    'class': 'stateBox capsuleLeft capsuleRight ' + backgroundColour
                }).text('CS').attr("title", "<liferay-ui:message key='release.clearing.state' />: " + relCS );
                $state.append($csBox);
            } else {
                $state.text('<liferay-ui:message key="not.applicable" />');
            }
            return $state[0].outerHTML;
        }

        function getReleaseClearingStateBackgroundColour(relCS) {
            switch (relCS) {
                case 'Report approved': // -> green
                    return '<%=PortalConstants.CLEARING_STATE_CLOSED__CSS%>';
                case 'Report available': // -> blue
                    return 'bg-info';
                case 'Scan available': // -> orange
                case 'Sent to clearing tool':
                    return 'bg-primary';
                case 'New': // -> red
                    return '<%=PortalConstants.CLEARING_STATE_OPEN__CSS%>';
                case 'Under clearing': // -> yellow
                    return '<%=PortalConstants.CLEARING_STATE_INPROGRESS__CSS%>';
            }
            return '<%=PortalConstants.CLEARING_STATE_UNKNOWN__CSS%>';
        }

        function renderLicenseLink(lics, type, row) {
            var links = [],
                licensePortletURL = '<%=friendlyLicenseURL%>'
                .replace(/packages/g, "licenses");// DIRTY WORKAROUND

            for (var i = 0; i < lics.length; i++) {
                links[i] = render.linkTo(replaceFriendlyUrlParameter(licensePortletURL.toString(), lics[i], '<%=PortalConstants.PAGENAME_DETAIL%>'), lics[i]);
            }

            if (type == 'display') {
                return links.join(', ');
            } else if(type == 'print') {
                return lics.join(', ');
            } else if(type == 'type') {
                return 'string';
            } else {
                return lics.join(', ');
            }
        }

        function renderReleaseNameLink(releaseName, type, row, meta) {
            var releasePortletURL = '<%=friendlyReleaseURL%>'
                .replace(/packages/g, "components"); // DIRTY WORKAROUND
            if (releaseName) {
                return render.linkTo(replaceFriendlyUrlParameter(releasePortletURL.toString(), row.relId, '<%=PortalConstants.PAGENAME_RELEASE_DETAIL%>'), releaseName);
            } else {
                return '<liferay-ui:message key="no.linked.release" />';
            }
        }

        $('#packagesTable').on('click', 'svg.delete', function(event) {
            var data = $(event.currentTarget).data();
            deletePackage(data.packageId, data.packageName);
        });

        // Delete package action
        function deletePackage(id, name) {
            var $dialog;

            function deletePackageInternal(callback) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=deletePackageURL%>',
                    cache: false,
                    data: {
                        <portlet:namespace/><%=PortalConstants.PACKAGE_ID%>: id
                    },
                    success: function (data) {
                        callback();

                        if (data.result == 'SUCCESS') {
                            packagesTable.row('#' + id).remove().draw(false);
                            $dialog.info('<liferay-ui:message key="deleted.successfully" />!', true);
                            setTimeout(function() {
                                $dialog.close();
                            }, 5000);
                        } else if (data.result == 'IN_USE') {
                            $dialog.warning('<liferay-ui:message key="i.could.not.delete.the.package.since.it.is.used.by.another.project.please.unlink.it.before.deleting" />');
                        } else if (data.result == 'ACCESS_DENIED') {
                            $dialog.warning('<liferay-ui:message key="access.denied" />: <liferay-ui:message key="you.do.not.have.permission.to.delete.the.package" />!');
                        } else {
                            $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.package" />');
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.package" />');
                    }
                });
            }

                $dialog = dialog.confirm(
                    'danger',
                    'question-circle',
                    '<liferay-ui:message key="delete.package" />?',
                        '<p><liferay-ui:message key="do.you.really.want.to.delete.the.package.x" /></p>',
                    '<liferay-ui:message key="delete.package" />',
                    {
                        name: name
                    }, function(submit, callback) {
                        deletePackageInternal(callback);
                    }
                );
        }

        // helper functions
        function makePackageUrl(packageId, page) {
            var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                .setParameter('<%=PortalConstants.PAGENAME%>', page)
                .setParameter('<%=PortalConstants.PACKAGE_ID%>', packageId);
            return portletURL.toString();
        }

        function renderPackageActions(id, type, row) {
            var $actions = $('<div>', {
                'class': 'actions'
                }),
                $deleteAction = $('<svg>', {
                    'class': 'delete lexicon-icon',
                    'data-package-id': id,
                    'data-package-name': row.name,
                }),
                $editAction;

                if (row.writeAccess == true) {
                    $editAction = render.linkTo(
                            makePackageUrl(id, '<%=PortalConstants.PAGENAME_EDIT%>'),
                            "",
                            '<svg class="lexicon-icon"><title><liferay-ui:message key="edit.package" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>'
                    );
                    $deleteAction.append($('<title><liferay-ui:message key="delete.package" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));
                } else {
                    $editAction = $('<svg class="lexicon-icon disabled"><title><liferay-ui:message key="you.do.not.have.permission.to.edit.the.package" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>');
                    $deleteAction = ($('<svg class="lexicon-icon disabled"><title><liferay-ui:message key="you.do.not.have.permission.to.delete.the.package" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/></svg>'));
                }

            $actions.append($editAction, $deleteAction);
            return $actions[0].outerHTML;
        }

        function renderPackageNameLink(data, type, row) {
            return render.linkTo(makePackageUrl(row.DT_RowId, '<%=PortalConstants.PAGENAME_DETAIL%>'), row.name);
        }

        function replaceFriendlyUrlParameter(portletUrl, id, page) {
            return portletUrl
                .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
                .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', id);
        }
    });
});
</script>