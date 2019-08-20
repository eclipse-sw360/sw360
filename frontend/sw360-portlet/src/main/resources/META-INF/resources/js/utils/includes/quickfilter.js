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
define('utils/includes/quickfilter', ['jquery'], function(jquery) {
    function searchCallbackApiLegacy(searchTerm) {
        this.fnFilter(searchTerm);
    }

    function searchCallbackApi(searchTerm) {
        this.search(searchTerm).draw();
    }

    return {
        /**
         *
         */
        addTable: function(datatable, customSearchCallback) {
            var lastSearch,
                inputTimeout,
                updateSearchFunction,
                $form = jquery('#component-quickfilter form'),
                $inputField = jquery('#component-quickfilter input[type=text]');

               // We use the external search function if given. Otherwise use an API implementation depending on the datatables version
               // Bind to datatable so the table object is accessible with 'this' inside the search callback
            if(typeof customSearchCallback === 'function') {
                updateSearchFunction = customSearchCallback.bind(datatable);
            } else if(typeof datatable.fnFilter === 'function') {
                updateSearchFunction = searchCallbackApiLegacy.bind(datatable);
            } else if(typeof datatable.search === 'function') {
                updateSearchFunction = searchCallbackApi.bind(datatable);
            } else {
                console.error('The given table object is not compatible with datatables legacy or current API version nor is a custom search callback defined!');
            }

            // always perform a search on initializing, fixes e.g. #441
            updateSearchFunction($inputField.val());

            // prevent enter to reload page
            $form.on('submit', function() {
                return false;
            });

            // filter on input or click. Wait some time on input to increase performance
            $inputField.on('keyup change', function() {
                var newSearch = $inputField.val();

                if(lastSearch != newSearch) {
                    clearTimeout(inputTimeout);
                    inputTimeout = setTimeout(updateSearchFunction.bind(datatable, newSearch), 250);

                    lastSearch = newSearch;
                }
            });
        }
    };
});
