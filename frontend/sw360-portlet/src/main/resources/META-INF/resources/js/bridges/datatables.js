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

/**
 * Module bridge for datatables. Sets necessary defaults and loads all dependencies as well as css files
 * needed for the default datatables in SW360.
 */
define('bridges/datatables', [
	'jquery',
	'utils/cssloader',
	/* jquery-plugins */
	'datatables.net',
	'datatables.net-bs4',
	'datatables.net-buttons',
	'datatables.net-buttons.print',
	'datatables.net-select',
	'datatables.net-select-bs4',
	'datatables-plugins-sorting.natural',
	/* own extensions */
	'modules/datatables-renderer',
	'modules/datatables-utils'
], function($, cssloader) {

	cssloader.load([
		'/webjars/datatables.net-bs4/1.10.19/css/dataTables.bootstrap4.min.css',
		'/webjars/datatables.net-select-bs4/css/select.bootstrap4.min.css'
	]);
	initialize();

	function initialize() {
		/* Set the defaults for DataTables initialisation */
		$.fn.DataTable.ext.pager.numbers_length = 8;

		$.extend( true, $.fn.dataTable.defaults, {
			// the following parameter must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
			iDisplayStart: 0,
			displayStart: 0,

			autoWidth: false,
			dom:
				"<'row'<'col-sm-12 col-md-6'l><'col-sm-12 col-md-6'B>>" +
				"<'row'<'col-sm-12'tr>>" +
				"<'row'<'col-sm-12 col-md-5'i><'col-sm-12 col-md-7'p>>",
			buttons: [],
			info: true,
			lengthMenu: [ [10, 25, 50, 100, -1], [10, 25, 50, 100, "All"] ],
			pageLength: 10,
			paging: true,
			pagingType: 'simple_numbers',
			processing: true,
			search: { smart: false },
			searching: false,
			deferRender: true
		});
	}

	return {
		create: function(selector, config, printColumns, noSortColumns) {
			if(typeof printColumns !== 'undefined') {
				if(!config.buttons) {
					config.buttons = [];
				}

				config.buttons.push({
					extend: 'print',
					text: '<svg class="lexicon-icon"><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#print" /></svg> Print',
					autoPrint: true,
					className: 'btn btn-sm btn-secondary btn-print',
					exportOptions: {
						columns: printColumns
					}
				});
			}

			if(typeof noSortColumns !== 'undefined') {
				if(!config.columnDefs) {
					config.columnDefs = [];
				}

				config.columnDefs.push({
					targets: noSortColumns,
					orderable: false
				});
			}

			return $(selector).DataTable(config);
		},
		destroy: function(selector) {
			$(selector).DataTable({
				destroy: true
			}).destroy();
		},
		enableCheckboxForSelection: function($datatable, selectColumn) {

			// remove old listener
			$datatable.off('select.sw360.select-table');
			$datatable.off('deselect.sw360.select-table');

			// initialise selection
			$datatable.rows().deselect();
			$datatable.rows(function(idx, data, node) {
				return $(node).find(':checked').length > 0;
			}).select();

			$datatable.on('select.sw360.select-table', function(event, dataTable, type, indices) {
                var $input;

                if(type === 'row') {
					if(typeof indices.length === 'undefined') {
						indices = [ indices ];
					}

					indices.forEach(function(index) {
						$input = $(dataTable.cell(index, selectColumn).node()).find('input[type="checkbox"], input[type="radio"]');
						if($input.length > 0) {
							$input.prop('checked', true).trigger('change');
						} else {
							dataTable.row(index).deselect();
						}
					});
                }
			});

            $datatable.on('deselect.sw360.select-table', function(event, dataTable, type, indices) {
                var $input;

                if(type === 'row') {
                    if(typeof indices.length === 'undefined') {
						indices = [ indices ];
					}

					indices.forEach(function(index) {
						$input = $(dataTable.cell(index, selectColumn).node()).find('input[type="checkbox"], input[type="radio"]');
						if($input.length > 0) {
							$input.prop('checked', false).trigger('change');
						}
					});
                }
			});
		},
		showPageContainer: function($datatable) {
			$datatable = $datatable instanceof $ ? $datatable : this;

			if(!($datatable instanceof $)) {
				console.error('No jquery object found neither as parameter nor as \'this\'. Skipping.');
				return;
			}
			$datatable.parents('.container:first').show().siblings('.container-spinner').hide();
		}
	};
});
