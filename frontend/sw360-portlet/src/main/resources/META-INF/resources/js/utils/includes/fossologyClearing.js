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
define('utils/includes/fossologyClearing', [
    'jquery',
    'utils/object',
    'modules/dialog',
    'bridges/datatables'
], function($, object, dialog, datatables) {
    var fosstable,
        config,
        objectNamespacer,
        refreshClearing;

    function initialize() {
        config = $('#fossologyClearingDialog').data(),
        objectNamespacer = object.namespacerOf(config.portletNamespace),
        refreshClearing = false;

        createFossTable(config.fossologyStatusUrl);
    }

    function updateAttachmentName(attachment) {
        $('#attachmentFossology').val(attachment);
    }

    function createFossTable(fossologyStatusUrl) {
        var $releaseId = $('#releaseId');
        var $clearingTeam = $('#clearingTeam');

        fosstable = datatables.create('#attachmentFossologyStatus', {
            ajax: {
                url: fossologyStatusUrl,
                type: 'POST',
                data: function (data) {
                    var releaseId = $releaseId.val();
                    var clearingTeam = $clearingTeam.val();

                    data = data || {};
                    data.releaseId = releaseId;
                    data.clearingTeam = clearingTeam;
                    data.cached = !refreshClearing;

                    refreshClearing = true;

                    return objectNamespacer(data);
                },
                dataSrc: function(json) {
                    updateAttachmentName(json.attachment);
                    return json.data;
                }
            },
            serverSide: true,
            filter: false,
            sortable: false,
            paginate: false,
            scrollY: '75%',
            columns: [
                {title: "Clearing Team"},
                {title: "Fossology Status"}
            ]
        });
    }

    function openSelectClearingDialog(fieldId, releaseId) {
        refreshClearing = false;

        var $dialog = dialog.open('#fossologyClearingDialog', {
            fieldId: fieldId,
            releaseId: releaseId
        }, function(submit, callback) {
            if(submit === 'refresh') {
                fosstable.draw();
                callback();
            } else if(submit === 'send') {
                sendToFossology(config.fossologySendUrl, $('#fieldId').val(), $('#releaseId').val(), $('#clearingTeam').val()).then(function() {
                    $dialog.success('Data has been sent', true);
                }).catch(function() {
                    $dialog.alert('Files could not be send to fossolgy.', true);
                }).finally(function() {
                    callback();
                });
            }
        });
        $dialog.$.on('shown.bs.modal', function(event) {
            fosstable.draw();
        });
    }

    function sendToFossology(fossologySendUrl, fieldId, releaseId, clearingTeam, callback) {
        return new Promise(function(resolve, reject) {
            $.ajax({
                type: 'POST',
                url: fossologySendUrl,
                cache: false,
                data: objectNamespacer({
                    releaseId: releaseId,
                    clearingTeam: clearingTeam
                })
            }).done(function (data) {
                if (data.result == 'SUCCESS') {
                    resolve();
                } else {
                    reject();
                }
            }).fail(function () {
                reject();
            });
        });
    }

    return {
        initialize: initialize,
        openSelectClearingDialog: function(prefix, releaseId) {
            openSelectClearingDialog(prefix + releaseId, releaseId);
        }
    }
});
