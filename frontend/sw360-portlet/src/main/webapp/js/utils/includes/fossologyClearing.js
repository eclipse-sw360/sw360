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
define('utils/includes/fossologyClearing', ['jquery', /* jquery-plugins */ 'jquery-ui', 'datatables'], function($) {
    var fosstable,
        config = $('#fossologyClearing').data(),
        objectNamespacer = objectNamespacerOf(config.portletNamespace),
        refreshClearing = false;

    function initialize() {
        createFossTable(config.fossologyStatusUrl);

        $('#refreshStatusButton').on('click', redrawFossTable);
        $('#sendToFossolgyButton').on('click', function() {
            sendToFossology(config.fossologySendUrl, $('#fieldId').val(), $('#releaseId').val(), $('#clearingTeam').val());
        });
    }

    function redrawFossTable() {
        fosstable.draw();
    }

    function updateAttachmentName(attachment) {
        $('#attachmentFossology').val(attachment);
    }

    function createFossTable(fossologyStatusUrl) {
        var $releaseId = $('#releaseId');
        var $clearingTeam = $('#clearingTeam');

        fosstable = $('#attachmentFossologyStatus').DataTable({
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
            processing: true,
            scrollY: '75%',
            columns: [
                {title: "Clearing Team"},
                {title: "Fossology Status"}
            ],
            autoWidth: false
        });

        $releaseId.change(redrawFossTable);
    }

    function openSelectClearingDialog(fieldId, releaseId) {
        refreshClearing = false;
        $('#fieldId').val(fieldId);
        $('#releaseId').val(releaseId).change();
        openDialog('fossologyClearing', 'clearingTeam', .5, .5);
    }

    function sendToFossology(fossologySendUrl, fieldId, releaseId, clearingTeam) {
        closeOpenDialogs();
        var $responseField = $('#' + fieldId);
        $responseField.html("Sending...");

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
                $responseField.html("Sent");
            } else {
                $responseField.html("Error");
            }
        }).fail(function () {
            $responseField.html("Error");
        });
    }

    return {
        initialize: initialize,
        openSelectClearingDialog: function(prefix, releaseId) {
            openSelectClearingDialog(prefix + releaseId, releaseId);
        }
    }
});