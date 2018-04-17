<%--
  ~ Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.portal.portlets.Sw360Portlet" %>
<%@ page import="org.eclipse.sw360.portal.portlets.components.ComponentPortlet" %>

<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>


<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>

<jsp:useBean id="componentList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Component>"
             class="java.util.ArrayList" scope="request"/>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>"
             class="java.util.ArrayList" scope="request"/>

<div id="mySubscriptionsDiv">
  <span>
    <h4>My Subscriptions</h4>
  </span>
    <div class="homepageListingRight">
  <core_rt:if test="${componentList.size() > 0}">
      <h4>Components</h4>
      <core_rt:forEach var="component" items="${componentList}">
          <li style="color: red">
              <sw360:DisplayComponentLink component="${component}"/><br>
          </li>
      </core_rt:forEach>
  </core_rt:if>

     <core_rt:if test="${releaseList.size() > 0}">
         <h4>Releases</h4>
         <core_rt:forEach var="release" items="${releaseList}">
             <li style="color: red">
                 <sw360:DisplayReleaseLink release="${release}"/><br>
             </li>
         </core_rt:forEach>
     </core_rt:if>

  <core_rt:if test="${componentList.size() == 0  and releaseList.size() == 0}">
      <p style="color: red">No subscriptions available</p>
  </core_rt:if>
 </div>
</div>
