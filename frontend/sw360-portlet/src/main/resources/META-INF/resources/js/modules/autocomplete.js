/*
 * Copyright Siemens AG, 2017-2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('modules/autocomplete', ['jquery', 'bridges/jquery-ui'], function($) {

	function split(val) {
		return val.split(/,\s*/);
	}

	function extractLast(term) {
		return split(term).pop();
	}

	function unique(array) {
		var o = {}, i, l = array.length, r = [];
		for(i=0; i<l;i+=1) o[array[i]] = array[i];
		for(i in o) r.push(o[i]);
		return r;
	};

	function prepareAutocompleteForMultipleHits(fieldID ,tags) {
		$("#" + fieldID).bind( "keydown", function(event) {
			//don't navigate away from the field on tab when selecting an item
			if (event.keyCode === $.ui.keyCode.TAB
				&& $(this).data("ui-autocomplete").menu.active) {
				event.preventDefault();
			}
		}).autocomplete({
			minLength : 0,
			source : function(request, response) {
				// delegate back to autocomplete, but extract the last term
				response($.ui.autocomplete.filter(tags, extractLast(request.term)));
			},
			focus : function() {
				// prevent value inserted on focus
				return false;
			},
			select : function(event, ui) {
				var terms = split(this.value);
				// remove the current input
				terms.pop();
				// add the selected item
				terms.push(ui.item.value);

				//sort and unique
				terms.sort();
				terms = unique(terms);

				// add placeholder to get the comma-and-space at the end
				terms.push("");
				this.value = terms.join(", ");
				return false;
			}
		});
	}

	return {
		prepareForMultipleHits: prepareAutocompleteForMultipleHits
	};
});
