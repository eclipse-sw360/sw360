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
define('modules/datatables-utils', ['jquery', /* jquery-plugins */ 'datatables.net'], function($) {
    $.fn.dataTable.ext.order['sort-select'] = function(settings, col) {
        return this.api().column(col, {order:'index'}).nodes().map(function(td) {
            return $('select', td).val();
        });
    };

    $.extend($.fn.dataTableExt.oSort, {
	"version-asc": function (a, b) {
		return versionCmp(a, b);
	},
	"version-desc": function (a, b) {
		return versionCmp(b, a);
	}
	});

	function numberCmp(a, b) {
		var aN = Number(a);
		var bN = Number(b);

		return (aN > bN) ? 1 : ((aN < bN) ? -1 : 0);
	}

	function versionCmp(a, b) {
	    var nameCmp = a.name.localeCompare(b.name);
	    if (nameCmp != 0) {
	        return nameCmp;
	    }

	    var aVer = a.version.split('.');
	    var bVer = b.version.split('.');

	    var i;
	    for (i = 0; i < aVer.length; i++) {
	        if (i >= bVer.length)
	            return 1;

	        var aPart = aVer[i];
	        var bPart = bVer[i];

	        var numCmp = numberCmp(aPart, bPart);

	        if (numCmp != 0) {
	            return numCmp;
	        }
	        if (aPart !== bPart) {
	            return aPart.localeCompare(bPart);
	        }
	    }

	    return (i < bVer.length) ? -1 : 0;
	}
});
