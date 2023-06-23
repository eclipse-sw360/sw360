<%--
  ~ Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@page import="org.eclipse.sw360.portal.common.PortalConstants"%>
<%@page import="org.eclipse.sw360.datahandler.thrift.packages.Package" %>
<%@page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="packageList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.packages.Package>"  scope="request"/>

<core_rt:forEach items="${packageList}" var="pkg" varStatus="loop">
    <core_rt:set var="uuid" value="${pkg.id}"/>
    <tr id="packageLinkRow_${uuid}" >
        <td>
            <div class="form-group">
                <input type="hidden" value="${pkg.id}" name="<portlet:namespace/><%=PortalConstants.PACKAGE_IDS%>">
                <input id="packageName" type="text" placeholder="<liferay-ui:message key="enter.name" />" class="form-control"
                    value="<sw360:out value="${pkg.name}"/>" readonly/>
            </div>
        </td>
        <td>
            <div class="form-group">
                <input id="packageVersion" type="text" placeholder="<liferay-ui:message key="enter.version" />" class="form-control"
                    value="<sw360:out value="${pkg.version}"/>" readonly/>
            </div>
        </td>
        <td>
            <div class="form-group">
                <input id="licenses" type="text" class="form-control"
                    value="<sw360:DisplayLicenseCollection licenseIds="${pkg.licenseIds}" displayLink="false" scopeGroupId="${pageContext.getAttribute('scopeGroupId')}" />" readonly/>
            </div>
        </td>
        <td>
            <div class="form-group">
                <input id="packageManager" type="text" class="form-control"
                    value="<sw360:out value="${pkg.packageManager}"/>" readonly/>
            </div>
        </td>
        <td class="content-middle">
            <svg class="action lexicon-icon" data-uuid="${uuid}" data-action="delete-package" data-package-name="<sw360:out value='${pkg.name} (${pkg.version})' jsQuoting="true"/>">
                <title><liferay-ui:message key="delete.package" /></title> <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
            </svg>
        </td>
    </tr>
</core_rt:forEach>
