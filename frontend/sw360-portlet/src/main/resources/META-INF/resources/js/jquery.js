/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/**
 * Wrapper module for jquery to return the already existing global object created by liferay.
 * This way events between liferay and own modules become possible.
 */
define('jquery', function() {
	return $;
});
