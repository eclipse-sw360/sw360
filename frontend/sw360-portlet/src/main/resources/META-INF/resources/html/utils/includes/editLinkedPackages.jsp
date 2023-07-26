<%--
  ~ Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
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

<button type="button" class="btn btn-secondary" id="addLinkedPackages"><liferay-ui:message key="add.packages" /></button>
<table class="table edit-table mt-1" id="LinkedPackagesInfo">
    <colgroup>
        <col style="width: 25%;" />
        <col style="width: 20%;" />
        <col style="width: 25%;" />
        <col style="width: 20%;" />
        <col style="width: 1.7rem" />
    </colgroup>
    <thead>
        <tr>
            <th><liferay-ui:message key="package.name" /></th>
            <th><liferay-ui:message key="package.version" /></th>
            <th><liferay-ui:message key="license" /></th>
            <th><liferay-ui:message key="package.manager" /></th>
            <th></th>
        </tr>
    </thead>
    <tbody>
        <jsp:include page="/html/utils/ajax/linkedPackagesAjax.jsp" />
    </tbody>
</table>

<script>
require(['jquery', 'modules/dialog'], function($, dialog) {
    $('#LinkedPackagesInfo').on('click', "svg[data-action='delete-package']", function(event) {
        var packageRowId = $(event.currentTarget).data().uuid,
            packageName = $(event.currentTarget).data().packageName;

        $dialog = dialog.confirm('danger', 'question-circle', '<liferay-ui:message key="delete.link.to.package" />',
                '<p><liferay-ui:message key="do.you.really.want.to.remove.the.link.to.package.x" /></p>',
                '<liferay-ui:message key="delete.link" />', {
            packageName: packageName
        }, function(submit, callback) {
            $('#packageLinkRow_' + packageRowId).remove();
            callback(true);
        });
    });
});
</script>
