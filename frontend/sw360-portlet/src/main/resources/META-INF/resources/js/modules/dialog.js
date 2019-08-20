/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('modules/dialog', [
	'jquery',
	'modules/button',
	'bridges/bootstrap',
], function($, button) {
	function closeDialog() {
		var $dialog = $(this);

		$dialog.one('hidden.bs.modal', function() {
			$(this).modal('dispose');
		});
		$dialog.modal('hide');
	}

	function addMessage(type, message, noRetry) {
		var $dialog = $(this),
		$alert = $('<div>', {
			'class': 'alert alert-dialog fade show',
			role: 'alert'
		});
		$alert.addClass('alert-' + type);
		$alert.append(message);

		$dialog.find('.modal-body').prepend($alert);

		if(noRetry) {
			$dialog.find('.modal-footer button').hide();
			$dialog.find('.modal-footer').append($('<button>', {
				'class': 'btn btn-primary',
				type: 'button',
				'data-dismiss': 'modal',
				'data-cause': 'done'
			}).text('Close'));
		}
	}

	function closeMessage() {
		var $dialog = this;
		$dialog.find('.modal-body .alert-dialog').remove();
	}

	function createHeader(title, type) {
		var $header = $('<div>', {
				'class': 'modal-header'
			}),
			$title = $('<h5>', {
				'class': 'modal-title'
			}).html(title),
			$closeButton = $('<button>', {
				'class': 'close',
				type: 'button',
				'data-dismiss': 'modal',
				'aria-label': 'Close'
			}).append($('<span>', {
				'aria-hidden': 'true',
			}).html('&times;'));

		$header.append($title);
		$header.append($closeButton);

		$(this).find('.modal-header').remove();
		$(this).find('.modal-content').prepend($header);
	}

	function enableButtons(state) {
		$(this).find('[data-dismiss="modal"]').prop('disabled', !state);
		$(this).find('.modal-footer button').prop('disabled', !state);
	}

	function enablePrimaryButtons(state) {
		$(this).find('.modal-footer button.btn-primary').prop('disabled', !state);
	}

	function setTitle(title) {
		$(this).find('.modal-title').html(title);
	}

	function open(selector, data, submitCallback, beforeShowFn, afterShowFn) {
		var $dialog = $(selector);

		if($dialog.length === 0) {
			console.error('Dialog not found: ' + selector);
			return;
		}

		data = data || {};
		submitCallback = submitCallback || function(callback) { callback(true); };

		// create/replace header
		if($dialog.attr('data-title')) {
			createHeader.call($dialog, $dialog.attr('data-title'));
		}

		// reset input fields
		$dialog.find('input[type="text"], input[type="email"], input[type="url"], input[type="number"], input[type="search"], input[type="tel"], select, textarea').val('');
		// remove optional alerts
		$dialog.find('.alert-dialog').remove();
		// remove optional 'Close' button
		$dialog.find('button[data-cause="done"]').remove();
		// display optionally hidden buttons
		$dialog.find('.modal-footer button').show();

		// add show listener to update dialog data
		$dialog.one('show.bs.modal', function (event) {
			var $dialog = $(this);

			$dialog.find('[data-name]').text('');
			Object.keys(data).forEach(function(key) {
				$dialog.find('[data-name="' + key + '"]').filter(':not(input)').text(data[key]);
				$dialog.find('input[data-name="' + key + '"]').val(data[key]);
			});

			$dialog.find('[data-hide]').show();
			$dialog.find('[data-hide]').each(function(index, element) {
				var $element = $(element);
				if(data[$element.data('hide')] === true) {
					$element.hide();
				}
			});
		});

		// finish and re-enable buttons
		$dialog.find('.modal-footer button:not([data-dismiss])').each(function(index, element) {
			button.finish(element);
		});
		enableButtons.call($dialog, true);

		// remove previous listeners
		$dialog.find('button:not([data-dismiss])').off('.sw360.dialog');
		// add listener to action button
		$dialog.find('.modal-footer button:not([data-dismiss])').on('click.sw360.dialog', function(event) {
			var $button = $(event.currentTarget);

			// remove optional alerts
			$dialog.find('.alert-dialog').remove();

			// update buttons
			button.wait($button);
			enableButtons.call($dialog, false);

			// collect data
			var submitData = {};
			$dialog.find('input[data-name], select[data-name], textarea[data-name]').each(function(index, element) {
				var $element = $(element);
				if($element.attr('type') === 'radio' || $element.attr('type') === 'checkbox') {
					if($element.is(':checked')) {
						submitData[$element.data().name] = $element.val();
					}
				} else {
					submitData[$element.data().name] = $element.val();
				}
			});

			submitCallback.call($dialog, $button.data('submit'), function(close) {
				if(close) {
					closeDialog.call($dialog);
				}

				button.finish($button);
				enableButtons.call($dialog, true);
			}, submitData);
		});

		// return an object with some helper methods
		var dialogObject = {
			$: $dialog,
			closeMessage: closeMessage.bind($dialog),
			close: closeDialog.bind($dialog),
			alert: addMessage.bind($dialog, 'danger'),
			warning: addMessage.bind($dialog, 'warning'),
			info: addMessage.bind($dialog, 'info'),
			success: addMessage.bind($dialog, 'success'),
			enablePrimaryButtons: enablePrimaryButtons.bind($dialog),
			enableButtons: enableButtons.bind($dialog),
			setTitle: setTitle.bind($dialog)
		};

		if(beforeShowFn) {
			$dialog.one('show.bs.modal', function(event) {
				beforeShowFn.call(dialogObject, event);
			});
		}

		$dialog.one('shown.bs.modal', function(event) {
			if(afterShowFn) {
				afterShowFn.call(dialogObject, event);
			}
			setTimeout(function() {
				$dialog.find('[autofocus]').focus();
			}, 50);
		});

		// open the dialog
		$dialog.modal({
			backdrop: 'static'
		});

		return dialogObject;
	}

	function confirm(type, icon, title, body, actionButtonText, data, submitCallback, beforeShowFn, afterShowFn) {
		var $dialogs = $('body .auto-dialogs:first'),
			$dialog = $(
				'<div id="confirmDialog" class="modal fade" tabindex="-1" role="dialog">' +
					'<div class="modal-dialog modal-lg modal-dialog-centered" role="document">' +
						'<div class="modal-content">' +
							'<div class="modal-header">' +
								'<h5 class="modal-title"></h5>' +
								'<button type="button" class="close" data-dismiss="modal" aria-label="Close">' +
									'<span aria-hidden="true">&times;</span>' +
								'</button>' +
							'</div>' +
							'<div class="modal-body">' +
							'</div>' +
							'<div class="modal-footer">' +
								'<button type="button" class="btn btn-light" data-dismiss="modal">Cancel</button>' +
								'<button type="button" class="btn"></button>' +
							'</div>' +
						'</div>' +
					'</div>' +
				'</div>'
			);

		if($dialogs.length == 0) {
			console.error('Could not find dialogs container. Skipping.');
			return;
		}

		if(type) {
			$dialog.find('.modal-dialog').addClass('modal-' + type);
		}
		if(icon) {
			$dialog.find('.modal-title').append(
				'<svg class="lexicon-icon">' +
			'<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#' + icon + '"/>' +
		'</svg>'
			);
		}
		$dialog.find('.modal-title').append(' ' + title);
		$dialog.find('.modal-body').append(body);
		if(actionButtonText) {
			if(type) {
				$dialog.find('.modal-footer button:last').addClass('btn-' + type).text(actionButtonText);
			} else {
				$dialog.find('.modal-footer button:last').addClass('btn-primary').text(actionButtonText);
			}
		} else {
			$dialog.find('.modal-footer button:last').remove();
		}

		$dialogs.find('#confirmDialog').remove();
		$dialogs.append($dialog);

		return open($dialog, data, submitCallback, beforeShowFn, afterShowFn);
	}

	function info(title, body, data, beforeShowFn, afterShowFn) {
		var $dialogs = $('body .auto-dialogs:first'),
			$dialog = $(
				'<div id="infoDialog" class="modal fade" tabindex="-1" role="dialog">' +
					'<div class="modal-dialog modal-lg modal-dialog-centered modal-info" role="document">' +
						'<div class="modal-content">' +
							'<div class="modal-header">' +
								'<h5 class="modal-title">' +
									'<svg class="lexicon-icon">' +
										'<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open"/>' +
									'</svg>' +
								'</h5>' +
								'<button type="button" class="close" data-dismiss="modal" aria-label="Close">' +
									'<span aria-hidden="true">&times;</span>' +
								'</button>' +
							'</div>' +
							'<div class="modal-body">' +
							'</div>' +
							'<div class="modal-footer">' +
								'<button type="button" class="btn btn-light" data-dismiss="modal">OK</button>' +
							'</div>' +
						'</div>' +
					'</div>' +
				'</div>'
			);

		if($dialogs.length == 0) {
			console.error('Could not find dialogs container. Skipping.');
			return;
		}

		$dialog.find('.modal-title').append(' ' + title);
		$dialog.find('.modal-body').append(body);
		$dialogs.find('#infoDialog').remove();
		$dialogs.append($dialog);

		return open($dialog, data, function() {}, beforeShowFn, afterShowFn);
	}

	function warn(body, data) {
		var $dialogs = $('body .auto-dialogs:first'),
			$dialog = $(
				'<div id="warningDialog" class="modal fade" tabindex="-1" role="dialog">' +
					'<div class="modal-dialog modal-lg modal-dialog-centered modal-warning" role="document">' +
						'<div class="modal-content">' +
							'<div class="modal-header">' +
								'<h5 class="modal-title">' +
									'<svg class="lexicon-icon">' +
										'<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#warning"/>' +
									'</svg>' +
									' Warning' +
								'</h5>' +
								'<button type="button" class="close" data-dismiss="modal" aria-label="Close">' +
									'<span aria-hidden="true">&times;</span>' +
								'</button>' +
							'</div>' +
							'<div class="modal-body">' +
							'</div>' +
							'<div class="modal-footer">' +
								'<button type="button" class="btn btn-light" data-dismiss="modal">OK</button>' +
							'</div>' +
						'</div>' +
					'</div>' +
				'</div>'
			);

		if($dialogs.length == 0) {
			console.error('Could not find dialogs container. Skipping.');
			return;
		}

		$dialog.find('.modal-body').append(body);
		$dialogs.find('#warningDialog').remove();
		$dialogs.append($dialog);

		return open($dialog, data);
	}

	return {
		open: open,
		confirm: confirm,
		warn, warn,
		info: info
	};
});
