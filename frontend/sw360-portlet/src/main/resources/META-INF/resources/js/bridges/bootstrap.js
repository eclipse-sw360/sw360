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
 * Module bridge for Bootstrap. Extends jQueyr with Bootstrap functions.
 */
define('bridges/bootstrap', [
	'jquery',
	/* boostrap is loaded by default by liferay */
], function($) {
	$.fn.modal = bootstrap.Modal._jQueryInterface;
});
