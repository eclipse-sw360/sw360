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

/**
 * Module bridge for jquery-ui. Sets necessary defaults and loads all dependencies as well as css files.
 */
define('bridges/jquery-ui', [
	'jquery',
	'utils/cssloader',
	/* jquery-plugins */
	'jquery-ui'
], function($, cssloader) {
	cssloader.load([
		'/webjars/jquery-ui/themes/base/jquery-ui.min.css'
	]);
});
