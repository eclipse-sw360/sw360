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
        return '<h4 class="mt-4">' + name + '</h4>';
    };

    mergeWizard.createSingleMergeLine = function createSingleMergeLine(propName, target, source, detailFormatter) {
        var line;

        target = target == null ? '' : target;
        source = source == null ? '' : source;
        detailFormatter = detailFormatter || function(element) { return element; };

        line = $.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="merge line">' +
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

        result = $($.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="merge line">' +
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

        result = $($.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="merge line">' +
            '    <div class="merge multi header">' + propName + '</div>' +
            '</fieldset>'));

        $.each(target, function(key, value) {
            if (!source[key]) {
                result.append(mergeWizard.createSingleMergeLine(key, value, [], detailFormatter));
            } else {
                result.append(mergeWizard.createSingleMergeLine(key, value, source[key], detailFormatter));
                existInBoth.push(key);
            }
            keys.push(key);
        });
        $.each(source, function(key, value) {
            if ($.inArray(key, existInBoth) === -1) {
                result.append(mergeWizard.createSingleMergeLine(key, [], value, detailFormatter));
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

        result = $($.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="merge line">' +
                               '    <h5>' + propName + '</h5>' +
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

    // private

    function createSingleMergeContent(target, source, rowIndex, detailFormatter) {
        var row,
            left,
            mid,
            right;

        row =   $.parseHTML('        <div class="row"></div>');

        left =  $.parseHTML('        <div class="merge single left col-5" data-row-index="' + rowIndex + '">' +
                            '            <span>' + detailFormatter(target) + '</span>' +
                            '        </div>');
        mid =   $.parseHTML('        <div class="merge single mid col-2" data-row-index="' + rowIndex + '">' +
                            (target === source ? '            <span class="text-success">&#10003;</span>' : '            <input class="btn btn-secondary" type="button" value="&#8656;" />') +
                            '        </div>');
        right = $.parseHTML('        <div class="merge single right col-5" data-row-index="' + rowIndex + '">' +
                            '            <span>' + detailFormatter(source) + '</span>' +
                            '        </div>');

        $(left).data('origVal', target);
        $(right).data('origVal', source);

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

        line = $.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="display line">' +
                           '    <h5>' + propName + '</h5>' +
                           '</fieldset>');

        return $(line).append(createSingleDisplayContent(value, detailFormatter));
    };

    mergeWizard.createMultiDisplayLine = function createMultiDisplayLine(propName, values, detailFormatter) {
        var result;

        values = values == null ? [] : values;
        detailFormatter = detailFormatter || function(element) { return element; };

        result = $($.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="display line">' +
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

        result = $($.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="display line">' +
            '    <div class="display multi header">' + propName + '</div>' +
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

        result = $($.parseHTML('<fieldset id="' + propName.replace(/ /g, '') + '" class="display line">' +
                               '    <h5>' + propName + '</h5>' +
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

    mergeWizard.registerClickHandlers = function registerClickHandlers() {
        wizardRoot.find('fieldset div.mid input').each(function(index, element) {
            registerCopyClickHandler($(element));
        });
    };

    // private

    function registerCopyClickHandler(element) {
        element.off('click');
        setTimeout(
                function() {
                    element.on('click', function(event) {
                        copySourceToTarget(element.parent().parent().parent().attr('id'), element.parent().data('rowIndex'));
                    });
                },
                10
        );
    }

    function registerUndoClickHandler(element) {
        element.off('click');
        /* if the handler is registered directly, it receives the same click event in which it was registered */
        setTimeout(
                function() {
                    element.on('click', function(event) {
                        undoCopySourceToTarget(element.parent().parent().parent().attr('id'), element.parent().data('rowIndex'));
                    });
                },
                10
        );
    }

    function copySourceToTarget(propName, rowIndex) {
        var $fieldset = $('#' + propName),
            sourceNode = $('.right[data-row-index="' + rowIndex + '"] span', $fieldset),
            source = sourceNode.text(),
            buttonNode = $('.mid[data-row-index="' + rowIndex + '"] input', $fieldset),
            targetNode = $('.left[data-row-index="' + rowIndex + '"] span', $fieldset),
            target = targetNode.text();

        $fieldset.addClass('modified');

        /* https://stackoverflow.com/questions/11591174/escaping-of-attribute-values-using-jquery-attr ... */
        buttonNode.val($('<div/>').html('&#8631;').text());
        registerUndoClickHandler(buttonNode, rowIndex);

        targetNode.parent().data('newVal', sourceNode.parent().data('origVal'));
        targetNode.parent().attr('title', target);
        targetNode.text(source);
    }

    function undoCopySourceToTarget(propName, rowIndex) {
        var $fieldset = $('#' + propName),
            sourceNode = $('.right[data-row-index="' + rowIndex + '"] span', $fieldset),
            buttonNode = $('.mid[data-row-index="' + rowIndex + '"] input', $fieldset),
            targetNode = $('.left[data-row-index="' + rowIndex + '"] span', $fieldset),
            target = targetNode.parent().attr('title');

        $fieldset.removeClass('modified');

        /* https://stackoverflow.com/questions/11591174/escaping-of-attribute-values-using-jquery-attr ... */
        buttonNode.val($('<div/>').html('&#8656;').text());
        registerCopyClickHandler(buttonNode, rowIndex);

        targetNode.parent().removeData('newVal');
        targetNode.parent().removeAttr('title');
        targetNode.text(target);
    }

    /* ******************** *********************
     * result methods
     ********************* ******************** */

    // public

    mergeWizard.getFinalSingleValue = function getFinalSingleValue(propName) {
        var $fieldset = $('#' + propName.replace(/ /g, '')),
            targetNode = $('.left[data-row-index="0"]', $fieldset);

        return getFinalValue(targetNode);
    };

    mergeWizard.getFinalMultiValue = function getFinalMultiValue(propName) {
        var $fieldset = $('#' + propName.replace(/ /g, '')),
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

    mergeWizard.getFinalMapValue = function getFinalMapValue(propName) {
        var $fieldset = $('#' + propName.replace(/ /g, '')),
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
        var $fieldset = $('#' + propName.replace(/ /g, '')),
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
        } else if (newVal) {
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

    return mergeWizard;
});
