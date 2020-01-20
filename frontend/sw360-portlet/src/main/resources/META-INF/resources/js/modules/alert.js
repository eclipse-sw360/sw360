/*
 * Copyright Siemens AG, 2017-2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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