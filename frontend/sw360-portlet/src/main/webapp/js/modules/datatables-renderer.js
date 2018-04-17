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

/**
 * This module provides some useful renderers for jquery DataTable. Please the jsdoc on the
 * rendering functions for more information.
 */
define('modules/datatables-renderer', ['jquery', /* jquery-plugins */ 'datatables', 'jquery-confirm'], function($) {

    // helper functions
    function createEllipsisSpan(text) {
        return $('<span>', {
            title: text,
            "class": "sw360-ellipsis",
        }).text(text);
    }

    function createSelectInput(selectDataKey, selectData, name, clazz, optionClazz, currentKey) {
        var $select = $('<select>', { name: name, "class": clazz, title: selectData[currentKey] });

        Object.keys(selectData).forEach(function(key) {
            $select.append($('<option>', { "class": optionClazz, value: key }).text(selectData[key]));
        });
        // use the html attribute to select the value in order to be serializable
        $select.find('option[value=' + currentKey + ']').attr('selected', true);

        return $select;
    }

    function createTextInput(name, clazz, placeholder, value) {
        var $text = $('<input>', { type: 'text', name: name, "class": clazz, placeholder: placeholder, title: value, value: value });

        return $text;
    }

    // renderer definitions

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
            var select;

            if(type === 'filter' || type === 'sort') {
                return meta.settings.json[selectDataKey][key];
            } else if(type === 'display') {
                select = createSelectInput(selectDataKey, meta.settings.json[selectDataKey], name, clazz, optionClazz, key);
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

            $.confirm({
                title: dialogTitle,
                content: '<form action="">' +
                            '<div style="padding-right: 15px;">' +
                                '<input type="text" placeholder="' + dialogTitle + '" style="width: 100%;" />' +
                            '</div>' +
                         '</form>',
                confirmButtonClass: 'btn-info',
                cancelButtonClass: 'btn-danger',
                buttons: {
                    confirm: function() {
                        var input = this.$content.find('input');
                         $text.val(input.val()).trigger('change');
                    },
                    cancel: function() {
                        // close
                    }
                },
                escapeKey: 'cancel',
                animation: 'none',
                onContentReady: function() {
                    var dialog = this,
                        input = dialog.$content.find('input');

                    input.val($text.val());
                    // for an unknown reason, the field is not focused if called directly
                    setTimeout(function() {
                        input.focus();
                    }, 50);

                    // if the user submits the form by pressing enter in the field.
                    this.$content.find('form').on('submit', function (event) {
                        event.preventDefault();
                        dialog.$$confirm.trigger('click');
                    });
                },
            });
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
     * </ol>
     *
     * @param {Array} actions the actions as defined before
     *
     * @return {Function} render function for DataTable
     *
     * Example usage in column definition: <code>..., renderer: $.fn.dataTable.render.actions( [{ key: 'attachmentContentId', 'class': 'delete-attachment', icon: 'Trash', title: 'Delete Attachment', approvalKey: 'checkStatus' }] ), ...</code>
     */
    $.fn.dataTable.render.actions = function(actions) {
        return function(data, type, row) {
            var actionsHtml = '';
            if(type === 'display') {
                actions.forEach(function(action) {
                    actionsHtml += $('<img>', {
                        'class': action['class'],
                        src: '/sw360-portlet/images/' + action.icon + '.png',
                        alt: action.title,
                        title: action.title,
                        'data-key': row[action.key],
                        'data-approvalstate': row[action.approvalKey]
                    })[0].outerHTML;
                });
                return actionsHtml;
            } else {
                return '';
            }
        };
    };

    // return empty object since all renderers are put into $.fn.dataTable.render.ellipsis which is a convenient location for such renderer
    return {};
});
