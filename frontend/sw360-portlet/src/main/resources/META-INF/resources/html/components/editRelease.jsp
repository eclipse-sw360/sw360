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
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ComponentType" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Release" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.RequestedAction" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%-- use require js on this page --%>
<%@include file="/html/utils/includes/requirejs.jspf" %>


<portlet:actionURL var="updateReleaseURL" name="updateRelease">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
    <portlet:param name="<%=PortalConstants.RELEASE_ID%>" value="${release.id}"/>
</portlet:actionURL>

<portlet:actionURL var="deleteReleaseURL" name="deleteRelease">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
    <portlet:param name="<%=PortalConstants.RELEASE_ID%>" value="${release.id}"/>
</portlet:actionURL>

<portlet:actionURL var="deleteAttachmentsOnCancelURL" name='<%=PortalConstants.ATTACHMENT_DELETE_ON_CANCEL%>'>
</portlet:actionURL>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="component" class="org.eclipse.sw360.datahandler.thrift.components.Component" scope="request"/>
    <jsp:useBean id="release" class="org.eclipse.sw360.datahandler.thrift.components.Release" scope="request"/>

    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>"
                 scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="usingComponents" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.components.Component>" scope="request"/>

    <core_rt:set var="programmingLanguages" value='<%=PortalConstants.PROGRAMMING_LANGUAGES%>'/>
    <core_rt:set var="operatingSystemsAutoC" value='<%=PortalConstants.OPERATING_SYSTEMS%>'/>
    <core_rt:set var="platformsAutoC" value='<%=PortalConstants.SOFTWARE_PLATFORMS%>'/>

    <core_rt:set var="addMode" value="${empty release.id}"/>
    <core_rt:set var="cotsMode" value="<%=component.componentType == ComponentType.COTS%>"/>

    <jsp:useBean id="isUserAtLeastClearingAdmin" type="java.lang.Boolean" scope="request" />
    <core_rt:set var="mainlineStateEnabledForUserRole" value='<%=PortalConstants.MAINLINE_STATE_ENABLED_FOR_USER%>'/>
</c:catch>

<%--These variables are used as a trick to allow referencing enum values in EL expressions below--%>
<c:set var="WRITE" value="<%=RequestedAction.WRITE%>"/>
<c:set var="DELETE" value="<%=RequestedAction.DELETE%>"/>
<c:set var="hasWritePermissions" value="${release.permissions[WRITE]}"/>

