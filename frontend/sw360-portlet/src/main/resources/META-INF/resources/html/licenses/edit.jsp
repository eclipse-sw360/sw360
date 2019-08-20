<%--
  ~ Copyright Siemens AG, 2016-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="java.util.ArrayList" %>

<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<portlet:actionURL var="updateURL" name="update">
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}" />
</portlet:actionURL>

<portlet:actionURL var="deleteURL" name="delete">
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}"/>
</portlet:actionURL>

<portlet:renderURL var="cancelToDetailURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_DETAIL%>" />
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}" />
</portlet:renderURL>

<portlet:renderURL var="cancelToViewURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_VIEW%>" />
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="${licenseDetail.id}" />
</portlet:renderURL>


<c:catch var="attributeNotFoundException">
    <jsp:useBean id="isUserAtLeastClearingAdmin" class="java.lang.String" scope="request" />
    <jsp:useBean id="licenseDetail" type="org.eclipse.sw360.datahandler.thrift.licenses.License" scope="request" />
    <jsp:useBean id="licenseTypeChoice" class="java.util.ArrayList" scope="request" />
    <core_rt:set  var="addMode"  value="${empty licenseDetail.id}" />
</c:catch>

<%@include file="/html/utils/includes/logError.jspf" %>
<core_rt:if test="${empty attributeNotFoundException || exceptionInBackend}">

    <div class="container" style="display: none;">
        <div class="row portlet-toolbar">
            <div class="col-auto">
                <div class="btn-toolbar" role="toolbar">
                    <div class="btn-group" role="group">
                        <core_rt:if test="${addMode}" >
                            <button type="button" id="formSubmit" class="btn btn-primary">Create License</button>
                        </core_rt:if>

                        <core_rt:if test="${not addMode}" >
                            <button type="button" id="formSubmit" class="btn btn-primary">Update License</button>
                        </core_rt:if>
                    </div>

                    <core_rt:if test="${not addMode}" >
                        <div class="btn-group" role="group">
                            <button id="deleteLicenseButton" type="button" class="btn btn-danger">Delete License</button>
                        </div>
                    </core_rt:if>

                    <div class="btn-group" role="group">
                        <core_rt:if test="${not addMode}" >
                            <button id="cancelEditButton" type="button" class="btn btn-light" onclick="window.location.href='<%=cancelToDetailURL%>' + window.location.hash">Cancel</button>
                        </core_rt:if>
                        <core_rt:if test="${addMode}" >
                            <button id="cancelEditButton" type="button" class="btn btn-light" onclick="window.location.href='<%=cancelToViewURL%>' + window.location.hash">Cancel</button>
                        </core_rt:if>
                    </div>
                </div>
            </div>
            <div class="col portlet-title column text-truncate" title="<sw360:out value="${licenseDetail.fullname}"/> (<sw360:out value="${licenseDetail.shortname}"/>)">
                <sw360:out value="${licenseDetail.fullname}"/> (<sw360:out value="${licenseDetail.shortname}"/>)
                <core_rt:if test="${licenseDetail.checked != true}">
                    <span class="badge badge-danger">UNCHECKED</span>
                </core_rt:if>
                <core_rt:if test="${licenseDetail.checked == true}">
                    <span class="badge badge-success">CHECKED</span>
                </core_rt:if>
            </div>
        </div>
        <div class="row">
            <div class="col">
                <form  id="licenseEditForm" name="licenseEditForm" action="<%=updateURL%>" class="needs-validation" method="post" novalidate
                  data-license-name="${licenseDetail.fullname} (${licenseDetail.shortname})"
                  data-delete-url="<%=deleteURL%>">
                    <%@include file="/html/licenses/includes/editDetailSummary.jspf"%>
                    <%@include file="/html/licenses/includes/editDetailText.jspf"%>
                </form>
            </div>
        </div>
    </div>
    <%@ include file="/html/utils/includes/pageSpinner.jspf" %>

    <div class="dialogs auto-dialogs">
        <div id="deleteLicenseDialog" class="modal fade" tabindex="-1" role="dialog">
            <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            <clay:icon symbol="question-circle" />
                            Delete License?
                        </h5>
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <p>Do you really want to delete the license <b data-name="name"></b>?</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-danger">Delete License</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <%--for javascript library loading --%>
    <%@ include file="/html/utils/includes/requirejs.jspf" %>
    <script>
        document.title = "${licenseDetail.shortname} - " + document.title;

        require(['jquery', 'modules/dialog', 'modules/validation', 'bridges/jquery-ui'], function($, dialog, validation) {
            $('#licenseEditForm').parents('.container:first').show().siblings('.container-spinner').hide();

            validation.enableForm('#licenseEditForm');

            $('#lic_shortname').autocomplete({
                source: <%=PortalConstants.LICENSE_IDENTIFIERS%>
            });

            $('#formSubmit').on('click', function() {
                $('#licenseEditForm').submit();
            });

            $('#deleteLicenseButton').on('click', deleteLicense);

            function deleteLicense() {
                var $dialog,
                    data = $('#licenseEditForm').data(),
                    name = data.licenseName;

                $dialog = dialog.open('#deleteLicenseDialog', {
                    name: name
                }, function(submit, callback) {
                    window.location.href = "<%=deleteURL%>";
                });
            }
        });
    </script>
</core_rt:if>

