/*
 * Copyright Siemens AG, 2017, 2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('components/includes/vendors/searchVendor', ['jquery', 'bridges/datatables', 'components/includes/vendors/addVendor', 'modules/button', 'modules/dialog', 'utils/keyboard' ], function($, datatables, vendoradd, button, dialog, keyboard) {
    var $dialog,
        $datatable;

    function showSetVendorDialog(fullnameKey, shortnameKey, urlKey, vendorSelectedCb) {
        $dialog = dialog.open('#searchVendorDialog', {}, function(submit, callback, data) {
            if(submit ==  'select') {
                vendorSelectedCb(data.vendor);
                callback(true);
            } else if(submit == 'add') {
                callback(true);
                vendoradd.showDialog(fullnameKey, shortnameKey, urlKey, vendorSelectedCb);
            }
        }, function() {
            var dialog = this;

            // we do not reset the datatable. This way a new search is not necessary
            if(!$datatable) {
                $datatable = createDatatable();
                datatables.enableCheckboxForSelection($datatable, 0);
            }

            $('#searchresultstable').off('change.vendor-search');
            $('#searchresultstable').on('change.vendor-search', 'input[data-name="vendor"]', function() {
                dialog.enablePrimaryButtons(dialog.$.find('input[data-name="vendor"]:checked').length > 0);
            });

            dialog.enablePrimaryButtons(false);
            keyboard.bindkeyPressToClick('searchvendor', 'searchbuttonvendor');
        }, function() {
            this.enablePrimaryButtons(this.$.find('input[data-name="vendor"]:checked').length > 0);
        });
    }

    function createDatatable() {
        return datatables.create('#searchresultstable', {
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
            initComplete: function() {
                $('#searchbuttonvendor').prop('disabled', false);
            },
            language: {
                emptyTable: 'Please perform a new search.'
            },
            select: 'single'
        });
    }

    function vendorContentFromAjax(id, whatKey, what, whereKey, where) {
        var data = {},
            viewVendorUrl = $('#searchVendorDialog').data().viewVendorUrl;

        data[whatKey] = what;
        data[whereKey] = where;

        button.wait('#searchbuttonvendor');
        $dialog.closeMessage();

        $.ajax({
            type: 'POST',
            url: viewVendorUrl,
            data: data,
            success: function (data) {
                button.finish('#searchbuttonvendor');

                $datatable.clear();
                $datatable.destroy();

                $('#' + id + ' tbody').html(data);
                $datatable = createDatatable();
            },
            error: function() {
                button.finish('#searchbuttonvendor');
                $dialog.alert('Error searching for vendors.')
            }
        });
    }

    return {
        openSearchDialog: function(whatKey, whereKey, fullnameKey, shortnameKey, urlKey, vendorSelectedCb) {
            $('#searchbuttonvendor').off('click.vendor-search');
            $('#searchbuttonvendor').on('click.vendor-search', function(event) {
                vendorContentFromAjax('searchresultstable', whatKey, 'vendorSearch', whereKey, $('#searchvendor').val());
            });

            showSetVendorDialog(fullnameKey, shortnameKey, urlKey, vendorSelectedCb);
        }
    }
});
