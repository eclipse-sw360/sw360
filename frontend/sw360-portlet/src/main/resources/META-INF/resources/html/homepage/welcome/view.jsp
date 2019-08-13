<%--
  ~ Copyright Siemens AG, 2013-2016, 2019. Part of the SW360 Portal Project.
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

<div class="jumbotron">
	<h1 class="display-4">Welcome to SW360!</h1>
	<p class="lead">
		SW360 is an open source software project that provides both a web application and a repository to collect,
		organize and make available information about software components. It establishes a central hub for software
		components in an organization.
	</p>
	<hr class="my-4">
	<core_rt:if test="${themeDisplay.signedIn}">
		<h3>You are signed in, please go ahead using SW360!</h3>
		<div class="buttons">
			<a class="btn btn-primary btn-lg" href="/group/guest/home" role="button">Start</a>
		</div>
	</core_rt:if>
	<core_rt:if test="${not themeDisplay.signedIn}">
		<h3>In order to go ahead, please sign in or create a new account!</h3>
		<div class="buttons">
			<span class="sign-in"><a class="btn btn-primary btn-lg" href="${ themeDisplay.getURLSignIn() }" role="button">Sign In</a></span>
			<a class="btn btn-outline-primary btn-lg" href="/web/guest/sign-up" role="button">Create Account</a>
		</div>
	</core_rt:if>
</div>
