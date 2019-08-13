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
define('modules/sw360Validate', ['jquery', /* jquery-plugins: */ 'jquery.validate-addon', /* legacy code */ 'main' ], function($) {
	return {
		validateWithInvalidHandler: function(selector) {
			$(selector).validate({
				invalidHandler: invalidHandlerShowErrorTab
			});
		},
		validateWithInvalidHandlerNoIgnore: function(selector) {
			$(selector).validate({
			    ignore: [],
	            invalidHandler: invalidHandlerShowErrorTab
			});
		},
	};
});