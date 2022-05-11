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
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<jsp:useBean id="obligationName" class="java.lang.String" scope="request" />

<div class="container">
    <div class="row">
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="nav nav-pills justify-content-center bg-light font-weight-bold" id="pills-tab"
                            role="tablist">
                            <a class="nav-item nav-link active" id="pills-changelogs-list-tab" data-toggle="pill"
                                href="#pills-changelogslist" role="tab" aria-controls="pills-changeloglist" aria-selected="true">
                                <liferay-ui:message key="change.log" />
                            </a>
                            <a class="nav-item nav-link" id="pills-changelogs-view-tab" href="#pills-changelogsView"
                                role="tab" aria-controls="pills-changelogsView" aria-selected="false">
                                <liferay-ui:message key="changes" />
                            </a>
                        </div>
                        <div class="btn-group" role="group" style="margin-left:1rem;">
                            <a type="button" class="btn btn-light" style="width: 90px;" id="obligation-view-page" href="${baseUrl}">
                                <liferay-ui:message key="back" />
                            </a>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="<sw360:out value=" ${obligationName}" />">
                    <sw360:out value="${obligationName}" />
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <jsp:include page="/html/changelogs/elementView.jsp" />
                </div>
            </div>
        </div>
    </div>
</div>

<script>
require(['jquery'], function($) {
    $("div.alert-container").removeClass("cadmin");
    if ('${obligationName}' == '') {
        $('#portlet_sw360_portlet_todos > div > div.portlet-content-container > div').attr('style','display:none')
        $('#portlet_sw360_portlet_todos').append('<div class=" portlet-content-container"> <div class="portlet-body"><div class="btn-group" role="group">\
        <a type="button" class="btn btn-light" style="width: 90px;" id="obligation-view-page1" href="${baseUrl}">Back<a/></div>\
        <div class="alert alert-danger"> <liferay-ui:message key="obligation.change.log.is.unavailable.because.obligation.does.not.exist" /></div> </div> </div>')
    }
});
</script>
