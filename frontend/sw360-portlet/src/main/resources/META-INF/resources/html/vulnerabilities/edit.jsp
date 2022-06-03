<%--
  ~ Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.PortalUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityAccessAuthentication"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityAccessComplexity"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityAccessVector"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityImpact"%>
<jsp:useBean id="vulnerability" class="org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability" scope="request" />
<jsp:useBean id="documentID" class="java.lang.String" scope="request" />

<core_rt:set var="addMode"  value="${empty vulnerability.id}" />

<portlet:actionURL var="updateVulnerabilityURL" name="updateVulnerability">
    <portlet:param name="<%=PortalConstants.VULNERABILITY_ID%>" value="${vulnerability.id}" />
</portlet:actionURL>

<portlet:actionURL var="deleteVulnerabilityURL" name="removeVulnerability">
    <portlet:param name="<%=PortalConstants.VULNERABILITY_ID%>" value="${vulnerability.id}"/>
</portlet:actionURL>

<style>
    input::-webkit-outer-spin-button,
    input::-webkit-inner-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }
    input[type=number] {
      -moz-appearance: textfield;
    }
</style>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <core_rt:if test="${addMode}" >
                            <div class="btn-group">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="create.vulnerability" /></button>
                            </div>
                        </core_rt:if>
						<core_rt:if test="${not addMode}" >
                            <div class="btn-group">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="update.vulnerability" /></button>
                            </div>
                            <div class="btn-group">
                                <button type="button" class="btn btn-danger" data-action="delete" data-external-id="<sw360:out value="${vulnerability.externalId}"/>"><liferay-ui:message key="delete.vulnerability" /></button>
						    </div>
                        </core_rt:if>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message key="cancel" /></button>
                        </div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<sw360:out value="${vulnerability.title}"/>">
					<sw360:out value="${vulnerability.title}"/>
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <form id="vulnerabilityEditForm" method="post" class="form needs-validation" action="<%=updateVulnerabilityURL%>" >
                        <table id="VulnerabilityEdit" class="table edit-table four-columns">
                            <thead>
                                <tr>
                                    <th colspan="4"><liferay-ui:message key="detail.vulnerability" /></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityExternalId" class="mandatory"><liferay-ui:message key="vulnerability.external.id" /></label>
                                            <input id="vulnerabilityExternalId" required type="text" class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.external.id" />"  value="<sw360:out value="${vulnerability.externalId}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.EXTERNAL_ID%>"/>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.external.id" />
                                            </div>
                                            <div class="invalid-feedback" id="externalIdFeedBack">
                                            </div>
                                        </div>
                                    </td>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityTitle"><liferay-ui:message key="vulnerability.title" /></label>
                                            <input id="vulnerabilityTitle" type="text" class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.title" />" value="<sw360:out value="${vulnerability.title}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.TITLE%>"/>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.title" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="4">
                                        <div class="form-group">
                                            <label for="vulnerabilityDescription"><liferay-ui:message key="vulnerability.description" /></label>
                                            <textarea class="form-control" id="vulnerabilityDescription"  name="<portlet:namespace/><%=Vulnerability._Fields.DESCRIPTION%>"  rows="4"
                                             placeholder="<liferay-ui:message key="enter.vulnerability.description" />" ><sw360:out value="${vulnerability.description}"/></textarea>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.description" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityPriority"><liferay-ui:message key="vulnerability.priority" /></label>
                                            <input id="vulnerabilityPriority" type="text"  class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.priority" />" value="<sw360:out value="${vulnerability.priority}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.PRIORITY%>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.vulnerability.priority" />
                                            </div>
                                        </div>
                                    </td>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityPriorityText"><liferay-ui:message key="vulnerability.priority.text" /></label>
                                            <input id="vulnerabilityPriorityText" type="text"  class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.priority.text" />" value="<sw360:out value="${vulnerability.priorityText}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.PRIORITY_TEXT%>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.vulnerability.priority.text" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityAction"><liferay-ui:message key="vulnerability.action" /></label>
                                            <input id="vulnerabilityAction" type="text"  class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.action" />" value="<sw360:out value="${vulnerability.action}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.ACTION%>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.vulnerability.action" />
                                            </div>
                                        </div>
                                    </td>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityLegalNotice"><liferay-ui:message key="vulnerability.legal.notice" /></label>
                                            <input id="vulnerabilityLegalNotice" type="text"  class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.legal.notice" />" value="<sw360:out value="${vulnerability.legalNotice}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.LEGAL_NOTICE%>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.vulnerability.legal.notice" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="2">
                                        <div class="form-group" >
                                            <label for="vulnerabilityCwe"><liferay-ui:message key="vulnerability.cwe" /></label>
                                            <div style="display:flex;width:100%;">
                                                <label class="sub-label" style="padding-top:10px;font-weight:400;padding-right:10px">CWE-</label>
                                                <input id="vulnerabilityCwe" type="text"  class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.cwe" />" value="<sw360:out value="${vulnerability.cwe.substring(4)}"/>"
                                                    name="<portlet:namespace/><%=Vulnerability._Fields.CWE%>" />
                                            <div>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.vulnerability.cwe" />
                                            </div>
                                        </div>
                                    </td>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityExtendedDescription"><liferay-ui:message key="vulnerability.extended.description" /></label>
                                            <input id="vulnerabilityExtendedDescription" type="text"  class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.extended.description" />" value="<sw360:out value="${vulnerability.extendedDescription}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.EXTENDED_DESCRIPTION%>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.vulnerability.extended.description" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td colspan="2">
                                        <div class="form-group">
                                            <label for="vulnerabilityScore" class="mandatory"><liferay-ui:message key="vulnerability.cvss.score" /></label>
                                            <input id="vulnerabilityScore" required type="number" step="0.01" onkeydown="return event.keyCode !== 69 && event.keyCode !== 107 && event.keyCode !== 109 && event.keyCode !== 189"
                                                class="form-control" placeholder="<liferay-ui:message key="enter.vulnerability.cvss.score" />" value="<sw360:out value="${vulnerability.cvss}"/>"
                                                name="<portlet:namespace/><%=Vulnerability._Fields.CVSS%>" onKeyPress="if(this.value.length==4) return false;" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.cvss.score" />
                                            </div>
                                            <div class="cvss-invalid" style="color:red;display:none;font-weight:bold">
                                                 <liferay-ui:message key="cvss.score.must.be.positive.number.and.smaller.than.10" />
                                            </div>
                                        </div>
                                    </td>
                                    <core_rt:if test = "${(vulnerability.cvssTime != null) and (fn:length(vulnerability.cvssTime) >= 19)}">
                                        <td style="width:25%">
                                             <div class="form-group">
                                                 <label for="vulnerabilityCvssDate"><liferay-ui:message key="vulnerability.cvss.date" /></label>
                                                 <input id="vulnerabilityCvssDate" class="form-control datepicker" type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                   name="<portlet:namespace/><%=PortalConstants.CVSS_DATE%>"
                                                   placeholder="<liferay-ui:message key="cvss.date.yyyy.mm.dd" />"
                                                   autocomplete="off"
                                                   value = "<sw360:out value="${vulnerability.cvssTime.substring(0,10)}"/>"
                                                   />
                                                 <div class="invalid-feedback">
                                                     <liferay-ui:message key="please.enter.cvss.date" />
                                                 </div>
                                             </div>
                                        </td>
                                        <td style="width:25%">
                                            <div class="form-group">
                                                <label for="vulnerabilityCvssTime"><liferay-ui:message key="vulnerability.cvss.time" /></label>
                                                <input id="vulnerabilityCvssTime" type="time" step="1" class="form-control spdx-time needs-validation" rule="required"
                                                    name="<portlet:namespace/><%=PortalConstants.CVSS_TIME%>"
                                                    value = "<sw360:out value="${vulnerability.cvssTime.substring(11,19)}"/>"
                                                 />
                                                <div id="createdTime-error-messages">
                                                     <div class="invalid-feedback" rule="required">
                                                          Invalid format!
                                                     </div>
                                                </div>
                                            </div>
                                        </td>
                                    </core_rt:if>
                                    <core_rt:if test = "${(vulnerability.cvssTime == null) or (fn:length(vulnerability.cvssTime) < 19)}">
                                        <td style="width:25%">
                                             <div class="form-group">
                                                 <label for="vulnerabilityCvssDate"><liferay-ui:message key="vulnerability.cvss.date" /></label>
                                                 <input id="vulnerabilityCvssDate" class="form-control datepicker" type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                   name="<portlet:namespace/><%=PortalConstants.CVSS_DATE%>"
                                                   placeholder="<liferay-ui:message key="cvss.date.yyyy.mm.dd" />"
                                                   autocomplete="off"
                                                   />
                                                 <div class="invalid-feedback">
                                                     <liferay-ui:message key="please.enter.cvss.date" />
                                                 </div>
                                             </div>
                                        </td>
                                        <td style="width:25%">
                                            <div class="form-group">
                                                <label for="vulnerabilityCvssTime"><liferay-ui:message key="vulnerability.cvss.time" /></label>
                                                <input id="vulnerabilityCvssTime" type="time" step="1" class="form-control spdx-time needs-validation" rule="required"
                                                    name="<portlet:namespace/><%=PortalConstants.CVSS_TIME%>"
                                                 />
                                                <div id="createdTime-error-messages">
                                                     <div class="invalid-feedback" rule="required">
                                                          Invalid format!
                                                     </div>
                                                </div>
                                            </div>
                                        </td>
                                    </core_rt:if>
                                </tr>
                                <tr>
                                    <core_rt:if test = "${(vulnerability.publishDate != null) and (fn:length(vulnerability.publishDate) >= 19)}">
                                        <td style="width:25%">
                                             <div class="form-group">
                                                 <label for="publishDate"><liferay-ui:message key="vulnerability.publish.date" /></label>
                                                 <input id="publishDate" class="form-control datepicker"  type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                   name="<portlet:namespace/><%=PortalConstants.PUBLISH_DATE%>"
                                                   placeholder="<liferay-ui:message key="publish.date.yyyy.mm.dd" />"
                                                   autocomplete="off"
                                                   value = "<sw360:out value="${vulnerability.publishDate.substring(0,10)}"/>"
                                                   />
                                                 <div class="invalid-feedback">
                                                     <liferay-ui:message key="please.enter.publish.date" />
                                                 </div>
                                             </div>
                                        </td>

                                        <td style="width:25%">
                                            <div class="form-group">
                                                <label for="publishTime"><liferay-ui:message key="vulnerability.publish.time" /></label>
                                                <input id="publishTime" type="time" step="1" class="form-control spdx-time needs-validation" rule="required"
                                                   name="<portlet:namespace/><%=PortalConstants.PUBLISH_TIME%>"
                                                   value = "<sw360:out value="${vulnerability.publishDate.substring(11,19)}"/>">
                                                <div id="createdTime-error-messages">
                                                     <div class="invalid-feedback" rule="required">
                                                          Invalid format!
                                                     </div>
                                                 </div>
                                            </div>
                                        </td>
                                    </core_rt:if>

                                    <core_rt:if test = "${(vulnerability.publishDate == null) or (fn:length(vulnerability.publishDate) < 19)}">
                                        <td style="width:25%">
                                             <div class="form-group">
                                                 <label for="publishDate"><liferay-ui:message key="vulnerability.publish.date" /></label>
                                                 <input id="publishDate" class="form-control datepicker"  type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                   name="<portlet:namespace/><%=PortalConstants.PUBLISH_DATE%>"
                                                   placeholder="<liferay-ui:message key="publish.date.yyyy.mm.dd" />"
                                                   autocomplete="off"
                                                   />
                                                 <div class="invalid-feedback">
                                                     <liferay-ui:message key="please.enter.publish.date" />
                                                 </div>
                                             </div>
                                        </td>

                                        <td style="width:25%">
                                            <div class="form-group">
                                                <label for="publishTime"><liferay-ui:message key="vulnerability.publish.time" /></label>
                                                <input id="publishTime" type="time" step="1" class="form-control spdx-time needs-validation" rule="required"
                                                   name="<portlet:namespace/><%=PortalConstants.PUBLISH_TIME%>"
                                                   >
                                                <div id="createdTime-error-messages">
                                                     <div class="invalid-feedback" rule="required">
                                                          Invalid format!
                                                     </div>
                                                 </div>
                                            </div>
                                        </td>
                                    </core_rt:if>

                                    <core_rt:if test = "${(vulnerability.lastExternalUpdate != null) and (fn:length(vulnerability.lastExternalUpdate) >= 19)}">
                                        <td style="width:25%">
                                             <div class="form-group">
                                                 <label for="lastExternalUpdateDate"><liferay-ui:message key="vulnerability.last.external.update.date" /></label>
                                                 <input id="lastExternalUpdateDate" class="form-control datepicker"  type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                   name="<portlet:namespace/><%=PortalConstants.EXTERNAL_UPDATE_DATE%>"
                                                   placeholder="<liferay-ui:message key="last.external.update.date.yyyy.mm.dd" />"
                                                   autocomplete="off"
                                                   value = "<sw360:out value="${vulnerability.lastExternalUpdate.substring(0,10)}"/>"
                                                   />
                                                 <div class="invalid-feedback">
                                                     <liferay-ui:message key="please.enter.last.external.update.date" />
                                                 </div>
                                             </div>
                                        </td>
                                        <td style="width:25%">
                                            <div class="form-group">
                                                <label for="lastExternalUpdateTime"><liferay-ui:message key="vulnerability.last.external.update.time" /></label>
                                                <input id="lastExternalUpdateTime" type="time" step="1" class="form-control spdx-time needs-validation" rule="required"
                                                   name="<portlet:namespace/><%=PortalConstants.EXTERNAL_UPDATE_TIME%>"
                                                   value = "<sw360:out value="${vulnerability.lastExternalUpdate.substring(11,19)}"/>">
                                                <div id="createdTime-error-messages">
                                                     <div class="invalid-feedback" rule="required">
                                                          Invalid format!
                                                     </div>
                                                 </div>
                                            </div>
                                        </td>
                                    </core_rt:if>

                                    <core_rt:if test = "${(vulnerability.lastExternalUpdate == null) or (fn:length(vulnerability.lastExternalUpdate) < 19)}">
                                        <td style="width:25%">
                                             <div class="form-group">
                                                 <label for="lastExternalUpdateDate"><liferay-ui:message key="vulnerability.last.external.update.date" /></label>
                                                 <input id="lastExternalUpdateDate" class="form-control datepicker"  type="text" pattern="\d{4}-\d{2}-\d{2}"
                                                   name="<portlet:namespace/><%=PortalConstants.EXTERNAL_UPDATE_DATE%>"
                                                   placeholder="<liferay-ui:message key="last.external.update.date.yyyy.mm.dd" />"
                                                   autocomplete="off"
                                                   />
                                                 <div class="invalid-feedback">
                                                     <liferay-ui:message key="please.enter.last.external.update.date" />
                                                 </div>
                                             </div>
                                        </td>
                                        <td style="width:25%">
                                            <div class="form-group">
                                                <label for="lastExternalUpdateTime"><liferay-ui:message key="vulnerability.last.external.update.time" /></label>
                                                <input id="lastExternalUpdateTime" type="time" step="1" class="form-control spdx-time needs-validation" rule="required"
                                                   name="<portlet:namespace/><%=PortalConstants.EXTERNAL_UPDATE_TIME%>">
                                                <div id="createdTime-error-messages">
                                                     <div class="invalid-feedback" rule="required">
                                                          Invalid format!
                                                     </div>
                                                 </div>
                                            </div>
                                        </td>
                                    </core_rt:if>
                                </tr>
                            </tbody>
                        </table>
                        <%@include file="/html/vulnerabilities/includes/vulnerabilityImpact.jsp"%>
                        <%@include file="/html/vulnerabilities/includes/vulnerabilityAccess.jsp"%>
                        <%@include file="/html/vulnerabilities/includes/vulnerabilityCVEReferences.jsp"%>
                        <br />
                        <br />
                        <%@include file="/html/vulnerabilities/includes/vulnerabilityExtComponentIds.jsp"%>
                        <br />
                        <br />
                        <%@include file="/html/vulnerabilities/includes/vulnerabilityReferences.jsp"%>
                        <br />
                        <br />
                        <%@include file="/html/vulnerabilities/includes/vulnerabilityVendorAdvisory.jsp"%>
                        <br />
                        <br />
                        <%@include file="/html/vulnerabilities/includes/vulnerabilityConfig.jsp"%>
                    </form>
                </div>
            </div>
		</div>
	</div>
