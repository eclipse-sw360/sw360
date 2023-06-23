<%--
  ~ Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.Package" %>

<jsp:useBean id="packageSearch" type="java.util.List<org.eclipse.sw360.datahandler.thrift.packages.Package>" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="isSearchTruncated" class="java.lang.Boolean" scope="request" />

<core_rt:if test="${packageSearch.size() > 0 }" >
    <core_rt:forEach items="${packageSearch}" var="entry">
        <tr>
            <td>
                <div class="form-check">
                    <input type="checkbox" class="form-check-input" name="<portlet:namespace/>packageId" value="${entry.id}">
                    <input type="hidden" value="${entry.releaseId}">
                </div>
            </td>
            <td><sw360:out value="${entry.name}"/></td>
            <td><sw360:out value="${entry.version}"/></td>
            <td><sw360:DisplayLicenseCollection licenseIds="${entry.licenseIds}" scopeGroupId="${pageContext.getAttribute('scopeGroupId')}"/></td>
            <td><sw360:DisplayEnum value="${entry.packageManager}"/></td>
            <td><sw360:out value="${entry.purl}"/></td>
        </tr>
    </core_rt:forEach>
</core_rt:if>

<script>
    if("${isSearchTruncated}" == "true") {
        $("#pkgTruncationAlert").show();
    } else {
        $("#pkgTruncationAlert").hide();
    }
</script>