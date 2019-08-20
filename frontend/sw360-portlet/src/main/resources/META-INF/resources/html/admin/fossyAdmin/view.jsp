<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
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
<portlet:actionURL var="setFingerPrintsURL" name="setFingerPrints"/>
<portlet:resourceURL var="checkConnectionURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.FOSSOLOGY_CHECK_CONNECTION%>"/>
</portlet:resourceURL>
<portlet:resourceURL var="deployScriptsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.FOSSOLOGY_DEPLOY_SCRIPTS%>"/>
</portlet:resourceURL>

<jsp:useBean id="fingerPrints"
             type="java.util.List<org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint>"
             scope="request"/>

<core_rt:set var="fossologyEnabled" value="<%=FossologyConnectionHelper.getInstance().isFossologyConnectionEnabled()%>"/>
<core_rt:choose>
	<core_rt:when test="${fossologyEnabled}">
	    <core_rt:set var="currentConnStatus" value="SUCCESS" />
        <core_rt:set var="currentConnStatusBadge" value="badge-success" />
	</core_rt:when>
	<core_rt:otherwise>
		<core_rt:set var="currentConnStatus" value="FAILURE" />
        <core_rt:set var="currentConnStatusBadge" value="badge-danger" />
	</core_rt:otherwise>
</core_rt:choose>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" onclick="window.location.href='<%=getPubkeyURL%>'">Download Publickey</button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="Fossology Connection Administration">
					Fossology Connection Administration
				</div>
            </div>
            <div class="row mb-2">
                <div class="col">
			        <button type="button" class="btn btn-secondary btn-sm text-left" data-action="check-connectivity">Check connectivity to Fossology server</button>
                    <span id="checkResult" class="badge ${currentConnStatusBadge} ml-3">${currentConnStatus}</span>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <button type="button" class="btn btn-secondary btn-sm text-left" data-action="deploy-scripts">Deploy Scripts To Fossology Server</button>
                    <span id="deployResult" class="badge badge-light ml-3">Unknown</span>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <h4 class="mt-4">Fingerprints</h4>
                    <form id="FingerPrintForm" action="<%=setFingerPrintsURL%>" method="post" class="form">
                        <core_rt:if test="${fingerPrints.size() > 0}">
                            <core_rt:forEach items="${fingerPrints}" var="fingerPrint" varStatus="loop">
                                <div class="form-check">
                                    <input type="checkbox" id="FingerPrint${loop.count}" class="form-check-input"
                                        <core_rt:if test="${fingerPrint.trusted}">checked="" </core_rt:if>
                                        value="on"
                                        name="<portlet:namespace/>${fingerPrint.fingerPrint}">
                                    <label for="FingerPrint${loop.count}" class="form-check-label">${fingerPrint.fingerPrint}</label>
                                </div>
                            </core_rt:forEach>
                            <button type="submit" class="btn btn-secondary">Accept fingerprints</button>
                        </core_rt:if>
                        <core_rt:if test="${fingerPrints.size() < 1}">
                            <div class="alert alert-info">No fossology finger print in the system</div>
                        </core_rt:if>
                    </form>
                </div>
            </div>
		</div>
	</div>
</div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery' ], function($) {
        $('button[data-action="check-connectivity"]').on('click', function() {
            checkConnection('checkResult');
        });

        $('button[data-action="deploy-scripts"]').on('click', function() {
            deployScripts('deployResult');
        });

        function doAjax(url, $resultElement) {
            $resultElement.addClass('badge-light');
            $resultElement.removeClass('badge-danger');
            $resultElement.removeClass('badge-success');
            $resultElement.text("...");

            $.ajax({
                type: 'POST',
                url: url,
                success: function (data) {
                    $resultElement.removeClass('badge-light');
                    if(data.result === 'FAILURE') {
                        $resultElement.addClass('badge-danger');
                    } else {
                        $resultElement.addClass('badge-success');
                    }

                    $resultElement.text(data.result);
                },
                error: function () {
                    $resultElement.removeClass('badge-light');
                    $resultElement.addClass('badge-danger');
                    $resultElement.text("Error");
                }
            });
        }

        function checkConnection(id) {
            doAjax('<%=checkConnectionURL%>', $('#' + id));
        }

        function deployScripts(id) {
            doAjax('<%=deployScriptsURL%>', $('#' + id));
        }
    });
</script>
