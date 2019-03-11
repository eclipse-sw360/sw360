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
define('components/includes/releases/linkProject', ['jquery', 'datatables.net', 'modules/datatables-renderer', /* jquery-plugins */ 'jquery-ui', 'jquery-confirm', /* datatables pluigns */ 'datatables.net-select' ], function($) {
    var dataTable,
        releaseId,
        releaseName,
        homeUrl = $('#search-project-form').data().homeUrl.replace(/\/web\//, '/group/'),
        contextPath = $('#search-project-form').data().contextPath;

    $('#linkToProjectButton').on('click', function() {
        linkToProject($('#projectSearchResultstable input[name=project]:checked').val());
    });
    $('#searchbuttonproject').on('click', function() {
        searchProjects($('#searchproject').val());
    });
    $('#filterlinkedprojects').on('click', function() {
        if($('#filterlinkedprojects').is(':checked')) {
            $.fn.dataTable.ext.search.pop();
        } else {
            $.fn.dataTable.ext.search.push(
                   function(settings, data, index) {
                       return !dataTable.row(index).data().releaseIdToUsage[releaseId];
                   }
               );
        }
        dataTable.draw();
    });
    $('.action.done button').on('click', function(event) {
        closeOpenDialogs();
        event.preventDefault();
        event.stopPropagation();
    });
    Liferay.on('allPortletsReady', function() {
        bindkeyPressToClick('searchproject', 'searchbuttonproject');
    });

    function makeProjectsDataTable() {
        return $('#projectSearchResultstable').DataTable({
               rowId: 'id',
               dom: 'rti',
               scrollY: 220,
               info: false,
               paging: false,
            autoWidth: false,
            select: {
                style: 'single'
            },
            columns: [
                /* 0 */ { data: 'id',
                          render: function(data, type, row, meta) {
                                        if(row.releaseIdToUsage && row.releaseIdToUsage[releaseId]) {
                                            if(type === 'display') {
                                                return $('<img>', {
                                                   style: 'width: 16px;',
                                                   src: contextPath + '/images/ok.png'
                                                })[0].outerHTML;
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
            order: [ [1, 'asc'] ],
            deferRender: true,
            language: {
                emptyTable: "Please search for a project."
            },
            destroy: true
        });
    }

    function showProjectDialog() {
        var $close = $('#search-project-form .action.done'),
            $input = $('#search-project-form .header, #search-project-form .table, #search-project-form .action.link'),
            $result = $('#search-project-form .result');

        $input.show();
        $result.html('');
        $result.hide();
        $close.hide();
        $('#linkToProjectButton').prop('disabled', false);
        $('#linkToProjectButton').removeClass('spinner');

        openDialog('search-project-form', 'searchproject');

        dataTable = makeProjectsDataTable();
        dataTable.clear();
        dataTable.on('select', function(event, dataTable, type, index) {
            var $input;

            if(type === 'row') {
                $input = $(dataTable.row(index).node()).find('input[name=project]');
                if($input.length > 0) {
                    $input.prop('checked', true);
                } else {
                    dataTable.row(index).deselect();
                }
            }
        });
        dataTable.draw();
    }

    function searchProjects(searchTerm) {
        var data = {},
            config = $('#projectSearchResultstable').data(),
            table = $('#projectSearchResultstable').DataTable();

        $('#searchbuttonproject').prop('disabled', true);
        $('#loadingProjectsTableNotifier').show();

        data[config.whereKey] = searchTerm;
        jQuery.ajax({
            type: 'POST',
            url: config.searchUrl,
            data: data,
            success: function (data) {
                table.clear();
                table.rows.add(data);
                table.draw();
            },
            complete: function() {
                $('#searchbuttonproject').prop('disabled', false);
                $('#loadingProjectsTableNotifier').hide();
            }
        });
    }

    function linkToProject(projectId) {
        var data = {},
            config = $('#projectSearchResultstable').data();

        if(!projectId) {
            return;
        }

        $('#linkToProjectButton').prop('disabled', true);
        $('#linkToProjectButton').addClass('spinner');


        data[config.projectIdKey] = projectId;
        data[config.releaseIdKey] = releaseId;
        jQuery.ajax({
            type: 'POST',
            url: config.linkUrl,
            data: data,
            success: function (data) {
                var $close = $('#search-project-form .action.done'),
                    $input = $('#search-project-form .header, #search-project-form .table, #search-project-form .action.link'),
                    $result = $('#search-project-form .result'),
                    projectName = $('#projectSearchResultstable input[name=project]:checked').parents('tr').find('td .name').data().name;

                $result.html();

                var $p1 = $('<p/>');
                $p1.append('The release ');
                $('<b/>').text(releaseName).appendTo($p1);
                $p1.append(' has been successfully linked to project ');
                $('<b/>').text(projectName).appendTo($p1);
                $p1.appendTo($result);

                var $p2 = $('<p/>');
                $p2.append('Click ');
                $('<a/>', {
                  href: homeUrl + '/projects/-/project/edit/' + projectId + '#tab-linkedProjects',
                  style: 'text-decoration: underline;'
                }).text('here').appendTo($p2);
                $p2.append(' to edit the release relation as well as the project mainline state in the project.');
                $p2.appendTo($result);

                $input.hide();
                $result.fadeIn(500);
                $close.show();
            }
        });
    }

    return {
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
