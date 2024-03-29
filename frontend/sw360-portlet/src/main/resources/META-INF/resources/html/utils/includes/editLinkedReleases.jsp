<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>

<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ReleaseRelationship" %>

<table class="table edit-table" id="LinkedReleasesInfo">
       <colgroup>
           <col style="width: 25%;" />
           <col style="width: 25%;" />
           <col style="width: 25%;" />
           <col style="width: 25%;" />
           <col style="width: 1.7rem" />
       </colgroup>
    <thead>
        <tr>
            <th><liferay-ui:message key="vendor.name" /></th>
            <th><liferay-ui:message key="release.name" /></th>
            <th><liferay-ui:message key="release.version" /></th>
            <th><liferay-ui:message key="release.relation" /> <sw360:DisplayEnumInfo type="<%=ReleaseRelationship.class%>"/></th>
            <th></th>
        </tr>
    </thead>
    <tbody>
        <jsp:include page="/html/utils/ajax/linkedReleasesRelationAjax.jsp" />
    </tbody>
</table>

<div class="stateinfo" id="inaccessibleRows_info" role="status" aria-live="polite">
    <liferay-ui:message key="inaccessible.count" /> (<span id="inaccessibleCounter">${totalInaccessibleRows}</span>)
</div>

<button type="button" class="btn btn-secondary" id="addLinkedReleasesToReleaseButton"><liferay-ui:message key="click.to.add.releases" /></button>

<script>
    require(['jquery', 'modules/dialog'], function($, dialog) {
        $('#LinkedReleasesInfo').on('click', "svg[data-action='delete-release']", function(event) {
            var releaseRowId = $(event.currentTarget).data().uuid,
                releaseName = $(event.currentTarget).data().releaseName;

            $dialog = dialog.confirm('danger', 'question-circle', '<liferay-ui:message key="delete.link.to.release" />', '<p><liferay-ui:message key="do.you.really.want.to.remove.the.link.to.release.x.1" />?</p>', '<liferay-ui:message key="delete.link" />', {
                releaseName: releaseName
            }, function(submit, callback) {
                $('#releaseLinkRow' + releaseRowId).remove();
                callback(true);
            });
        });
    });
</script>
