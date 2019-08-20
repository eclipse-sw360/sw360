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
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.attachments.Attachment" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Release" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.portal.portlets.projects.ProjectPortlet" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.RequestedAction" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:actionURL var="updateComponentURL" name="updateComponent">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
</portlet:actionURL>

<portlet:renderURL var="addReleaseURL">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT_RELEASE%>"/>
</portlet:renderURL>

<portlet:actionURL var="deleteComponentURL" name="deleteComponent">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
</portlet:actionURL>

<portlet:actionURL var="deleteAttachmentsOnCancelURL" name='<%=PortalConstants.ATTACHMENT_DELETE_ON_CANCEL%>'>
</portlet:actionURL>

<portlet:resourceURL var="sw360ComponentUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.CODESCOOP_ACTION_COMPONENT%>'/>
</portlet:resourceURL>
<portlet:resourceURL var="sw360AutocompleteUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.CODESCOOP_ACTION_AUTOCOMPLETE%>'/>
</portlet:resourceURL>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="component" class="org.eclipse.sw360.datahandler.thrift.components.Component" scope="request"/>
    <jsp:useBean id="documentID" class="java.lang.String" scope="request"/>
    <jsp:useBean id="documentType" class="java.lang.String" scope="request"/>

    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>"
                 scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="usingComponents" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.components.Component>"
                 scope="request"/>
</c:catch>

<%--These variables are used as a trick to allow referencing enum values in EL expressions below--%>
<c:set var="WRITE" value="<%=RequestedAction.WRITE%>"/>
<c:set var="DELETE" value="<%=RequestedAction.DELETE%>"/>
<c:set var="hasWritePermissions" value="${component.permissions[WRITE]}"/>

