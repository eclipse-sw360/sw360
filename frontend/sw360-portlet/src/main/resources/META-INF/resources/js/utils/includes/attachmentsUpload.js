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
define('utils/includes/attachmentsUpload', ['jquery', 'resumable', 'modules/dialog' ], function($, Resumable, dialog) {
    var $dialog,
        $dialogCb,
        attachmentAddedCb,
        r,
        ra,
        urls,
        data,
        portletNamespace;

    function initialize() {
        urls = {};
        data = $('#fileUploadDialog').data();
        portletNamespace = data.portletNamespace;

        urls.linkAttachment = data.linkAttachmentUrl;
        urls.cancelAttachment = data.cancelAttachmentUrl;
        urls.newAttachment = data.newAttachmentUrl;
        urls.uploadAttachmentPart = data.uploadAttachmentPartUrl;

        r = new Resumable({
            target: urls.uploadAttachmentPart,
            parameterNamespace: portletNamespace,
            simultaneousUploads: 1,
            generateUniqueIdentifier: getAttachmentIdPromise,
            chunkRetryInterval: 2000,
            maxChunkRetries: 3
        });

        ra = new ResumableAttachments(r, $('#fileupload-files'));

        r.assignBrowse($('#fileupload-browse')[0]);
        r.assignDrop($('#fileupload-drop')[0]);

        r.on('fileAdded', ra.addFile);
        r.on('fileProgress', function (file) {
            ra.drawFileProgress(file);
        });
        r.on('fileSuccess', function (file) {
            var attachmentId = file.uniqueIdentifier;

            linkAttachment(attachmentId).then(function () {
                ra.removeFile(file);
                if (ra.isEmpty() && $dialogCb) {
                    $dialogCb(true);
                    $dialogCb = undefined;
                }
            });
        });

        r.on('fileError', function (file) {
            $dialog.alert("I could not upload the file: " + file.fileName);
        });

        ra.on('fileCancel', function (file) {
            var attachmentId = file.uniqueIdentifier;

            if (attachmentId) {
                cancelAttachment(attachmentId);
            }

            if (ra.isEmpty() && $dialogCb) {
                $dialogCb(false);
                $dialogCb = undefined;
            }
        });

        // helper functions
        function linkAttachment(attachmentId) {
            var data = {};
            data[portletNamespace + "attachmentId"] = attachmentId;

            return $.ajax({
                url: urls.linkAttachment,
                cache: false,
                data: data
            }).done(function (data) {
                attachmentAddedCb(data);
            }).fail(function () {
                cancelAttachment(attachmentId);
            });
        }

        function cancelAttachment(attachmentId) {
            var data = {};
            data[portletNamespace + "attachmentId"] = attachmentId;

            $.ajax({
                url: urls.cancelAttachment,
                cache: false,
                data: data
            });
        }

        function getAttachmentIdPromise(file) {
            var data = {};
            data[portletNamespace + "fileName"] = file.fileName || file.name;

            return $.ajax({
                url: urls.newAttachment,
                cache: false,
                dataType: 'text',
                data: data
            });
        }

        // helper class
        function ResumableAttachments(resumable, $container) {
            var callbacks = {},
                fileControllers = {};

            var $fileControllers = $("<div></div>").appendTo($container);

            var fileToId = function(file) {
                return file.uniqueIdentifier;
            }

            var doCallback = function(name, file) {
                if (callbacks.hasOwnProperty(name)) {
                    var callback = callbacks[name];
                    if (typeof callback == 'function') {
                        callback(file);
                    }
                }
            };

            var removeFile = function (file) {
                fileControllers[fileToId(file)].$fileController.remove();
                delete fileControllers[fileToId(file)];
                resumable.removeFile(file);

                if(Object.keys(fileControllers).length == 0) {
                    // disable upload button, no files left
                    $dialog.$.find('button[data-submit="upload"]').prop('disabled', true);
                }
            };

            var addFile = function (file) {
                var $infoContainer,
                    $progressContainer,
                    $deleteButton,
                    $buttonContainer;

                $infoContainer = $('<div class="row"></div>').appendTo($fileControllers),

                $('<div class="col-8"></div>').text(file.fileName + " (" + file.size + "b)").appendTo($infoContainer);

                $buttonContainer = $('<div class="col content-right"></div>').appendTo($infoContainer);
                $deleteButton = $('<button type="button" class="btn btn-danger btn-sm" data-for="delete">Delete</button>');
                $deleteButton.attr('data-for', 'delete');
                $deleteButton.prop('disabled', resumable.isUploading());
                $deleteButton.appendTo($buttonContainer).click(function () {
                    file.abort();
                    doCallback('fileCancel', file);
                    removeFile(file);

                    if (ra.isEmpty() && $dialogCb) {
                        $dialogCb(false);
                        $dialogCb = undefined;
                    }
                });

                $progressContainer = $('<div class="row"></div>').appendTo($fileControllers);
                $progressContainer.append('<div class="col"><div class="progress"></div></div>');
                var $progress = $('<div style="width: 0%;" class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>');
                $fileControllers.find('.progress').append($progress);

                var controller = {
                    file: file,
                    $progress: $progress,
                    $fileController: $([$infoContainer[0], $progressContainer[0]])
                };

                fileControllers[fileToId(file)] = controller;
                // enable upload button at least one file is there
                $dialog.$.find('button[data-submit="upload"]').prop('disabled', false);
            };

            var drawFileProgress = function (file) {
                var controller = fileControllers[fileToId(file)],
                    progress = file.progress() * 100;

                controller.$progress.width(progress + '%');
                controller.$progress.attr('aria-valuenow', progress);
            };

            return {
                drawFileProgress: drawFileProgress,
                addFile: addFile,
                removeFile: removeFile,
                isEmpty: function () {
                    return Object.keys(fileControllers).length == 0;
                },
                on: function (eventName, callback) {
                    callbacks[eventName] = callback;
                },
                clear: function() {
                    Object.keys(fileControllers).forEach(function(id) {
                        removeFile(fileControllers[id].file);
                    });
                }
            }

        };
    }

    return {
        initialize: initialize,

        showUploadDialog: function(callback) {
            attachmentAddedCb = callback;

            $dialog = dialog.open('#fileUploadDialog', {
            }, function(submit, callback) {
                if(submit == "upload") {
                    $dialogCb = callback;
                    r.upload();
                    // we need to reenable the pause button because it is disabled during progress of an button
                    // by default
                    $dialog.$.find('button[data-submit="pause"]').prop('disabled', false);
                    // disable delete buttons while upload is in progress
                    $dialog.$.find('button[data-for="delete"]').prop('disabled', true);
                } else if(submit == "pause") {
                    r.pause();
                    callback(false);
                    // finish update button as well
                    $dialogCb(false);
                    $dialogCb = undefined;
                    // we need to disable the pause button if no upload is running
                    $dialog.$.find('button[data-submit="pause"]').prop('disabled', true);
                    // reenable remove buttons for attachments
                    $dialog.$.find('button[data-for="delete"]').prop('disabled', false);
                }
            }, function() {
                ra.clear();
                // we need to disable the pause button if no upload is running
                this.$.find('button[data-submit="pause"]').prop('disabled', true);
                // initially disable upload button while no file was added
                this.$.find('button[data-submit="upload"]').prop('disabled', true);
            });
        }
    };
});
