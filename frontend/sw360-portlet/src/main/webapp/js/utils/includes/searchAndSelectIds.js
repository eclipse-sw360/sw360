/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

define('utils/includes/searchAndSelectIds', ['jquery', /* jquery-plugins: */ 'datatables'], function($) {

    var selectedIds = [];

    var SearchAndSelectIds = function (options) {

        var opts = {
            $addButton: options.$addButton,
            $searchButton: options.$searchButton,
            $resetButton: options.$resetButton,
            $searchInput: options.$searchInput,
            $tableBody: options.$tableBody,
            $table: options.$table,
            $searchDiv: options.$searchDiv,
            emptyRow: options.emptyRow,
            ajaxSearch: options.ajaxSearch,
            prepareData: options.prepareData,
            extractIds: options.extractIds,
            renderInput: options.renderInput
        };

        var currentState = {
            multi: false,
            $resultInput: false,
            resultFullData: [],
            $resultInputDisplay: false
        };

        var pr = {
            resetSearchTable: function (data) {
                var tableData = opts.prepareData(data, currentState);
                pr.setDataToIdSearchTableAndRefresh(tableData);
            },

            enableSearchButton: function() {
                opts.$searchButton.prop('disabled', false);
            },

            disableSearchButton: function() {
                opts.$searchButton.prop('disabled', true);
            },

            cleanUp: function() {
                $('#truncationAlerter').hide();
                pr.enableSearchButton();
            },

            destroyIdSearchDataTable: function() {
                opts.$table.DataTable().destroy();

            },

            makeIdSearchDataTable: function() {
                opts.$table.DataTable(
                    {
                        "sPaginationType": "full_numbers",
                        "paging": false,
                        "scrollY": "220",
                        "info": false,
                        "bFilter": false,
                        "language": {"processing":     "Processing..."},
                        "processing": true,
                        "initComplete": pr.enableSearchButton
                    });
            },

            setDataToIdSearchTableAndRefresh: function(data) {
                if (data.indexOf("No") == -1 || data.indexOf("found") == -1) {
                    pr.destroyIdSearchDataTable();
                    opts.$tableBody.html(data);
                    pr.makeIdSearchDataTable();
                } else {
                    opts.$tableBody.html(data);
                    pr.enableSearchButton();
                }
            },

            setOutput: function () {
                var selected = opts.$tableBody.find(':checked').map(
                    function (index, obj) {
                        return obj.value;
                    }
                );

                pr.addUniquely(selected);

                var allIds = opts.extractIds(currentState);
                var ids        = allIds['ids'];
                var displayIds = allIds['displayIds'];

                currentState.$resultInput.val(ids.join(", "));

                currentState.$resultInputDisplay.val(displayIds.join(", "));
                currentState.$resultInputDisplay.trigger('change');
            },

            addUniquely: function(input) {
                currentState.resultFullData.splice(0, currentState.resultFullData.length);
                for (var i=0;i<input.length;i++) {
                    if (!(currentState.resultFullData.includes(input[i]))) {
                        currentState.resultFullData.push(input[i]);
                    }
                }
            },

            doSearch: function () {
                pr.disableSearchButton();
                opts.ajaxSearch(
                    opts.$searchInput.val().replace(","," "),
                    currentState.multi
                ).done(
                    pr.resetSearchTable
                );
            },

            doAdd: function () {
                pr.setOutput();
                closeOpenDialogs();
            },

            doReset: function () {
                opts.$searchInput.val("");
                currentState.resultFullData = [];
                pr.doSearch();
            }
        };

        bindkeyPressToClick(opts.$searchInput, opts.$searchButton);

        return {
            open: function (multi, resultInputId, firstRun) {
                closeOpenDialogs();
                pr.cleanUp();

                currentState.$resultInput = $('#' + resultInputId);
                currentState.$resultInputDisplay = $('#' + resultInputId + 'Display');
                currentState.multi = multi;

                if(!selectedIds.hasOwnProperty(resultInputId)) {
                    selectedIds[resultInputId] = [];
                    if (currentState.$resultInput.val().length > 0) {
                        var alreadyPresentIds = currentState.$resultInput.val().split(",");
                        for(var i=0; i<alreadyPresentIds.length; i++) {
                            selectedIds[resultInputId].push(opts.renderInput(alreadyPresentIds[i]));
                        }
                    }
                }
                currentState.resultFullData = selectedIds[resultInputId];

                opts.$addButton.off('click');
                opts.$addButton.on('click', pr.doAdd);
                opts.$searchButton.off('click');
                opts.$searchButton.on('click', pr.doSearch);
                opts.$resetButton.off('click');
                opts.$resetButton.on('click', pr.doReset);

                openDialog(opts.$searchDiv, opts.$searchInput);
                if(!firstRun) {
                    pr.destroyIdSearchDataTable();
                }

                var htmlTable = "";
                if (currentState.resultFullData.length == 0) {
                    htmlTable = opts.emptyRow;
                } else {
                    htmlTable = opts.prepareData("", currentState);
                }
                opts.$tableBody.html(htmlTable);
                pr.makeIdSearchDataTable();
            }
        }
    };

    return {
        openSearchDialog: function (multi, resultInputId, htmlElements, functions, firstRun) {
            var searchIds = SearchAndSelectIds({
                $addButton: htmlElements['addButton'],
                $searchButton: htmlElements['searchButton'],
                $resetButton: htmlElements['resetButton'],
                $searchInput: htmlElements['searchInput'],
                $tableBody: htmlElements['resultTableBody'],
                $table: htmlElements['resultTable'],
                $searchDiv: htmlElements['searchDiv'],
                emptyRow: htmlElements['emptyRow'],
                ajaxSearch: functions['ajaxSearch'],
                prepareData: functions['prepareData'],
                extractIds: functions['extractIds'],
                renderInput: functions['renderInput']
            });
            searchIds.open(multi, resultInputId, firstRun)
        }
    }
});
