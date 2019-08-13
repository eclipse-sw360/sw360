/*
 * Copyright Siemens AG, 2017, 2019.
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

    var tabView;

    /**
     * This tab view component is managing its state via the url hash. This way it generates bookmarkable urls, supports
     * back and forward browser actions and also has a mechanism through which the server can set an initial tab (the
     * data attribute 'data-initial-tab' has to be set on the root dom node of this tabview which is identified by the
     * id parameter of this method).
     */
    function createTabView(id, options) {
        YUI().use('aui-tabview', function(Y) {
            tabView = renderTabView(Y, id, options);

            window.addEventListener('hashchange', onHashChanges, true);

            tabView.after('selectionChange', onTabChanges);

            setInitialTab(id);
        });
    }

    function renderTabView(Y, id, options) {
        options = options || {
            stacked: true,
            type: 'tab'
        };
        options.srcNode = '#' + id;

        return new Y.TabView(options).render();
    }

    function onHashChanges() {
        var hash = location.hash,
            idx;
        if (tabView.get('selection').get('srcNode').getDOMNode().getAttribute('href') !== hash) {
            // since we get this event as well when the user clicks a tab, we only want to select a tab if the
            // hash does not reflect the selected tab (which will be in case that the back/forward button was used)
            idx = $('> div > div', $(tabView.get('srcNode').getDOMNode())).index( $(hash) );
            tabView.selectChild(idx >= 0 ? idx : 0);
        }
        // in some browsers the hash change might invoke a scroll to the anchor in every case - which we want to revoke
        window.scrollTo(0, 0);
    }

    function onTabChanges(e) {
        var tabId = e.newVal.get('srcNode').getDOMNode().getAttribute('href');
        location.hash = tabId;
    }

    function setInitialTab(id) {
        // set default tab after initialization
        // url hash currently overrules server setting on default tab but this might be changed
        var initialIdx = 0,
            hash = location.hash;
        if (hash) {
            initialIdx = $('#' + id + ' > div > div').index( $(hash) );
        } else if ($('#' + id).data('initialTab')) {
            initialIdx = $('#' + id + ' > div > div').index( $('#' + $('#' + id).data('initialTab')) );
        }

        initialIdx = initialIdx >= 0 ? initialIdx : 0;
        tabView.selectChild(initialIdx);

        if (initialIdx === 0) {
            // normally an initialIdx of 0 means no one set a special tab. Which means that we do not have a hash.
            // just to be consistent we set the hash of the first tab in this case.
            location.hash = tabView.get('selection').get('srcNode').getDOMNode().getAttribute('href');
        }

        // lets scroll to top if someone used a direct link with an anchor (the browser might have scrolled to the anchor)
        window.scrollTo(0, 0);
    }

    return {
        create: createTabView
    };
});