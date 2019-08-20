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
