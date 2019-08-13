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
define('utils/object', function() {
	return {
		namespacerOf: function(prefix) {
            return function(data) {
                var result = {};
                for (var i in data) {
                    if (data.hasOwnProperty(i)) {
                        result[prefix + i] = data[i];
                    }
                }
                return result;
            }
		}
	};
});
