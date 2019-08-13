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
