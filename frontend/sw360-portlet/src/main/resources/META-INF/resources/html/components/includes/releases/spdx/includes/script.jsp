<style>
    .sub-label {
        margin-right: 0.5rem;
        margin-top: 0.5rem;
        font-weight: 400;
        font-size: 1rem;
    }

    .sub-title {
        width: 10rem;
        margin-top: 0.7rem;
        margin-right: 1rem;
    }

    .sub-input {
        width: auto;
    }

    .spdx-checkbox {
        margin-top: 0.75rem;
        width: 1rem;
        height: 1rem;
    }

    .spdx-select {
        width: auto;
        flex: auto;
        margin-right: 2rem;
    }

    .spdx-radio {
        margin-top: 0.75rem;
        margin-right: 0.5rem;
        width: 1rem;
        height: 1rem;
    }

    .spdx-date {
        width: 12rem;
        text-align: center;
    }

    .spdx-time {
        width: 12rem;
        text-align: center;
        margin-left: 0.6rem;
    }

    .label-select {
        flex: 1;
        text-decoration: underline;
    }

    .spdx-delete-icon-main {
        margin-top: 0.3rem;
        margin-right: 1rem;
        width: 1rem;
        height: auto;
    }

    .spdx-delete-icon-sub {
        margin-top: 0.3rem;
        margin-right: 4rem;
        width: 1rem;
        height: auto;
    }

    .spdx-add-button-main {
        margin-left: 11rem;
        margin-bottom: 2rem;
        width: 10rem;
    }

    .spdx-add-button-sub {
        width: 10rem;
    }

    thead {
        cursor: pointer;
    }
</style>
<script>
    require(['jquery'], function ($) {

        // $('#formSubmit').on('click', function (event) {
        //     event.preventDefault();
        //     var myValidator = validator();
        //     myValidator.setFormId('editOtherLicensingInformationDetected');
        //     myValidator.validate();
        //     myValidator.showAllErrors();
        // });

        function validator() {
            var errors = [];
            var totalErrors = 0;
            var formId = '';

            const numRegex = /^[-+]?\d*$/;
            const urlRegex = /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/;
            const downloadUrlRegex = /(?:git|ssh|https?|ftp|git@[-\w.]+):(\/\/)?(.*?)(\.git)?(\/?|\#[-\d\w._]+?)$/;

            function regex(val, params) {
                if (!isNotNull(val)) {
                    return true;
                }
                var regexText = new RegExp(params);
                if (val.match(regexText) != null) {
                    return true;
                } else {
                    return false;
                }
            }

            function isNotNull(val) {
                if ((val == null) || (val == '')) {
                    return false;
                } else {
                    return true;
                }
            }

            function required(val, params) {
                if (value == 'checked') {
                    return true;
                } else {
                    return false;
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
                if (!isNotNull(val)) {
                    return true;
                }
                if (val.match(urlRegex)) {
                    return true;
                } else {
                    return false;
                }
            }

            function isDownloadUrl(val, params) {
                if (!isNotNull(val)) {
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
                    const elementId = String($(element).attr('id'));
                    const elementErrors = validateElement(element);

                    errors.push({
                        id: elementId,
                        rules: elementErrors
                    });

                    totalErrors += elementErrors.length;
                }
            }

            function allValid() {
                return totalErrors == 0;
            }

            function validateElement(element) {
                var errors = [];

                if ($(element).attr('rule') === 'undefined' || $(element).attr('rule').length == 0) {
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
                if (rules.length == 0) {
                    $('#' + elementId)[0].setCustomValidity('');
                    return;
                }

                $('#' + elementId)[0].setCustomValidity('error');
                rules.forEach(rule => {
                    var tmp = "[rule='" + rule + "']";
                    var tmp2 = "[rule=\"${rule}\"]";
                    console.log(tmp);
                    console.log(tmp2);
                    $('#' + elementId + '-error-messages').find(tmp).addClass('d-block');
                })


                // $('#' + elementId)[0].setCustomValidity('error');
                // $('#error-invalid-char').addClass('d-block');
            }

            function hideAllErrors() {
                $('#' + formId).removeClass('needs-validation');
                const elements = $('#' + formId).find('.needs-validation');

                for (var i = 0; i < elements.length; i++) {
                    $(elements[i])[0].setCustomValidity('');
                    $('.invalid-feedback').css('display', 'none');
                    $('.invalid-feedback').removeClass('d-block');
                }
            }

            return {
                setFormId: setFormId,
                validate: validate,
                allValid: allValid,
                showAllErrors: showAllErrors,
            }
        }
    });

    function enableSection(section, state) {
        if (!state) {
            section.find('button').attr('disabled', 'disabled');
            section.find('input').attr('disabled', 'disabled');

            section.find('.spdx-delete-icon-main').css('cursor', 'not-allowed');
            section.find('.spdx-delete-icon-sub').css('cursor', 'not-allowed');
        } else {
            section.find('button').removeAttr('disabled');
            section.find('input').removeAttr('disabled');

            section.find('select').removeAttr('disabled');

            section.find('.spdx-delete-icon-main').css('cursor', 'pointer');
            section.find('.spdx-delete-icon-sub').css('cursor', 'pointer');
        }

        // Keep the main button Add enable
        section.find('.spdx-add-button-main').removeAttr('disabled');
    }

    function clearSection(section) {
        section.find('input').val('');

        //select box: first value

        //radio button: no selection
    }

    $('.spdx-add-button-main').on('click', function (e) {
        e.preventDefault();
    })

    $('.spdx-add-button-sub').on('click', function (e) {
        e.preventDefault();
    })

    function deleteMain(deleteBtn) {
        let selectbox = $(deleteBtn).prev('select');

        if ($(deleteBtn).css('cursor') == 'not-allowed') {
            return;
        }

        selectbox.find('option:selected').remove();

        // If all options were deleted, disable the select box
        if (selectbox.find('option').length == 0) {
            selectbox.attr('disabled', 'true');
            $(deleteBtn).css('cursor', 'not-allowed')
        }

        let newItem = selectbox.find('option:selected').val();

        // Fill data of new item here
        // alert(newItem);

        if (typeof (newItem) == 'undefined') {
            //Clear all textboxes and disable all textboxes/selectboxes/radio buttons/buttons

            section = selectbox.closest('.form-group');

            enableSection(section, false);

            clearSection(section);
        }
    }

    function deleteSub(deleteBtn) {
        $(deleteBtn).parent().remove();
    }

    function addMain(addBtn) {
        let selectbox = $(addBtn).prev().find('select');

        let newIndex = parseInt(selectbox.find('option').last().val()) + 1;

        if (isNaN(newIndex)) {
            newIndex = 1;
        }

        selectbox.append('<option value="' + newIndex + '">' + newIndex + '</option>');

        selectbox.val(newIndex);

        section = selectbox.closest('.form-group');

        enableSection(section, true);

        clearSection(section);
    }

    $(function () {
        // Expand/collapse section when click on the header
        $('thead').on('click', function () {
            if ($(this).next().css('display') == 'none') {
                $(this).next().css('display', '');
            } else {
                $(this).next().css('display', 'none');
            }

        })

        // $('.spdx-delete-icon-main').on('click', function() {
        // 	console.log('xxx');
        // })
    });
</script>