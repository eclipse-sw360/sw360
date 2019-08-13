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
	return {
		enableForm: function(formSelector) {
			var form,
				$form = $(formSelector);

			if($form.length == 0) {
				console.error('Form not found: ' + formSelector);
				return;
			}
			form = $form[0];

			form.addEventListener('submit', function(event) {
				if(form.checkValidity() === false) {
		          event.preventDefault();
		          event.stopPropagation();
		        }
		        form.classList.add('was-validated');
			}, false);
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
