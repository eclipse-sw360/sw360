<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vendors.Vendor" %>


<jsp:useBean id="vendor" class="org.eclipse.sw360.datahandler.thrift.vendors.Vendor" scope="request" />

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>"  scope="request"/>
<jsp:useBean id="documentID" class="java.lang.String" scope="request" />

<core_rt:set  var="addMode"  value="${empty vendor.id}" />

<portlet:actionURL var="updateURL" name="updateVendor">
    <portlet:param name="<%=PortalConstants.VENDOR_ID%>" value="${vendor.id}" />
</portlet:actionURL>

<portlet:actionURL var="deleteVendorURL" name="removeVendor">
    <portlet:param name="<%=PortalConstants.VENDOR_ID%>" value="${vendor.id}"/>
</portlet:actionURL>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
<script src="<%=request.getContextPath()%>/webjars/jquery/1.12.4/jquery.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/jquery.validate.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/additional-methods.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.min.js"></script>


<div id="where" class="content1">
    <p class="pageHeader"><span class="pageHeaderBigSpan"><sw360:out value="${vendor.fullname}"/></span>
        <core_rt:if test="${not addMode}" >
            <input type="button" class="addButton" onclick="deleteConfirmed('Do you really want to delete the vendor <b><sw360:out value="${vendor.fullname}"/></b> ?', deleteVendor)"
                   value="Delete <sw360:out value="${vendor.fullname}"/>">
        </core_rt:if>
    </p>
</div>

<div id="editField" class="content2">

    <form  id="vendorEditForm" name="vendorEditForm" action="<%=updateURL%>" method="post" >
        <table class="table info_table" id="VendorEdit">
            <thead>
            <tr>
                <th colspan="3" class="headlabel">Edit Vendor</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td width="30%">
                    <label class="textlabel stackedLabel mandatory" for="vendorFullname">Full Name</label>
                    <input id="vendorFullname" type="text" required class="toplabelledInput" placeholder="Enter vendor fullname" name="<portlet:namespace/><%=Vendor._Fields.FULLNAME%>"
                           value="<sw360:out value="${vendor.fullname}"/>" />
                </td>

                <td width="30%">
                    <label class="textlabel stackedLabel mandatory" for="vendorShortname">Short Name</label>
                    <input id="vendorShortname" type="text" required class="toplabelledInput" placeholder="Enter vendor short name" name="<portlet:namespace/><%=Vendor._Fields.SHORTNAME%>"
                           value="<sw360:out value="${vendor.shortname}"/>" />
                </td>

                <td width="30%">
                    <label class="textlabel stackedLabel mandatory" for="vendorURL">URL</label>
                    <input id="vendorURL" type="text" required class="toplabelledInput" placeholder="Enter vendor url" name="<portlet:namespace/><%=Vendor._Fields.URL%>"
                           value="<sw360:out value="${vendor.url}"/>" />
                </td>
            </tr>

            </tbody>
        </table>
        <core_rt:if test="${not addMode}" >
            <input type="submit" value="Update Vendor" class="addButton">
            <input type="button" value="Cancel" onclick="cancel()" class="cancelButton">
        </core_rt:if>
        <core_rt:if test="${addMode}" >
            <input type="submit" value="Add Vendor" class="addButton">
            <input type="button" value="Cancel" onclick="cancel()" class="cancelButton">
        </core_rt:if>
    </form>

    <core_rt:if test="${releaseList.size() > 0}" >
        <p>Used by the following release(s)</p>
        <table style="padding-left: 3px; padding-right: 3px"> <tr>
        <core_rt:forEach var="release" items="${releaseList}" varStatus="loop">
            <td><sw360:DisplayReleaseLink release="${release}" /></td>
            <core_rt:if test="${loop.count > 0 and  loop.count %  4 == 0}" ></tr> <tr> </core_rt:if>
        </core_rt:forEach>
        </tr>
        </table>
    </core_rt:if>

</div>

<script>
    function cancel() {
        var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
        var portletURL = Liferay.PortletURL.createURL( baseUrl )
                <core_rt:if test="${not addMode}" >
                .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_DETAIL%>')
                </core_rt:if>
                <core_rt:if test="${addMode}" >
                .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
                </core_rt:if>
                .setParameter('<%=PortalConstants.VENDOR_ID%>','${vendor.id}');
        window.location = portletURL.toString();
    }

    function deleteVendor() {
        window.location.href = '<%=deleteVendorURL%>';
    }

    var contextpath;
    $( document ).ready(function() {
        contextpath = '<%=request.getContextPath()%>';
        $('#vendorEditForm').validate({
            ignore: [],
            invalidHandler: invalidHandlerShowErrorTab
        });
    });

</script>
