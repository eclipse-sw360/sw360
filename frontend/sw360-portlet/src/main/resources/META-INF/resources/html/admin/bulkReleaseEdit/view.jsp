<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>


<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>"
             scope="request"/>

<portlet:resourceURL var="updateReleaseURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.RELEASE%>"/>
</portlet:resourceURL>


<div class="container">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col portlet-title text-truncate" title="<liferay-ui:message key="release.bulk.edit" />">
					<liferay-ui:message key="release.bulk.edit" />
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="ComponentBasicInfo" class="table table-bordered">
                        <thead>
                            <tr>
                                <th width="13%"><liferay-ui:message key="status" /></th>
                                <th width="20%"><liferay-ui:message key="cpe.id1" /></th>
                                <th width="20%"><liferay-ui:message key="vendor" /></th>
                                <th width="20%"><liferay-ui:message key="release.name" /></th>
                                <th width="20%"><liferay-ui:message key="release.version" /></th>
                                <th width="7%"><liferay-ui:message key="submit" /></th>
                            </tr>
                        </thead>
                        <tbody>
                            <core_rt:forEach items="${releaseList}" var="release">
                                <tr id="TableRow${release.id}">
                                    <td id="Status${release.id}" class="text-center">
                                        <div class="spinner text-center" style="display: none;">
                                            <div class="spinner-border" role="status">
                                                <span class="sr-only"><liferay-ui:message key="update" />...</span>
                                            </div>
                                        </div>
                                        <span class="badge" data-type="result"></span>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <input id='cpeid${release.id}' type="text"
                                                    class="form-control"
                                                    placeholder="<liferay-ui:message key="enter.cpe.id" />" required value="${release.cpeid}" />
                                        </div>
                                        <%-- this and following hidden spans are added to make display filter and sorting using dataTables work--%>
                                        <span style="display:none" id='plaincpeid${release.id}'>${release.cpeid}</span>
                                    </td>
                                    <td>
                                        <sw360:DisplayVendorEdit id='vendorId${release.id}' label=""
                                                                    vendor="${release.vendor}" releaseId="${release.id}"/>
                                        <span style="display:none" id='plainvendor${release.id}'>${release.vendor.fullname}</span>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <input id='name${release.id}' type="text" class="form-control"
                                                    placeholder="<liferay-ui:message key="enter.name" />"
                                                        value="<sw360:out value="${release.name}"/>"
                                                    />
                                        </div>
                                        <span style="display:none" id='plainname${release.id}'>${release.name}</span>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <input id='version${release.id}'  type="text" class="form-control"
                                                placeholder="<liferay-ui:message key="enter.version" />"
                                                value="<sw360:out value="${release.version}"/>"/>
                                        </div>
                                        <span style="display:none" id='plainversion${release.id}'>${release.version}</span>
                                    </td>
                                    <td>
                                        <button type="button" name="submit" class="btn btn-sm btn-primary" data-release-id="${release.id}"><liferay-ui:message key="update" /></button>
                                    </td>
                                </tr>
                            </core_rt:forEach>
                        </tbody>
                    </table>
                </div>
            </div>

		</div>
	</div>
</div>

<%@include file="/html/components/includes/vendors/searchVendor.jspf" %>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;

        require(['jquery', 'bridges/datatables', 'components/includes/vendors/searchVendor', 'utils/includes/quickfilter'], function($, datatables, vendorsearch, quickfilter) {

            $(".clearSelection").click(function(event) {
                var vendorTextBoxData = $(event.currentTarget.previousSibling).data();
                $('#vendorId'+ vendorTextBoxData.releaseId).val("");
                $('#vendorId'+ vendorTextBoxData.releaseId+'Display').val("").attr("placeholder", "Click to set vendor");
            });

            var componentsInfoTable;

            // initializing
            componentsInfoTable = configureComponentBasicInfoTable();
            quickfilter.addTable(componentsInfoTable);

            // register event handlers
            $('#ComponentBasicInfo').on('click', 'input.edit-vendor', function(event) {
                var data = $(event.currentTarget).data();
                displayVendors(data.releaseId);
            });
            $('#ComponentBasicInfo').on('click', 'button[name=submit]', function(event) {
                var data = $(event.currentTarget).data();
                submitRow(data.releaseId);
            });

            function configureComponentBasicInfoTable(){
                return datatables.create('#ComponentBasicInfo', {
                    searching: true,
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />" ,
                        //emptyTable: "<liferay-ui:message key="no.releases.found" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    columnDefs: [
                        { targets: [0], orderable: false}
                    ],
                    order: [
                        [3, 'asc']
                    ]
                });
            }

            function submitRow(id) {
                var resultElement  = $('#Status'+id);

                resultElement.find('.spinner').show();
                resultElement.find('[data-type="result"]').removeClass('badge-success');
                resultElement.find('[data-type="result"]').removeClass('badge-danger');

                jQuery.ajax({
                    type: 'POST',
                    url: '<%=updateReleaseURL%>',
                    data: {
                        <portlet:namespace/>releaseId: id,
                        <portlet:namespace/>VENDOR_ID: $('#vendorId'+id).val(),
                        <portlet:namespace/>CPEID:$('#cpeid'+id).val(),
                        <portlet:namespace/>NAME:$('#name'+id).val(),
                        <portlet:namespace/>VERSION:$('#version'+id).val()
                    },
                    success: function (data) {
                        resultElement.find('.spinner').hide();
                        resultElement.find('[data-type="result"]').addClass('badge-success');
                        resultElement.find('[data-type="result"]').text(data.result);
                    },
                    error: function () {
                        resultElement.find('.spinner').hide();
                        resultElement.find('[data-type="result"]').addClass('badge-danger');
                        resultElement.find('[data-type="result"]').text('Failed');
                    }
                });
            }

            function displayVendors(releaseId) {
                vendorsearch.openSearchDialog('<portlet:namespace/>what', '<portlet:namespace/>where',
                  '<portlet:namespace/>FULLNAME', '<portlet:namespace/>SHORTNAME', '<portlet:namespace/>URL', function(vendorInfo) {
                    fillVendorInfo(releaseId, vendorInfo);
                });
            }

            function fillVendorInfo(releaseId, vendorInfo) {
                var beforeComma = vendorInfo.substr(0, vendorInfo.indexOf(","));
                var afterComma = vendorInfo.substr(vendorInfo.indexOf(",") + 1);

                $('#vendorId'+ releaseId).val(beforeComma.trim());
                $('#vendorId'+ releaseId+'Display').val(afterComma.trim());
            }
        });
    });
</script>
