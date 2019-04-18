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
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.Todo" %>


<jsp:useBean id="todo" class="org.eclipse.sw360.datahandler.thrift.licenses.Todo" scope="request" />



<portlet:actionURL var="addURL" name="addTodo">
</portlet:actionURL>


<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/themes/base/jquery-ui.min.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-confirm2/dist/jquery-confirm.min.css">
<script src="<%=request.getContextPath()%>/webjars/jquery/dist/jquery.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/dist/jquery.validate.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/dist/additional-methods.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-confirm2/dist/jquery-confirm.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/jquery-ui.min.js"></script>


<div id="where" class="content1">
    <p class="pageHeader"><span class="pageHeaderBigSpan"></span>
    </p>
</div>

<div id="editField" class="content2">

    <form  id="todoAddForm" name="todoAddForm" action="<%=addURL%>" method="post" >
        <table class="table info_table" id="todoAddTable">
            <thead>
            <tr>
                <th colspan="3" class="headlabel">Add TODO</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td width="30%">
                    <label class="textlabel stackedLabel mandatory" for="todoTitle">Title</label>
                    <input id="todoTitle" type="text" required class="toplabelledInput" placeholder="Enter TODO Title" name="<portlet:namespace/><%=Todo._Fields.TITLE%>"/>
                </td>

                <td width="50%">
                    <label class="textlabel stackedLabel mandatory" for="todoText">Text</label>
                    <input id="todoText" type="text" required class="toplabelledInput" placeholder="Enter TODO Text" name="<portlet:namespace/><%=Todo._Fields.TEXT%>"/>
                </td>

                <td width="10%">
                    <label class="textlabel stackedLabel mandatory" for="todoValidForProject">Valid For Projects</label>
                    <input id="todoValidForProject" type="checkbox" class="toplabelledInput" name="<portlet:namespace/><%=Todo._Fields.VALID_FOR_PROJECT%>"/>
                </td>
            </tr>

            </tbody>
        </table>
        <input type="submit" value="Add Todo" class="addButton">
        <input type="button" value="Cancel" onclick="cancel()" class="cancelButton">
    </form>


</div>

<script>
    function cancel() {
        var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
        var portletURL = Liferay.PortletURL.createURL( baseUrl )
            .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
        window.location = portletURL.toString();
    }


    var contextpath;
    $( document ).ready(function() {
        contextpath = '<%=request.getContextPath()%>';
        $('#todoAddForm').validate({
            ignore: [],
            invalidHandler: invalidHandlerShowErrorTab
        });
    });

</script>
