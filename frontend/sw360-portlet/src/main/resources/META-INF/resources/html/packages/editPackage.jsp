<%--
  ~ Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.datahandler.common.SW360Utils" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.Package" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.PackageManager" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%-- use require js on this page --%>
<%@include file="/html/utils/includes/requirejs.jspf" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="updatePackageURL" name="updatePackage">
    <portlet:param name="<%=PortalConstants.PACKAGE_ID%>" value="${pkg.id}"/>
</portlet:actionURL>

<portlet:actionURL var="deletePackageURL" name="deletePackage">
    <portlet:param name="<%=PortalConstants.PACKAGE_ID%>" value="${pkg.id}"/>
</portlet:actionURL>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="pkg" type="org.eclipse.sw360.datahandler.thrift.packages.Package" scope="request"/>
    <jsp:useBean id="releaseName" class="java.lang.String" scope="request"/>
    <core_rt:set var="addMode"  value="${empty pkg.id}" />
    <core_rt:set var="isSingleRelease" value="${true}" scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
 </c:catch>

<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">
<div class="container" style="display: none;">
    <div class="row">
        <div class="col-3 sidebar">
            <div id="detailTab" class="list-group" data-initial-tab="${selectedTab}" role="tablist">
                <a class="list-group-item list-group-item-action <core_rt:if test="${selectedTab == 'tab-Summary'}">active</core_rt:if>" href="#tab-Summary" data-toggle="list" role="tab"><liferay-ui:message key="summary" /></a>
            </div>
        </div>
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <core_rt:if test="${addMode}" >
                                <button type="button" id="formSubmit" class="btn btn-primary"><liferay-ui:message key="create.package" /></button>
                            </core_rt:if>
                            <core_rt:if test="${not addMode}" >
                                <button type="button" id="formSubmit" class="btn btn-primary"><liferay-ui:message key="update.package" /></button>
                            </core_rt:if>
                        </div>

                        <core_rt:if test="${not addMode}" >
                            <div class="btn-group" role="group">
                                <button id="deletePackageButton" type="button" class="btn btn-danger"
                                    <core_rt:if test="${allUsingProjectsCount > 0}"> disabled="disabled" title="<liferay-ui:message key="deletion.is.disabled.as.the.package.is.used.in.project" />" </core_rt:if>>
                                    <liferay-ui:message key="delete.package" />
                                </button>
                            </div>
                        </core_rt:if>

                        <div class="btn-group" role="group">
                            <button id="cancelEditButton" type="button" class="btn btn-light"><liferay-ui:message key="cancel" /></button>
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
                        <form  id="packageEditForm" name="packageEditForm" action="<%=updatePackageURL%>" class="needs-validation" method="post" novalidate
                            data-name="<sw360:PackageName pkg="${pkg}" />"
                            data-delete-url="<%=deletePackageURL%>" >
                        <div id="tab-Summary" class="tab-pane <core_rt:if test="${selectedTab == 'tab-Summary'}">active show</core_rt:if>" >
                            <table class="table edit-table three-columns" id="packageEdit">
                                <thead>
                                    <tr>
                                        <th colspan="3"><liferay-ui:message key="summary" /></th>
                                    </tr>
                                </thead>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="pkg_name"><liferay-ui:message key="name" /></label>
                                            <input id="pkg_name" name="<portlet:namespace/><%=Package._Fields.NAME%>" type="text"
                                                placeholder="<liferay-ui:message key="enter.name" />" class="form-control" minlength="2"
                                                value="<sw360:out value="${pkg.name}"/>" required pattern=".*\S.*"/>
                                             <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.valid.name" />
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="pkg_version"><liferay-ui:message key="version" /></label>
                                            <input id="pkg_version" class="form-control" name="<portlet:namespace/><%=Package._Fields.VERSION%>" type="text"
                                                placeholder="<liferay-ui:message key="enter.version" />" value="<sw360:out value="${pkg.version}"/>" required pattern=".*\S.*" />
                                             <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.valid.version" />
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="pkg_type"><liferay-ui:message key="package.type" /></label>
                                            <select class="form-control border" id="pkg_type" name="<portlet:namespace/><%=Package._Fields.PACKAGE_TYPE%>" required>
                                                <option value>-- <liferay-ui:message key="select" /> <liferay-ui:message key="package.type" /> --</option>
                                                <sw360:DisplayEnumOptions type="<%=CycloneDxComponentType.class%>" selected="${pkg.packageType}"/>
                                            </select>
                                             <div class="invalid-feedback">
                                                <liferay-ui:message key="please.select.the.package.type" />!
                                            </div>
                                            <small class="form-text">
                                                <sw360:DisplayEnumInfo type="<%=CycloneDxComponentType.class%>"/>
                                                <liferay-ui:message key="learn.more.about.package.types" />
                                            </small>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="pkg_purl"><liferay-ui:message key="purl.package.url" /></label>
                                            <input id="pkg_purl" class="form-control" name="<portlet:namespace/><%=Package._Fields.PURL%>" required pattern="^pkg:[a-zA-Z]+/[^/]+(/.*)?$"
                                                placeholder="<liferay-ui:message key="enter.purl" />" value="<sw360:out value="${pkg.purl}"/>" />
                                             <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.valid.purl" />
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label class="mandatory" for="pkg_manager"><liferay-ui:message key="package.manager" /></label>
                                            <select class="form-control border" id="pkg_manager" name="<portlet:namespace/><%=Package._Fields.PACKAGE_MANAGER%>" disabled>
                                                <option value>-- <liferay-ui:message key="will.be.set.automatically" /> <liferay-ui:message key="via.purl" /> --</option>
                                                <sw360:DisplayEnumOptions type="<%=PackageManager.class%>" selected="${pkg.packageManager}"/>
                                            </select>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label for="pkg_vcs"><liferay-ui:message key="vcs.version.control.system" /></label>
                                            <input id="pkg_vcs" class="form-control" name="<portlet:namespace/><%=Package._Fields.VCS%>" type="url"
                                                placeholder="<liferay-ui:message key="enter.vcs.url" />" value="<sw360:out value="${pkg.vcs}"/>" />
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <sw360:DisplayLicensesEdit id="<%=Package._Fields.LICENSE_IDS.toString()%>" licenseIds="${pkg.licenseIds}"/>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label for="pkg_release"><liferay-ui:message key="release" /></label>
                                            <input type="hidden" readonly value="${pkg.releaseId}" id="<%=Package._Fields.RELEASE_ID.toString()%>" name="<portlet:namespace/><%=Package._Fields.RELEASE_ID%>">
                                            <div class="input-group">
                                            <input id="pkg_release" class="form-control releaseSearchDialog clickable" type="text" aria-describedby="clearRelease"
                                                placeholder="<liferay-ui:message key="click.to.link.a.release" />" value="<sw360:out value="${releaseName}"/>" required readonly/>
                                                <div class="input-group-append">
                                                    <span class="input-group-text clearSelection" id="clearRelease">&times;</span>
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label for="pkg_homepageUrl"><liferay-ui:message key="homepage.url" /></label>
                                            <input id="pkg_homepageUrl" class="form-control" name="<portlet:namespace/><%=Package._Fields.HOMEPAGE_URL%>" type="url"
                                                placeholder="<liferay-ui:message key="enter.homepage.url" />" value="<sw360:out value="${pkg.homepageUrl}"/>" />
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="created_on"><liferay-ui:message key="created.on" /></label>
                                            <input id="created_on" name="<portlet:namespace/><%=Package._Fields.CREATED_ON%>" type="date"
                                                placeholder="<liferay-ui:message key="creation.date.yyyy.mm.dd" />" required
                                                    <core_rt:if test="${addMode}">
                                                        value="<%=SW360Utils.getCreatedOn()%>"
                                                    </core_rt:if>
                                                    <core_rt:if test="${not addMode}">
                                                        value="<sw360:out value="${pkg.createdOn}"/>"
                                                    </core_rt:if>
                                                readonly class="form-control"/>
                                        </div>
                                    </td>
                                    <td>
                                        <sw360:DisplayUserEdit email="${pkg.createdBy}" id="<%=Package._Fields.CREATED_BY.toString()%>" description="created.by" multiUsers="false" readonly="true"/>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <sw360:DisplayUserEdit email="${pkg.modifiedBy}" id="<%=Package._Fields.MODIFIED_BY.toString()%>" description="modified.by" multiUsers="false" readonly="true"/>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="3">
                                        <div class="form-group">
                                            <label for="pkg_description"><liferay-ui:message key="description" /></label>
                                            <textarea id="pkg_description" class="form-control" name="<portlet:namespace/><%=Package._Fields.DESCRIPTION%>" rows="3"
                                                placeholder="<liferay-ui:message key="enter.description" />" ><sw360:out value="${pkg.description}"/></textarea>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
    <jsp:include page="/html/utils/includes/searchLicenses.jsp" />
    <jsp:include page="/html/utils/includes/searchAndSelectLicenses.jsp" />
    <jsp:include page="/html/utils/includes/searchReleases.jsp" />