<%@include file="/html/utils/includes/logError.jspf" %>
<core_rt:if test="${empty attributeNotFoundException}">
    <core_rt:set var="softwarePlatformsAutoC" value='<%=PortalConstants.SOFTWARE_PLATFORMS%>'/>
    <core_rt:set var="componentCategoriesAutocomplete" value='<%=PortalConstants.COMPONENT_CATEGORIES%>'/>

    <core_rt:set var="componentDivAddMode" value="${empty component.id}"/>

    <div class="container" style="display: none;">
        <div class="row">
            <div class="col-3 sidebar">
                <div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                    <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab">Summary</a>
                    <core_rt:if test="${not componentDivAddMode}" >
                        <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Releases'}">active</core_rt:if>" href="#tab-Releases" data-toggle="list" role="tab">Releases</a>
                        <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Attachments'}">active</core_rt:if>" href="#tab-Attachments" data-toggle="list" role="tab">Attachments</a>
                    </core_rt:if>
                </div>
            </div>
            <div class="col">
                <div class="row portlet-toolbar">
                    <div class="col-auto">
                        <div class="btn-toolbar" role="toolbar">
                            <div class="btn-group" role="group">
                                <core_rt:if test="${componentDivAddMode}" >
                                    <button type="button" id="formSubmit" class="btn btn-primary">Create Component</button>
                                </core_rt:if>

                                <core_rt:if test="${not componentDivAddMode}" >
                                    <button type="button" id="formSubmit" class="btn btn-primary">Update Component</button>
                                </core_rt:if>
                            </div>

                            <core_rt:if test="${not componentDivAddMode}" >
                                <div class="btn-group" role="group">
                                    <button id="deleteComponentButton" type="button" class="btn btn-danger"
                                        <core_rt:if test="${usingComponents.size()>0 or usingProjects.size()>0}"> disabled="disabled" title="Deletion is disabled as the component is used." </core_rt:if>
                                        <core_rt:if test="${component.releasesSize>0}"> disabled="disabled" title="Deletion is disabled as the component contains releases." </core_rt:if>
                                    >Delete Component</button>
                                </div>
                            </core_rt:if>

                            <div class="btn-group" role="group">
                                <button id="cancelEditButton" type="button" class="btn btn-light">Cancel</button>
                            </div>
                        </div>
                    </div>
                    <div class="col portlet-title text-truncate" title="${sw360:printComponentName(component)}">
                        <sw360:out value="${component.name}"/>
                    </div>
                </div>
                <div class="row">
                    <div class="col">
                        <form  id="componentEditForm" name="componentEditForm" action="<%=updateComponentURL%>" class="needs-validation" method="post" novalidate
                            data-delete-url="<%=deleteComponentURL%>"
                            data-component-name="${component.name}"
                            data-comment-parameter-name="<%=PortalConstants.MODERATION_REQUEST_COMMENT%>"
                            data-attachments="${component.attachmentsSize}"
                        >
                            <div class="tab-content">
                                <div id="tab-Summary" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Summary'}">active show</core_rt:if>" >
                                    <%@include file="/html/components/includes/components/editBasicInfo.jspf" %>

                                    <core_rt:set var="externalIdsSet" value="${component.externalIds.entrySet()}"/>
                                    <core_rt:set var="externalIdKeys" value="<%=PortalConstants.COMPONENT_EXTERNAL_ID_KEYS%>"/>
                                    <%@include file="/html/utils/includes/editExternalIds.jsp" %>

                                    <core_rt:set var="additionalDataSet" value="${component.additionalData.entrySet()}"/>
                                    <%@include file="/html/utils/includes/editAdditionalData.jsp" %>

                                    <core_rt:set var="documentName"><sw360:out value='${component.name}'/></core_rt:set>
                                    <%@include file="/html/utils/includes/usingProjectsTable.jspf" %>
                                    <%@include file="/html/utils/includes/usingComponentsTable.jspf"%>
                                </div>
                                <div id="tab-Releases" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Releases'}">active show</core_rt:if>" >
                                    <table class="table table-bordered">
                                        <thead>
                                            <tr>
                                                <th>Name</th>
                                                <th>Version</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <core_rt:forEach items="${component.releases}" var="myRelease">
                                                <tr>
                                                    <td><a href="${myRelease.id}">${myRelease.name}</a></td>
                                                    <td>${myRelease.version}</td>
                                                </tr>
                                            </core_rt:forEach>
                                        </tbody>
                                    </table>
                                    <button type="button" class="btn btn-secondary mt-3" onclick="window.location.href='<%=addReleaseURL%>'">Add Release</button>
                                </div>
                                <core_rt:if test="${not componentDivAddMode}" >
                                    <div id="tab-Attachments" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Attachments'}">active show</core_rt:if>">
                                        <%@include file="/html/utils/includes/editAttachments.jspf" %>
                                    </div>
                                </core_rt:if>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <%@ include file="/html/utils/includes/pageSpinner.jspf" %>

    <div class="dialogs auto-dialogs">
        <div id="deleteComponentDialog" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            <clay:icon symbol="question-circle" />
                            Delete Component?
                        </h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <p>Do you really want to delete the component <b data-name="name"></b>?</p>
                        <div data-hide="hasNoAttachments">
                            <p>
                                This component <span data-name="name"></span> contains <b><span data-name="attachments"></span></b> attachments.
                            </p>
                        </div>
                        <hr/>
                        <form>
                            <div class="form-group">
                                <label for="deleteComponentDialogComment">Please comment your changes</label>
                                <textarea id="deleteComponentDialogComment" class="form-control" data-name="comment" rows="4" placeholder="Comment your request..."></textarea>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-danger">Delete Component</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <c:if test="${codescoopActive}">
        <script>
            document.addEventListener("DOMContentLoaded", function() {
                require(['modules/codeScoop' ], function(codeScoop) {
                    var api = new codeScoop();
                    api.activateAutoFill("<%=sw360ComponentUrl%>", "<%=sw360AutocompleteUrl%>");
                });
            });
        </script>
    </c:if>

    <jsp:include page="/html/utils/includes/searchAndSelectUsers.jsp" />
    <jsp:include page="/html/utils/includes/searchUsers.jsp" />
    <%@include file="/html/components/includes/vendors/searchVendor.jspf" %>


    <script>
    require(['jquery', 'components/includes/vendors/searchVendor', 'modules/autocomplete', 'modules/dialog', 'modules/listgroup', 'modules/validation' ], function($, vendorsearch, autocomplete, dialog, listgroup, validation) {
        document.title = "${component.name} - " + document.title;

        listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');

        validation.enableForm('#componentEditForm');
        validation.jumpToFailedTab('#componentEditForm');

        autocomplete.prepareForMultipleHits('comp_platforms', ${softwarePlatformsAutoC});
        autocomplete.prepareForMultipleHits('comp_categories', ${componentCategoriesAutocomplete});

        $('#formSubmit').click(
            function () {
                <core_rt:choose>
                    <core_rt:when test="${componentDivAddMode || component.permissions[WRITE]}">
                        $('#componentEditForm').submit();
                    </core_rt:when>
                    <core_rt:otherwise>
                        showCommentDialog();
                    </core_rt:otherwise>
                </core_rt:choose>
            }
        );
        $('#cancelEditButton').on('click', cancel);
        $('#deleteComponentButton').on('click', deleteComponent);

        $('#ComponentGeneralInfo input.edit-vendor').on('click', function() {
            vendorsearch.openSearchDialog('<portlet:namespace/>what', '<portlet:namespace/>where',
                    '<portlet:namespace/>FULLNAME', '<portlet:namespace/>SHORTNAME', '<portlet:namespace/>URL', fillVendorInfo);
        });

        function cancel() {
            $.ajax({
                type: 'POST',
                url: '<%=deleteAttachmentsOnCancelURL%>',
                cache: false,
                data: {
                    "<portlet:namespace/><%=PortalConstants.DOCUMENT_ID%>": "${component.id}"
                }
            }).always(function() {
                var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
                    portletURL = Liferay.PortletURL.createURL(baseUrl)
                <core_rt:choose>
                    <core_rt:when test="${not componentDivAddMode}">
                            .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
                            .setParameter('<%=PortalConstants.COMPONENT_ID%>', '${component.id}');
                    </core_rt:when>
                    <core_rt:otherwise>
                            .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>')
                    </core_rt:otherwise>
                </core_rt:choose>
                window.location.href = portletURL.toString() + window.location.hash;
            });
        }

        function deleteComponent() {
            var $dialog,
                data = $('#componentEditForm').data(),
                name = data.componentName,
                attachmentsSize = data.attachments;

            function deleteComponentInternal() {
                var baseUrl = data.deleteUrl,
                    deleteURL = Liferay.PortletURL.createURL( baseUrl ).setParameter(data.commentParameterName, btoa($("#moderationDeleteCommentField").val()));
                window.location.href = deleteURL;
            }

            $dialog = dialog.open('#deleteComponentDialog', {
                name: name,
                attachments: attachmentsSize,
                hasNoAttachments: attachmentsSize == 0
            }, function(submit, callback) {
                deleteComponentInternal();
            });
        }

        function showCommentDialog() {
            var $dialog;

            // validate first to be sure that form can be submitted
            if(!validation.validate('#componentEditForm')) {
                return;
            }

            $dialog = dialog.confirm(
                null,
                'pencil',
                'Create moderation request',
                '<form>' +
                    '<div class="form-group">' +
                        '<label for="moderationRequestCommentField">Please comment your changes</label>' +
                        '<textarea form="componentEditForm" name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="form-control" placeholder="Leave a comment on your request" data-name="comment" autofocus></textarea>' +
                    '</div>' +
                '</form>',
                'Send moderation request',
                {
                    comment: ''
                },
                function() {
                    $('#componentEditForm').submit();
                }
            );
        }

        // vendor handling
        function fillVendorInfo(vendorInfo) {
            var beforeComma = vendorInfo.substr(0, vendorInfo.indexOf(","));
            var afterComma = vendorInfo.substr(vendorInfo.indexOf(",") + 1);

            $('#<%=Component._Fields.DEFAULT_VENDOR_ID.toString()%>').val(beforeComma.trim());
            $('#<%=Component._Fields.DEFAULT_VENDOR_ID.toString()%>Display').val(afterComma.trim());
        }
    });
    </script>
</core_rt:if>