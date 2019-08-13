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
define('components/includes/vendors/addVendor', ['jquery', 'modules/dialog', 'modules/validation', 'utils/keyboard' ], function($, dialog, validation, keyboard) {

    function showDialog(fullnameKey, shortnameKey, urlKey, vendorAddedCb) {
        var $dialog = dialog.open('#addVendorDialog', {
            fullname: '',
            shortname: '',
            url: ''
        }, function(submit, callback, data) {
            if(validation.validate('#addVendorDialog .modal-body form')) {
                addVendor(data.fullname, data.shortname, data.url, fullnameKey, shortnameKey, urlKey, $dialog, callback, vendorAddedCb);
            } else {
                callback();
            }
        }, function() {
            this.$.find('.modal-body form').removeClass('was-validated');
            keyboard.bindkeyPressToClick('vendorURL', this.$.find('button[name="add-vendor"]'));
        });
    }

    function addVendor(fullname, shortname, url, fullnameKey, shortnameKey, urlKey, $dialog, callback, vendorAddedCb) {
        var data = {},
            addVendorUrl = $('#addVendorDialog').data().addVendorUrl;

        data[fullnameKey] = fullname;
        data[shortnameKey] = shortname;
        data[urlKey] = url;

        jQuery.ajax({
            type: 'POST',
            url: addVendorUrl,
            data: data,
            success: function (data) {
                vendorAddedCb(data.id + ',' + fullname);
                callback(true);
            },
            error: function() {
                $dialog.alert('Cannot add vendor.');
                callback();
            }
        });
    }

    return {
        showDialog: showDialog
    };
});
