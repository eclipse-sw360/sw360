/*
 * Copyright Siemens AG, 2017, 2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
        var line = createSingleLine(propName, target, source, detailFormatter);
        return $(line).append(createSingleMergeContent(target, source, 0, detailFormatter));
    };

    mergeWizard.createSingleMergeLineForHtml = function createSingleMergeLineForHtml(propName, target, source, detailFormatter) {
        let line = createSingleLine(propName, target, source, detailFormatter);
        let content = createSingleMergeContent(target, source, 0, detailFormatter);
        let txt= $(content).find(".left span").text();
        $(content).find(".left span").text("");
        $(content).find(".left span").html(txt);

        txt= $(content).find(".right span").text();
        $(content).find(".right span").text("");
        $(content).find(".right span").html(txt);

        return $(line).append(content);
    };

    mergeWizard.createSingleSplitLine = function createSingleSplitLine(propName, target, source, detailFormatter) {
        let line = createSingleLine(propName, target, source, detailFormatter);

        return $(line).append(createSingleSplitContent(target, source, 0, detailFormatter, true, '', false, null));
    };

    function createSingleLine(propName, target, source, detailFormatter) {
        target = target == null ? '' : target;
        source = source == null ? '' : source;
        detailFormatter = detailFormatter || function(element) { return element; };

        let singleLine = $.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="merge line">' +
                           '    <h5></h5>' +
                           '</fieldset>');
        $(singleLine).find("h5:eq(0)").text(propName);
        return singleLine;
    }

    mergeWizard.createMultiMergeLine = function createMultiMergeLine(propName, target, source, detailFormatter) {
        return createMultiLine(propName, target, source, detailFormatter, false, '', false , null);
    };

    mergeWizard.createMultiSplitLine = function createMultiMergeLine(propName, target, source, detailFormatter, midElement, addTooltip, tooltipFormatter) {
        return createMultiLine(propName, target, source, detailFormatter, true, midElement, addTooltip, tooltipFormatter);
    };

    function createMultiLine(propName, target, source, detailFormatter, split, midElement, addTooltip, tooltipFormatter) {
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
                result.append(split ? createSingleSplitContent(value, '', rowIndex++, detailFormatter, false, midElement, addTooltip, tooltipFormatter)
                                    : createSingleMergeContent(value, '', rowIndex++, detailFormatter));
            } else {
                result.append(split ? createSingleSplitContent(value, source[foundIndex], rowIndex++, detailFormatter, true, '', addTooltip, tooltipFormatter) 
                                    : createSingleMergeContent(value, source[foundIndex], rowIndex++, detailFormatter));
                existInBoth.push(foundIndex);
            }
        });
        $.each(source, function(index, value) {
            if ($.inArray(index, existInBoth) === -1) {
                result.append(split ? createSingleSplitContent('', value, rowIndex++, detailFormatter, true, '', addTooltip, tooltipFormatter)
                                    : createSingleMergeContent('', value, rowIndex++, detailFormatter));
            }
        });

        return result;
    }

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
                let lineSrcEmpty = mergeWizard.createSingleMergeLine(key, value, '', detailFormatter);
                $(lineSrcEmpty).attr("id", normalizePropName(propName+key));
                result.append(lineSrcEmpty);
            } else {
                let lineSrcNotEmpty = mergeWizard.createSingleMergeLine(key, value, source[key], detailFormatter);
                $(lineSrcNotEmpty).attr("id", normalizePropName(propName+key));
                result.append(lineSrcNotEmpty);
                existInBoth.push(key);
            }
            keys.push(key);
        });
        $.each(source, function(key, value) {
            if ($.inArray(key, existInBoth) === -1) {
                let lineNotExistInBoth = mergeWizard.createSingleMergeLine(key, '', value, detailFormatter);
                $(lineNotExistInBoth).attr("id", normalizePropName(propName+key))
                result.append(lineNotExistInBoth);
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
        return propName.replace(/[^a-zA-Z\d]/g, '_');
    }

    function createSingleSplitContent(target, source, rowIndex, detailFormatter, locked, midElement, addTooltip, tooltipFormatter) {
        return createSingleLineContent(target, source, rowIndex, detailFormatter, locked, midElement, addTooltip, tooltipFormatter);
    }

    function createSingleMergeContent(target, source, rowIndex, detailFormatter, locked) {
        let midElement = (target === source ? '<span class="text-success">&#10003;</span>' : '<input class="btn btn-secondary" type="button" value="&#8656;" />');
        return createSingleLineContent(target, source, rowIndex, detailFormatter, locked, midElement, false, null);
    }

    function createSingleLineContent(target, source, rowIndex, detailFormatter, locked, midElement, addTooltip, tooltipFormatter) {
        var row,
            left,
            mid,
            right;
        detailFormatter = detailFormatter || function(element) { return element; };

        row =   $.parseHTML('        <div class="row"></div>');

        left =  $.parseHTML('        <div class="merge single left col-5" data-row-index="' + rowIndex + '">' +
                            '            <span></span>' +
                            '        </div>');
        mid =   $.parseHTML('        <div class="merge single mid col-2" data-row-index="' + rowIndex + '">' +
                                         midElement +
                            '        </div>');
        right = $.parseHTML('        <div class="merge single right col-5" data-row-index="' + rowIndex + '">' +
                            '            <span></span>' +
                            '        </div>');

        $(left).find("span:eq(0)").text(detailFormatter(target));
        $(right).find("span:eq(0)").text(detailFormatter(source));

        if (addTooltip) {
            $(left).find("span:eq(0)").attr("title", tooltipFormatter(target));
            $(right).find("span:eq(0)").attr("title", tooltipFormatter(source));
        }

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
                           '    <h5></h5>' +
                           '</fieldset>');
        $(line).find("h5:eq(0)").text(propName);
        return $(line).append(createSingleDisplayContent(value, detailFormatter));
    };

    mergeWizard.createSingleDisplayLineForHtml = function createSingleDisplayLineForHtml(propName, value, detailFormatter) {
        var line;

        value = value == null ? '' : value;
        detailFormatter = detailFormatter || function(element) { return element; };

        line = $.parseHTML('<fieldset id="' + normalizePropName(propName) + '" class="display line">' +
                           '    <h5></h5>' +
                           '</fieldset>');
        $(line).find("h5:eq(0)").text(propName);
        return $(line).append(createSingleDisplayContentHtml(value, detailFormatter));
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
        let displayElement = $.parseHTML('        <div class="displayRow">' +
                           '            <span></span>' +
                           '        </div>');
        $(displayElement).find("span:eq(0)").text(detailFormatter(value));
        return displayElement;
    }

    function createSingleDisplayContentHtml(value, detailFormatter) {
        let displayElement = $.parseHTML('        <div class="displayRow">' +
                           '            <span></span>' +
                           '        </div>');
        $(displayElement).find("span:eq(0)").html(detailFormatter(value));
        return displayElement;
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

    mergeWizard.registerClickHandlersForIcons = function registerClickHandlersForIcons(propNameMap, callback) {
        propNameMap = propNameMap || {};

        wizardRoot.find('fieldset div.mid input').each(function(index, element) {
            var propName = $(element).parents('fieldset').attr('id');
            if(propNameMap[propName]) {
                registerCopyClickHandler($(element), null, true);
            }
        });
    };

    // private

    function registerCopyClickHandler(element, callback, html) {
        element.off('click.merge');
        element.on('click.merge', function(event) {
            var propName = element.parent().parent().parent().attr('id'),
                rowIndex = element.parent().data('rowIndex'),
                $fieldset = $('#' + propName),
                sourceNode = $('.right[data-row-index="' + rowIndex + '"]', $fieldset),
                targetNode = $('.left[data-row-index="' + rowIndex + '"]', $fieldset);

            if($(event.currentTarget).hasClass('undo')) {
                undoCopySourceToTarget(propName, rowIndex, html);
                if(callback) {
                    callback(propName, false, targetNode.data('origVal'), sourceNode.data('origVal'));
                }
            } else {
                copySourceToTarget(propName, rowIndex, html);
                if(callback) {
                    callback(propName, true, targetNode.data('origVal'), sourceNode.data('origVal'));
                }
            }
        });
    }

    function copySourceToTarget(propName, rowIndex, html) {
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

        if(html){
            targetNode.find('span:first').html($row.data('detailFormatter')(sourceNode.data('origVal')));
        }
        else {
            targetNode.find('span:first').text($row.data('detailFormatter')(sourceNode.data('origVal')));
        }
    }

    function undoCopySourceToTarget(propName, rowIndex, html) {
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
        if(html){
            targetNode.find('span:first').html($row.data('detailFormatter')(targetNode.data('origVal')));
        }
        else {
            targetNode.find('span:first').text($row.data('detailFormatter')(targetNode.data('origVal')));
        }
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

    mergeWizard.getFinalSingleValueTarget = function getFinalSingleValueTarget(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            targetNode = $('.right[data-row-index="0"]', $fieldset);

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

    mergeWizard.getFinalMultiValueTarget = function getFinalMultiValueTarget(propName) {
        var $fieldset = $('#' + normalizePropName(propName)),
            targetNodes = $('.right', $fieldset),
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
            finalVal = mergeWizard.getFinalSingleValue(propName + value);
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
