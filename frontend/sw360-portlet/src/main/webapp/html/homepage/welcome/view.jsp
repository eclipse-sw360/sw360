<%--
  ~ Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
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


<h4>Welcome to SW360!</h4>

<p>This the entry page of the SW360 web application. SW360 is a component catalogue for managing software components and
    projects.</p>
<h5>Getting started<h5>
<core_rt:if test="${themeDisplay.signedIn}">
    <p style="font-weight: bold;">You are signed in, please go to the private pages on the top-right corner of this site:</p>
    <img src="<%=request.getContextPath()%>/images/welcome/select_private_pages.png" alt=""
         border="0" width="150"/><br/>
</core_rt:if>
<core_rt:if test="${not themeDisplay.signedIn}">
    <p style="font-weight: bold;"> In order to go ahead, please use the "Sign In" with your account. If you don&apos;t have an account, go to the <a href="/web/guest/sign-up">Sign Up</a> page to request one.</p>
</core_rt:if>
