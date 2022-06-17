/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/**
 * Module for loading CSS files via javascript.
 */
define('utils/cssloader', [], function() {
	// blank it if it is not set
	contextPath = contextPath || '';

	return {
		load: function(files) {
			files.forEach(function(file) {
				var $link = $('<link>', {
					type: 'text/css',
					rel: 'stylesheet',
					href: contextPath + file
				});
				$('head').append($link);
			});
		}
	};
});
