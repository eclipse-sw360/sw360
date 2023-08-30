<%--
  ~ Copyright (C) TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>
<portlet:resourceURL var="loadBulkDeletePreviewRowsURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value='<%=PortalConstants.PAGENAME_DELETE_BULK_RELEASE_PREVIEW%>'/>
</portlet:resourceURL>

<jsp:useBean id="bulkOperationResultList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode>"
             scope="request"/>
<jsp:useBean id="releaseId" class="java.lang.String" scope="request"/>

<%@ include file="/html/utils/includes/logError.jspf" %>

<table class="table table-bordered" id="BulkDeletePreviewInfo"
  data-load-node-url="<%=loadBulkDeletePreviewRowsURL%>"
  data-portlet-namespace="<portlet:namespace/>"
  data-parent-branch-key="<%=PortalConstants.PARENT_BRANCH_ID%>"
  data-scope-group-id="${pageContext.getAttribute('scopeGroupId')}"
 >

    <div class="btn-BulkDeleteExecution" role="group" style="justify-content: flex-end; display: grid;">
        <button type="button" data-release-id="${release.id}" id="BulkDeletePreviewExecutionButton" class="btn btn-primary "><liferay-ui:message key="bulk.delete.execution" /></button>
    </div>

    <thead>
        <tr>
            <th><liferay-ui:message key="name" /></th>
            <th><liferay-ui:message key="type" /></th>
            <th><liferay-ui:message key="operation" /></th>
        </tr>
    </thead>
    <tbody>
        <%@include file="/html/init.jsp" %>
        <core_rt:if test="${empty parentScopeGroupId}">
            <core_rt:set var="concludedScopeGroupId" value="${pageContext.getAttribute('scopeGroupId')}"/>
        </core_rt:if>
        <core_rt:if test="${not empty parentScopeGroupId}">
            <core_rt:set var="concludedScopeGroupId" value="${parentScopeGroupId}"/>
        </core_rt:if>

        <core_rt:forEach items="${bulkOperationResultList}" var="bulkDeletePreview" varStatus="loop">
            <tr id="releaseLinkRow${loop.count}" data-tt-id="${bulkDeletePreview.additionalData['presentationId']}" data-tt-branch="${bulkDeletePreview.childList.size()>0}"
                data-tt-parent-id="${bulkDeletePreview.additionalData['parentPresentationId']}"
            >
                <td>
                    <core_rt:choose>
                        <core_rt:when test="${bulkDeletePreview.type == 'RELEASE'}">
                            <a href="<sw360:DisplayReleaseLink releaseId="${bulkDeletePreview.id}" bare="true" scopeGroupId="${concludedScopeGroupId}" />">
                                <sw360:out value="${bulkDeletePreview.name}"/>
                                <sw360:out value="${bulkDeletePreview.version}"/>
                            </a>
                        </core_rt:when>
                        <core_rt:when test="${bulkDeletePreview.type == 'COMPONENT'}">
                            <sw360:DisplayComponentLink componentId="${bulkDeletePreview.id}" showName="false">
                                <sw360:out value="${bulkDeletePreview.name}"/>
                            </sw360:DisplayComponentLink>
                        </core_rt:when>
                        <core_rt:otherwise>
                            <a href="<sw360:DisplayReleaseLink releaseId="${bulkDeletePreview.id}" bare="true" scopeGroupId="${concludedScopeGroupId}" />">
                                <sw360:out value="${bulkDeletePreview.name}"/>
                                <sw360:out value="${bulkDeletePreview.version}"/>
                            </a>
                        </core_rt:otherwise>
                    </core_rt:choose>
                </td>
                <td>
                    <sw360:out value="${bulkDeletePreview.type}"/>
                </td>
                <td>
                    <sw360:out value="${bulkDeletePreview.additionalData['presentationStatus']}" />
                </td>
            </tr>
        </core_rt:forEach>

        <core_rt:if test="${bulkOperationResultList.size() < 1}">
            <tr>
                <td colspan="3"><liferay-ui:message key="no.linked.releases.yet" /></td>
            </tr>
        </core_rt:if>
    </tbody>
</table>

<script>
    require(['jquery', 'modules/ajax-treetable'], function($, ajaxTreeTable) {
        var table = $('#BulkDeletePreviewInfo');
        table.treetable({
            expandable:true,
            initialState:"expanded",
        })
    });

    $('#BulkDeletePreviewExecutionButton').on('click',function(event){
        var releaseId = "${releaseId}";
        var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
        portletURL = Liferay.PortletURL.createURL(baseUrl)
                                        .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DELETE_BULK_RELEASE%>')
                                        .setParameter('<%=PortalConstants.RELEASE_ID%>', releaseId);
         window.location = portletURL.toString();
    })

</script>
