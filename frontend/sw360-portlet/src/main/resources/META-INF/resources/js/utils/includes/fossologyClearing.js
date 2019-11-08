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
        $dialog,
        // it is possible that we are waiting on a ajax response while the dialog gets closed by the user. so there is
        // no timeout running which can be cancelled - but we still have to cancel the recursive calling, so we need an
        // extra flag for that
        dialogOpen,
        timeoutId;

    function initialize() {
        config = $('#fossologyClearingDialog').data(),
        objectNamespacer = object.namespacerOf(config.portletNamespace);
    }

    function openFossologyProcessDialog(releaseId) {
        $dialog = dialog.open('#fossologyClearingDialog',
            // data:
            {},
            // submitCallback
            function(submit, callback, data) {
                if (submit == 'outdated') {
                    setOutdated(releaseId);
                    callback(false);
                }
            },
            // beforeShow:
            function() {
                startRecursion(releaseId);
            }
        );
        $dialog.$.on('hidden.bs.modal', function() {
            clearTimeout(timeoutId);
            dialogOpen = false;
        });
    }

    function setOutdated(releaseId) {
        // stop the recursive default processing
        clearTimeout(timeoutId);
        dialogOpen = false;
        $('.auto-refresh').hide();

        dialog.confirm(
            'danger',
            'question-circle',
            'Reset FOSSology Process?',
            '<p>Do you really want to set the current FOSSology process to state "OUTDATED"? This cannot be undone and a new process is started the next time you open this popup.</p>',
            'Set To Outdated',
            {},
            // submitCallback
            function(submit, callback) {
                fossologyRequest(config.fossologyOutdatedUrl, releaseId)
                    .then(function(data) {
                        $dialog.closeMessage();
                        $dialog.success("The FOSSology process has been set to OUTDATED and will start all over.", false);
                    })
                    .catch(function(message) {
                        $dialog.closeMessage();
                        $dialog.alert(message, false);
                    })
                    .finally(function() {
                        callback(true);
                        startRecursion(releaseId);
                    })
                ;
            }
        );
    }

    function startRecursion(releaseId) {
        dialogOpen = true;

        $('#name-of-source-attachment').text("").addClass("spinner-border spinner-border-sm");
        $('.auto-refresh').hide();

        fossologyRequest(config.fossologyStatusUrl, releaseId)
            .then(function(data) {
                var numberOfSourceAttachments = data["sourceAttachments"];
                if (numberOfSourceAttachments === 1) {
                    $('.auto-refresh').show();
                    $('#name-of-source-attachment').text(data["sourceAttachmentName"]).removeClass("spinner-border spinner-border-sm");
                    handleSuccessResult($dialog, releaseId, data);
                } else {
                    $('#name-of-source-attachment').text("unknown").removeClass("spinner-border spinner-border-sm");
                    $dialog.closeMessage();
                    $dialog.alert("There has to be exactly one source attachment, but there are "
                            + numberOfSourceAttachments + " at this release. Please come back once you corrected that.", true);
                }
            })
            .catch(function(message) {
                $('#name-of-source-attachment').text("unknown").removeClass("spinner-border spinner-border-sm");
                $dialog.closeMessage();
                $dialog.alert(message, false);
            })
        ;
    }

    function fossologyRequest(fossologyUrl, releaseId) {
        return new Promise(function(resolve, reject) {
            $.ajax({
                type: 'POST',
                url: fossologyUrl,
                cache: false,
                data: objectNamespacer({
                    releaseId: releaseId
                })
            }).done(function (data) {
                if (data["error"]) {
                    reject(data["error"]);
                } else {
                    resolve(data);
                }
            }).fail(function (jqXHR, textStatus, errorThrown) {
                reject(errorThrown);
            });
        });
    }

    function handleSuccessResult($dialog, releaseId, data) {
        var finished = updateUI(data);
        if (!finished) {
            if (dialogOpen) {
                timeoutId = setTimeout(process.bind(this, $dialog, releaseId, 5), 1000);
            }
        } else {
            $('.auto-refresh').hide();
            clearTimeout(timeoutId);
            dialogOpen = false;
            $dialog.closeMessage();
            $dialog.success("The FOSSology process already finished. You should find the resulting report as attachment at this release.", false);
        }
    }

    function process($dialog, releaseId, secondsTillRefresh) {
        secondsTillRefresh--;

        if (secondsTillRefresh > 0) {
            $('#auto-refresh-seconds').text(secondsTillRefresh);
            timeoutId = setTimeout(process.bind(this, $dialog, releaseId, secondsTillRefresh), 1000);
        } else {
            $('#auto-refresh-seconds').text("").addClass("spinner-border spinner-border-sm");

            fossologyRequest(config.fossologyProcessUrl, releaseId).then(function(data) {
                // even though the dialog might be closed, lets finish the update gui and then decide if a new recursion
                // should be started
                handleSuccessResult($dialog, releaseId, data);
                $('#auto-refresh-seconds').text("5").removeClass("spinner-border spinner-border-sm");
            }).catch(function(message) {
                $('.auto-refresh').hide();
                clearTimeout(timeoutId);
                dialogOpen = false;
                $dialog.closeMessage();
                $dialog.alert(message, false);
            });
        }
    }

    function updateUI(data) {
        // 7 steps from 0 to 100 means 100/6 = 16.66 each step size
        var stepSize = 16.66,
            progressText = "",
            progressPercent = 0;
        if (data["stepName"] == config.stepNameReport) {
            progressText += "Report generation";
            progressPercent = 4 * stepSize;
        } else if (data["stepName"] == config.stepNameScan) {
            progressText += "Scanning source";
            progressPercent = 2 * stepSize;
        } else {
            // upload as default
            progressText += "Uploading source";
            progressPercent = 0 * stepSize;
        }

        if (data["stepStatus"] == config.stepStatusDone) {
            progressText += " done";
            progressPercent += 2 * stepSize;
        } else if (data["stepStatus"] == config.stepStatusInwork) {
            progressText += " in progress";
            progressPercent += 1 * stepSize;
        } else {
            // new as default
            progressText += " to be started";
            progressPercent += 0 * stepSize;
        }

        $('.progress-bar').css("width", progressPercent + "%").attr("aria-valuenow", progressPercent).text(progressText);

        return data["stepName"] == config.stepNameReport && data["stepStatus"] == config.stepStatusDone;
    }

    return {
        initialize: initialize,
        openFossologyDialog: openFossologyProcessDialog
    }
});
