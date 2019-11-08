/*
 * Copyright Siemens AG, 2017-2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * This module provides some useful renderers for jquery DataTable. Please the jsdoc on the
 * rendering functions for more information.
 */
define('modules/datatables-renderer', ['jquery', 'modules/dialog', /* jquery-plugins */ 'datatables.net' ], function($, dialog) {

    // helper functions
    function createEllipsisSpan(text) {
        return $('<span>', {
            title: text,
            "class": "text-truncate",
        }).text(text);
    }

    function createStateBoxes(projectState, clearingState) {
        var projectStateMap = {
            '': "",
             0: "Active",
             1: "Phase Out",
             2: "Unknown"
        };
        var projectStateCssMap = {
            '': "projectStateInactive",
             0: "projectStateActive",
             1: "projectStateInactive",
             2: "projectStateInactive"
        };
        var clearingStateMap = {
            '': "",
             0: "Open",
             1: "In Progress",
             2: "Closed"
        };
        var clearingStateCssMap = {
            '': "clearingStateUnknown",
             0: "clearingStateOpen",
             1: "clearingStateInProgress",
             2: "clearingStateClosed"
        };

        projectState = typeof projectState != 'undefined' ? projectState : '';
        var $projectStateBox = $('<div>', {
            title: projectStateMap[projectState] || '',
            "class": "stateBox capsuleLeft " + projectStateCssMap[projectState]
        }).text(" PS ");

        clearingState = typeof clearingState != 'undefined' ? clearingState : '';
        var $clearingStateBox = $('<div>', {
            title: clearingStateMap[clearingState] || '',
            "class": "stateBox capsuleRight " + clearingStateCssMap[clearingState]
        }).text(" CS ");

        return $projectStateBox[0].outerHTML +  $clearingStateBox[0].outerHTML;
    }

    function createSelectInput(selectDataKey, selectData, name, clazz, optionClazz, currentKey) {
        var $container = $('<div class="form-group"></div>'),
            $select = $('<select>', { name: name, "class": clazz, title: selectData[currentKey] });
        $select.addClass('form-control');

        Object.keys(selectData).forEach(function(key) {
            $select.append($('<option>', { "class": optionClazz, value: key }).text(selectData[key]));
        });
        // use the html attribute to select the value in order to be serializable
        $select.find('option[value="' + currentKey + '"]').attr('selected', true);

        $container.append($select);
        return $container;
    }

    function createTextInput(name, clazz, placeholder, value) {
        var $container = $('<div class="form-group"></div>'),
            $text = $('<input>', { type: 'text', name: name, "class": clazz, placeholder: placeholder, title: value, value: value });
        $text.addClass('form-control');

        $container.append($text);
        return $container;
    }

    function createRadioInput(name, clazz, value, selected) {
        var $container = $('<div class="form-check"></div>'),
            $radio = $('<input>', { type: 'radio', name: name, "class": clazz, title: value, value: value });
        $radio.addClass('form-check-input');

        if(selected) {
            $radio.attr('checked', 'checked');
        }

        $container.append($radio);
        return $container;
    }

    function createCheckboxInput(name, clazz, value, checked) {
        var $container = $('<div class="form-check"></div>'),
            $checkbox = $('<input>', { type: 'checkbox', name: name, "class": clazz, title: value, value: value });
        $checkbox.addClass('form-check-input');

        if(checked) {
            $checkbox.attr('checked', 'checked');
        }

        $container.append($checkbox);
        return $container;
    }

    function createInfoText(text, tooltip, icon) {
        if(text || tooltip) {
            var $text = $('<span>', {
                'class': 'info-text',
                title: tooltip,
            }).text($('<div/>').html(text).text());

            if(icon && (text || tooltip)) {
                $text.prepend('<svg class="lexicon-icon"><use href="' + icon + '" /></svg>&nbsp;');
            }

            return $text;
        } else {
            return $('<span/>');
        }
    }

    // renderer definitions

    /**
     * Renders the data in span and additionally adds a class and a data attribute with the given name. This way
     * the data becomes available via attribute as well.
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.data('name', 'name-column'), ...</code>
     *
     * @param name name for the data attribute
     * @param clazz additional class to add
     */
    $.fn.dataTable.render.data = function(name, clazz) {
        return function(data, type, row, meta) {
            if(type === 'display') {
                return $('<span>', {
                    "class": clazz
                }).attr('data-' + name, data).text(data)[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return data;
            }
        };
    };

    /**
     * Renders the text with an ellipsis if space is unsufficient. The whole value will always be in the title.
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.ellipsis, ...</code>
     */
    $.fn.dataTable.render.ellipsis = function(data, type) {
        if(type === 'display') {
            return createEllipsisSpan(data)[0].outerHTML;
        } else if(type === 'type') {
            return 'string';
        } else {
            return data;
        }
    };

    /**
     * Expects an object as data with the attributes text and tooltips. Renders the text with a tooltip as title attribute.
     * Can optionally show an info icon.
     *
     * @param {string} icon path to icon
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.infoText('/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#info-circle-open'), ...</code>
     */
    $.fn.dataTable.render.infoText = function(icon) {
        return function(data, type, row, meta) {
            if(type === 'display') {
                return createInfoText(data.text, data.tooltip, icon)[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return data.text;
            }
        };
    }

    /**
     * Renders an input select field with the given parameters. This function returns the appropriate render function on call.
     * The values for the select field must be available in <code>settings.json</code>.
     *
     * @param {String} selectDataKey name of the key in <code>settings.json</code> holding the values for the select field. The values must be an array
     *                                     of objects where an object of the form <code>{ VAL: TEXT }</code> will be rendered
     *                                     as <code>&lt;option value="VAL"&gt;TEXT&lt;code&gt;</code>
     * @param {String} name name for the select field
     * @param {String} clazz class or classes to be added to the select fields. Multiple classes must be space separated
     * @param {String} optionClazz class or classes to be added to each option. Multiple classes must be space separated
     * @param {Function} hook the hook is called on creation with this set to the input field and the default render parameters of datatables (value, type, row, meta)
     *
     * @return {Function} render function for DataTable
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.inputSelect("attachmentTypes", "type", "select-field", "select-option"), ...</code>
     */
    $.fn.dataTable.render.inputSelect = function(selectDataKey, name, clazz, optionClazz, hook) {
        return function(key, type, row, meta) {
            var select,
                isString = typeof selectDataKey === 'string',
                isObject = typeof selectDataKey === 'object';

            if(type === 'filter' || type === 'sort' || type === 'print') {
                if (isString) {
                    return meta.settings.json[selectDataKey][key];
                } else if (isObject) {
                    return selectDataKey[key];
                }
            } else if(type === 'display') {
                if (isString) {
                    select = createSelectInput(selectDataKey, meta.settings.json[selectDataKey], name, clazz, optionClazz, key);
                } else if (isObject) {
                    select = createSelectInput(selectDataKey, selectDataKey, name, clazz, optionClazz, key);
                }
                if(typeof hook === 'function') {
                    hook.call(select, key, type, row, meta);
                }
                return select[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return key;
            }
        }

    };
    /**
     * Automatically updates the title attribute of the select field if the value changes.
     *
     *  Must be called after a cell has been created.
     *
     *  Example datatable configuration:
     *  <code>
     *      columnDefs: [{
     *              targets: [1, 6],
     *              createdCell: function(td, cellData, rowData, row, col) {
     *                  $.fn.dataTable.render.inputSelect.updateTitle(td);
     *              }
     *        }
     *  </code>
     */
    $.fn.dataTable.render.inputSelect.updateTitle = function(td) {
        var $select = $(td).find('select');
        $select.on('change', function() {
            var $option = $select.find('option[value=' + $select.val() + ']');
            $select.attr('title', $option.text());
        });
    };

    /**
     * Renders an input text field with the given parameters. This function returns the appropriate render function on call.
     *
     * @param {String} name name for the input field
     * @param {String} clazz class or classes to be added to the select fields. Multiple classes must be space separated
     * @param {String} placeholder placeholder to be shown inside the input field
     * @param {Function} hook the hook is called on creation with this set to the input field and the default render parameters of datatables (value, type, row, meta)
     *
     * @return {Function} render function for DataTable
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.inputText("comment", "comment edit", "Please enter a comment"), ...</code>
     */
    $.fn.dataTable.render.inputText = function(name, clazz, placeholder, hook) {
        return function(value, type, row, meta) {
            var input;

            if(type === 'display') {
                input = createTextInput(name, clazz, placeholder, value);
                if(typeof hook === 'function') {
                    hook.call(input, value, type, row, meta);
                }
                return input[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return value;
            }
        }
    };

    /**
     * Renders an input radio field with the given parameters. This function returns the appropriate render function on call.
     *
     * @param {String} name name for the input field
     * @param {String} clazz class or classes to be added to the select fields. Multiple classes must be space separated
     * @param {Function} hook the hook is called on creation with this set to the input field and the default render parameters of datatables (value, type, row, meta)
     *
     * @return {Function} render function for DataTable
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.inputRadio("gender", "", "female", true), ...</code>
     */
    $.fn.dataTable.render.inputRadio = function(name, clazz, selected, hook) {
        return function(value, type, row, meta) {
            var input;

            if(type === 'display') {
                input = createRadioInput(name, clazz, value, selected);
                if(typeof hook === 'function') {
                    hook.call(input, value, type, row, meta);
                }
                return input[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return value;
            }
        }
    };

    /**
     * Renders a checkbox field with the given parameters. This function returns the appropriate render function on call.
     *
     * @param {String} name name for the input field
     * @param {String} clazz class or classes to be added to the select fields. Multiple classes must be space separated
     * @param {Function} hook the hook is called on creation with this set to the input field and the default render parameters of datatables (value, type, row, meta)
     *
     * @return {Function} render function for DataTable
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.inputCheckbox("gender", "", "female", true), ...</code>
     */
    $.fn.dataTable.render.inputCheckbox = function(name, clazz, checked, hook) {
        return function(value, type, row, meta) {
            var input;

            if(type === 'display') {
                input = createCheckboxInput(name, clazz, value, checked);
                if(typeof hook === 'function') {
                    hook.call(input.find('input'), value, type, row, meta);
                }
                return input[0].outerHTML;
            } else if(type === 'type') {
                return 'string';
            } else {
                return value;
            }
        }
    };

    /**
     * Automatically updates the title attribute of the input field if the value changes.
     *
     *  Must be called after a cell has been created.
     *
     *  Example datatable configuration:
     *  <code>
     *      columnDefs: [{
     *              targets: [1, 6],
     *              createdCell: function(td, cellData, rowData, row, col) {
     *                  $.fn.dataTable.render.inputText.updateTitle(td);
     *              }
     *        }
     *  </code>
     */
    $.fn.dataTable.render.inputText.updateTitle = function(td) {
        var $text = $(td).find('input');
        $text.on('change', function() {
            $text.attr('title', $text.val());
        });
    };

    /**
     * Automatically shows a bigger dialog for entering a value after the original input gained focus.
     *
     *  Must be called after a cell has been created.
     *
     *  Example datatable configuration:
     *  <code>
     *      columnDefs: [{
     *              targets: 2,
     *              createdCell: function(td, cellData, rowData, row, col) {
     *                  $.fn.dataTable.render.inputText.useInputDialog("Update create comment");
     *              }
     *        }
     *  </code>
     */
    $.fn.dataTable.render.inputText.useInputDialog = function(td, dialogTitle) {
        var $text = $(td).find('input');
        $text.on('focus', function() {
            $text.blur(); // unfocus otherwise the dialog will immediately open again after closing

            if($text.is('[readonly]') || $text.is(':disabled')) {
                return;
            }

            var $dialog = dialog.confirm(
                null,
                'pencil',
                dialogTitle,
                '<form>' +
                    '<div class="form-group">' +
                        '<input type="text" placeholder="' + dialogTitle + '" class="form-control" data-name="comment" />' +
                    '</div>' +
                '</form>',
                'Update',
                {
                    comment: $text.val()
                },
                function(sumbit, callback, data) {
                    $text.val(data.comment).trigger('change');
                    callback(true);
                }, function() {
                    var $dialog = this;

                    // supppress animation to be quicker
                    $dialog.$.removeClass('fade');

                    $dialog.$.find('form').on('submit', function (event) {
                        event.preventDefault();
                        $dialog.$.find('.modal-footer button:last').trigger('click');
                    });
                }, function() {
                    setTimeout(function() {
                        $dialog.$.find('input').focus();
                    }, 50);
                }
            );
        });
    };

    /**
     * Renders the given actions. Actions are an array of action objects. Each action object must provide the following properties:
     * <ol>
     *   <li>key:         Will be set as "data-key" attribute to be able to identify the row</li>
     *   <li>class:       class name or space space separated class names for the action element</li>
     *   <li>icon:        name of the icon to be shown, e.g. "Trash"
     *   <li>title:       Title to be shown on mouse over or if the icon could not be found</li>
     *   <li>approvalKey: Will be set as "data-approval-state" attribute. This allows to decide whether an attachment is deletable.</li>
     *   <li>usageCountsKey: Will be set as "data-usages-count" attribute. This allows to decide whether an attachment is deletable.</li>
     * </ol>
     *
     * @param {Array} actions the actions as defined before
     *
     * @return {Function} render function for DataTable
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.actions( [{ key: 'attachmentContentId', 'class': 'delete-attachment', icon: 'Trash', title: 'Delete Attachment', approvalKey: 'checkStatus', usageCountsKey: 'usageCounts' }] ), ...</code>
     */
    $.fn.dataTable.render.actions = function(actions) {
        return function(data, type, row, meta) {
            var actionsHtml = '';
            if(type === 'display') {
                actions.forEach(function(action) {
                    var $title = $('<title></title>'),
                        $action = $('<svg>', {
                            'class': action['class'] + ' lexicon-icon',
                            'data-key': row[action.key],
                            'data-approvalstate': row[action.approvalKey],
                            'data-usages-count': meta.settings.json[action.usageCountsKey][row.attachmentContentId]
                        });
                    $action.append('<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#' + action.icon + '"/>');

                    $title.text(action.title);
                    $action.prepend($title);

                    actionsHtml += $action[0].outerHTML;
                });
                return '<div class="actions">' + actionsHtml + '</div>';
            } else {
                return '';
            }
        };
    };

    /**
     * Renders the clearing state boxes. It expects the data to be a two-dimensional array with the clearing state.
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.stateBoxes, data: function(row) { return [ row.state, row.clearingState]; }, ...</code>
     */
    $.fn.dataTable.render.stateBoxes = function(data, type, row, meta) {
        if(type === 'display') {
            return createStateBoxes(data[0], data[1]);
        } else if(type == 'type') {
            return 'num';
        } else {
            return data[0];
        }
    }

    // return empty object since all renderers are put into $.fn.dataTable.render.ellipsis which is a convenient location for such renderer
    return {};
});
