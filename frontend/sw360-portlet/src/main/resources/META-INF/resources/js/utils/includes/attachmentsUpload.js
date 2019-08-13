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
define('utils/includes/attachmentsUpload', ['jquery', 'resumable', /* jquery-plugins */ 'jquery-ui' ], function(jquery, Resumable) {
    var attachmentAddedCb,
        r = false,
        ra = false,
        urls = {},
        data = $('#fileupload-form').data(),
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
            if (ra.isEmpty()) {
                closeOpenDialogs();
            }
        });
    });

    r.on('fileError', function (file) {
        alert("I could not upload the file: " + file.fileName);
    });

    ra.on('fileCancel', function (file) {
        var attachmentId = file.uniqueIdentifier;

        if (attachmentId) {
            cancelAttachment(attachmentId);
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

        $("<button>Upload</button>").appendTo($container).click(function () {
            resumable.upload();
        });
        $("<button>Pause</button>").appendTo($container).click(function () {
            resumable.pause();
        });

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
        };

        var addFile = function (file) {
            var $fileController = $("<div></div>").appendTo($fileControllers);

            $("<span></span>").text(file.fileName + " (" + file.size + "b)").appendTo($fileController);

            $("<button>Restart</button>").appendTo($fileController).click(function () {
                file.retry();
            });
            $("<button>Cancel</button>").appendTo($fileController).click(function () {
                file.abort();
                doCallback('fileCancel', file);
                removeFile(file);
            });

            var $progress = $("<div></div>").appendTo($fileController)
                .progressbar({
                    max: 1
                });

            var controller = {
                $fileController: $fileController,
                $progress: $progress
            };

            fileControllers[fileToId(file)] = controller;
        };

        var drawFileProgress = function (file) {
            var controller = fileControllers[fileToId(file)];
            controller.$progress.progressbar("value", file.progress());
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
            }
        }

    };

    return {
        showUploadDialog: function(callback) {
            attachmentAddedCb = callback;
            openDialog('fileupload-form', 'fileupload-files');
        }
    };
});
