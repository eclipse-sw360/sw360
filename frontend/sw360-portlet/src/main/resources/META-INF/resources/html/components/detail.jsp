<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~ With modifications by Bosch Software Innovations GmbH, 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Component" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="sw360ComponentUrl">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.CODESCOOP_ACTION_COMPONENT%>'/>
</portlet:resourceURL>

<portlet:actionURL var="updateComponentURL" name="updateComponent">
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
</portlet:actionURL>


<c:catch var="attributeNotFoundException">
    <jsp:useBean id="component" class="org.eclipse.sw360.datahandler.thrift.components.Component" scope="request"/>
    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>" scope="request"/>
    <jsp:useBean id="usingComponents" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.components.Component>" scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="documentType" class="java.lang.String" scope="request"/>
    <jsp:useBean id="isUserAllowedToMerge" type="java.lang.Boolean" scope="request"/>
    <jsp:useBean id="vulnerabilityVerificationEditable" type="java.lang.Boolean" scope="request"/>
    <core_rt:if test="${vulnerabilityVerificationEditable}">
        <jsp:useBean id="numberOfIncorrectVulnerabilities" type="java.lang.Long" scope="request"/>
    </core_rt:if>
    <jsp:useBean id="numberOfCheckedOrUncheckedVulnerabilities" type="java.lang.Long" scope="request"/>
 </c:catch>

<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">
    <core_rt:set var="inComponentDetailsContext" value="true" scope="request" />
    <%@include file="/html/components/includes/components/detailOverview.jspf"%>
</core_rt:if>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<c:if test="${codescoopActive}">
    <form id="component_edit_form" name="componentEditForm" action="<%=updateComponentURL%>&updateOnlyRequested" method="post" style="display: none;">
    </form>
    <script>
        var edit_form_fields = {
            description: '<portlet:namespace/><%=Component._Fields.DESCRIPTION%>',
            homepage: '<portlet:namespace/><%=Component._Fields.HOMEPAGE%>',
            categories: '<portlet:namespace/><%=Component._Fields.CATEGORIES%>',
            languages: '<portlet:namespace/><%=Component._Fields.LANGUAGES%>',
            licenses: '<portlet:namespace/><%=Component._Fields.MAIN_LICENSE_IDS%>',
            externalIdKey: '<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_KEY%>externalIdsTableRow',
            externalIdValue: '<portlet:namespace/><%=PortalConstants.EXTERNAL_ID_VALUE%>externalIdsTableRow'
        };
        document.addEventListener("DOMContentLoaded", function() {
            require(['modules/codeScoop' ], function(codeScoop) {
                var api = new codeScoop();
                api.activateMerge("<%=sw360ComponentUrl%>");
            });
        });
    </script>
</c:if>
