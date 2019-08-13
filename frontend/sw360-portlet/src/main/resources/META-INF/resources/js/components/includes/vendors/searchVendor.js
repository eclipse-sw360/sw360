/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('components/includes/vendors/searchVendor', ['jquery', 'components/includes/vendors/addVendor', /* jquery-plugins: */ 'jquery-ui'], function(jQuery, vendoradd) {
    function showSetVendorDialog() {
        openDialog('search-vendor-form', 'searchvendor');
    }

    function selectVendor(vendorSelectedCb) {
        $('#searchresultstable').find(':radio').each(
            function(){
                if(this.checked){
                    vendorSelectedCb($(this).val());
                    closeOpenDialogs();
                    return false;
                }
            }
        );
    }

    function vendorContentFromAjax(id, whatKey, what, whereKey, where) {
        var data = {},
            viewVendorUrl = $('#search-vendor-form').data().viewVendorUrl;

        data[whatKey] = what;
        data[whereKey] = where;

        jQuery.ajax({
            type: 'POST',
            url: viewVendorUrl,
            data: data,
            success: function (data) {
                $('#' + id + ' tbody').html(data);
            }
        });
    }

    jQuery(document).ready(function () {
        bindkeyPressToClick('searchvendor', 'searchbuttonvendor');
    });

    return {
        openSearchDialog: function(whatKey, whereKey, fullnameKey, shortnameKey, urlKey, vendorSelectedCb) {
            $('#searchbuttonvendor').off('click.vendor-search');
            $('#searchbuttonvendor').on('click.vendor-search', function(event) {
                vendorContentFromAjax('searchresultstable', whatKey, 'vendorSearch', whereKey, $('#searchvendor').val());
            });

            $('#vendorsearchresults input[name=select-vendor]').off('click.vendor-search');
            $('#vendorsearchresults input[name=select-vendor]').on('click.vendor-search', function(event) {
                selectVendor(vendorSelectedCb);
            });

            $('#vendorsearchresults input[name=add-vendor]').off('click.vendor-search');
            $('#vendorsearchresults input[name=add-vendor]').on('click.vendor-search', function(event) {
                vendoradd.showDialog(fullnameKey, shortnameKey, urlKey, vendorSelectedCb);
            });

            showSetVendorDialog();
        }
    }
});
