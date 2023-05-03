<%--
  ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.moderation.DocumentType" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<jsp:useBean id="moderationRequest" class="org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest" scope="request"/>
<jsp:useBean id="actual_SPDXDocument" class="org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument" scope="request" />
<jsp:useBean id="defaultLicenseInfoHeaderText" class="java.lang.String" scope="request" />

<core_rt:set var="spdxDocument" value="${actual_SPDXDocument}" scope="request"/>

<div class="container" id="moderation-request-merge" data-document-type="<%=DocumentType.SPDX_DOCUMENT%>">

    <core_rt:set var="moderationTitle" value="Change ${sw360:printSPDXDocumentName(spdxDocument)}" scope="request" />
    <%@include file="/html/moderation/includes/moderationHeader.jspf"%>

    <div class="row">
        <div class="col">
            <div id="moderation-wizard" class="accordion">
                <div class="card">
                    <div id="moderation-header-heading" class="card-header">
                        <h2 class="mb-0">
                            <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#moderation-header" aria-expanded="true" aria-controls="moderation-header">
                                <liferay-ui:message key="moderation.request.information" />
                            </button>
                        </h2>
                    </div>
                    <div id="moderation-header" class="collapse show" aria-labelledby="moderation-header-heading" data-parent="#moderation-wizard">
                        <div class="card-body">
                            <%@include file="/html/moderation/includes/moderationInfo.jspf"%>
                        </div>
                    </div>
                </div>
                <core_rt:if test="${sw360:isOpenModerationRequest(moderationRequest)}">
                    <div class="card">
                        <div id="moderation-changes-heading" class="card-header">
                            <h2 class="mb-0">
                                <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#moderation-changes" aria-expanded="false" aria-controls="moderation-changes">
                                    <liferay-ui:message key="proposed.changes" />
                                </button>
                            </h2>
                        </div>
                        <div id="moderation-changes" class="collapse" aria-labelledby="moderation-changes-heading" data-parent="#moderation-wizard">
                            <div class="card-body">
                                <h4 class="mt-2"><liferay-ui:message key="basic.fields" /></h4>
                                <sw360:DisplaySPDXDocumentChanges
                                    actual="${actual_SPDXDocument}"
                                    additions="${moderationRequest.SPDXDocumentAdditions}"
                                    deletions="${moderationRequest.SPDXDocumentDeletions}"
                                    idPrefix="basicFields"
                                    tableClasses="table table-bordered"
                                />
                            </div>
                        </div>
                    </div>
                </core_rt:if>
                <div class="card">
                    <div id="current-document-heading" class="card-header">
                        <h2 class="mb-0">
                            <button class="btn btn-secondary btn-block" type="button" data-toggle="collapse" data-target="#current-document" aria-expanded="false" aria-controls="current-document">
                                <liferay-ui:message key="current.spdxdocument" />
                            </button>
                        </h2>
                    </div>
                    <div id="current-document" class="collapse" aria-labelledby="current-document-heading" data-parent="#moderation-wizard">
                        <div class="card-body">
                            <%@include file="/html/utils/includes/requirejs.jspf" %>
                            <%@include file="/html/components/includes/releases/spdx/view.jsp"%>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    $('#spdxFullMode').hide();
    $('#spdxLiteMode').hide();
    $('#DocumentCreationInformation').hide();
    $('#PackageInformation').hide();
    $('#annotationSourceSelect').val('SPDX Document');
    changeAnnotationSource($('#annotationSourceSelect'));
</script>