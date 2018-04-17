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

<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.UserGroup" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>


<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:actionURL var="createAccountURL" name="createAccount">
</portlet:actionURL>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
<script src="<%=request.getContextPath()%>/webjars/jquery/1.12.4/jquery.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/jquery.validate.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/additional-methods.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.min.js"></script>

<h4>Welcome to SW360!</h4>

<core_rt:if test="${themeDisplay.signedIn}">
    <p style="font-weight: bold;">You are signed in, please go to the private pages on the top-right corner of this site. You do not need to sign up.</p>
    <img src="<%=request.getContextPath()%>/images/welcome/select_private_pages.png" alt=""
         border="0" width="150"/><br/>
</core_rt:if>
<core_rt:if test="${not themeDisplay.signedIn}">
    <h5>Sign Up For an Account<h5>
<div id="createAccount">
    <form action="<%=createAccountURL%>" id="signup_form" method="post">
        <table>
            <thead>
            <tr>
                <th class="infoheading">
                    Create Account
                </th>
            </tr>
            </thead>
            <tbody style="background-color: #f8f7f7; border: none;">
            <tr>
                <td>
                    <label class="textlabel mandatory" for="given_name">First Name</label>
                    <input type="text" name="<portlet:namespace/><%=User._Fields.GIVENNAME%>" required=""
                           value="<sw360:out value="${newuser.givenname}"/>" id="given_name">
                </td>
            </tr>
            <tr>
                <td>
                    <label class="textlabel mandatory" for="last_name">Last Name</label>
                    <input type="text" name="<portlet:namespace/><%=User._Fields.LASTNAME%>" required=""
                           value="<sw360:out value="${newuser.lastname}"/>" id="last_name">
                </td>
            </tr>
            <tr>
                <td>
                    <label class="textlabel mandatory" for="email">Email</label>
                    <input type="text" name="<portlet:namespace/><%=User._Fields.EMAIL%>" required=""
                           value="<sw360:out value="${newuser.email}"/>" id="email">
                </td>
            </tr>
            <tr>
                <td>
                    <label class="textlabel mandatory" for="department">Group</label>
                    <select class="toplabelledInput" id="department" name="<portlet:namespace/><%=User._Fields.DEPARTMENT%>"
                                        style="min-width: 162px; min-height: 28px;">
                        <core_rt:forEach items="${organizations}" var="org">
                            <option value="${org.name}" class="textlabel stackedLabel"
                            <core_rt:if test="${org.name == newuser.department}"> selected="selected"</core_rt:if>
                            >${org.name}</option>
                        </core_rt:forEach>
                    </select>
                </td>
            </tr>
            <tr>
                <td>
                    <label class="textlabel mandatory" for="usergroup">Requested Role</label>
                    <select class="toplabelledInput" id="usergroup" name="<portlet:namespace/><%=User._Fields.USER_GROUP%>"
                            style="min-width: 162px; min-height: 28px;">

                        <sw360:DisplayEnumOptions type="<%=UserGroup.class%>" selected="${newuser.userGroup}"/>
                    </select>
                    <sw360:DisplayEnumInfo type="<%=UserGroup.class%>"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label class="textlabel mandatory" for="externalid">External ID</label>
                    <input type="text" name="<portlet:namespace/><%=User._Fields.EXTERNALID%>" required=""
                           value="${newuser.externalid}" id="externalid">
                </td>
            </tr>
            <tr>
                <td>
                    <label class="textlabel mandatory" for="password">Password</label>
                    <input type="password" name="<portlet:namespace/><%=PortalConstants.PASSWORD%>" required=""
                           value="" id="password">
                </td>
            </tr>
            <tr>
                <td>
                    <label class="textlabel mandatory" for="password_repeat">Repeat Password</label>
                    <input type="password" name="<portlet:namespace/><%=PortalConstants.PASSWORD_REPEAT%>" required=""
                           value="" id="password_repeat">
                </td>
            </tr>
            </tbody>
        </table>
        <br/>
        <input type="submit" class="addButton" value="Sign Up">
    </form>
</div>
<script>

    Liferay.on('allPortletsReady', function() {
        $('#signup_form').validate({
            rules: {
                "<portlet:namespace/><%=PortalConstants.PASSWORD%>": "required",
                "<portlet:namespace/><%=PortalConstants.PASSWORD_REPEAT%>": {
                    equalTo: '#password'
                }
            },
            messages: {
                "<portlet:namespace/><%=PortalConstants.PASSWORD_REPEAT%>": {
                    equalTo: "Passwords must match."
                }
            }
        });
    });

</script>
</core_rt:if>
