/*
 * Copyright TOSHIBA CORPORATION, 2022.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */


define('components/includes/departments/searchDepartment', ['jquery', 'bridges/datatables', 'modules/button', 'modules/dialog', 'utils/keyboard' ], function($, datatables, button, dialog, keyboard) {
    var $dialog,
        $datatable;

    function showSetDepartmentDialog(departmentKey, priorityKey,DepartmentSelectedCb) {
        $dialog = dialog.open('#searchDepartmentDialog', {}, function(submit, callback, data) {
            if(submit ==  'select') {
            	DepartmentSelectedCb(data.departmentinput)
                callback(true);
            }

        }, function() {
            var dialog = this;

            // we do not reset the datatable. This way a new search is not necessary
            if(!$datatable) {
                $datatable = createDatatable();
                datatables.enableCheckboxForSelection($datatable, 0);
            }

            $('#departmentsearchresultstable').off('change.department-search');
            $('#departmentsearchresultstable').on('change.department-search', 'input[data-name="department"]', function() {
                var checkedDepartmentValue=$dialog.$.find('input[data-name="department"]:checked').val();
                $('#departmentsearchinputform').val(checkedDepartmentValue.substr(0, checkedDepartmentValue.indexOf(",")));
                dialog.enablePrimaryButtons($('#departmentsearchinputform').val().length >0);
            });

            $('#departmentsearchinputform').on('input',function(){
                if($('#departmentsearchinputform').val().length >0){
                    dialog.enablePrimaryButtons(true);
                }
                else{
                    dialog.enablePrimaryButtons(false);
                }
            })

            dialog.enablePrimaryButtons(false);
        }, function() {
            this.enablePrimaryButtons($('#departmentsearchinputform').val().length >0);
        });
    }

    function createDatatable() {
        return datatables.create('#departmentsearchresultstable', {
            destroy: true,
            paging: false,
            info: false,
            searching: false,
            order: [
                [2, 'asc']
            ],
            columnDefs: [
                { targets: [0], orderable: false },
            ],
            language: {
                emptyTable: 'Please perform a new search.'
            },
            select: 'single'
        });
    }

    function departmentContentFromAjax(id, whatKey, what, whereKey, where) {
        var data = {},
            viewDepartmentUrl = $('#searchDepartmentDialog').data().viewDepartmentUrl;

        data[whatKey] = what;
        data[whereKey] = where;

        $dialog.closeMessage();

        $.ajax({
            type: 'POST',
            url: viewDepartmentUrl,
            data: data,
            success: function (data) {

                $datatable.clear();
                $datatable.destroy();

                $('#' + id + ' tbody').html(data);
                $datatable = createDatatable();

                initSelectRadioButton();
            },
            error: function() {
                $dialog.alert('Error searching for departments.')
            }
        });
    }

    function initSelectRadioButton(){
        var departmentElement;
        var parent;
        var parentClassStr;
    	var departmentDisplayValue = $('#' + 'BUSINESS_UNITDisplay').val();
        var inputs=$(`input[data-name="department"]`).map(function(index, element){return element.value;});

        if($.inArray(departmentDisplayValue.concat(",",departmentDisplayValue),inputs) > 0){
            var departmentTableValue=departmentDisplayValue.concat(",",departmentDisplayValue);
            departmentElement=$(`input[data-name="department"][value= "${departmentTableValue}"]`);
            departmentElement.prop('checked',true);

        }
        else{
            departmentElement=$(`input[data-name="department"]`);
            departmentElement.first().prop('checked',true);

        }
        var departmentElementValue=departmentElement.val();
        $('#departmentsearchinputform').val(departmentElementValue.substr(0, departmentElementValue.indexOf(",")));
        $dialog.enablePrimaryButtons($('#departmentsearchinputform').val().length >0);
        datatables.enableCheckboxForSelection($datatable, 0);
    }

    return {
        openSearchDialog: function(whatKey, whereKey, departmentKey, priorityKey,DepartmentSelectedCb) {
            showSetDepartmentDialog(departmentKey, priorityKey,DepartmentSelectedCb);
            departmentContentFromAjax('departmentsearchresultstable', whatKey, 'departmentSearch', whereKey, $('#searchdepartment').val());
        }
    }
});
