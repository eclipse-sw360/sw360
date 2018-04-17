<%--
  ~ Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
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
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="subscribeComponentURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.SUBSCRIBE%>"/>
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
</portlet:resourceURL>


<portlet:resourceURL var="unsubscribeComponentURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.UNSUBSCRIBE%>"/>
    <portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/>
</portlet:resourceURL>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="component" class="org.eclipse.sw360.datahandler.thrift.components.Component" scope="request"/>
    <jsp:useBean id="selectedTab" class="java.lang.String" scope="request"/>
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

    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">

    <div id="header"></div>
    <p class="pageHeader"><span class="pageHeaderBigSpan">Component: <sw360:out value="${component.name}"/></span>
        <span class="pull-right">
        <core_rt:if test="${isUserAllowedToMerge}">
            <input type="button" data-component-id="${component.id}" id="merge" value="Merge" class="addButton">
        </core_rt:if>
        <input type="button" id="edit" value="Edit" class="addButton">
        <sw360:DisplaySubscribeButton email="<%=themeDisplay.getUser().getEmailAddress()%>" object="${component}"
                                      id="SubscribeButton" />
    </span>
    </p>
    <core_rt:set var="inComponentDetailsContext" value="true" scope="request"/>
    <%@include file="/html/components/includes/components/detailOverview.jspf"%>
</core_rt:if>

<script>
    require(['jquery', 'modules/tabview'], function($, tabview) {
        tabview.create('myTab');

        $('#edit').on('click', function() {
            window.location ='<portlet:renderURL ><portlet:param name="<%=PortalConstants.COMPONENT_ID%>" value="${component.id}"/><portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/></portlet:renderURL>'
        });

        $('#SubscribeButton').on('click', function(event) {
            var $button = $(event.currentTarget),
                subscribed = $button.hasClass('subscribed'),
                url = subscribed ? '<%=unsubscribeComponentURL%>' : '<%=subscribeComponentURL%>';

            $button.val('...');
            doAjax(url, function(data) {
                if(data.result === "SUCCESS") {
                    $button.val(!subscribed ? 'Unsubscribe': 'Subscribe');
                    $button.toggleClass('subscribed');
                } else {
                    $button.val(data.result);
                }
            }, function() {
                $button.val('error');
            });
        });

        $('#merge').on('click', function(event) {
            var data = $(event.currentTarget).data();
            mergeComponent(data.componentId);
        });

        function doAjax(url, successCallback, errorCallback) {
            $.ajax({
                type: 'POST',
                url: url,
                success: function (data) {
                    successCallback(data);
                },
                error: function () {
                    errorCallback();
                }
            });
        }

        function mergeComponent(componentId) {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
                portletURL = Liferay.PortletURL.createURL(baseUrl)
                                               .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_MERGE_COMPONENT%>')
                                               .setParameter('<%=PortalConstants.COMPONENT_ID%>', componentId);
            window.location = portletURL.toString();
        }
    });
</script>
