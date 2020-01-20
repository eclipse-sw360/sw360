/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