<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">

    <div class="container" style="display: none;">
        <div class="row">
            <div class="col-3 sidebar">
                <div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                    <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab">Summary</a>
                    <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-linkedReleases'}">active</core_rt:if>" href="#tab-linkedReleases" data-toggle="list" role="tab">Linked Releases</a>

                    <core_rt:if test="${not addMode}" >
                        <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-ClearingDetails'}">active</core_rt:if>" href="#tab-ClearingDetails" data-toggle="list" role="tab">Clearing Details</a>
                        <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-ECCDetails'}">active</core_rt:if>" href="#tab-ECCDetails" data-toggle="list" role="tab">ECC Details</a>
                        <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Attachments'}">active</core_rt:if>" href="#tab-Attachments" data-toggle="list" role="tab">Attachments</a>
                    </core_rt:if>
                    <core_rt:if test="${cotsMode}">
                        <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-CommercialDetails'}">active</core_rt:if>" href="#tab-CommercialDetails" data-toggle="list" role="tab">Commercial Details</a>
                    </core_rt:if>
                </div>
            </div>
            <div class="col">
                <div class="row portlet-toolbar">
                    <div class="col-auto">
                        <div class="btn-toolbar" role="toolbar">
                            <div class="btn-group" role="group">
                                <core_rt:if test="${addMode}" >
                                    <button type="button" id="formSubmit" class="btn btn-primary">Create Release</button>
                                </core_rt:if>

                                <core_rt:if test="${not addMode}" >
                                    <button type="button" id="formSubmit" class="btn btn-primary">Update Release</button>
                                </core_rt:if>
                            </div>

                            <core_rt:if test="${not addMode}" >
                                <div class="btn-group" role="group">
                                    <button id="deleteReleaseButton" type="button" class="btn btn-danger"
                                        <core_rt:if test="${usingComponents.size()>0 or usingProjects.size()>0}"> disabled="disabled" title="Deletion is disabled as the release is used." </core_rt:if>
                                    >Delete Release</button>
                                </div>
                            </core_rt:if>

                            <div class="btn-group" role="group">
                                <button id="cancelEditButton" type="button" class="btn btn-light">Cancel</button>
                            </div>
                        </div>
                    </div>
                    <div class="col portlet-title text-truncate" title="<sw360:out value="${release.name}"/> <sw360:out value="${release.version}" />">
                        <sw360:out value="${release.name}"/> <sw360:out value="${release.version}" />
                    </div>
                </div>
                <div class="row">
                    <div class="col">
                        <form  id="releaseEditForm" name="releaseEditForm" action="<%=updateReleaseURL%>" class="needs-validation" method="post" novalidate
                            data-name="<sw360:ReleaseName release="${release}" />"
                            data-delete-url="<%= deleteReleaseURL %>"
                            data-linked-releases="${release.releaseIdToRelationshipSize}"
                            data-attachments="${release.attachmentsSize}"
                        >
                            <div class="tab-content">
                                <div id="tab-Summary" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Summary'}">active show</core_rt:if>" >
                                    <%@include file="/html/components/includes/releases/editReleaseInformation.jspf" %>

                                    <core_rt:set var="keys" value="<%=PortalConstants.RELEASE_ROLES%>"/>
                                    <core_rt:set var="mapTitle" value="Additional Roles"/>
                                    <core_rt:set var="inputType" value="email"/>
                                    <core_rt:set var="inputSubtitle" value="Enter mail address"/>
                                    <core_rt:set var="customMap" value="${release.roles}"/>
                                    <%@include file="/html/utils/includes/mapEdit.jspf" %>

                                    <core_rt:set var="externalIdsSet" value="${release.externalIds.entrySet()}"/>
                                    <core_rt:set var="externalIdKeys" value="<%=PortalConstants.RELEASE_EXTERNAL_ID_KEYS%>"/>
                                    <%@include file="/html/utils/includes/editExternalIds.jsp" %>

                                    <core_rt:set var="additionalDataSet" value="${release.additionalData.entrySet()}"/>
                                    <%@include file="/html/utils/includes/editAdditionalData.jsp" %>

                                    <%@include file="/html/components/includes/releases/editReleaseRepository.jspf" %>
                                </div>
                                <div id="tab-linkedReleases" class="tab-pane <core_rt:if test="${selectedTab == 'tab-linkedReleases'}">active show</core_rt:if>" >
                                    <%@include file="/html/utils/includes/editLinkedReleases.jspf" %>
                                </div>
                                <div id="tab-ClearingDetails" class="tab-pane <core_rt:if test="${selectedTab == 'tab-ClearingDetails'}">active show</core_rt:if>" >
                                    <%@include file="/html/components/includes/releases/editReleaseClearingInformation.jspf" %>
                                </div>
                                <div id="tab-ECCDetails" class="tab-pane <core_rt:if test="${selectedTab == 'tab-ECCDetails'}">active show</core_rt:if>" >
                                    <%@include file="/html/components/includes/releases/editReleaseECCInformation.jspf" %>
                                </div>
                                <core_rt:if test="${not addMode}" >
                                    <div id="tab-Attachments" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Attachments'}">active show</core_rt:if>">
                                        <%@include file="/html/utils/includes/editAttachments.jspf" %>
                                    </div>
                                </core_rt:if>
                                 <core_rt:if test="${cotsMode}">
                                    <div id="tab-CommercialDetails" class="tab-pane <core_rt:if test="${selectedTab == 'tab-CommercialDetails'}">active show</core_rt:if>">
                                        <%@include file="/html/components/includes/releases/editCommercialDetails.jspf" %>
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
        <div id="deleteReleaseDialog" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            <clay:icon symbol="question-circle" />
                            Delete Release?
                        </h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <p>Do you really want to delete the release <b data-name="name"></b>?</p>
                        <div data-hide="hasNoDependencies">
                            <p>
                                This release <span data-name="name"></span> contains:
                            </p>
                            <ul>
                                <li data-hide="hasNoLinkedReleases"><span data-name="linkedReleases"></span> linked releases</li>
                                <li data-hide="hasNoAttachments"><span data-name="attachments"></span> attachments</li>
                            </ul>
                        </div>
                        <hr/>
                        <form>
                            <div class="form-group">
                                <label for="deleteReleaseDialogComment">Please comment your changes</label>
                                <textarea id="deleteReleaseDialogComment" class="form-control" data-name="comment" rows="4" placeholder="Comment your request..."></textarea>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-danger">Delete Release</button>
                    </div>
                </div>
            </div>
        </div>
    </div>


    <jsp:include page="/html/utils/includes/searchLicenses.jsp" />
    <jsp:include page="/html/utils/includes/searchAndSelectLicenses.jsp" />

    <jsp:include page="/html/utils/includes/searchUsers.jsp" />
    <jsp:include page="/html/utils/includes/searchAndSelectUsers.jsp" />

    <%@include file="/html/components/includes/vendors/searchVendor.jspf" %>

    <core_rt:set var="enableSearchForReleasesFromLinkedProjects" value="${false}" scope="request"/>
    <jsp:include page="/html/utils/includes/searchReleases.jsp" />
