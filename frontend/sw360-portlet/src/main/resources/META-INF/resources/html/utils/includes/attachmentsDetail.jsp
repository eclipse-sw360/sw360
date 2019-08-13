<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>

<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<core_rt:catch var="attributeNotFoundException">
    <jsp:useBean id="attachments" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment>" scope="request" />
    <jsp:useBean id="attachmentUsages" type="java.util.Map<java.lang.String, java.util.List<org.eclipse.sw360.datahandler.thrift.projects.Project>>" scope="request" />
    <jsp:useBean id="attachmentUsagesRestrictedCounts" type="java.util.Map<java.lang.String, java.lang.Long>" scope="request" />
    <jsp:useBean id="documentType" type="java.lang.String" scope="request" />
    <jsp:useBean id="documentID" class="java.lang.String" scope="request" />
</core_rt:catch>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">

    <core_rt:if test="${empty attachments}">
        <div class="alert alert-info" role="alert">
            No attachments yet.
        </div>

        <script>
            require(['jquery'], function($) {
                $('#downloadAttachmentBundle').hide();
            });
        </script>
    </core_rt:if>

    <core_rt:if test="${not empty attachments}">
        <table id="attachmentsDetail" class="table table-bordered" title="Attachment Information">
            <colgroup>
                <col />  <!-- set by class -->
                <col style="width: 25%;" />
                <col style="width: 10%;" />
                <col style="width: 8%;" />
                <col style="width: 8%;" />
                <col style="width: 16%;" />
                <col style="width: 11%;" />
                <col style="width: 16%;" />
                <col style="width: 6%;" />
                <col /> <!-- set by class -->
            </colgroup>
            <thead>
                <tr>
                    <th class="one action">
                        <sw360:DisplayDownloadAttachmentBundle attachments="${attachments}"
                            name="AttachmentBundle.zip"
                            contextType="${documentType}"
                            contextId="${documentID}" />
                    </th>
                    <th>File name</th>
                    <th>Size</th>
                    <th>Type</th>
                    <th>Group</th>
                    <th>Uploaded by</th>
                    <th>Group</th>
                    <th>Checked by</th>
                    <th>Usages</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>

        <script>
            require(['jquery', 'bridges/datatables', 'modules/dialog' ], function($, datatables, dialog) {
                var attachmentJSON = [];
                var usageLinks;

                /* Print all attachment table data as array into the html page */
                <core_rt:forEach items="${attachments}" var="attachment">
                    usageLinks = [];
                    <core_rt:forEach items="${attachmentUsages[attachment.attachmentContentId]}" var="project">
                    usageLinks.push("<sw360:DisplayProjectLink project="${project}"/>");
                    </core_rt:forEach>
                    attachmentJSON.push({
                        "fileName": "<sw360:out value="${attachment.filename}"/>",
                        "size": "n/a",
                        "type": "<sw360:DisplayEnumShort value="${attachment.attachmentType}"/>",
                        "uploadedTeam": "<sw360:DisplayEllipsisString value="${attachment.createdTeam}"/>",
                        "uploadedBy": "<sw360:DisplayEllipsisString value="${attachment.createdBy}"/>",
                        "checkedTeam":  "<sw360:DisplayEllipsisString value="${attachment.checkedTeam}"/>",
                        "checkedBy":  "<sw360:DisplayEllipsisString value="${attachment.checkedBy}"/>",
                        "usage":  {links: usageLinks, restrictedCount: ${attachmentUsagesRestrictedCounts.getOrDefault(attachment.attachmentContentId, 0)}},
                        "actions":     "<div class=\"actions\"><sw360:DisplayDownloadAttachmentFile attachment="${attachment}" contextType="${documentType}" contextId="${documentID}"/></div>",
                        "sha1": "<sw360:out value="${attachment.sha1}"/>",
                        "uploadedOn": "<sw360:out value="${attachment.createdOn}"/>",
                        "uploadedComment": "<core_rt:if test="${not empty attachment.createdComment}">Comment: <sw360:DisplayEllipsisString value="${attachment.createdComment}"/></core_rt:if>",
                        "checkedOn": "<sw360:out value="${attachment.checkedOn}"/>",
                        "checkedComment": "<core_rt:if test="${not empty attachment.checkedComment}">Comment: <sw360:DisplayEllipsisString value="${attachment.checkedComment}"/></core_rt:if>",
                        "checkStatus": "<sw360:out value="${attachment.checkStatus}"/>"
                    });
                </core_rt:forEach>

                /* register listener to toolbar button */
                $('#downloadAttachmentBundle').on('click', function() {
                    window.location = $('#attachmentsDetail th:first a').attr('href');
                });

                /* create table */
                var table = datatables.create('#attachmentsDetail', {
                    "data": attachmentJSON,
                    "columns": [
                        {
                            "className":      'one action details-control',
                            "orderable":      false,
                            "data":           null,
                            "defaultContent": ''
                        },
                        { "data": "fileName" },
                        { "data": "size" },
                        { "data": "type" },
                        { "data": "uploadedTeam" },
                        { "data": "uploadedBy" },
                        { "data": "checkedTeam" },
                        { "data": "checkedBy" },
                        { "data": "usage", "render": renderAttachmentUsages, orderable: false, className: 'text-center' },
                        { "data": "actions", className: "one action", orderable: false }
                    ],
                    "columnDefs": [
                        {
                            "targets": [ 6, 7 ],
                            "createdCell": function (td, cellData, rowData, row, col) {
                                if (rowData.checkStatus === 'REJECTED') {
                                    $(td).addClass('text-danger');
                                } else if (rowData.checkStatus === 'ACCEPTED') {
                                    $(td).addClass('text-success');
                                }
                            }
                        },
                        {
                            "targets": [8],
                            "createdCell": function (td, cellData, rowData, row, col) {
                                $(td).on('click', 'a', function() {
                                    var dialogContent = '';
                                    dialogContent += rowData.usage.links.join(", ");
                                    if (rowData.usage.restrictedCount > 0){
                                        if (rowData.usage.links.length > 0) {
                                            dialogContent += ", and ";
                                        }
                                        dialogContent += rowData.usage.restrictedCount + " restricted project(s)";
                                    }
                                    dialog.info(
                                        'Projects using this attachment',
                                        dialogContent
                                    );
                                });
                            }
                        }
                    ],
                    "order": [[1, 'asc']]
                } );

                /* Add event listener for opening and closing details as child row */
                $('#attachmentsDetail tbody').on('click', 'td.details-control', function () {
                    var tr = $(this).closest('tr');
                    var row = table.row( tr );

                    if ( row.child.isShown() ) {
                        row.child.hide();
                        tr.removeClass('shown');
                    } else {
                        row.child( createChildRow(row.data()) ).show();
                        tr.addClass('shown');
                    }
                } );

                /* Define function for child row creation, which will contain additional data for a clicked table row */
                function createChildRow(rowData) {
                    var childHtmlString = '' +
                            '<div>' +
                                '<span class="dataTableChildRowCell" style="padding-right: 10px; width:  4%;"/>' +
                                '<span class="dataTableChildRowCell" style="padding-right: 50px; width: 36%;">' + rowData.sha1 + '</span>' +
                                '<span class="dataTableChildRowCell" style="padding-right: 30px; width: 22%;">' + rowData.uploadedOn + ' ' + rowData.uploadedComment + '</span>';
                    if (rowData.checkStatus === 'ACCEPTED') {
                        childHtmlString += '' +
                                '<span class="dataTableChildRowCell foregroundOK" style="padding-right: 30px; width: 22%;">' + rowData.checkedOn + ' ' + rowData.checkedComment + '</span>';
                    } else if (rowData.checkStatus === 'REJECTED') {
                        childHtmlString += '' +
                                '<span class="dataTableChildRowCell foregroundAlert" style="padding-right: 30px; width: 22%;">' + rowData.checkedOn + ' ' + rowData.checkedComment + '</span>';
                    } else {
                        childHtmlString += '' +
                                '<span class="dataTableChildRowCell" style="padding-right: 30px; width: 22%;">' + rowData.checkedOn + ' ' + rowData.checkedComment + '</span>';
                    }
                    childHtmlString += '' +
                                '<span class="dataTableChildRowCell" style="padding-right: 30px; width: 16%;"/>'+
                            '</div>';
                    return childHtmlString;
                }

                function renderAttachmentUsages(data, type, row, meta) {
                    if (type === 'display') {
                        var usagesHtml = '';
                        if (data.links.length === 0 && data.restrictedCount === 0) {
                            usagesHtml += 'n/a';
                        } else {
                            usagesHtml += '<a href="javascript:;" title="visible / restricted">' + data.links.length + ' / ' + data.restrictedCount + '</a>';
                        }
                        return usagesHtml;
                    } else if(type === 'type') {
                        return 'string';
                    } else {
                        return null;
                    }
                }

            });
        </script>
    </core_rt:if>
</core_rt:if>
