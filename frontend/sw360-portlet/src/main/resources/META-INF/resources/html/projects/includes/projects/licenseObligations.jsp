<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest"%>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<liferay-portlet:renderURL var="friendlyLicenseURL" portletName="sw360_portlet_licenses">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.LICENSE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<liferay-portlet:renderURL var="friendlyReleaseURL" portletName="sw360_portlet_components">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.RELEASE_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</liferay-portlet:renderURL>

<core_rt:if test="${not empty unusedReleases}">
    <div class="alert alert-warning alert-dismissible fade show">
        <strong><liferay-ui:message key="warning" />!</strong> <liferay-ui:message key="there.are.some.releases.not.having.license.info.attachment.usage" /><br>
        <core_rt:forEach items="${unusedReleases}" var="unusedRelease" varStatus="loop">
            <sw360:DisplayReleaseLink release="${unusedRelease}" showFullname="true"/>
            <core_rt:if test="${!loop.last}"> , </core_rt:if>
        </core_rt:forEach>
        <button type="button" class="close" data-dismiss="alert">&times;</button>
    </div>
</core_rt:if>
<!-- Obligation View -->
<div id="license-obligation-view">
    <table id="licenseObligationsDetailTable" class="table table-bordered" >
        <colgroup>
            <col />
            <col />
            <col style="width: 66%;" />
            <col style="width: 10%;" />
            <col style="width: 10%;" />
            <col style="width: 7%;" />
            <col style="width: 7%;" />
        </colgroup>
        <thead>
            <tr>
                <th class="license-more-info">
                    <span title="<liferay-ui:message key="expand.all" />" data-show="false">&#x25BA</span>
                </th>
                <th id="select-all">
                    <input title='<liferay-ui:message key="select.all" />' type="checkbox" id="selectAllLicenseObligation" class="form-check-input"/>
                </th>
                <th><liferay-ui:message key="license.obligation" /></th>
                <th><liferay-ui:message key="licenses" /></th>
                <th><liferay-ui:message key="releases" /></th>
                <th><liferay-ui:message key="id" /></th>
                <th><liferay-ui:message key="type" /></th>
            </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
</div>
<script>
require(['jquery', 'bridges/datatables', 'utils/render'], function ($, datatables, render) {
    var licenseObligationJSON = [],
        licenseObligation_licenseIds;

    /* Print all attachment table data as array into the html page */
    <core_rt:forEach items="${licenseObligationData}" var="entry" varStatus="loop">
        <core_rt:set var="licenseObligations" value="${entry.value}" />
        licenseObligation_licenseLinks = [];
        licenseObligation_releaseLinks = [];
        <core_rt:forEach items="${licenseObligations.licenseIds}" var="licenseId">
            licenseObligation_licenseLinks.push("<sw360:out value='${licenseId}'/>");
        </core_rt:forEach >

        <core_rt:if test="${not empty licenseObligations.releases}">
        <core_rt:forEach items="${licenseObligations.releases}" var="release">
            var fullName = "<sw360:out value='${release.name}'/> (<sw360:out value='${release.version}'/>)";
            licenseObligation_releaseLinks.push({
                id: "${release.id}",
                name: fullName,
                attachmentId: "${release.attachments.iterator().next().attachmentContentId}"
            });
        </core_rt:forEach>
    </core_rt:if>
        licenseObligationJSON.push({
            "obligation": "<sw360:out value='${entry.key}'/>",
            "licenseLinks": licenseObligation_licenseLinks,
            "releaseLinks": licenseObligation_releaseLinks,
            "type": "<sw360:DisplayEnum value='${licenseObligations.obligationType}'/>",
            "id": "<sw360:out value='${licenseObligations.id}'/>",
            "text": '<sw360:out value="${licenseObligations.text}"/>',
        });
    </core_rt:forEach >

    /* create table */
    var licensetable = datatables.create('#licenseObligationsDetailTable', {
        "data": licenseObligationJSON,
        "deferRender": false, // do not change this value
        "columns": [
            {
                "className": 'license-details-control',
                "data": null,
                "defaultContent": '&#x25BA'
            },
            {
                "data": null,
                'className': 'dt-body-center',
                'render': function (data, type, full, meta){
                    return '<input type="checkbox" class="checkbox-control" >';
                }
            },
            { "data":  function(row) {
                           return $('<span></span>').html(row.obligation).text();
                       }, render: $.fn.dataTable.render.ellipsis
            },
            { "data": "licenseLinks", "render": { display: renderLicenseLink } },
            { "data": "releaseLinks", "render": { display: renderReleaseLink } },
            { "data": "id", className: 'text-center' },
            { "data": "type", className: 'text-center' }
        ],
        "columnDefs": [
            {
                "targets": 2,
                "createdCell": function (td, cellData, rowData, row, col) {
                    $(td).attr('title', 'click the icon to toggle obligation text');
                }
            },
        ],
        "order": [[2, 'asc']],
        "initComplete": datatables.showPageContainer
    }, [2, 3, 4, 5, 6], [0, 1], true);

    function renderLicenseLink(licenseLinks, type, row) {
        let licenses = [],
            licensePortletURL = '<%=friendlyLicenseURL%>'.replace(/projects/g, "licenses");
        for (let i = 0; i < licenseLinks.length; i++) {
            licenses[i] = render.linkTo(replaceFriendlyUrlParameter(licensePortletURL.toString(), licenseLinks[i], '<%=PortalConstants.PAGENAME_DETAIL%>'), licenseLinks[i]);
        }
        return render.renderExpandableUrls(licenses, '<liferay-ui:message key="license" />', 21);
    }

    function renderReleaseLink(releaseLinks, type, row, meta) {
        if (releaseLinks && releaseLinks.length > 0) {
        let releases = [],
            releasePortletURL = '<%=friendlyReleaseURL%>'.replace(/projects/g, "components");
        for (let i = 0; i < licenseObligation_releaseLinks.length; i++) {
            releases[i] = render.linkTo(replaceFriendlyUrlParameter(releasePortletURL.toString(), licenseObligation_releaseLinks[i].id, '<%=PortalConstants.PAGENAME_RELEASE_DETAIL%>'), releaseLinks[i].name);
        }
        return render.renderExpandableUrls(releases, '<liferay-ui:message key="release" />', 25);
        } else {
            return getOrphanObligationMessage();
        }
    }

    function replaceFriendlyUrlParameter(portletUrl, id, page) {
        return portletUrl
            .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
            .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', id);
    }

    /* Add event listener for opening and closing individual child row */
    $('#licenseObligationsDetailTable tbody').on('click', 'td.license-details-control', function () {
        render.toggleChildRow($(this), licensetable);
    });

    /* Add event listener for opening and closing all the child rows */
    $('#licenseObligationsDetailTable thead').on('click', 'th.license-more-info', function() {
        render.toggleAllChildRows($(this), licensetable);
    });
    
    $('#selectAllLicenseObligation').on('click', function() {
       let selectAll = $('#selectAllLicenseObligation').prop("checked");
       licensetable.rows().every(function (rowIdx, tableLoop, rowLoop) {
           $node = $(this.node()),
           $node.find("input.checkbox-control").prop('checked', selectAll);
       });
    });

    $('#licenseObligationsDetailTable tbody').on('change', 'input[type="checkbox"]', function() {
       if (!this.checked) {
          var all = $('#selectAllLicenseObligation').get(0);
          if (all && all.checked && ('indeterminate' in all)) {
             all.indeterminate = true;
          }
       }
    });
});
</script>