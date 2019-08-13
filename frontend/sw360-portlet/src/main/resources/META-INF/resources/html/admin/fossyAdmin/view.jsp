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

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.portal.common.FossologyConnectionHelper" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="getPubkeyURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.FOSSOLOGY_GET_PUBKEY%>"/>
</portlet:resourceURL>

<core_rt:set var="fossologyEnabled" value="<%=FossologyConnectionHelper.getInstance().isFossologyConnectionEnabled()%>"/>
<core_rt:choose>
	<core_rt:when test="${fossologyEnabled}">
	    <core_rt:set var="currentConnStatus" value="SUCCESS" />
	</core_rt:when>
	<core_rt:otherwise>
		<core_rt:set var="currentConnStatus" value="FAILURE" />
	</core_rt:otherwise>
</core_rt:choose>

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Fossology Connection Administration</span> </p>
<h4 class = "headerMarginTop"><u>Public Key</u></h4>
<div><input type="button" class="addButton" onclick="window.location.href='<%=getPubkeyURL%>'" value="Download Public Key"></div>
<h4 class = "headerMarginTop"><u>Fossology Connection</u></h4>
<h5>Fossology Connection Status: <span id = "checkResult">${currentConnStatus}</span></h5>
<div><button class="addButton" onclick="checkConnection('checkResult')">Check connectivity To Fossology Server</button></div>

<portlet:actionURL var="setFingerPrintsURL" name="setFingerPrints"/>
<h4 class="headerMarginTop"><u>Fossology Fingerprint</u></h4>
<div>
<jsp:useBean id="fingerPrints"
             type="java.util.List<org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint>"
             scope="request"/>
<form id="FingerPrintForm" action="<%=setFingerPrintsURL%>" method="post">

    <core_rt:if test="${fingerPrints.size()>0}">
        <h5 class="headerMarginButtom"><span>Known FingerPrints</span></h5>

        <core_rt:forEach items="${fingerPrints}" var="fingerPrint" varStatus="loop">
            <label for="FingerPrint${loop.count}">${fingerPrint.fingerPrint}</label>
            <input type="checkbox" id="FingerPrint${loop.count}"
                   <core_rt:if test="${fingerPrint.trusted}">checked="" </core_rt:if>
                   value="on"
                   name="<portlet:namespace/>${fingerPrint.fingerPrint}"> <br>
        </core_rt:forEach>
        <input type="submit" value="Accept fingerprints" class="addButton">
    </core_rt:if>

    <core_rt:if test="${fingerPrints.size()<1}">
        <h5 class="headerMarginButtom">No fossology finger print in the system</h5>
    </core_rt:if>
</form>
</div>

<h4 class="headerMarginTop"><u>Deploy Scripts to Fossology Server</u></h4>
<div>
<button class="addButton" onclick="deployScripts('deployResult')">Deploy Scripts To Fossology Server</button>
<span id="deployResult"></span>
</div>

<portlet:resourceURL var="checkConnectionURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.FOSSOLOGY_CHECK_CONNECTION%>"/>
</portlet:resourceURL>
<portlet:resourceURL var="deployScriptsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.FOSSOLOGY_DEPLOY_SCRIPTS%>"/>
</portlet:resourceURL>


<script>

    function doAjax(url, $resultElement) {
        $resultElement.text("...");
        $.ajax({
            type: 'POST',
            url: url,
            success: function (data) {
                $resultElement.text(data.result);
            },
            error: function () {
                $resultElement.text("error");
            }
        });
    }

    function checkConnection(id) {
        doAjax('<%=checkConnectionURL%>', $('#' + id));
    }

    function deployScripts(id) {
        doAjax('<%=deployScriptsURL%>', $('#' + id));
    }

</script>
