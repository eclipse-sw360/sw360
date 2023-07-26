<%--
  ~ Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="pkg" type="org.eclipse.sw360.datahandler.thrift.packages.Package" scope="request"/>
    <jsp:useBean id="releaseName" class="java.lang.String" scope="request"/>
    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>" scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="writeAccessUser" type="java.lang.Boolean" scope="request"/>
 </c:catch>
<portlet:renderURL var="editPackageURL">
    <portlet:param name="<%=PortalConstants.PACKAGE_ID%>" value="${pkg.id}"/>
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>
</portlet:renderURL>
<core_rt:set var = "docid" scope = "request" value = "${pkg.id}"/>
<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">
<div class="container" style="display: none;">
    <div class="row">
        <div class="col-3 sidebar">
            <div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab"><liferay-ui:message key="summary" /></a>
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-ChangeLogs'}">active</core_rt:if>" href="#tab-ChangeLogs" data-toggle="list" role="tab"><liferay-ui:message key="change.log" /></a>
            </div>
        </div>
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-primary"
                            <core_rt:choose>
                            <core_rt:when test="${writeAccessUser}"> onclick="window.location.href='<%=editPackageURL%>' + window.location.hash" </core_rt:when>
                            <core_rt:otherwise>disabled="disabled" title="<liferay-ui:message key="you.do.not.have.permission.to.edit.the.package" />" </core_rt:otherwise>
                            </core_rt:choose>>
                                <liferay-ui:message key="edit.package" />
                            </button>
                        </div>
                        <div class="list-group-companion" data-belong-to="tab-ChangeLogs">
                            <div class="nav nav-pills justify-content-center bg-light font-weight-bold" id="pills-tab" role="tablist">
                                <a class="nav-item nav-link active" id="pills-changelogs-list-tab" data-toggle="pill" href="#pills-changelogslist" role="tab" aria-controls="pills-changeloglist" aria-selected="true">
                                    <liferay-ui:message key="change.log" /></a>
                                <a class="nav-item nav-link" id="pills-changelogs-view-tab" href="#pills-changelogsView" role="tab" aria-controls="pills-changelogsView" aria-selected="false">
                                    <liferay-ui:message key="changes" /></a>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="${sw360:printPackageName(pkg)}">
                    <sw360:out value="${sw360:printPackageName(pkg)}"/>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <div class="tab-content">
                        <div id="tab-Summary" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Summary'}">active show</core_rt:if>" >
                            <%@include file="/html/packages/summaryPackage.jspf" %>
                            <core_rt:set var="documentName"><sw360:PackageName pkg="${pkg}"/></core_rt:set>
                            <core_rt:set var="tableId" value="usingProjectsTableSummary"/>
                            <%@include file="/html/utils/includes/usingProjectsTable.jspf" %>
                        </div>
                        <div id="tab-ChangeLogs" class="tab-pane <core_rt:if test="${selectedTab == 'tab-ChangeLogs'}">active show</core_rt:if>">
                            <jsp:include page="/html/changelogs/elementView.jsp" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</core_rt:if>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<script>
require(['jquery', 'modules/listgroup', 'utils/includes/clipboard'], function($, listgroup, clipboard) {
    document.title = $("<span></span>").html("<sw360:out value='${pkg.name}'/> - " + document.title).text();
    listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');

    $('#copyToClipboard').on('click', function(event) {
        let textSelector = "table tr td#documentId",
        textToCopy = $(textSelector).clone().children().remove().end().text().trim();
        clipboard.copyToClipboard(textToCopy, '#copyToClipboard');
    });
});
</script>