</core_rt:if>

<c:if test="${codescoopActive}">
    <script>
        var sw360Purl = "${componentpurl}";
    </script>
</c:if>

<script>
    require(['jquery', 'components/includes/vendors/searchVendor', 'modules/autocomplete', 'modules/dialog', 'modules/listgroup', 'modules/validation' ], function($, vendorsearch, autocomplete, dialog, listgroup, validation) {
        document.title = "${component.name} - " + document.title;

        listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');

        validation.enableForm('#releaseEditForm');
        validation.jumpToFailedTab('#releaseEditForm');

        autocomplete.prepareForMultipleHits('programminglanguages', ${programmingLanguages});
        autocomplete.prepareForMultipleHits('op_systems', ${operatingSystemsAutoC});
        autocomplete.prepareForMultipleHits('platformsTB', ${platformsAutoC});

        $('#formSubmit').click(
            function() {
                <core_rt:choose>
                    <core_rt:when test="${addMode || release.permissions[WRITE]}">
                        $('#releaseEditForm').submit();
                    </core_rt:when>
                    <core_rt:otherwise>
                        showCommentDialog();
                    </core_rt:otherwise>
                </core_rt:choose>
            }
        );
        $('#cancelEditButton').on('click', cancel);
        $('#deleteReleaseButton').on('click', deleteRelease);

        $('#ComponentBasicInfo input.edit-vendor').on('click', function() {
            vendorsearch.openSearchDialog('<portlet:namespace/>what', '<portlet:namespace/>where',
                      '<portlet:namespace/>FULLNAME', '<portlet:namespace/>SHORTNAME', '<portlet:namespace/>URL', fillVendorInfo);
        });

        function cancel() {
            $.ajax({
                type: 'POST',
                url: '<%=deleteAttachmentsOnCancelURL%>',
                cache: false,
                data: {
                    "<portlet:namespace/><%=PortalConstants.DOCUMENT_ID%>": "${release.id}"
                }
            }).always(function() {
                var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
                    portletURL = Liferay.PortletURL.createURL(baseUrl)
                        .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
                        .setParameter('<%=PortalConstants.COMPONENT_ID%>', '${component.id}');
                window.location.href = portletURL.toString() + window.location.hash;
            });
        }

        function deleteRelease() {
            var $dialog,
                data = $('#releaseEditForm').data(),
                linkedReleases = data.linkedReleases,
                attachmentsSize = data.attachments;

            function deleteReleaseInternal() {
                var baseUrl = data.deleteUrl,
                    deleteURL = Liferay.PortletURL.createURL( baseUrl ).setParameter(data.commentParameterName, btoa($("#moderationDeleteCommentField").val()));
                window.location.href = deleteURL;
            }

            $dialog = dialog.open('#deleteReleaseDialog', {
                name: data.name,
                attachments: attachmentsSize,
                hasNoAttachments: attachmentsSize == 0,
                linkedReleases: linkedReleases,
                hasNoLinkedReleases: linkedReleases == 0,
                hasNoDependencies: attachmentsSize == 0 && linkedReleases == 0
            }, function(submit, callback) {
                deleteReleaseInternal();
            });
        }

        function showCommentDialog() {
            var $dialog;

            // validate first to be sure that form can be submitted
            if(!validation.validate('#releaseEditForm')) {
                return;
            }

            $dialog = dialog.confirm(
                null,
                'pencil',
                'Create moderation request',
                '<form>' +
                    '<div class="form-group">' +
                        '<label for="moderationRequestCommentField">Please comment your changes</label>' +
                        '<textarea form=releaseEditForm name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="form-control" placeholder="Leave a comment on your request" data-name="comment" autofocus></textarea>' +
                    '</div>' +
                '</form>',
                'Send moderation request',
                {
                    comment: ''
                },
                function() {
                    $('#releaseEditForm').submit();
                }
            );
        }

        function fillVendorInfo(vendorInfo) {
            var beforeComma = vendorInfo.substr(0, vendorInfo.indexOf(","));
            var afterComma = vendorInfo.substr(vendorInfo.indexOf(",") + 1);

            $('#<%=Release._Fields.VENDOR_ID.toString()%>').val(beforeComma.trim());
            $('#<%=Release._Fields.VENDOR_ID.toString()%>Display').val(afterComma.trim());
        }
    });
</script>
