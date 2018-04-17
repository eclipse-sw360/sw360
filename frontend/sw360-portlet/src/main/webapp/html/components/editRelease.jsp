<%--
  ~ Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
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

    <core_rt:set var="addMode" value="${empty release.id}"/>
    <core_rt:set var="cotsMode" value="<%=component.componentType == ComponentType.COTS%>"/>
</c:catch>

<%--These variables are used as a trick to allow referencing enum values in EL expressions below--%>
<c:set var="WRITE" value="<%=RequestedAction.WRITE%>"/>
<c:set var="DELETE" value="<%=RequestedAction.DELETE%>"/>
<c:set var="hasWritePermissions" value="${release.permissions[WRITE]}"/>

<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">

    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">

    <div id="header"></div>
    <p class="pageHeader"><span class="pageHeaderBigSpan"><sw360:out value="${component.name}"/>: <sw360:ReleaseName release="${release}" /> Edit</span>
        <span class="pull-right">
                   <core_rt:if test="${not addMode}">
                       <input type="button" class="addButton delete-release"
                               data-release-name="<sw360:ReleaseName release="${release}" />"
                               data-linked-releases="${release.releaseIdToRelationshipSize}"
                               data-attachments="${release.attachmentsSize}"
                               value="Delete <sw360:ReleaseName release="${release}"/>"
                            <core_rt:if test="${usingComponents.size()>0 or usingProjects.size()>0}"> disabled="disabled" title="Deletion is disabled as the release is used." </core_rt:if>
                       />
                   </core_rt:if>
    </span>
    </p>

    <div id="content">
        <div class="container-fluid">
            <form id="releaseEditForm" name="releaseEditForm" action="<%=updateReleaseURL%>" method="post">
                <div id="myTab" class="row-fluid">
                    <ul class="nav nav-tabs span2">
                        <li><a href="#tab-ReleaseInformation">Summary</a></li>
                        <li><a href="#tab-ReleaseLinks">Linked Releases</a></li>
                        <core_rt:if test="${not addMode}">
                            <li><a href="#tab-ReleaseClearingInformation">Clearing Details</a></li>
                            <li><a href="#tab-ReleaseECCInformation">ECC Details</a></li>
                            <li><a href="#tab-Attachments">Attachments</a></li>
                        </core_rt:if>
                        <core_rt:if test="${cotsMode}">
                            <li><a href="#tab-COTSDetails">Commercial Details</a></li>
                        </core_rt:if>
                    </ul>
                    <div class="tab-content span10">
                        <div id="tab-ReleaseInformation" class="tab-pane">
                            <%@include file="/html/components/includes/releases/editReleaseInformation.jspf" %>
                            <core_rt:set var="keys" value="<%=PortalConstants.RELEASE_ROLES%>"/>
                            <core_rt:set var="mapTitle" value="Additional Roles"/>
                            <core_rt:set var="inputType" value="email"/>
                            <core_rt:set var="inputSubtitle" value="Enter mail address"/>
                            <core_rt:set var="customMap" value="${release.roles}"/>
                            <%@include file="/html/utils/includes/mapEdit.jspf" %>

                            <core_rt:set var="externalIdsSet" value="${release.externalIds.entrySet()}"/>
                            <%@include file="/html/utils/includes/editExternalIds.jsp" %>
                            <%@include file="/html/components/includes/releases/editReleaseRepository.jspf" %>
                        </div>
                        <div id="tab-ReleaseLinks">
                            <%@include file="/html/utils/includes/editLinkedReleases.jspf" %>
                        </div>
                        <core_rt:if test="${not addMode}">
                        <div id="tab-ReleaseClearingInformation">
                                <%@include file="/html/components/includes/releases/editReleaseClearingInformation.jspf" %>
                             </div>
                             <div id="tab-ReleaseECCInformation">
                                 <%@include file="/html/components/includes/releases/editReleaseECCInformation.jspf" %>
                             </div>
                        <div id="tab-Attachments">
                            <%@include file="/html/utils/includes/editAttachments.jspf" %>
                        </div>
                        </core_rt:if>
                        <core_rt:if test="${cotsMode}">
                            <div id="tab-COTSDetails">
                                <%@include file="/html/components/includes/releases/editCommercialDetails.jspf" %>
                            </div>
                        </core_rt:if>
                    </div>
                </div>
                <core_rt:if test="${not addMode}">
                    <input type="hidden" value="true" name="<portlet:namespace/>clearingInformation">
                    <input type="button" id="formSubmit" value="Update Release" class="addButton" >
                </core_rt:if>
                <core_rt:if test="${addMode}">
                    <input type="hidden" value="false" name="<portlet:namespace/>clearingInformation">
                    <input type="submit" value="Add Release" class="addButton" >
                </core_rt:if>
                <input type="button" value="Cancel" class="cancelButton">
                <div id="moderationRequestCommentDialog" style="display: none">
                    <hr>
                    <label class="textlabel stackedLabel">Comment your changes</label>
                    <textarea form=releaseEditForm name="<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>" id="moderationRequestCommentField" class="moderationCreationComment" placeholder="Leave a comment on your request"></textarea>
                    <input type="button" class="addButton" id="moderationRequestCommentSendButton" value="Send moderation request">
                </div>
            </form>
        </div>
    </div>

    <jsp:include page="/html/utils/includes/searchAndSelectUsers.jsp" />
    <jsp:include page="/html/utils/includes/searchAndSelectLicenses.jsp" />
    <jsp:include page="/html/utils/includes/searchUsers.jsp" />
    <jsp:include page="/html/utils/includes/searchLicenses.jsp" />
    <core_rt:set var="enableSearchForReleasesFromLinkedProjects" value="${false}" scope="request"/>
    <jsp:include page="/html/utils/includes/searchReleases.jsp" />
