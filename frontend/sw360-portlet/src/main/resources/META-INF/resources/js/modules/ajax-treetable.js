/*
 * Copyright Siemens AG, 2017-2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
define('modules/ajax-treetable', [ 'jquery', 'utils/cssloader', /* jquery-plugins: */ 'jquery-treetable' ], function($, cssloader) {

    cssloader.load([
		'/webjars/jquery-treetable/css/jquery.treetable.css'
	]);

    return {
        setup: function(tableId, ajaxUrl, dataCallback, renderCallback) {
            var table = $("#" + tableId);
            table.treetable({
                expandable: true,
                onNodeExpand: function () {
                    var node = this,
                        data = dataCallback(table, node);

                    if (node.children.length === 0 && node.row.data("children-loaded") !== true) {
                        jQuery.ajax({
                            type: 'POST',
                            url: ajaxUrl,
                            cache: false,
                            data: data
                        }).done(function (result) {
                            node.row.data("children-loaded", true);
                            renderCallback(table, node, result);
                        });
                    }
                }
            });
            return table;
        },
        showPageContainer: function($treetable) {
			$treetable = $treetable ? $treetable : $(this.table);

			if(!($treetable instanceof $)) {
				console.error('No jquery object found neither as parameter nor as \'this.table\'. Skipping.');
				return;
			}
			$treetable.parents('.container:first').show().siblings('.container-spinner').hide();
		}
    };
});
