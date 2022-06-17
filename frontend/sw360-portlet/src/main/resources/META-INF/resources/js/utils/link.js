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
				'edit': 'moderation/-/moderation/edit/',
				'list': 'moderation'
			},
			'vendor': {
			    'list': 'vendors'
			}
		};

	return {
		to: function(type, action, id) {
			return prefix + linkMap[type][action] + encodeURI(id);
		}
	};
});
