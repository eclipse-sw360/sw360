/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('modules/linkListDialog', ['jquery', /* jquery-plugins: */ 'jquery-confirm' ], function($) {

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
	 * The title will be set as title of the opened jquery-confirm dialog while errors - if set - or links
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
            $.confirm({
                title: 'Loading...',
                content: function () {
                    var self = this,
                        postData = {};

                    postData[postParamKey] = $(elementSelector).val();

                    return $.ajax({
                            type: 'POST',
                            url: postUrl,
                            dataType: 'json',
                            data: postData
                        }).done(function(data, textStatus, jqXHR) {
                            self.setTitle(data.title, true);
                            if (data.errors.length > 0) {
                                self.setContentAppend('<div class="alert alert-error">Sorry, an error occured while looking for links:');
                                data.errors.forEach(function(error) {
                                    self.setContentAppend('<br />' + error);
                                });
                                self.setContentAppend('</div>');
                            } else if (data.links.length === 0) {
                                self.setContentAppend('<div class="alert alert-success">No reasonable links found!</div>');
                                self.setContentAppend('<div class="link-list-dialog-links">');
                                self.setContentAppend('    <ul>');
                                self.setContentAppend('    </ul>');
                                self.setContentAppend('</div>');
                            } else {
                                self.setContentAppend('<div class="link-list-dialog-links">');
                                self.setContentAppend('    <ul>');
                                data.links.forEach(function(link) {
                                    self.setContentAppend('        <li><a target="_blank" href="' + link.target + '">' + link.text + '</a></li>');
                                });
                                self.setContentAppend('    </ul>');
                                self.setContentAppend('</div>');
                            }
                        }).fail(function(jqXHR, textStatus, errorThrown) {
                            self.setContentAppend('<div class="alert alert-error">');
                            self.setContentAppend('    Sorry, communication to server failed! Please check your internet connection and try again:<br /><br />');
                            self.setContentAppend('   ' + textStatus + ' - ' + errorThrown);
                            self.setContentAppend('</div>');
                        })
                    ;
                },
                buttons: {
                    close: {
                        action: function () {}
                    }
                }
            });
        });
    }

    return {
        'registerLinkListDialog': registerLinkListDialog
    };
});