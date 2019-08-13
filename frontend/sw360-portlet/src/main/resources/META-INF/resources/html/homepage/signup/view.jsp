<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@ page import="com.liferay.portal.kernel.servlet.SessionMessages" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.UserGroup" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@include file="/html/init.jsp" %>

<%-- the following is needed by liferay to display error messages--%>

	<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%--
	<liferay-ui:success key="request_processed" message="<%=SessionMessages.get(request, "request_processed") != null ? SessionMessages.get(request, "request_processed").toString() : ""%>" />
--%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="createAccountURL" name="createAccount">
</portlet:actionURL>

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
		<h3>You may now create an account to sign in into SW360!</h3>
		<div id="createAccount" class="container">
			<form action="<%=createAccountURL%>" id="signup" class="needs-validation" method="post" novalidate>
				<div class="form-group">
					<label class="mandatory" for="given_name">First Name</label>
		            <input type="text" class="form-control" name="<portlet:namespace/><%=User._Fields.GIVENNAME%>" required
		                           value="<sw360:out value="${newuser.givenname}"/>" id="given_name">
		            <div class="invalid-feedback">
					Please enter your first name!
				</div>
				</div>
				<div class="form-group">
					<label class="mandatory" for="last_name">Last Name</label>
		            <input type="text" class="form-control" name="<portlet:namespace/><%=User._Fields.LASTNAME%>" required
		                           value="<sw360:out value="${newuser.lastname}"/>" id="last_name">
		            <div class="invalid-feedback">
					Please enter your last name!
				</div>
				</div>
				<div class="form-group">
					<label class="mandatory" for="email">Email</label>
		            <input type="email" class="form-control" name="<portlet:namespace/><%=User._Fields.EMAIL%>" required
		                           value="<sw360:out value="${newuser.email}"/>" id="email">
		            <div class="invalid-feedback">
					Please enter your first email!
				</div>
				</div>
				<div class="form-group">
					<label class="mandatory" for="department">Group</label>
                    <select class="form-control" id="department" name="<portlet:namespace/><%=User._Fields.DEPARTMENT%>" required>
                        <core_rt:forEach items="${organizations}" var="org">
                            <option value="${org.name}" class="textlabel stackedLabel"
                            <core_rt:if test="${org.name == newuser.department}"> selected="selected"</core_rt:if>
                            >${org.name}</option>
                        </core_rt:forEach>
                    </select>
                    <div class="invalid-feedback">
					Please select a group!
				</div>
				</div>
				<div class="form-group">
					<label class="mandatory" for="usergroup">Requested Role</label>
                    <select class="form-control" id="usergroup" name="<portlet:namespace/><%=User._Fields.USER_GROUP%>" required aria-describedby="usergroup-help">

                        <sw360:DisplayEnumOptions type="<%=UserGroup.class%>" selected="${newuser.userGroup}"/>
                    </select>
                    <div class="invalid-feedback">
					Please select a role!
				</div>
                    <small id="usergroup-help" class="form-text">
			<sw360:DisplayEnumInfo type="<%=UserGroup.class%>"/>
			Learn more about user groups.
                    </small>
				</div>
				<div class="form-group">
					<label class="mandatory" for="externalid">External ID</label>
			<input type="text" class="form-control" name="<portlet:namespace/><%=User._Fields.EXTERNALID%>" required
	                           value="${newuser.externalid}" id="externalid">
	                <div class="invalid-feedback">
					Please enter your external id!
				</div>
				</div>
				<div class="form-group">
					<label class="mandatory" for="password">Password</label>
                    <input type="password" class="form-control" name="<portlet:namespace/><%=PortalConstants.PASSWORD%>" required
                           value="" id="password">
                    <div class="invalid-feedback">
					Please enter a password!
				</div>
				</div>
				<div class="form-group">
					<label class="mandatory" for="password_repeat">Repeat Password</label>
                    <input type="password" class="form-control" name="<portlet:namespace/><%=PortalConstants.PASSWORD_REPEAT%>" required
                           value="" id="password_repeat">
                    <div class="invalid-feedback">
					Please confirm the password!
				</div>
				</div>
			<button type="submit" class="btn btn-primary">Create Account</button>
		    </form>
		</div>
	</core_rt:if>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
	require(['modules/validation'], function(validation) {
		validation.enableForm('#signup');
		validation.confirmField('#password', '#password_repeat');
	});
</script>
