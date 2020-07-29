<%--
  ~ Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:resourceURL var="cleanUpURL" >
  <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.CLEANUP%>'/>
</portlet:resourceURL>


<div class="container">
    <div class="row">
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-danger" data-action="cleanup"><liferay-ui:message key="clean.up.attachments" /></button>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="attachment.administration" />">
                    <liferay-ui:message key="attachment.administration" />
                </div>
            </div>
        </div>
    </div>
</div>

<div class="dialogs auto-dialogs"></div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'modules/dialog' ], function($, dialog) {
        $('.portlet-toolbar button[data-action="cleanup"]').on("click", function() {
            var $dialog;

            function cleanUpInternal(callback) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=cleanUpURL%>',
                    cache: false,
                    data: "",
                    success: function (data) {
                        callback();

                        if(data.result == 'SUCCESS')
                            $dialog.success(`<liferay-ui:message key="i.deleted.x.out.of.y.attachments.in.the.database" />`, true);
                        else {
                            $dialog.alert('<liferay-ui:message key="i.could.not.cleanup.the.attachments" />');
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert('<liferay-ui:message key="i.could.not.cleanup.the.attachments" />');
                    }
                });
            }

            $dialog = dialog.confirm(
                'danger',
                'question-circle',
                '<liferay-ui:message key="cleanup.attachment.database" />?',
                '<p><liferay-ui:message key="do.you.really.want.to.clean.up.the.attachment.database" /></p>',
                '<liferay-ui:message key="clean.up" />',
                {},
                function(submit, callback) {
                    cleanUpInternal(callback);
                }
            );
        });
    });
</script>
