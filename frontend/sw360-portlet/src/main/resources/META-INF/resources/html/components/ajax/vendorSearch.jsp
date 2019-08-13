<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<jsp:useBean id="vendorsSearch" type="java.util.List<org.eclipse.sw360.datahandler.thrift.vendors.Vendor>" class="java.util.ArrayList" scope="request"/>

<core_rt:if test="${vendorsSearch.size()>0}" >
    <core_rt:forEach items="${vendorsSearch}" var="entry">
        <tr>
            <td>
                <input type="radio" data-name="vendor" name="<portlet:namespace/>vendorId" value="<sw360:out value="${entry.id},${entry.fullname}"/>">
            </td>
            <td><sw360:out value="${entry.fullname}"/></td>
            <td><sw360:out value="${entry.shortname}"/></td>
            <td><sw360:out value="${entry.url}"/></td>
        </tr>
  </core_rt:forEach>
</core_rt:if>
