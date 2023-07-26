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

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.packages.Package" %>

<portlet:resourceURL var="viewPackageURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.VIEW_LINKED_PACKAGES%>"/>
</portlet:resourceURL>

<core_rt:set var="portletName" value="<%=themeDisplay.getPortletDisplay().getPortletName() %>"/>

<div class="dialogs">
    <div id="searchPackagesDialog" data-title="<liferay-ui:message key="link.packages" /> " class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable mw-100 w-75" role="document">
            <div class="modal-content">
                <div class="modal-body container">
                    <form>
                        <div class="row form-group">
                            <div class="col-4">
                                <input type="text" name="searchPackage" id="searchPackage" placeholder="<liferay-ui:message key="enter.package.name.or.version" />" class="form-control" autofocus/>
                            </div>
                            <div class="col-1">
                                <button type="button" class="btn btn-secondary" id="searchBtnPackage"><liferay-ui:message key="search" /></button>
                            </div>
                        </div>
                        <div class="form-check pt-2">
                            <input class="form-check-input" type="checkbox" value="On" id="exactMatchPackage">
                            <label class="form-check-label" for="exactMatchPackage"><liferay-ui:message key="exact.match" /></label>
                            <sup title="<liferay-ui:message key="the.search.result.will.display.elements.exactly.matching.the.input.equivalent.to.using.x.around.the.search.keyword" />" >
                                        <liferay-ui:icon icon="info-sign"/>
                            </sup>
                            <core_rt:if test="${portletName == 'sw360_portlet_components'}">
                                <span class="alert alert-info my-0 p-2 mx-4"><liferay-ui:message key="only.orphan.packages.can.be.searched.and.linked.to.a.release" />.</span>
                            </core_rt:if>
                            <span id="pkgTruncationAlert" class="alert alert-warning my-0 p-2 mx-4" role="alert" style="display: none;">
                                <liferay-ui:message key="output.limited.to.100.results.please.narrow.your.search" />
                            </span>
                        </div>
                        <div id="search-package-form">
                            <div class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only"><liferay-ui:message key="loading" /></span>
                                </div>
                            </div>
                            <table id="packageSearchResultsTable" class="table table-bordered">
                                <colgroup>
                                    <col style="width: 3%;" />
                                    <col style="width: 30%;" />
                                    <col style="width: 10%;" />
                                    <col style="width: 15%;" />
                                    <col style="width: 12%;" />
                                    <col style="width: 30%;" />
                                </colgroup>
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th><liferay-ui:message key="name" /></th>
                                        <th><liferay-ui:message key="version" /></th>
                                        <th><liferay-ui:message key="license" /></th>
                                        <th><liferay-ui:message key="package.manager" /></th>
                                        <th><liferay-ui:message key="purl" /></th>
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
                    <button id="selectPackageButton" type="button" class="btn btn-primary" title="<liferay-ui:message key="link.packages" />"><liferay-ui:message key="link.packages" /></button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
require(['jquery', 'modules/dialog', 'bridges/datatables', 'utils/keyboard'], function($, dialog, datatables, keyboard) {
    var $dataTable,
         $dialog;

     keyboard.bindkeyPressToClick('searchPackage', 'searchBtnPackage');

     var homeUrl = themeDisplay.getURLHome().replace(/\/web\//, '/group/');

     $('#addLinkedPackages').on('click', showPackageDialog);
     $('#searchBtnPackage').on('click', function(){
         var searchPackage = $('#searchPackage').val();
         if ($('#exactMatchPackage').is(':checked') && !(searchPackage.startsWith("\"") && searchPackage.endsWith("\""))) {
             searchPackage = '"' + searchPackage + '"';
         }
         packageContentFromAjax('<%=PortalConstants.PACKAGE_SEARCH%>', searchPackage, function(data) {
             if ($dataTable) {
                 $dataTable.destroy();
             }
             $('#packageSearchResultsTable tbody').html(data);
             addLinkToPackageNameAndSanitizePurl();
             makePackageDataTable();
         });
     });

     $('#packageSearchResultsTable').on('change', 'input', function() {
         $dialog.enablePrimaryButtons($('#packageSearchResultsTable input:checked').length > 0);
     });

     function showPackageDialog(event) {
         if ($dataTable) {
             $dataTable.destroy();
             $dataTable = undefined;
         }
         let inputId = event.target.id;
         $dialog = dialog.open('#searchPackagesDialog', {
         }, function(submit, callback) {
             var packageIds = [];

             $('#packageSearchResultsTable').find(':checked').each(function () {
                 packageIds.push(this.value);
             });
             packageContentFromAjax('<%=PortalConstants.ADD_LINKED_PACKAGES%>', packageIds, function(data) {
                 $('#LinkedPackagesInfo tbody').append(data);
             });
             callback(true);
         }, function() {
             this.$.find('.spinner').hide();
             this.$.find('#packageSearchResultsTable').hide();
             this.$.find('#searchPackage').val('');
             this.$.find('#pkgTruncationAlert').hide();
             this.enablePrimaryButtons(false);
         });
     }

     function makePackageDataTable() {
         $dataTable = datatables.create('#packageSearchResultsTable', {
             destroy: true,
             paging: false,
             info: false,
             language: {
                 emptyTable: "<liferay-ui:message key="no.packages.found" />",
                 processing: "<liferay-ui:message key="processing" />",
                 loadingRecords: "<liferay-ui:message key="loading" />"
             },
             order: [
                 [1, 'asc']
             ],
             select: 'multi+shift'
         }, undefined, [0]);
         datatables.enableCheckboxForSelection($dataTable, 0);
     }

     function packageContentFromAjax(what, where, callback) {
         $dialog.$.find('.spinner').show();
         $dialog.$.find('#packageSearchResultsTable').hide();
         $dialog.$.find('#searchBtnPackage').prop('disabled', true);
         $dialog.enablePrimaryButtons(false);

         jQuery.ajax({
             type: 'POST',
             url: '<%=viewPackageURL%>',
             data: {
                 '<portlet:namespace/><%=PortalConstants.WHAT%>': what,
                 '<portlet:namespace/><%=PortalConstants.WHERE%>': where
             },
             success: function (data) {
                 callback(data);

                 $dialog.$.find('.spinner').hide();
                 $dialog.$.find('#packageSearchResultsTable').show();
                 $dialog.$.find('#searchBtnPackage').prop('disabled', false);
             },
             error: function() {
                 $dialog.alert('<liferay-ui:message key="failed.to.load.packages" />');
             }
         });
     }

     function makePackageViewUrl(pkgId) {
         return homeUrl + '/packages/-/package/detail/' + pkgId;
     }

     function detailUrl(name, url) {
         let viewUrl = $("<a></a>").attr("href",url).attr("target","_blank").css("word-break","break-word").text(name);
         return viewUrl[0].outerHTML;
     }

     function addLinkToPackageNameAndSanitizePurl() {
         $('#packageSearchResultsTable > tbody  > tr').each(function() {
             let $pkgId = $('td:eq(0)', this).find("input[type='checkbox']").val(),
                 $pkgName = $('td:eq(1)', this),
                 $purl = $('td:eq(5)', this),
                 linkOnPkgName = detailUrl($pkgName.text(), makePackageViewUrl($pkgId));
             $pkgName.html(linkOnPkgName);
             if ($purl.text().length > 45) {
                 let truncatedPurl = $purl.text().substring(0, 42) + '...'
                 $purl.html($("<span></span>").attr('title', $purl.text()).text(truncatedPurl)[0].outerHTML);
             }
      });
     }
 });
</script>
