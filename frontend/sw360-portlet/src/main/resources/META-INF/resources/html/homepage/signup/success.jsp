<%--
  ~ Copyright Siemens AG, 2013-2016, 2019. Part of the SW360 Portal Project.
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


<div class="jumbotron">
    <core_rt:if test="${not empty welcomePageGuideLine}">
        ${welcomePageGuideLine}
    </core_rt:if>
    <core_rt:if test="${empty welcomePageGuideLine}">
        <h1 class="display-4">Welcome to SW360!</h1>
        <p class="lead">
		SW360 is an open source software project that provides both a web application and a repository to collect,
		organize and make available information about software components. It establishes a central hub for software
		components in an organization.
        </p>
    </core_rt:if>
	<hr class="my-4">
	<div class="alert alert-success" role="alert">
		Your account has been created, but it is still inactive. An administrator will review it and activate it.
		You will be notified.
	</div>
	<div class="buttons">
		<span class="sign-in"><a class="btn btn-primary btn-lg" href="${ themeDisplay.getURLSignIn() }" role="button">Sign In</a></span>
	</div>
</div>
