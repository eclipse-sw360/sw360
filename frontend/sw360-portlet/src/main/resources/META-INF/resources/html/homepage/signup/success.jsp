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
        <h1 class="display-4"><liferay-ui:message key="welcome.to.sw360" /></h1>
        <p class="lead">
		<liferay-ui:message key="sw360.is.an.open.source.software.project.that.provides.both.a.web.application.and.a.repository.to.collect.organize.and.make.available.information.about.software.components.it.establishes.a.central.hub.for.software.components.in.an.organization" />
        </p>
    </core_rt:if>
	<hr class="my-4">
	<div class="alert alert-success" role="alert">
		<liferay-ui:message key="your.account.has.been.created.but.it.is.still.inactive.an.administrator.will.review.it.and.activate.it" />
		<liferay-ui:message key="you.will.be.notified" />
	</div>
	<div class="buttons">
		<span class="sign-in"><a class="btn btn-primary btn-lg" href="${ themeDisplay.getURLSignIn() }" role="button"><liferay-ui:message key="sign.in" /></a></span>
	</div>
</div>
