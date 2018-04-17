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
define('modules/tabview', [ 'jquery' ], function($) {

    function createTabView(id, options) {
        var tabView;

        YUI().use('aui-tabview', function(Y) {
            options = options || {
                stacked: true,
                type: 'tab'
            };
            options.srcNode = '#' + id;

            tabView = new Y.TabView(options).render();
        });
    }

    return {
        create: createTabView
    };
});