</core_rt:if>

<%@include file="/html/components/includes/vendors/searchVendor.jspf" %>

<script>
    var tabView; // we still need this global variable, until invalidHandlerShowErrorTab is modularized

    YUI().use(
            'aui-tabview',
            function (Y) {
                tabView = new Y.TabView(
                        {
                            srcNode: '#myTab',
                            stacked: true,
                            type: 'tab'
                        }
                ).render();
            }
    );

    require(['jquery', 'modules/sw360Validate', 'components/includes/vendors/searchVendor', 'modules/confirm', 'modules/autocomplete', /* jquery-plugins */ 'jquery-ui' ], function($, sw360Validate, vendorsearch, confirm, autocomplete) {

        Liferay.on('allPortletsReady', function() {
            autocomplete.prepareForMultipleHits('programminglanguages', ${programmingLanguages});
            autocomplete.prepareForMultipleHits('op_systems', ${operatingSystemsAutoC});

            sw360Validate.validateWithInvalidHandlerNoIgnore('#releaseEditForm');

            $('#formSubmit').click(
                function() {
                    <core_rt:choose>
                    <core_rt:when test="${addMode || release.permissions[WRITE]}">
                    $('#releaseEditForm').submit();
                    </core_rt:when>
                    <core_rt:otherwise>
                    showCommentField();
                    </core_rt:otherwise>
                    </core_rt:choose>
                }
            );
        });

    	$('#ComponentBasicInfo input.edit-vendor').on('click', function() {
            vendorsearch.openSearchDialog('<portlet:namespace/>what', '<portlet:namespace/>where',
                      '<portlet:namespace/>FULLNAME', '<portlet:namespace/>SHORTNAME', '<portlet:namespace/>URL', fillVendorInfo);
        });

        $('input[type=button].delete-release').on('click', function(event) {
            var message = '',
                data = $(event.target).data(),
                releaseName = data.releaseName,
                linkedReleases = data.linkedReleases,
                attachments = data.attachments;

            message = 'Do you really want to delete the release <b>' + releaseName + '</b> ?';
            if(linkedReleases > 0 || attachments > 0) {
                message += '<ul>';
                if(linkedReleases > 0) {
                    message += '<li><b>' + linkedReleases + '</b> linked releases</li>';
                }
                if(attachments > 0) {
                    message += '<li><b>' + attachments + '</b> attachments</li>';
                }
                message += '</ul>';
            }

            message += '<div ' + styleAsHiddenIfNeccessary(${release.permissions[DELETE] == true}) + '><hr><label class=\'textlabel stackedLabel\'>Comment your changes</label><textarea id=\'moderationDeleteCommentField\' class=\'moderationCreationComment\' placeholder=\'Comment on request...\'></textarea></div>';
            confirm.confirmDeletion(message, deleteRelease);
        });

        $('#releaseEditForm .cancelButton').on('click', function() {
        	deleteAttachmentsOnCancel(function() {
        		var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
                var portletURL = Liferay.PortletURL.createURL(baseUrl)
                        .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
                        .setParameter('<%=PortalConstants.COMPONENT_ID%>', '${component.id}');
                window.location = portletURL.toString();
        	});
        });

        $('#moderationRequestCommentSendButton').on('click', function() {
        	$('#releaseEditForm').submit();
        });

        function fillVendorInfo(vendorInfo) {
            var beforeComma = vendorInfo.substr(0, vendorInfo.indexOf(","));
            var afterComma = vendorInfo.substr(vendorInfo.indexOf(",") + 1);

            $('#<%=Release._Fields.VENDOR_ID.toString()%>').val(beforeComma.trim());
            $('#<%=Release._Fields.VENDOR_ID.toString()%>Display').val(afterComma.trim());
        }

        function deleteRelease() {
            var commentText_encoded = btoa($("#moderationDeleteCommentField").val());
            var baseUrl = '<%=deleteReleaseURL%>';
            var deleteURL = Liferay.PortletURL.createURL( baseUrl ).setParameter('<%=PortalConstants.MODERATION_REQUEST_COMMENT%>',commentText_encoded);
            window.location.href = deleteURL;
        }

        function deleteAttachmentsOnCancel(callback) {
            $.ajax({
                type: 'POST',
                url: '<%=deleteAttachmentsOnCancelURL%>',
                cache: false,
                data: {
                    "<portlet:namespace/><%=PortalConstants.DOCUMENT_ID%>": "${release.id}"
                },
                success: callback
            });
        }

        function focusOnCommentField() {
            $("#moderationRequestCommentField").focus();
            $("#moderationRequestCommentField").select();
        }

        function showCommentField() {
            $("#moderationRequestCommentDialog").show();
            $("#formSubmit").attr("disabled","disabled");
            focusOnCommentField();
        }
    });
</script>
