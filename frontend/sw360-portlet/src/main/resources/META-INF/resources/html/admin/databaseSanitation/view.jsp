<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  - With contributions by Bosch Software Innovations GmbH, 2016-2017.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="getDuplicatesURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DUPLICATES%>'/>
</portlet:resourceURL>

<div class="container">
    <div class="row portlet-toolbar">
        <div class="col-auto">
            <div class="btn-toolbar" role="toolbar">
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-primary" data-action="search-duplicates">Search duplicate identifiers</button>
                </div>
            </div>
        </div>
        <div class="col portlet-title text-truncate" title="Database Administration">
            Database Administration
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div id="duplicateSearch">
                <div class="alert alert-info" data-type="progress" style="display: none;">
                    <p>Searching for duplicate identifiers...</p>
                    <div class="progress">
                        <div class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100" style="width: 100%"></div>
                    </div>
                </div>
                <div class="alert" data-type="result" style="display: none;">
                </div>

                <div data-type="tables">
                </div>
            </div>
        </div>
    </div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables' ], function($, datatables) {
        $('.portlet-toolbar button[data-action="search-duplicates"]').on("click", function() {
            var $container = $('#duplicateSearch'),
                $tables = $container.find('div[data-type="tables"]'),
                $progress = $container.find('.alert[data-type="progress"]'),
                $result = $container.find('.alert[data-type="result"]');

            $tables.html('');
            $progress.show();

            $result.hide();
            $result.removeClass('alert-danger');
            $result.removeClass('alert-warning');
            $result.removeClass('alert-success');

            jQuery.ajax({
                type: 'POST',
                url: '<%=getDuplicatesURL%>',
                cache: false,
                data: "",
                success: function (data) {
                    $progress.hide();

                    if (data.result == 'SUCCESS') {
                        $result.addClass('alert-success');
                        $result.text("No duplicate identifiers were found");
                    } else if (data.result == 'FAILURE') {
                        $result.addClass('alert-danger');
                        $result.text("Error in looking for duplicate identifiers");
                    } else {
                        $result.addClass('alert-warning');
                        $result.text('The following duplicate identifiers were found.');
                        $tables.append(data);
                    }
                    $result.show();

                    setupDatatables('#duplicateReleasesTable');
                    setupDatatables('#duplicateReleaseSourcesTable');
                    setupDatatables('#duplicateComponentsTable');
                    setupDatatables('#duplicateProjectsTable');
                },
                error: function () {
                    $progress.hide();

                    $result.addClass('alert-danger');
                    $result.text("Error in looking for duplicate identifiers");
                }
            });
        });

        function setupDatatables(tableId) {
            if($(tableId)) {
                datatables.create(tableId);
            }
        }
    });
</script>
