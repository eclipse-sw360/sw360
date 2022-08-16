<%--
  ~ Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
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

<jsp:useBean id="primaryDepartment" type="java.lang.String" scope="request" />
<jsp:useBean id="secondaryDepartmentList" type="java.util.List<java.lang.String>" scope="request" />

<tr>
    <td>
        <input type="radio" data-name="department" name="<portlet:namespace/>departmentId" value="<sw360:out value="${primaryDepartment},${primaryDepartment}"/>">
    </td>
    <td><sw360:out value="${primaryDepartment}"/></td>
    <td>PRIMARY</td>
</tr>

<core_rt:if test="${secondaryDepartmentList.size()>0}" >
    <core_rt:forEach items="${secondaryDepartmentList}" var="secondaryDepartment">
        <tr>
            <td>
                <input type="radio" data-name="department" name="<portlet:namespace/>departmentId" value="<sw360:out value="${secondaryDepartment},${secondaryDepartment}"/>">
            </td>
            <td><sw360:out value="${secondaryDepartment}"/></td>
            <td>SECONDARY</td>
        </tr>
  </core_rt:forEach>
</core_rt:if>
