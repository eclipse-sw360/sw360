<%--
  ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<jsp:useBean id="obligationElement" class="org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement" scope="request" />

<portlet:resourceURL var="viewObligationELementURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_IMPORT_OBLIGATION_ELEMENTS%>"/>
    <portlet:param name="<%=PortalConstants.OBLIGATION_ELEMENT_ID%>" value="${obligationElement.id}"/>
</portlet:resourceURL>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<div class="dialogs">
    <div id="searchObligationElementsDialog" data-title="<liferay-ui:message key="import.obligation.element" />" class="modal fade" tabindex="-1" role="dialog">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
      <div class="modal-content">
        <div class="modal-body container">
            <form>
            <div class="row form-group">
                            <div class="col">
                                <input type="text" name="searchobligationelement" id="searchobligationelement" placeholder="<liferay-ui:message key="enter.search.text" />" class="form-control"/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="searchbuttonobligation"><liferay-ui:message key="search" /></button>
                            </div>
                        </div>

                        <div id="ObligationElementsearchresults">
                            <div class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only"><liferay-ui:message key="loading" /></span>
                                </div>
                            </div>

                            <table id="obligationElementSearchResultstable" class="table table-bordered">
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th><liferay-ui:message key="language.element" /></th>
                                        <th><liferay-ui:message key="action" /></th>
                                        <th><liferay-ui:message key="object" /></th>
                                    </tr>
                                </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                    </form>
				</div>
			    <div class="modal-footer">
		            <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="close" /></button>
			        <button id="importObligationElementsButton" type="button" class="btn btn-primary" title="<liferay-ui:message key="import.obligation.element" />"><liferay-ui:message key="import.obligation.element" /></button>
			    </div>
			</div>
		</div>
	</div>
</div>