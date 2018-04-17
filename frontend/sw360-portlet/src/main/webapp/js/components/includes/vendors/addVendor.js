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
define('components/includes/vendors/addVendor', ['jquery', /* jquery-plugins: */ 'jquery-ui', 'jquery.validate.min'], function($) {

    function addVendor() {
        openDialog('add-vendor-form', 'addVendorFullName');
        $('#addVendorFullName').val("");
        $('#addVendorShortName').val("");
        $('#addVendorUrl').val("");
        $('#divVendorSearchAddVendorError').html("");

        $('#add-vendor-form form').validate().resetForm();
    }

    function submitAddVendor(fullnameId, shortnameId, urlId, fullnameKey, shortnameKey, urlKey, vendorAddedCb) {
        var data = {},
            fullnameText = $('#' + fullnameId).val(),
            shortnameText = $('#' + shortnameId).val(),
            urlText = $('#' + urlId).val(),
            addVendorUrl = $('#add-vendor-form').data().addVendorUrl;

        data[fullnameKey] = fullnameText;
        data[shortnameKey] = shortnameText;
        data[urlKey] = urlText;

        if ($('#add-vendor-form form').valid()) {
            jQuery.ajax({
                type: 'POST',
                url: addVendorUrl,
                data: data,
                success: function (data) {
                    closeOpenDialogs();
                    vendorAddedCb(data.id + ',' + $('#' + fullnameId).val());
                }
            });
        }
    }

    return {
        showDialog: function(fullnameKey, shortnameKey, urlKey, vendorAddedCb) {
            addVendor();

            $('#add-vendor-form input[name=add-vendor]').off('click.add-vendor');
            $('#add-vendor-form input[name=add-vendor]').on('click.add-vendor', function() {
                submitAddVendor('addVendorFullName', 'addVendorShortName', 'addVendorUrl',
                        fullnameKey, shortnameKey, urlKey, vendorAddedCb);
            });

        }
    };
});