<div class="dialogs auto-dialogs">
    <div id="deletePackageDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <clay:icon symbol="question-circle" />
                        <liferay-ui:message key="delete.package" />?
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p><liferay-ui:message key="do.you.really.want.to.delete.the.package.x" /></p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="cancel" /></button>
                    <button type="button" class="btn btn-danger"><liferay-ui:message key="delete.package" /></button>
                </div>
            </div>
        </div>
    </div>
</div>
</core_rt:if>

<script>
require(['jquery', 'modules/autocomplete', 'modules/dialog', 'modules/listgroup', 'modules/validation'], function($, autocomplete, dialog, listgroup, validation) {
    document.title = $("<span></span>").html("<sw360:out value='${sw360:printPackageName(pkg)}'/> - " + document.title).text();
    listgroup.initialize('detailTab', $('#detailTab').data('initial-tab') || 'tab-Summary');

    validation.enableForm('#packageEditForm');
    validation.jumpToFailedTab('#packageEditForm');

    $("#clearRelease").click(function() {
        $('#<%=Package._Fields.RELEASE_ID.toString()%>').val("");
        $('#pkg_release').val("").attr("placeholder", "<liferay-ui:message key='click.to.link.a.release' />");
    });

    $('#formSubmit').click(
        function() {
            $('#packageEditForm').submit();
        });
    $('#cancelEditButton').on('click', cancel);
    $('#deletePackageButton').on('click', deletePackage);

    function cancel() {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>',
                portletURL = Liferay.PortletURL.createURL(baseUrl)
            <core_rt:if test="${not addMode}">
                    .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
            </core_rt:if>
            <core_rt:if test="${addMode}">
                    .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>')
            </core_rt:if>
                    .setParameter('<%=PortalConstants.PACKAGE_ID%>', '${pkg.id}');
            window.location = portletURL.toString() + window.location.hash;
    }

    function deletePackage() {
        var $dialog,
            data = $('#packageEditForm').data();

        function deletePackageInternal() {
            var baseUrl = data.deleteUrl,
                deleteURL = Liferay.PortletURL.createURL(baseUrl);
            window.location.href = deleteURL;
        }

        $dialog = dialog.open('#deletePackageDialog', {
            name: data.name
        }, function(submit, callback) {
            deletePackageInternal();
        });
    }
});
</script>
