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
define('modules/button', [ 'jquery' ], function($) {

    function wait(selector) {
        var $spinner,
            $button = $(selector);

        if($button.length === 0) {
            console.error('Cannot find button: ' + selector);
            return;
        }

        $spinner = $('<span>', {
            'class': 'spinner-border spinner-border-sm',
            'role': 'status',
            'aria-hidden': 'true'
        });
		$button.append($spinner);
		$button.attr('disabled', 'disabled');
    }

    function finish(selector) {
        var $button = $(selector);

        if($button.length === 0) {
            console.error('Cannot find button: ' + selector);
            return;
        }

        $button.find('.spinner-border').remove();
		$button.removeAttr('disabled');
    }

    function disable(selector) {
        var $button = $(selector);

        if($button.length === 0) {
            console.error('Cannot find button: ' + selector);
            return;
        }

		$button.attr('disabled', 'disabled');
    }

    function enable(selector) {
        var $button = $(selector);

        if($button.length === 0) {
            console.error('Cannot find button: ' + selector);
            return;
        }

		$button.removeAttr('disabled');
    }

    return {
        wait: wait,
        finish: finish,
        enable: enable,
        disable: disable
    }
});
