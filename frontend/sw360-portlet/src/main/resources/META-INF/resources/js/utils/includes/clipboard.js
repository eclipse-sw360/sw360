/*
 * Copyright Siemens AG, 2021.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
define('utils/includes/clipboard', ['jquery'], function($) {

const toast = '<div class="toast position-absolute d-inline" data-delay="2100"> <div class="toast-header">'+Liferay.Language.get('copied')+'</div></div>';
const Failedtoast = '<div class="toast position-absolute d-inline" data-delay="2100"> <div class="toast-header">'+Liferay.Language.get('failed')+'</div></div>';

    function copyToClipboard(text, textSelector) {
        navigator.clipboard.writeText(text)
        .then(() => {
            $(textSelector).append(toast);
            $('.toast').toast('show');
        })
        .catch(() => {
            $(textSelector).append(Failedtoast);
            $('.toast').toast('show');
        })

        setTimeout(function() {
            $(textSelector+" .toast").remove();
        }, 2200);
    }

	return {
		copyToClipboard: copyToClipboard
	};
});
