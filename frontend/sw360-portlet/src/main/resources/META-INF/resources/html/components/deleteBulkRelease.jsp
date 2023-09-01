<%--
  ~ Copyright (C) TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
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
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>
<portlet:resourceURL var="loadBulkDeleteRowsURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value='<%=PortalConstants.PAGENAME_DELETE_BULK_RELEASE%>'/>
</portlet:resourceURL>

<jsp:useBean id="bulkOperationResultList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode>" scope="request"/>

<%@ include file="/html/utils/includes/logError.jspf" %>

<table class="table table-bordered" id="BulkDeleteInfo"
  data-load-node-url="<%=loadBulkDeleteRowsURL%>"
  data-portlet-namespace="<portlet:namespace/>"
  data-parent-branch-key="<%=PortalConstants.PARENT_BRANCH_ID%>"
  data-scope-group-id="${pageContext.getAttribute('scopeGroupId')}"
 >

     <div class="btn-BulkDeleteExecution" role="group" style="justify-content: flex-end; display: grid;">
        <button type="button" data-release-id="${release.id}" id="BulkDeleteExecutionButton" class="btn btn-primary "><liferay-ui:message key="bulk.delete.execution" /></button>
    </div>

    <thead>
        <tr>
            <th><liferay-ui:message key="name" /></th>
            <th><liferay-ui:message key="type" /></th>
            <th><liferay-ui:message key="result" /></th>
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

        <core_rt:forEach items="${bulkOperationResultList}" var="bulkDeleteResult" varStatus="loop">
            <tr id="releaseLinkRow${loop.count}" data-tt-id="${bulkDeleteResult.additionalData['presentationId']}" data-tt-branch="${bulkDeleteResult.childList.size()>0}"
                data-tt-parent-id="${bulkDeleteResult.additionalData['parentPresentationId']}"
            >
                <td>
                    <core_rt:if test="${bulkDeleteResult.additionalData['presentationStatus'] == 'Deleted'}">
                        <sw360:out value="${bulkDeleteResult.name}"/>
                        <sw360:out value="${bulkDeleteResult.version}"/>
                        <sw360:out value="-Deleted Item-"/>
                    </core_rt:if>
                    <core_rt:if test="${bulkDeleteResult.additionalData['presentationStatus'] != 'Deleted'}">
                        <core_rt:choose>
                            <core_rt:when test="${bulkDeleteResult.type == 'RELEASE'}">
                                <a href="<sw360:DisplayReleaseLink releaseId="${bulkDeleteResult.id}" bare="true" scopeGroupId="${concludedScopeGroupId}" />">
                                    <sw360:out value="${bulkDeleteResult.name}"/>
                                    <sw360:out value="${bulkDeleteResult.version}"/> 
                                </a>
                            </core_rt:when>
                            <core_rt:when test="${bulkDeleteResult.type == 'COMPONENT'}">
                                <sw360:DisplayComponentLink componentId="${bulkDeleteResult.id}" showName="false">
                                    <sw360:out value="${bulkDeleteResult.name}"/>
                                </sw360:DisplayComponentLink>
                            </core_rt:when>
                            <core_rt:otherwise>
                                <a href="<sw360:DisplayReleaseLink releaseId="${bulkDeleteResult.id}" bare="true" scopeGroupId="${concludedScopeGroupId}" />">
                                    <sw360:out value="${bulkDeleteResult.name}"/>
                                    <sw360:out value="${bulkDeleteResult.version}"/> 
                                </a>
                            </core_rt:otherwise>
                        </core_rt:choose>
                    </core_rt:if>
                </td>
                <td>
                    <sw360:out value="${bulkDeleteResult.type}"/>
                </td>
                <td>
                    <sw360:out value="${bulkDeleteResult.additionalData['presentationStatus']}" />
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
        var table = $('#BulkDeleteInfo');
        table.treetable({
            expandable:true,
            initialState:"expanded",
        })
    });

    $('#BulkDeleteExecutionButton').on('click',function(event){
        var releaseId = "${releaseId}";
        var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
        portletURL = Liferay.PortletURL.createURL(baseUrl)
                                        .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DELETE_BULK_RELEASE%>')
                                        .setParameter('<%=PortalConstants.RELEASE_ID%>', releaseId);
         window.location = portletURL.toString();
    })
</script>