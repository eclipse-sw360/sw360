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
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="userList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.users.User>"
             class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="how" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="usersearchGotTruncated" type="java.lang.Boolean" scope="request"/>

<core_rt:if test="${userList.size()>0}">
    <core_rt:forEach items="${userList}" var="entry">

        <tr>
            <td>
                <input
                <core_rt:if test="${how}">
                        type="checkbox"
                </core_rt:if>
                <core_rt:if test="${how ==false}">
                        type="radio"
                </core_rt:if>
                        name="id" value="<sw360:out value="${entry.givenname},${entry.lastname},${entry.email},${entry.department},${entry.fullname}"/>">
            </td>
            <td><sw360:out value="${entry.givenname}"/></td>
            <td><sw360:out value="${entry.lastname}"/></td>
            <td><sw360:DisplayUserEmail email="${entry.email}" bare="true"/></td>
            <td><sw360:out value="${entry.department}"/></td>
        </tr>
    </core_rt:forEach>
</core_rt:if>
<core_rt:if test="${userList.size() == 0}">
    <tr>
        <td colspan="5">
            No user found with your search.
        </td>
    </tr>

</core_rt:if>

<script>
    if("${usersearchGotTruncated}"=="true") {
        $("#truncationAlerter").show();
    } else {
        $("#truncationAlerter").hide();
    }
</script>