</div>

<div class="dialogs auto-dialogs"></div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
            var PortletURL = Liferay.PortletURL;
            require(['bridges/datatables', 'modules/dialog', 'utils/includes/quickfilter', 'utils/link', 'modules/validation', 'jquery'], function( datatables, dialog, quickfilter, linkutil, validation, $) {
                 validation.enableForm('#vulnerabilityEditForm');
                 let oldExternalId = $("#vulnerabilityExternalId").val();
                 window.onload = () => {
                      document.getElementById('vulnerabilityScore').onpaste = e => e.preventDefault();
                 }

                 $('.portlet-toolbar button[data-action="cancel"]').on('click', function() {
                       var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
                       var portletURL = Liferay.PortletURL.createURL( baseUrl )
                           .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
                       window.location = portletURL.toString();
                 });

                 $('.portlet-toolbar button[data-action="delete"]').on('click', function(event) {
                      var data = $(event.currentTarget).data();

                      dialog.confirm(
                            'danger',
                            'question-circle',
                            '<liferay-ui:message key="delete.vulnerability" />?',
                            '<p><liferay-ui:message key="do.you.really.want.to.delete.the.vulnerability.x" />?</p>',
                            '<liferay-ui:message key="delete.vulnerability" />',
                            {
                                  name: data.externalId,
                            },
                            function(submit, callback) {
                                 window.location.href = '<%=deleteVulnerabilityURL%>';
                            }
                      );
                 });

                 $('.portlet-toolbar button[data-action="save"]').on('click', function() {
                       let addMode = "${vulnerability.id}";
                       let cvssScore = $("#vulnerabilityScore").val().trim();
                       let newExternalId = $("#vulnerabilityExternalId").val().trim();
                       let cvssScoreValid = true;
                       let cveYearValid = true;
                       let cveNumberValid = true;

                       if (cvssScore > 10.0 || cvssScore < 0){
                            $(".cvss-invalid").css("display", "block");
                            $("#vulnerabilityScore").css("border-color", "#5aca75");
                            cvssScoreValid = false;
                       } else{
                            $(".cvss-invalid").css("display", "none");
                       }

                       $(".cve-year").each(function() {
                            let value = $(this).val();
                            if (parseInt(value) <= 0) {
                                $(this).closest('td').find('.cve-year-invalid').css("display","block");
                                $(this).css("border-color", "#5aca75");
                                cveYearValid = false;
                            } else {
                                $(this).closest('td').find('.cve-year-invalid').css("display","none");
                            }
                       });

                       $(".cve-number").each(function() {
                            let value = $(this).val();
                            if (parseInt(value) < 0) {
                                $(this).closest('td').find('.cve-number-invalid').css("display","block");
                                $(this).css("border-color", "#5aca75");
                                cveNumberValid = false;
                            } else {
                                $(this).closest('td').find('.cve-number-invalid').css("display","none");
                            }
                       });

                       if (addMode === "" || addMode == undefined || newExternalId != oldExternalId){
                            let externalId = $("#vulnerabilityExternalId").val();
                            let resourceURL = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RESOURCE_PHASE) %>';
                            let validateExternalIdURL = Liferay.PortletURL.createURL( resourceURL )
                               .setParameter('<%=PortalConstants.ACTION%>', '<%=PortalConstants.FIND_BY_EXTERNAL_ID%>')
                               .setParameter('<%=PortalConstants.VULNERABILITY_EXTERNAL_ID%>', newExternalId);

                            jQuery.ajax({
                                type: 'GET',
                                url: validateExternalIdURL,
                                cache: false,
                                success: function (data) {
                                    if (data.result == 'SUCCESS') {
                                        $("#externalIdFeedBack").css("display", "none");
                                        $("#vulnerabilityExternalId").css("border-color", "#5aca75");
                                        if (cvssScoreValid == true && cveYearValid == true && cveNumberValid == true){
                                            trimValue();
                                            $('#vulnerabilityEditForm').submit();
                                        }
                                    } else {
                                        $("#externalIdFeedBack").text("External id has existed");
                                        $("#externalIdFeedBack").css("display", "block");
                                    }
                                },
                                error: function () {
                                    console.log("ERROR");
                                }
                            });
                       }
                       else {
                            if (cvssScoreValid == true && cveYearValid == true && cveNumberValid == true){
                                trimValue();
                                $('#vulnerabilityEditForm').submit();
                            }
                       }
                 });

            });

            require(['jquery', 'bridges/jquery-ui'], function($) {
                    $(".datepicker").datepicker({changeMonth:true,changeYear:true,dateFormat: "yy-mm-dd"});
                    $(".datepicker").keydown(false);
            });

            function trimValue() {
                let externalId = $("#vulnerabilityExternalId").val().trim();
                let title = $("#vulnerabilityTitle").val().trim();
                let description = $("#vulnerabilityDescription").val().trim();
                let priority = $("#vulnerabilityPriority").val().trim();
                let priorityText = $("#vulnerabilityPriorityText").val().trim();
                let action = $("#vulnerabilityAction").val().trim();
                let legalNotice = $("#vulnerabilityLegalNotice").val().trim();
                let vulnerabilityCwe = $("#vulnerabilityCwe").val().trim();
                let vulnerabilityExtendedDescription = $("#vulnerabilityExtendedDescription").val().trim();

                $("#vulnerabilityExternalId").val(externalId);
                $("#vulnerabilityTitle").val(title);
                $("#vulnerabilityDescription").val(description);
                $("#vulnerabilityPriority").val(priority);
                $("#vulnerabilityPriorityText").val(priorityText);
                $("#vulnerabilityAction").val(action);
                $("#vulnerabilityLegalNotice").val(legalNotice);
                $("#vulnerabilityCwe").val(vulnerabilityCwe);
                $("#vulnerabilityExtendedDescription").val(vulnerabilityExtendedDescription);

                $('.reference-link').each(function() {$(this).val($(this).val().trim())});
                $('.advisory-vendor').each(function() {$(this).val($(this).val().trim())});
                $('.advisory-name').each(function() {$(this).val($(this).val().trim())});
                $('.advisory-url').each(function() {$(this).val($(this).val().trim())});

                $('.cve-year').each(function() {$(this).val($(this).val().trim())});
                $('.cve-number').each(function() {$(this).val($(this).val().trim())});
            }
    });
</script>
