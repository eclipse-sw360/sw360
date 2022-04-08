<%@ taglib prefix="sw360" uri="http://example.com/tld/customTags.tld" %>
<%--
  ~ Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ include file="/html/init.jsp" %>
<%@ include file="/html/utils/includes/logError.jspf" %>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.PortalUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>
<jsp:useBean id="departmentKey" type="java.lang.String" scope="request"/>

<jsp:useBean id="emailByDepartment" type="java.lang.String" scope="request"/>
<jsp:useBean id="emailOtherDepartment" type="java.lang.String" scope="request"/>

<portlet:resourceURL var="deleteDepartmentURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_DEPARTMENT_BY_EMAIL%>'/>
</portlet:resourceURL>
<portlet:actionURL var="updateURL" name="updateDepartment">
    <portlet:param name="<%=PortalConstants.DEPARTMENT_KEY%>" value="${departmentKey}"/>
</portlet:actionURL>


<div class="container" style="display: none;" id="container">
    <div class="row">
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group">
                            <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message
                                    key="Update Department"/></button>
                        </div>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message
                                    key="Cancel"/></button>
                        </div>

                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <form id="departmentEditForm" name="departmentEditForm" action="<%=updateURL%>" method="post" class="form needs-validation" novalidate>
                        <table  class="table edit-table two-columns-with-actions" id="secDepartmentRolesTable">
                            <thead>
                            <input style="display: none;" type="text" id="listEmail1" name="<portlet:namespace/><%=PortalConstants.ADD_LIST_EMAIL%>" value="" />
                            <input style="display: none;" type="text" id="listEmail2" name="<portlet:namespace/><%=PortalConstants.DELETE_LIST_EMAIL%>" value="" />
                            <tr>
                                <th colspan="3" class="headlabel" ><liferay-ui:message key="Edit Department"/> <sw360:out value="${departmentKey}"/> </th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr id="" class="bodyRow" display="none">
                                <td>
                                    <input  list="suggestionsList" class="form-control secGrp" name="email" placeholder="<liferay-ui:message key="Search User" />" title="<liferay-ui:message key="select.secondary.department.role" />"   />
                                </td>
                                <datalist class="suggestion" id="suggestionsList">
                                </datalist>
                                <td class="content-middle">
                                    <svg class="action lexicon-icon delete-btn" data-value="" data-row-id="" onclick="">
                                        <title>Delete</title>
                                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                                    </svg>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                        <button type="button" class="btn btn-secondary" id="add-sec-grp-roles-id">
                            <liferay-ui:message key="Add User" />
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="dialogs">
    <div id="deleteSecGrpRolesDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <clay:icon symbol="question-circle" />
                        <liferay-ui:message key="delete.item" />
                        ?
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p data-name="text"></p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">
                        <liferay-ui:message key="cancel" />
                    </button>
                    <button type="button" class="btn btn-danger">
                        <liferay-ui:message key="delete.item" />
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="dialogs auto-dialogs"></div>

