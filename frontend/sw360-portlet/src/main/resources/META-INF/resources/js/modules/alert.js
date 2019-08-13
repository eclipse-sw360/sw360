/*
 * Copyright Siemens AG, 2017-2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('modules/alert', [ 'jquery' ], function($) {

    function showMessage(type, containerSelector, message, timeout) {
		var $container = $(containerSelector),
            $alert = $('<div>', {
                'class': 'alert fade show',
                role: 'alert'
            }),
            $closeButton = $('<button>', {
                'class': 'close',
                type: 'button',
                'data-dismiss': 'alert',
                'aria-label': 'Close',
            }).append('<span aria-hidden="true">&times;</span>');

        $alert.addClass('alert-' + type);
		$alert.append(message);
        $alert.append($closeButton);

        $container.prepend($alert);

        if(timeout) {
            setTimeout(function() {
                $alert.alert('close');
            }, timeout * 1000);
        }
    }

    function closeMessage(containerSelector) {
        $(containerSelector).find('.alert').alert('close');
    }

    return {
        danger: showMessage.bind(null, 'danger'),
        warning: showMessage.bind(null, 'warning'),
        info: showMessage.bind(null, 'info'),
        success: showMessage.bind(null, 'success'),
        close: closeMessage
    }
});