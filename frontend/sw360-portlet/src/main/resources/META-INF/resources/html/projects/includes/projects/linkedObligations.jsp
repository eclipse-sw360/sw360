<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@include file="/html/init.jsp"%>


<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />
<portlet:renderURL var="downloadReportInfoWithSubProject">
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${projectid}"/>
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_LICENSE_INFO%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_WITH_SUBPROJECT%>" value="true"/>
    <portlet:param name="<%=PortalConstants.PREPARE_LICENSEINFO_OBL_TAB%>" value="true"/>
</portlet:renderURL>

<portlet:renderURL var="downloadReportInfoWithoutSubProject">
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${projectid}"/>
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_LICENSE_INFO%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_WITH_SUBPROJECT%>" value="false"/>
    <portlet:param name="<%=PortalConstants.PREPARE_LICENSEINFO_OBL_TAB%>" value="true"/>
</portlet:renderURL>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project"%>
<%@ page import="javax.portlet.PortletRequest"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>
<core_rt:if test="${isProjectObligationsEnabled and isObligationPresent}">
    <jsp:useBean id="obligationData" type="org.eclipse.sw360.datahandler.thrift.projects.ObligationList" scope="request" />
    <core_rt:set var="isObligationPresent" value="${not empty obligationData and not empty obligationData.linkedObligationStatus}" />
    <core_rt:set var="linkedObligations" value="${obligationData.linkedObligationStatus}" />
    <core_rt:if test="${isObligationPresent}">
        <jsp:useBean id="projectObligationsInfoByRelease" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult>" scope="request" />
        <jsp:useBean id="approvedObligationsCount" type="java.lang.Integer" scope="request"/>
        <jsp:useBean id="excludedReleases" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.components.Release>" scope="request" />
    </core_rt:if>
</core_rt:if>
<core_rt:if test="${not isObligationPresent}">
    <div class="alert alert-info" role="alert">
            <liferay-ui:message key="no.linked.obligations" />
    </div>
</core_rt:if>

<core_rt:if test="${isObligationPresent}">
    

<liferay-portlet:renderURL var="friendlyReleaseURL" portletName="sw360_portlet_components">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.RELEASE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<liferay-portlet:renderURL var="friendlyLicenseURL" portletName="sw360_portlet_licenses">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

    <core_rt:if test="${not empty excludedReleases}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            Obligation status associated with the following release(s) are orphaned due to changes in accepted license file.
            <ul>
                <core_rt:forEach items="${excludedReleases}" var="exRelease" varStatus="loop">
                    <li><sw360:DisplayReleaseLink release="${exRelease}" showFullname="true"/></li>
                </core_rt:forEach>
            </ul>
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                <span aria-hidden="true"><svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#times"/></svg></span>
            </button>
            <core_rt:if test="${not inProjectDetailsContext}">
                <input type="checkbox" name="deleteAllOrphanObligations" /> Select the checkbox and <b>Update</b> the project, to delete all orphaned obligations.
            </core_rt:if>
        </div>
    </core_rt:if>

<div class="tab-content" id="pills-tabContent">
    <div class="tab-pane show active" id="pills-obligationsView" role="tabpanel" aria-labelledby="pills-obligations-tab">
        <core_rt:choose>
            <core_rt:when test="${inProjectDetailsContext}">
                <%@include file="/html/projects/includes/projects/obligations.jspf" %>
            </core_rt:when>
            <core_rt:otherwise>
                <%@include file="/html/projects/includes/projects/obligationsEdit.jspf" %>
            </core_rt:otherwise>
        </core_rt:choose>
    </div>
    <div class="tab-pane" id="pills-releasesView" role="tabpanel" aria-labelledby="pills-releases-tab">
        <%@include file="/html/projects/includes/projects/obligationsByReleases.jspf" %>
    </div>
</div>
</core_rt:if>

<script>
AUI().use('liferay-portlet-url', function () {
    var PortletURL = Liferay.PortletURL;
    $('#downloadLicenseInfoObl a.dropdown-item').on('click', function(event) {
        var type=$(event.currentTarget).data('type');
        if(type === 'projectWithSubProject'){
            window.location.href = '<%=downloadReportInfoWithSubProject%>'
        }
        else{
            window.location.href = '<%=downloadReportInfoWithoutSubProject%>'
        }
    });
    <core_rt:if test="${not empty obligationData.linkedObligationStatus.size()}">
        let badgeClass = ""; 
        <core_rt:choose>
			<core_rt:when test="${approvedObligationsCount == 0}">
			badgeClass="badge badge-danger"
			</core_rt:when>
			<core_rt:when test="${approvedObligationsCount == linkedObligations.size()}">
			badgeClass="badge badge-success"
			</core_rt:when>
			<core_rt:otherwise>
			badgeClass="badge badge-light"
			</core_rt:otherwise>
		</core_rt:choose>
		$("#obligationCountBadge").append('<span id="obligtionsCount" class="'+badgeClass+'">${approvedObligationsCount} / ${obligationData.linkedObligationStatus.size()}</span>');
	</core_rt:if>
});
</script>