<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script >
    AUI().use('liferay-portlet-url', function () {
        require(['jquery', 'modules/dialog', 'modules/validation'], function ($, dialog, validation) {
            let emailStart=[];
            let emailsByDepartmentKey=[];
            let emailsOtherDepartment=[];
            let emailsAdd=[];
            let emailInDatabase=[];
            let emailsByDepartment = jQuery.parseJSON(JSON.stringify(${ emailByDepartment }));
            let emailOtherDepartment= jQuery.parseJSON(JSON.stringify(${emailOtherDepartment}));

            createSecDepartmentRolesTable();
            fillSuggestion();

            $('#container').css('display', '');
            $('.container-spinner').css('display', 'none');

            pageName = '<%=PortalConstants.PAGENAME%>';
            pageEdit = '<%=PortalConstants.PAGENAME_EDIT%>';
            validation.enableForm('#departmentEditForm');

            function arrayObjectToArrayString(arrayObject,arrayString){
                for(let object of arrayObject){
                    arrayString.push(object.email);
                }
                return arrayString.sort();
            }

            $('.portlet-toolbar button[data-action="cancel"]').on('click', function () {
                var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
                var portletURL = Liferay.PortletURL.createURL(baseUrl).setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>');
                window.location = portletURL.toString();
            });

            $('.portlet-toolbar button[data-action="save"]').on('click', function () {
                $('.secGrp').each(function() {
                    emailsAdd.push($(this).val());
                });
                emailStart=arrayObjectToArrayString(emailsByDepartment,emailStart);
                let emailInsert = emailsAdd.filter((o) => emailStart.indexOf(o) === -1);
                let emailDelete = emailStart.filter((o) => emailsAdd.indexOf(o) === -1);

                emailInsert = Array.from(new Set(emailInsert));
                emailDelete=Array.from(new Set(emailDelete));

                let jsonArrayEmailAdd = JSON.parse(JSON.stringify(emailInsert));
                let jsonArrayEmailDelete = JSON.parse(JSON.stringify(emailDelete));

                $('#listEmail1').val(JSON.stringify(jsonArrayEmailAdd));
                $('#listEmail2').val(JSON.stringify(jsonArrayEmailDelete));
                $('#departmentEditForm').submit();
            });

            function createSecDepartmentRolesTable() {

                emailsByDepartmentKey=arrayObjectToArrayString(emailsByDepartment,emailsByDepartmentKey);
                emailsOtherDepartment=arrayObjectToArrayString(emailOtherDepartment,emailsOtherDepartment);
                emailInDatabase=emailsByDepartmentKey.concat(emailsOtherDepartment);

                $('.delete-btn').first().bind('click', deleteRow);

                if (emailsByDepartmentKey.length == 0) {
                    return;
                }

                if (emailsByDepartmentKey.length == 1) {
                    $('.bodyRow').focusout(function() {
                        handleFocusOut($(this).find('input').first(),emailInDatabase);
                        let arr=[];
                        $('.secGrp').each(function(){
                            var value = $(this).val();
                            if (arr.indexOf(value) == -1){
                                arr.push(value);
                            }
                            else{
                                $(this).val("");
                            }
                        });
                    })
                }
                for (let i = 0; i < emailsByDepartmentKey.length - 1; i++) {
                    addNewRow();
                    $('.bodyRow').focusout(function() {
                        handleFocusOut($(this).find('input').first(),emailInDatabase);
                        let arr=[];
                        $('.secGrp').each(function(){
                            var value = $(this).val();
                            if (arr.indexOf(value) == -1){
                                arr.push(value);
                            }
                            else{
                                $(this).val("");
                            }
                        });
                    })
                }
                fillSuggestion();
                for (let i = 0; i < emailsByDepartmentKey.length; i++) {
                    $('.secGrp').eq(i).val(emailsByDepartmentKey[i]);

                }
            }

            $('#add-sec-grp-roles-id').on('click', function() {
                let arrayFocus= emailsOtherDepartment.slice()
                let emailLastInput = $('.secGrp').last().val();
                const index = emailsOtherDepartment.indexOf(emailLastInput);
                if (index > -1) {
                    emailsOtherDepartment.splice(index, 1);
                }
                emailsOtherDepartment = Array.from(new Set(emailsOtherDepartment));

                addNewRow()
                fillSuggestion();

                $('.bodyRow').last().focusout(function() {
                    handleFocusOut($(this).find('input').first(),arrayFocus);
                    let arr=[];
                    $('.secGrp').each(function(){
                        var value = $(this).val();
                        if (arr.indexOf(value) == -1){
                            arr.push(value);
                        }
                        else{
                            $(this).val("");
                        }
                    });
                })
            });

            function addNewRow() {
                if ($('.bodyRow').first().css('display') == 'none') {
                    $('.bodyRow').last().css('display', 'table-row');
                    return;
                }

                let newRow = $('.bodyRow').last().clone();

                $('#secDepartmentRolesTable').find('tbody').append(newRow);

                $('.secGrp').last().val('');

                $('.delete-btn').last().on('click', deleteRow);
            }

            function deleteRow() {
                let email = $(this).parent().parent().children('td').first().children('input').val();
                if(email !== "") {
                    emailsOtherDepartment.push(email);
                }

                if ($('.delete-btn').length > 1) {
                    $(this).closest('tr').remove();
                } else {
                    $('.secGrp').val('');
                    $(this).closest('tr').css('display', 'none');
                }
                fillSuggestion();
            }

            function fillSuggestion() {
                let suggestionsList = '';

                for(let email of emailsOtherDepartment) {
                    suggestionsList += '<option value="'+email+'">' + email+ '</option>';
                }

                $('.suggestion').empty();

                $('.suggestion').each(function() {
                    $(this).html(suggestionsList);
                });
            }

            function handleFocusOut(element,array) {
                let value = element.val();
                if(array.length ==0 ){
                    $(element).val("");
                }

                for (let i = 0; i < array.length; i++) {
                    if (array[i] == value) {
                        $(element).val(array[i]);
                        break;
                    } else {
                        $(element).val("");
                        continue;
                    }
                }
            }
        });
    });
</script>
