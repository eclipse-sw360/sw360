<%--
  ~ Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>


<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="licenseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.License>" scope="request"/>

<core_rt:if test="${licenseList.size()>0}">
    <core_rt:forEach items="${licenseList}" var="entry">

        <tr>
            <td>
                <input type="checkbox" name="id" value="<sw360:out value="${entry.id},${entry.fullname}"/>"/>
            </td>
            <td><sw360:out value="${entry.fullname}"/></td>
        </tr>
    </core_rt:forEach>
</core_rt:if>
<core_rt:if test="${licenseList.size() == 0}">
    <tr>
        <td colspan="2">
            <liferay-ui:message key="no.license.found.with.your.search" />
        </td>
    </tr>

</core_rt:if>
