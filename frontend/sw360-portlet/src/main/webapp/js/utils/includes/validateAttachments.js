/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('utils/includes/validateAttachments', ['jquery', 'jquery-confirm'], function($) {

function validateAttachment(url, formId, type) {
        $.ajax({
            type : 'POST',
            url : url,
            cache : false,
            data : $('#'+formId).serialize(),
            success : function(response) {
                if (response.result == true) {
                    showWarningDialog(formId, type);
                } else {
                    $('#'+formId).submit();
                }
            }
        });
    }

    function showWarningDialog(formId, type) {
        var message = "Duplicate attachments exist in "+ type + " record. Please confirm to proceed or cancel to make corrections?";
        return $.confirm({
                    title : 'Warning',
                    content : message,
                    buttons : {
                        confirm : {
                            btnClass : 'btn-green',
                            action : function() {
                                $('#'+formId).submit();
                            }
                        },
                        cancel : {
                            btnClass : 'btn-red',
                        }
                    }
                });
    }

    return {
        attachmentValidation:validateAttachment
    }
});
