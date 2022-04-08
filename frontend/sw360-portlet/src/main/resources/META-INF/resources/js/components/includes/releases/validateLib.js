/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

define('components/includes/releases/validateLib', ['jquery'], function ($) {
    var errors = [];
    var totalErrors = 0;
    var formId = '';

    const numRegex = /^[-+]?\d*$/;
    const urlRegex = /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/;
    const downloadUrlRegex = /(?:git|ssh|https?|ftp|git@[-\w.]+):(\/\/)?(.*?)(\.git)?(\/?|\#[-\d\w._]+?)$/;

    function regex(val, params) {
        if (!required(val)) {
            return true;
        }
        var regexText = new RegExp(params);
        if (val.match(regexText) != null) {
            return true;
        } else {
            return false;
        }
    }

    function required(val) {
        if ((val == null) || (val == '')) {
            return false;
        } else {
            return true;
        }
    }

    function integer(val, params) {
        if (isNaN(val)) {
            return false;
        }

        var x = parseFloat(val);

        return (x | 0) === x;
    }

    function max(val, params) {
        if (val.match(numRegex) == null) {
            return true;
        }

        if (isNaN(parseInt(val)) || isNaN(parseInt(params))) {
            return true;
        }

        return parseInt(val) <= parseInt(params);
    }

    function min(val, params) {
        if (val.match(numRegex) == null) {
            return true;
        }

        if (isNaN(parseInt(val)) || isNaN(parseInt(params))) {
            return true;
        }

        return parseInt(val) >= parseInt(params);
    }

    function isUrl(val, params) {
        if (!required(val)) {
            return true;
        }
        if (val.match(urlRegex)) {
            return true;
        } else {
            return false;
        }
    }

    function isDownloadUrl(val, params) {
        if (!required(val)) {
            return true;
        }
        if (val.match(downloadUrlRegex)) {
            return true;
        } else {
            return false;
        }
    }

    function getElementVal(element) {
        if ($(element).is('input')) {       // Textbox, checkbox
            var type = $(element).attr('type');

            switch (type) {
                case 'text':
                    return $(element).val();
                    break;

                case 'checkbox':
                    return $(element).is(':checked');
                    break;

                case 'radio':
                    var name = $(element).attr('name');
                    var all_radio_button = document.getElementsByName(name);
                    for (radio_button in all_radio_button) {
                        if ($(radio_button).is(':checked')) {
                            return 'checked';
                        }
                    }
                    return null;
                    break;
                case 'date':
                    return $(element).val();
                    break;
                case 'time':
                    return $(element).val();
                    break;
                default:
                    return undefined;
                    break;
            }
        }

        if ($(element).is('select')) {      // Selectbox
            return $(element).val();
        }

        if ($(element).is('textarea')) {    // Textarea
            return $(element).val();
        }

        return undefined;
    }

    function setFormId(val) {
        formId = val;
    }

    function validate() {
        errors = [];
        totalErrors = 0;

        const elements = $('#' + formId).find('.needs-validation');

        for (var i = 0; i < elements.length; i++) {
            const element = elements[i];
            if (typeof $(element).attr('disabled') === 'undefined') {
                const elementId = String($(element).attr('id'));
                const elementErrors = validateElement(element);

                errors.push({
                    id: elementId,
                    rules: elementErrors
                });

                totalErrors += elementErrors.length;
            }
        }
    }

    function allValid() {
        return totalErrors == 0;
    }

    function validateElement(element) {
        var errors = [];
        // var elementRule = $(element).attr('rule');
        if (typeof $(element).attr('rule') === 'undefined' || $(element).attr('rule').length == 0) {
            return errors;
        }

        var val = getElementVal($(element));

        var valdationRules = $(element).attr('rule').split('|');

        valdationRules.forEach(rule => {
            var ruleName = rule.split(':')[0];
            var params = rule.split(':')[1];

            if (!eval(ruleName)(val, params)) {
                errors.push(ruleName);
            }
        });
        return errors;
    }

    function showAllErrors() {
        hideAllErrors();

        if (allValid()) {
            return;
        }

        $('#' + formId).addClass('was-validated');

        errors.forEach(error => {
            showError(error.id, error.rules);
        })
    }

    function showError(elementId, rules) {
        try {
            if (rules.length == 0) {
                $('#' + elementId)[0].setCustomValidity('');
                return;
            }

            $('#' + elementId)[0].setCustomValidity('error');
        } catch {
            console.log('Cannot show error');
        }

        rules.forEach(rule => {
            $('#' + elementId + '-error-messages').find("[rule='" + rule + "']").addClass('d-block');
        })
    }

    function hideAllErrors() {
        $('#' + formId).removeClass('was-validated');
        const elements = $('#' + formId).find('.needs-validation');

        for (var i = 0; i < elements.length; i++) {
            $(elements[i])[0].setCustomValidity('');
            $('.invalid-feedback').css('display', 'none');
            $('.invalid-feedback').removeClass('d-block');
        }
    }

    function addError(elementId, elementErrors) {
        errors.push({
            id: elementId,
            rules: elementErrors
        });

        totalErrors += elementErrors.length;
    }

    return {
        setFormId: setFormId,
        validate: validate,
        allValid: allValid,
        addError: addError,
        showAllErrors: showAllErrors,
        hideAllErrors: hideAllErrors
    }
});