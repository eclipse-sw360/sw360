/*
 * Copyright Siemens AG, 2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
define('components/includes/releases/linkProject', ['jquery', 'bridges/datatables', 'modules/dialog', 'modules/button', 'utils/keyboard', 'utils/link' ], function($, datatables, dialog, button, keyboard, link) {
    var $dialog,
        datatable,
        releaseId,
        releaseName;

    function initialize() {
        $('#searchbuttonproject').on('click', function() {
            searchProjects($('#searchproject').val());
        });

        $('#filterlinkedprojects').on('click', function() {
            if($('#filterlinkedprojects').is(':checked')) {
                $.fn.dataTable.ext.search.pop();
            } else {
                $.fn.dataTable.ext.search.push(
                    function(settings, data, index) {
                        return !datatable.row(index).data().releaseIdToUsage[releaseId];
                    }
                );
            }
            datatable.draw();
        });

        keyboard.bindkeyPressToClick('searchproject', 'searchbuttonproject');
    }

    function makeProjectsDataTable() {
        var table = datatables.create('#projectSearchResultstable', {
            destroy: true,
            paging: false,
            info: false,
            searching: true,
            order: [ [1, 'asc'] ],
            rowId: 'id',
            columns: [
                /* 0 */ { data: 'id',
                          render: function(data, type, row, meta) {
                                        if(row.releaseIdToUsage && row.releaseIdToUsage[releaseId]) {
                                            if(type === 'display') {
                                                return $('<svg class="lexicon-icon text-success"><title>Already linked</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#check"/></svg>')[0].outerHTML;
                                            } else {
                                                return '';
                                            };
                                        } else {
                                              return $.fn.dataTable.render.inputRadio('project', '')(data, type, row, meta);
                                        }
                                  }, orderable: false
                        },
                /* 1 */ { data: 'name', render: $.fn.dataTable.render.data('name', 'name') },
                /* 2 */ { data: 'version', defaultContent: "" },
                /* 3 */ { data: function(row) {
                                    return [ row.state, row.clearingState];
                                }, render: $.fn.dataTable.render.stateBoxes, defaultContent: ""
                        },
                /* 4 */ { data: 'projectResponsible', defaultContent: "" },
                /* 5 */ { data: 'description', defaultContent: "", render: $.fn.dataTable.render.ellipsis }
            ],
            initComplete: function() {
                $('#searchbuttonproject').prop('disabled', false);
            },
            select: 'single',
            language: {
                emptyTable: "Please search for a project."
            }
        });

        datatables.enableCheckboxForSelection(table, 0);
        return table;
    }

    function showProjectDialog() {
        $dialog = dialog.open('#linkProjectDialog', {}, function(submit, callback) {
            linkToProject($('#projectSearchResultstable input[name=project]:checked').val(), callback);
        }, function() {
            var dialog = this;

            if(datatable) {
                datatable.clear();
            }
            datatable = makeProjectsDataTable();

            $('#projectSearchResultstable').off('change.project-search');
            $('#projectSearchResultstable').on('change.project-search', 'input[name="project"]', function() {
                dialog.enablePrimaryButtons(dialog.$.find('input[name="project"]:checked').length > 0);
            });

            dialog.enablePrimaryButtons(false);
        });
    }

    function searchProjects(searchTerm) {
        var data = {},
            config = $('#projectSearchResultstable').data();

        button.wait('#searchbuttonproject');

        data[config.whereKey] = searchTerm;
        jQuery.ajax({
            type: 'POST',
            url: config.searchUrl,
            data: data,
            success: function (data) {
                datatable.clear();
                datatable.rows.add(data);
                datatable.draw();
            },
            complete: function() {
                button.finish('#searchbuttonproject');
            }
        });
    }

    function linkToProject(projectId, callback) {
        var data = {},
            config = $('#projectSearchResultstable').data();

        if(!projectId) {
            return;
        }

        data[config.projectIdKey] = projectId;
        data[config.releaseIdKey] = releaseId;
        jQuery.ajax({
            type: 'POST',
            url: config.linkUrl,
            data: data,
            success: function (data) {
                var $result = $('<div></div>'),
                    projectName = $('#projectSearchResultstable input[name=project]:checked').parents('tr').find('td .name').data().name;

                callback();

                var $p1 = $('<p/>');
                $p1.append('The release ');
                $('<b/>').text(releaseName).appendTo($p1);
                $p1.append(' has been successfully linked to project ');
                $('<b/>').text(projectName).appendTo($p1);
                $p1.appendTo($result);

                var $p2 = $('<p/>');
                $p2.append('Click ');
                $('<a/>', {
                  href: link.to('project', 'edit', projectId) + '#/tab-linkedProjects',
                  style: 'text-decoration: underline;'
                }).on('click', function(event) {
                    $dialog.close();
                    window.location.href = $(event.currentTarget).attr('href');
                }).text('here').appendTo($p2);
                $p2.append(' to edit the release relation as well as the project mainline state in the project.');
                $p2.appendTo($result);
                $p2.addClass('mb-0');

                $dialog.success($result, true);
            },
            error: function() {
                callback();
                $dialog.alert('The release could not be linked to the project.');
            }
        });
    }

    return {
        /**
         * Initializes the module. Must be called before the module is used.
         */
        initialize: initialize,

        /**
         * Opens up the dialog for searching and linking the given release to a project.
         *
         * @param id id of the release to link
         * @param name name of the release to link
         */
        openLinkDialog: function(id, name) {
            releaseId = id;
            releaseName = name;
            showProjectDialog();
        }
    };
});
