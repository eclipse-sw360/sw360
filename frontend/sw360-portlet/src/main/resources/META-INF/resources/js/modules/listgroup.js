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
define('modules/listgroup', [
	'jquery',
], function($) {

	function selectTab(activeId, id) {
		if (id && id !== activeId) {
			// since we get this event as well when the user clicks a tab, we only want to select a tab if the
			// hash does not reflect the selected tab (which will be in case that the back/forward button was used)
			$(this).find('a[href="#' + id + '"]').tab('show');

			// blur all list items (otherwise the old tab may still be focused and highlighted)
			$(this).find('a[data-toggle="list"]').blur();

			// update belonging sections. The event below is not enough since the active tab may already be visible on reload
			// (e.g. its set from server side) and then the event is not fired resulting in hidden companion sections.
			$('.list-group-companion').hide();
			$('.list-group-companion[data-belong-to="' + id + '"]').show();
		}
	}

	function initialize(selector, initialTab) {
		var activeId,
			$listgroup = $('#' + selector);

		if($listgroup.length === 0) {
			console.error("Cannot find listgroup: " + selector);
			return;
		}

		$listgroup.find('a[data-toggle="list"]').on('shown.bs.tab', function (event) {
			var $activeListItem = $(event.target);

			// update active tab
			activeId = $activeListItem.attr('href').substring(1);

			// update hash (/ prevents jumping)
			var end = window.location.href.indexOf('#');
			window.location.replace((end > 0 ? window.location.href.substring(0, end) : window.location.href) + '#/' + activeId);

			// update belonging sections
			$('.list-group-companion').hide();
			$('.list-group-companion[data-belong-to="' + activeId + '"]').show();
		});

		window.addEventListener('hashchange', function() {
			if(location.hash && location.hash.startsWith('#/')) {
				selectTab.call($listgroup, activeId, location.hash.substring(2));
			}
		}, true);

		if(location.hash && location.hash.startsWith('#/') && $('#' + location.hash.substring(2)).length > 0) {
			selectTab.call($listgroup, activeId, location.hash.substring(2));
		} else if(initialTab && $('#' + initialTab).length > 0) {
			selectTab.call($listgroup, activeId, initialTab);
		} else {
			selectTab.call($listgroup, activeId, $listgroup.find('a:first').attr('href').substring(1));
		}

		$listgroup.parents('.container:first').show().siblings('.container-spinner').hide();
	}

	return {
		initialize: initialize
	}
});
