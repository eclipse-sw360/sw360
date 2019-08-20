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
define('utils/render', [
	'jquery',
	'utils/escape'
], function($, escape) {
	function renderTrashIcon() {
		var $trashIcon = $('<svg>', {
			'class': 'delete lexicon-icon'
		});
		$trashIcon.append($('<title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

		return $trashIcon;
	}

	function renderLinkTo(url, content, htmlContent) {
	    var $link = $("<a href='" + encodeURI(url) + "'/>");
	    if (typeof htmlContent == 'string' && htmlContent) {
	        $link.html(htmlContent);
	    } else if (typeof content == 'string' && content) {
	        $link.text(escape.decodeEntities(content));
	    } else {
	        $link.text(url);
	    }

	    return $link[0].outerHTML;
	}

	function renderUserEmail(user) {
	    if (typeof user == 'string') {
	        return renderLinkTo("mailto:" + user, user);
	    }

	    if (typeof user == 'object' && user.hasOwnProperty("email") && user.hasOwnProperty("givenname") && user.hasOwnProperty("lastname")) {
	        return renderLinkTo("mailto:" + user.email, user.givenname + " " + user.lastname);
	    } else {
	        return "N.A.";
	    }
	}

	function truncate(text, limit) {
		return (text.length > limit) ? text.substr(0, limit - 1) + '&hellip;' : text;
	}

	return {
		linkTo: renderLinkTo,
		userEmail: renderUserEmail,
		truncate: truncate,
		trashIcon: renderTrashIcon
	};
});
