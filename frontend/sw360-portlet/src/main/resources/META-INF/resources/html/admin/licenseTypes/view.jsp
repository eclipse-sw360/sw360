<%--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
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

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_LICENSE_TYPE%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="checkAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.CHECK_LICENSE_TYPE_IN_USE%>'/>
</portlet:resourceURL>

<portlet:renderURL var="addLicenseTypesURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_ADD%>" />
</portlet:renderURL>

<jsp:useBean id="licenseTypeList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.LicenseType>" scope="request"/>

<div class="container">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <%@ include file="/html/utils/includes/quickfilter.jspf" %>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
						<div class="btn-group" role="group">
							<button type="button" class="btn btn-primary" onclick="window.location.href='<%=addLicenseTypesURL%>'"><liferay-ui:message key="add.license.type" /></button>
						</div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="license.types" /> (${licenseTypeList.size()})">
					<liferay-ui:message key="license.types" /> (${licenseTypeList.size()})
				</div>
            </div>

            <div class="row">
                <div class="col">
			        <table id="licenseTypeTable" class="table table-bordered">
                        <colgroup>
                            <col />
                            <col style="width: 1.7rem"/>
                        </colgroup>
                    </table>
                </div>
            </div>

		</div>
	</div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs"></div>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'bridges/datatables', 'modules/dialog', 'utils/includes/quickfilter', 'modules/bannerMessage'], function($, datatables, dialog, quickfilter, bannerMessage) {
        bannerMessage.portletLoad();
        var licenseTypeTable = createLicenseTypeTable();
        quickfilter.addTable(licenseTypeTable);

        // register event handlers
        $('#licenseTypeTable').on('click', 'svg.delete', function (event) {
            var data = $(event.currentTarget).data();
            checkLicenseTypeInUse(data.id, data.licenseType);
        });

        function createLicenseTypeTable() {
            var licenseTypesTbl,
                result = [];

            <core_rt:forEach items="${licenseTypeList}" var="licenseType">
                result.push({
                    DT_RowId: "${licenseType.id}",
                    id: "${licenseType.id}",
                    licenseType: "<sw360:out value='${licenseType.licenseType}'/>"
                });
            </core_rt:forEach>

            licenseTypesTbl = datatables.create('#licenseTypeTable', {
                searching: true,
                data: result,
                columns: [
                    {"title": "<liferay-ui:message key="license.type" />", data: 'licenseType' },
                    {"title": "<liferay-ui:message key="actions" />", data: 'id', render: renderActions }
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                initComplete: datatables.showPageContainer
            }, [0, 1], [1]);

            return licenseTypesTbl;
        }

        function renderActions(value, type, row, meta) {
            if(type === 'display') {
                var $actions = $('<div>', {
                        'class': 'actions'
                    }),
                    $deleteAction = $('<svg>', {
                        'class': 'delete lexicon-icon',
                        'data-id': value,
                        'data-license-type': row.licenseType
                    });
                $deleteAction.append($('<title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

                $actions.append($deleteAction);
                return $actions[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return '';
            }
        }

        function deleteLicenseTypes(id, licenseType, licenseCount) {
            var $dialog;

            function deleteLicenseTypeInternal(callback, licenseType) {
                jQuery.ajax({
                    type: 'POST',
                    url: '<%=deleteAjaxURL%>',
                    cache: false,
                    data: {
                        <portlet:namespace/>id: id
                    },
                    success: function (data) {
                        callback();

                        if(data.result == 'SUCCESS') {
                            licenseTypeTable.row('#' + id).remove().draw(false);
                            $dialog.close();
                        } else if(data.result == 'ACCESS_DENIED') {
                            let accessDeniedMessage = '<liferay-ui:message key="do.you.really.want.to.delete.the.license.type.y" />';
                            accessDeniedMessage = accessDeniedMessage.replace('<licenseType>', licenseType);
                            $dialog.alert(accessDeniedMessage);
                        } else if(data.result == 'IN_USE') {
                            let inUseMessage = '<liferay-ui:message key="the.license.type.x.cannot.be.deleted.since.it.is.being.used.in.some.licenses.please.revoke.the.license.type.first" />';
                            inUseMessage = inUseMessage.replace('<licenseType>', licenseType);
                            $dialog.alert(inUseMessage);
                        } else {
                            $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.license.type" />');
                        }
                    },
                    error: function () {
                        callback();
                        $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.license.type" />');
                    }
                });
            }

            if (licenseCount > 0) {
                dialog.warn(
                    '<liferay-ui:message key="the.license.type.x.cannot.be.deleted.since.it.is.being.used.in.y.licenses.please.revoke.the.license.type.first" />',
                    {
                        licenseType: licenseType,
                        licenseCount: licenseCount
                    }
                );
            } else if (licenseCount < 0) {
                dialog.warn('<liferay-ui:message key="i.could.not.delete.the.license.type" />');
            } else {
                $dialog = dialog.confirm(
                    'danger',
                    'question-circle',
                    '<liferay-ui:message key="delete.license.type" />?',
                    '<p><liferay-ui:message key="do.you.really.want.to.delete.the.license.type.x" /> ?<p>',
                    '<liferay-ui:message key="delete.license.type" />',
                    {
                        licenseType: licenseType,
                    },
                    function(submit, callback) {
                        deleteLicenseTypeInternal(callback, licenseType);
                    }
                );
            }
        }

        function checkLicenseTypeInUse(id, licenseType) {
            jQuery.ajax({
                type: 'POST',
                url: '<%=checkAjaxURL%>',
                cache: false,
                data: {
                    <portlet:namespace/>id: id
                },
                success: function (data) {
                    deleteLicenseTypes(id, licenseType, data.result);
                },
                error: function () {
                    dialog.warn('<liferay-ui:message key="i.could.not.delete.the.license.type" />');
                }
            });
        }
    });

</script>
