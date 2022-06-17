/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
