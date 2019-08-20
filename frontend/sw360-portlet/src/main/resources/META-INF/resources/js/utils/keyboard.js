/*
 * Copyright Siemens AG, 2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
