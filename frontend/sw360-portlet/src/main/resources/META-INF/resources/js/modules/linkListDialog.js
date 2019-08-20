/*
 * Copyright Siemens AG, 2017, 2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('modules/linkListDialog', ['jquery', 'modules/dialog' ], function($, dialog) {

	/**
	 * Method registers a blur event handler on elements matching given elementSelector. On blur a
	 * confirmation dialog is opened, sending a post request to the given postUrl and adding a body with
	 * param key postParamKey and the value of the blurred field as param value.
	 * It expects a server side method listening on the url which return json in the format:
	 * {
	 *     "title": "Hello World!",
	 *     "errors": [
	 *              "Something happened"
	 *          ],
	 *     "links": [
	 *             {
	 *                 "target": "https://www.example.com/my-link1",
	 *                 "text": "My Link 1"
	 *             }
	 *         ]
	 * }
	 * The title will be set as title of the opened dialog while errors - if set - or links
	 * otherwise are displayed as content of the dialog.
	 *
	 * @param {string} elementSelector a jquery selector matching optimally exactly one element on whose blur
	 *                     the dialog should be opened and whose val() should be attached to the server call
	 * @param {string} postUrl a string representing the url that should be posted to, to get a link list to
	 *                     display
	 * @param {string} postParamKey a string that is used as post param key in the post body with the value
	 *                     of the matched field as param value
	 */
	function registerLinkListDialog(elementSelector, postUrl, postParamKey) {
        $(elementSelector).blur(function() {
            var $dialog;

            $dialog = dialog.info(
                'Please wait...',
                '<div class="spinner text-center">' +
                    '<div class="spinner-border" role="status">' +
                        '<span class="sr-only">Loading list...</span>' +
                    '</div>' +
                '</div>' +
                '<p class="description">' +
                '<div class="result-list">' +
                '</div>',
                {},
                function() {
                    var $list,
                        $dialog = this,
                        postData = {};

                    postData[postParamKey] = $(elementSelector).val();
                    $.ajax({
                        type: 'POST',
                        url: postUrl,
                        dataType: 'json',
                        data: postData
                    }).done(function(data, textStatus, jqXHR) {
                        $dialog.$.find('.spinner').hide();
                        $dialog.setTitle(data.title);
                        if (data.errors.length > 0) {
                            $list = $('<ul></ul>');
                            data.errors.forEach(function(error) {
                                $list.append('<li>' + error + '</li>');
                            });
                            $dialog.alert("Sorry, an error occured while looking for links:<br/>" + $list[0].outerHTML);
                        } else if (data.links.length === 0) {
                            $dialog.success('No reasonable links found!');
                        } else {
                            $dialog.$.find('.description').html(data.description);
                            $list = $('<ul></ul>').appendTo($dialog.$.find('.result-list'));
                            data.links.forEach(function(link) {
                                $list.append('<li><a target="_blank" href="' + link.target + '">' + link.text + '</a></li>');
                            });
                        }
                    }).fail(function(jqXHR, textStatus, errorThrown) {
                        $dialog.alert('Sorry, communication to server failed! Please check your internet connection and try again:<br/>' + textStatus + ' - ' + errorThrown);
                    });
                }
            );
        });
    }

    return {
        'registerLinkListDialog': registerLinkListDialog
    };
});
