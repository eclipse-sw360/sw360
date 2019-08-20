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
define('utils/link', function() {
	var prefix = themeDisplay.getPortalURL() + '/group/guest/',
		linkMap = {
			'project': {
				'show': 'projects/-/project/detail/',
				'edit': 'projects/-/project/edit/'
			},
			'component': {
				'show': 'components/-/component/detail/'
			},
			'moderationRequest': {
				'edit': 'moderation/-/moderation/edit/'
			}
		};

	return {
		to: function(type, action, id) {
			return prefix + linkMap[type][action] + encodeURI(id);
		}
	};
});
