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
define('modules/mergeWizard', [ 'jquery', 'modules/sw360Wizard' ], function($, sw360Wizard) {

    var wizardRoot;

    /**
     * This modules wraps the sw360Wizard module and can therefore be initialized like that one.
     *
     * It adds some "static" helper methods to the returned wizard object, which are especially useful if you
     * want to build wizard which has one or more steps where you want to merge two datasets.
     * The added methods can be used for generating the dom, managing merge events and retrieving the final
     * values for each (overridden) property.
     */
    var mergeWizard = function(config) {
        wizardRoot = config.wizardRoot;

        // initialize the real wizard
        sw360Wizard(config);
    };

    /* ******************** *********************
     * render merge methods
     ********************* ******************** */

    // public

    mergeWizard.createCategoryLine = function createCategoryLine(name) {
        return '<h4>' + name + '</h4>';
    };

    mergeWizard.createSingleMergeLine = function createSingleMergeLine(propName, target, source, detailFormatter) {
        var line;

        target = target == null ? '' : target;
        source = source == null ? '' : source;
        detailFormatter = detailFormatter || function(element) { return element; };

        line = $.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="merge line">' +
                           '    <h5>' + propName + '</h5>' +
                           '</fieldset>');

        return $(line).append(createSingleMergeContent(target, source, 0, detailFormatter));
    };

    mergeWizard.createMultiMergeLine = function createMultiMergeLine(propName, target, source, detailFormatter) {
        var result,
            rowIndex = 0,
            existInBoth = [];

        target = target == null ? [] : target;
        source = source == null ? [] : source;
        detailFormatter = detailFormatter || function(element) { return element; };

        result = $($.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="merge line">' +
                               '    <h5>' + propName + '</h5>' +
                               '</fieldset>'));

        $.each(target, function(index, value) {
            var foundIndex = -1;
            if ((foundIndex = $.inArray(value, source)) === -1) {
                result.append(createSingleMergeContent(value, '', rowIndex++, detailFormatter));
            } else {
                result.append(createSingleMergeContent(value, source[foundIndex], rowIndex++, detailFormatter));
                existInBoth.push(foundIndex);
            }
        });
        $.each(source, function(index, value) {
            if ($.inArray(index, existInBoth) === -1) {
                result.append(createSingleMergeContent('', value, rowIndex++, detailFormatter));
            }
        });

        return result;
    };

    mergeWizard.createMapMergeLine = function createMapMergeLine(propName, target, source, detailFormatter) {
        var result,
            keys = [],
            existInBoth = [];

        target = target == null ? {} : target;
        source = source == null ? {} : source;
        detailFormatter = detailFormatter || function(element) { return element; };

        result = $($.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="merge block">' +
                               '    <h4>' + propName + '</h4>' +
                               '</fieldset>'));

        $.each(target, function(key, value) {
            if (!source[key]) {
                result.append(mergeWizard.createSingleMergeLine(key, value, '', detailFormatter));
            } else {
                result.append(mergeWizard.createSingleMergeLine(key, value, source[key], detailFormatter));
                existInBoth.push(key);
            }
            keys.push(key);
        });
        $.each(source, function(key, value) {
            if ($.inArray(key, existInBoth) === -1) {
                result.append(mergeWizard.createSingleMergeLine(key, '', value, detailFormatter));
                keys.push(key);
            }
        });

        result.data('mapKeys', keys);
        return result;
    };

    mergeWizard.createMultiMapMergeLine = function createMultiMapMergeLine(propName, target, source, detailFormatter) {
        var result,
            keys = [],
            existInBoth = [];

        target = target == null ? {} : target;
        source = source == null ? {} : source;
        detailFormatter = detailFormatter || function(element) { return element; };

        result = $($.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="merge block">' +
                               '    <h4>' + propName + '</h4>' +
                               '</fieldset>'));

        $.each(target, function(key, value) {
            if (!source[key]) {
                result.append(mergeWizard.createMultiMergeLine(key, value, [], detailFormatter));
            } else {
                result.append(mergeWizard.createMultiMergeLine(key, value, source[key], detailFormatter));
                existInBoth.push(key);
            }
            keys.push(key);
        });
        $.each(source, function(key, value) {
            if ($.inArray(key, existInBoth) === -1) {
                result.append(mergeWizard.createMultiMergeLine(key, [], value, detailFormatter));
                keys.push(key);
            }
        });

        result.data('mapKeys', keys);
        return result;
    };
    
    /**
     * Merges the given property. The merge can be locked. A locked merge cannot be changed by the user.
     */
    mergeWizard.mergeByDefault = function(propName, rowIndex) {
        copySourceToTarget(propName.replace(/ /g, ''), rowIndex);
    }

    mergeWizard.lockRow = function(propName, rowIndex, lock) {
        var $fieldset = $('#' + propName.replace(/ /g, '')),
            buttonNode = $('.mid[data-row-index="' + rowIndex + '"] input', $fieldset);
        buttonNode.prop('disabled', lock);
    }

    mergeWizard.createCustomMergeLines = function createCustomMergeLine(propName, createLines) {
        var result;

        result = $($.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="merge line">' +
                               '    <h5>' + propName + '</h5>' +
                               '</fieldset>'));

        createLines(result, createSingleMergeContent);
        return result;
    }

    // private

    function normalizePropName(propName) {
        return propName.replace(/[\s\.]/g, '_');
    }

    function createSingleMergeContent(target, source, rowIndex, detailFormatter, locked) {
        var row,
            left,
            mid,
            right;

        detailFormatter = detailFormatter || function(element) { return element; };

        row =   $.parseHTML('        <div class="row"></div>');

        left =  $.parseHTML('        <div class="merge single left col-5" data-row-index="' + rowIndex + '">' +
                            '            <span>' + detailFormatter(target) + '</span>' +
                            '        </div>');
        mid =   $.parseHTML('        <div class="merge single mid col-2" data-row-index="' + rowIndex + '">' +
                            (target === source ? '<span class="text-success">&#10003;</span>' : '<input class="btn btn-secondary" type="button" value="&#8656;" />') +
                            '        </div>');
        right = $.parseHTML('        <div class="merge single right col-5" data-row-index="' + rowIndex + '">' +
                            '            <span>' + detailFormatter(source) + '</span>' +
                            '        </div>');

        $(row).data('detailFormatter', detailFormatter);
        $(left).data('origVal', target);
        $(right).data('origVal', source);

        if(locked) {
            $(mid).find('input.btn').prop('disabled', true);
        }

        return $(row).append(left).append(mid).append(right);
    }

    /* ******************** *********************
     * render display methods
     ********************* ******************** */

    // public

    mergeWizard.createSingleDisplayLine = function createSingleDisplayLine(propName, value, detailFormatter) {
        var line;

        value = value == null ? '' : value;
        detailFormatter = detailFormatter || function(element) { return element; };

        line = $.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="display line">' +
                           '    <h5>' + propName + '</h5>' +
                           '</fieldset>');

        return $(line).append(createSingleDisplayContent(value, detailFormatter));
    };

    mergeWizard.createMultiDisplayLine = function createMultiDisplayLine(propName, values, detailFormatter) {
        var result;

        values = values == null ? [] : values;
        detailFormatter = detailFormatter || function(element) { return element; };

        result = $($.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="display line">' +
                               '    <h5>' + propName + '</h5>' +
                               '</fieldset>'));

        $.each(values, function(index, value) {
            result.append(createSingleDisplayContent(value, detailFormatter));
        });

        return result;
    };

    mergeWizard.createMapDisplayLine = function createMapDisplayLine(propName, values, detailFormatter) {
        var result;

        values = values == null ? {} : values;
        detailFormatter = detailFormatter || function(element) { return element; };

        result = $($.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="display block">' +
                               '    <h4>' + propName + '</h4>' +
                               '</fieldset>'));

        $.each(values, function(key, value) {
            result.append(mergeWizard.createSingleDisplayLine(key, value, detailFormatter));
        });

        return result;
    };

    mergeWizard.createMultiMapDisplayLine = function createMultiMapDisplayLine(propName, values, detailFormatter) {
        var result;

        values = values == null ? {} : values;
        detailFormatter = detailFormatter || function(element) { return element; };

        result = $($.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="display block">' +
                               '    <h4>' + propName + '</h4>' +
                               '</fieldset>'));

        $.each(values, function(key, value) {
            result.append(mergeWizard.createMultiDisplayLine(key, value, detailFormatter));
        });

        return result;
    };

    // private

    function createSingleDisplayContent(value, detailFormatter) {
        return $.parseHTML('        <div class="displayRow">' +
                           '            <span>' + detailFormatter(value) + '</span>' +
                           '        </div>');
    }

    /* ******************** *********************
     * event handler methods
     ********************* ******************** */

    // public

    /**
     * Register a click handler to all merge lines. This method must be called in any case otherwise 
     * values cannot be copied from source to target. In addition this method allows to listen for 
     * distinct properties and a callback will be called if these properties are changed.
     * 
     * @param {Object|Boolean} map of properties to listen for changes, e.g. <code>{ 'Homepage': true }</code>.
     *  Keys must be normalized (no spaces). Set it to true to listen for all properties.
     * @param {Function} callback function to call if one of the properties changes. The callback will receive the following
     *  parameters:
     *      - property name (normalized = no spaces)
     *      - copied
     *      - target value
     *      - source value
     */
    mergeWizard.registerClickHandlers = function registerClickHandlers(propNameMap, callback) {
        propNameMap = propNameMap || {};

        wizardRoot.find('fieldset div.mid input').each(function(index, element) {
            var propName = $(element).parents('fieldset').attr('id');
            if(propNameMap === true || propNameMap[propName]) {
                registerCopyClickHandler($(element), callback);
            } else {
                registerCopyClickHandler($(element));
            }
        });
    };

    // private

    function registerCopyClickHandler(element, callback) {
        element.off('click.merge');
        element.on('click.merge', function(event) {
            var propName = element.parent().parent().parent().attr('id'),
                rowIndex = element.parent().data('rowIndex'),
                $fieldset = $('#' + propName),
                sourceNode = $('.right[data-row-index="' + rowIndex + '"]', $fieldset),
                targetNode = $('.left[data-row-index="' + rowIndex + '"]', $fieldset);

            if($(event.currentTarget).hasClass('undo')) {
                undoCopySourceToTarget(propName, rowIndex);
                if(callback) {
                    callback(propName, false, targetNode.data('origVal'), sourceNode.data('origVal'));
                }
            } else {
                copySourceToTarget(propName, rowIndex);
                if(callback) {
                    callback(propName, true, targetNode.data('origVal'), sourceNode.data('origVal'));
                }
            }
        });
    }

    function copySourceToTarget(propName, rowIndex) {
        var $fieldset = $('#' + propName),
            sourceNode = $('.right[data-row-index="' + rowIndex + '"]', $fieldset),
            buttonNode = $('.mid[data-row-index="' + rowIndex + '"] input', $fieldset),
            targetNode = $('.left[data-row-index="' + rowIndex + '"]', $fieldset),
            $row = sourceNode.parent();

        $row.addClass('modified');

        /* https://stackoverflow.com/questions/11591174/escaping-of-attribute-values-using-jquery-attr ... */
        buttonNode.val($('<div/>').html('&#8631;').text());
        buttonNode.addClass('undo');

        targetNode.data('newVal', sourceNode.data('origVal'));
        targetNode.find('span:first').html($row.data('detailFormatter')(sourceNode.data('origVal')));
    }

    function undoCopySourceToTarget(propName, rowIndex) {
        var $fieldset = $('#' + propName),
            sourceNode = $('.right[data-row-index="' + rowIndex + '"]', $fieldset),
            buttonNode = $('.mid[data-row-index="' + rowIndex + '"] input', $fieldset),
            targetNode = $('.left[data-row-index="' + rowIndex + '"]', $fieldset),
            $row = sourceNode.parent();

        $row.removeClass('modified');

        /* https://stackoverflow.com/questions/11591174/escaping-of-attribute-values-using-jquery-attr ... */
        buttonNode.val($('<div/>').html('&#8656;').text());
        buttonNode.removeClass('undo');

        targetNode.removeData('newVal');
        targetNode.find('span:first').html($row.data('detailFormatter')(targetNode.data('origVal')));
    }

    /* ******************** *********************
     * result methods
     ********************* ******************** */

    // public

    mergeWizard.getFinalSingleValue = function getFinalSingleValue(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            targetNode = $('.left[data-row-index="0"]', $fieldset);

        return getFinalValue(targetNode);
    };

    mergeWizard.getEnhancedFinalSingleValue = function getEnhancedFinalSingleValue(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            targetNode = $('.left[data-row-index="0"]', $fieldset);

        return getEnhancedFinalValue(targetNode);
    };

    mergeWizard.getFinalMultiValue = function getFinalMultiValue(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            targetNodes = $('.left', $fieldset),
            result = [],
            finalVal;

        targetNodes.each(function(index, value) {
            finalVal = getFinalValue($(value));
            if (finalVal !== undefined) {
                result.push(finalVal);
            }
        });

        return result;
    };

    mergeWizard.getEnhancedFinalMultiValue = function getEnhancedFinalMultiValue(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            targetNodes = $('.left', $fieldset),
            result = [],
            finalVal;

        targetNodes.each(function(index, value) {
            finalVal = getEnhancedFinalValue($(value));
            if (finalVal !== undefined) {
                result.push(finalVal);
            }
        });

        return result;
    };

    mergeWizard.getFinalMapValue = function getFinalMapValue(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            keys = $fieldset.data('mapKeys'),
            result = {},
            finalVal;

        $.each(keys, function(index, value) {
            finalVal = mergeWizard.getFinalSingleValue(value);
            if (finalVal !== undefined) {
                result[value] = finalVal;
            }
        });

        return result;
    };

    mergeWizard.getFinalMultiMapValue = function getFinalMultiMapValue(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            keys = $fieldset.data('mapKeys'),
            result = {},
            finalVal;

        $.each(keys, function(index, value) {
            finalVal = mergeWizard.getFinalMultiValue(value);
            if (finalVal !== undefined) {
                result[value] = finalVal;
            }
        });

        return result;
    };

    // private

    function getFinalValue(element) {
        var origVal = element.data('origVal'),
            newVal = element.data('newVal');

        if (newVal === '') {
            /* origVal should be deleted */
            return undefined;
        } else if (typeof newVal !== 'undefined' && newVal != null) {
            /* origVal should be overridden */
            return newVal;
        } else if (origVal === '') {
            /* origVal should be kept but has been empty*/
            return undefined;
        } else {
            /* origVal should be kept */
            return origVal;
        }
    }

    function getEnhancedFinalValue(element) {
        var origVal = element.data('origVal'),
            newVal = element.data('newVal');

        if (newVal === '') {
            /* origVal should be deleted */
            return {
                target: false,
                value: undefined
            };
        } else if (typeof newVal !== 'undefined' && newVal != null) {
            /* origVal should be overridden */
            return {
                target: false,
                value: newVal
            }
        } else if (origVal === '') {
            /* origVal should be kept but has been empty*/
            return {
                target: true,
                value: undefined
            }
        } else {
            /* origVal should be kept */
            return {
                target: true,
                value: origVal
            }
        }
    }

    return mergeWizard;
});
