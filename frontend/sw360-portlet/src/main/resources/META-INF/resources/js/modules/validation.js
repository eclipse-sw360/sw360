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
define('modules/validation', [ 'jquery' ], function($) {

	function jumpToFailedInput($form) {
		// wait a short moment for the tab to change
		setTimeout(function() {
			$form.find(':invalid').first().focus();
		}, 50);
	}

	function jumpToFailedTab($form) {
		var $input = $form.find(':invalid').first(),
			$tab = $input.parents('.tab-pane:first');

		window.location.hash = '/' + $tab.attr('id');
		jumpToFailedInput($form);
	}

	return {
		enableForm: function(formSelector) {
			var $form = $(formSelector);

			if($form.length == 0) {
				console.error('Form not found: ' + formSelector);
				return;
			}

			$form.on('submit', function(event) {
				if($form[0].checkValidity() === false) {
				    jumpToFailedInput($form);
		            event.preventDefault();
		            event.stopPropagation();
		        }
		        $form.addClass('was-validated');
			});
		},

		jumpToFailedTab: function(formSelector) {
			var $form = $(formSelector);

			if($form.length == 0) {
				console.error('Form not found: ' + formSelector);
				return;
			}

			$form.attr('data-jump-to-failed-tab', 'true');
			$form.on('submit', function(event) {
				if($form[0].checkValidity() === false) {
					jumpToFailedTab($form);
				}
			});
		},

		validate: function(formSelector) {
			var result,
				$form = $(formSelector);

			if($form.length == 0) {
				console.error('Form not found: ' + formSelector);
				return;
			}

			result = $form[0].checkValidity()
			$form.addClass('was-validated');

			if(result === false) {
				if($form.attr('data-jump-to-failed-tab') === 'true') {
					jumpToFailedTab($form);
				} else {
					jumpToFailedInput($form);
				}
			}

			return result;
		},

		confirmField: function(fieldSelector, confirmSelector, doesnotMatchText) {
			var $field = $(fieldSelector),
				$confirm = $(confirmSelector),
				$feedback = $confirm.siblings('.invalid-feedback'),
				confirmDefaultText = $feedback.text();

			if($field.length == 0) {
				console.error('Field not found: ' + fieldSelector);
				return;
			}
			if($confirm.length == 0) {
				console.error('Confirm field not found: ' + confirmSelector);
				return;
			}
			if($feedback.length == 0) {
				console.warn('Confirm field has no invalid feedback.');
			}

			doesnotMatchText = doesnotMatchText || 'Passwords do not match!';

			function matchFields() {
				if($confirm.val() && $field.val() !== $confirm.val()) {
					$confirm[0].setCustomValidity(doesnotMatchText);
					$feedback.text(doesnotMatchText);

			    } else {
				$feedback.text(confirmDefaultText);
				$confirm[0].setCustomValidity('');
			    }
			}

			$field.on('change', matchFields);
			$confirm.on('keyup', matchFields);
		}
	};
});
