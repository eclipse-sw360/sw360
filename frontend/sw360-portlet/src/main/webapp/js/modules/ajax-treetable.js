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
define('modules/ajax-treetable', [ 'jquery', /* jquery-plugins: */ 'jquery-treetable' ], function($) {
    return {
        setup: function(tableId, ajaxUrl, dataCallback, renderCallback) {
            var table = $("#" + tableId);
            table.treetable({
                expandable: true,
                onNodeExpand: function () {
                    var node = this,
                        data = dataCallback(table, node);

                    if (node.children.length === 0) {
                        jQuery.ajax({
                            type: 'POST',
                            url: ajaxUrl,
                            cache: false,
                            data: data
                        }).done(function (result) {
                            renderCallback(table, node, result);
                        });
                    }
                }
            });
        }
    };
});
