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
			'class': 'alert fade show',
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

	return {
		open: function(selector, data, submitCallback) {
			var $dialog = $(selector);

			if($dialog.length === 0) {
				console.error('Dialog not found: ' + selector);
				return;
			}

			// create/replace header
			if($dialog.attr('title')) {
				createHeader.call($dialog, $dialog.attr('title'));
			}

			// reset input fields
			$dialog.find('input, select, textarea').val('');
			// remove optional alerts
			$dialog.find('.alert').remove();
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
				$dialog.find('.alert').remove();

				// update buttons
				button.wait($button);
				enableButtons.call($dialog, false);

				// collect data
				var submitData = {};
				$dialog.find('input[data-name], select[data-name], textarea[data-name]').each(function(index, element) {
					var $element = $(element);
					submitData[$element.data().name] = $element.val();
				});

				submitCallback.call($dialog, $button.data('submit'), function(close) {
					if(close) {
						closeDialog.call($dialog);
					}

					button.finish($button);
					enableButtons.call($dialog, true);
				}, submitData);
			});

			// open the dialog
			$dialog.modal();

			// return an object with some helper methods
			return {
				$: $dialog,
				close: closeDialog.bind($dialog),
				alert: addMessage.bind($dialog, 'danger'),
				warning: addMessage.bind($dialog, 'warning'),
				info: addMessage.bind($dialog, 'info'),
				success: addMessage.bind($dialog, 'success'),
				enableButtons: enableButtons.bind($dialog)
			}
		}
	};
});
