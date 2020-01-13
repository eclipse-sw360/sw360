/*
 * Copyright Siemens AG, 2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
define('utils/keyboard', ['jquery'], function($) {

	function bindkeyPressToClick(keyed, clicked) {
		var $keyed = $('#' + keyed);
		var $clicked = toJQuery(clicked);

		if($keyed.length == 0) {
			console.error('Could not find input field: ' + keyed);
			return;
		}
		if($clicked.length == 0) {
			console.error('Could not find button: ' + clicked);
			return;
		}

		$keyed.off('keypress.' + keyed);
		$keyed.on('keypress.' + keyed, function (e) {
			if (e.which == 13) {
				e.preventDefault();
				$clicked.click();
			}
		});
	}

	function toJQuery(idOrElement) {
		if (typeof idOrElement == "string") {
			return $('#' + idOrElement);
		} else {
			return idOrElement;
		}
	}

	return {
		bindkeyPressToClick: bindkeyPressToClick
	};
});
