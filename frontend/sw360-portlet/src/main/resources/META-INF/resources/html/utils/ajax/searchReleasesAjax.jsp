<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
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

<%@ page import="org.eclipse.sw360.datahandler.thrift.components.Release" %>

<jsp:useBean id="releaseSearch" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>" class="java.util.ArrayList" scope="request"/>

<core_rt:if test="${releaseSearch.size()>0}" >
    <core_rt:forEach items="${releaseSearch}" var="entry">
        <tr>
            <td>
                <div class="form-check">
                    <input type="checkbox" class="form-check-input" name="<portlet:namespace/>releaseid" value="${entry.id}">
                    <input type="hidden" value="${entry.componentId}">
                </div>
            </td>
            <td><sw360:out value="${entry.vendor.fullname}"/></td>
            <td><sw360:out value="${entry.name}"/></td>
            <td><sw360:out value="${entry.version}"/></td>
            <td><sw360:DisplayEnum value="${entry.clearingState}"/></td>
            <td><sw360:DisplayEnum value="${entry.mainlineState}"/></td>
        </tr>
    </core_rt:forEach>
</core_rt:if>
