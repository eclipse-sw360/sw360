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

const toast = '<div class="toast w-25 position-absolute fixed-top"> <div class="toast-header">'+Liferay.Language.get('copied')+'</div></div>';
const Failedtoast = '<div class="toast w-25 position-absolute fixed-top"> <div class="toast-header">'+Liferay.Language.get('failed')+'</div></div>';

    function copyToClipboard(text, textSelector, alterDefaultDisplay) {
        navigator.clipboard.writeText(text)
        .then(() => {
            $(textSelector).append(toast);
            if(alterDefaultDisplay) {
                removeWidthClassAndAddInlineDisplay(textSelector);
            }
            $('.toast').toast({delay: 2000});
            $('.toast').toast('show');
        })
        .catch((error) => {
            $(textSelector).append(Failedtoast);
            if(removeWidthClassAndAddInlineDisplay) {
                removeWidthClassAndAddInlineDisplay(textSelector);
            }
            $('.toast').toast({delay: 2000});
            $('.toast').toast('show');
        })

        setTimeout(function() {
            $(textSelector+" .toast").remove();
        }, 2000);
    }

    function removeWidthClassAndAddInlineDisplay(textSelector) {
        $(textSelector).find("div.toast:first").removeClass("w-25");
        $(textSelector).find("div.toast-header:first").addClass("d-inline");
    }

	return {
		copyToClipboard: copyToClipboard
	};
});
