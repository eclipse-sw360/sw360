<style>
    /*-------------------ADD-------------------*/

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

        cursor: pointer;
    }

    .spdx-delete-icon-sub {
        margin-top: 0.3rem;
        margin-right: 4rem;
        width: 1rem;
        height: auto;

        cursor: pointer;
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

    .spdx-table .form-group {
        margin-right: 1rem;
    }

    .spdx-table textarea {
        margin-left: 0 !important;
    }

    .spdx-table .form-control {
        margin-left: 0 !important;
    }

    #spdxLiteMode {
        margin-left: -1px !important;
    }

</style>

<script>
    function enableSection(section, state) {
        if (!state) {
            section.find('button').attr('disabled', 'disabled');
            section.find('select').attr('disabled', 'disabled');
            section.find('input').attr('disabled', 'disabled');
            section.find('textarea').attr('disabled', 'disabled');

            section.find('.spdx-delete-icon-main').css('cursor', 'not-allowed');
            section.find('.spdx-delete-icon-sub').css('cursor', 'not-allowed');
        } else {
            section.find('button').removeAttr('disabled');
            section.find('select').removeAttr('disabled');
            section.find('input').removeAttr('disabled');
            section.find('textarea').removeAttr('disabled');

            section.find('select').removeAttr('disabled');

            section.find('.spdx-delete-icon-main').css('cursor', 'pointer');
            section.find('.spdx-delete-icon-sub').css('cursor', 'pointer');
        }

        section.find('.spdx-radio').each(function () {
            $(this).parent().parent().find('input[type=text]').attr('disabled', 'true');
            $(this).parent().parent().find('select').attr('disabled', 'true');
            $(this).parent().parent().find('textarea').attr('disabled', 'true');
        });

        // Keep the main button Add enable
        section.find('.spdx-add-button-main').removeAttr('disabled');
    }

    function clearSection(section) {
        section.find('input').val('');
        section.find('textarea').val('');

        //select box: first value
        section.find('select').not('.spdx-select').prop("selectedIndex", 0).change();

        //radio button: no selection
        section.find('input[type=radio]').prop('checked', false);
    }

    $('#spdxFullMode').on('click', function (e) {
    //    e.preventDefault();

        $(this).addClass('btn-info');
        $(this).removeClass('btn-secondary');

        $('#spdxLiteMode').addClass('btn-secondary');
        $('#spdxLiteMode').removeClass('btn-info');

        $('.spdx-full').css('display', '');
    });

    $('#spdxLiteMode').on('click', function (e) {
    //    e.preventDefault();

        $(this).addClass('btn-info');
        $(this).removeClass('btn-secondary');

        $('#spdxFullMode').addClass('btn-secondary');
        $('#spdxFullMode').removeClass('btn-info');

        $('.spdx-full').css('display', 'none');
    });

    // $('.spdx-add-button-main').on('click', function (e) {
    //     e.preventDefault();
    // })

    // $('.spdx-add-button-sub').on('click', function (e) {
    //     e.preventDefault();
    // })

    function deleteMain(deleteBtn) {
        if ($(deleteBtn).css('cursor') == 'not-allowed') {
            return;
        }

        let selectbox = $(deleteBtn).prev('select');

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

            section = selectbox.closest('.section');

            enableSection(section, false);

            clearSection(section);
        }
    }

    function deleteSub(deleteBtn) {
        if ($(deleteBtn).css('cursor') == 'not-allowed') {
            return;
        }

        let section = $(deleteBtn).parent().parent();

        if (section.find('.spdx-delete-icon-sub').length == 1) {
            $(deleteBtn).parent().css('display', 'none');
        } else {
            $(deleteBtn).parent().remove();
        }
    }

    function addMain(addBtn) {
        let selectbox = $(addBtn).prev().find('select');

        let newIndex = parseInt(selectbox.find('option').last().val()) + 1;

        if (isNaN(newIndex)) {
            newIndex = 1;
        }

        selectbox.append('<option value="' + newIndex + '">' + newIndex + '</option>');

        selectbox.val(newIndex);

        section = selectbox.closest('.section');

        enableSection(section, true);

        clearSection(section);
    }

    function addSub(addBtn) {
        if ($(addBtn).prev().css('display') == 'none') {
            $(addBtn).prev().css('display', 'flex');
        } else {
            let newItem = $(addBtn).prev().clone();
            newItem.find('input').val('');
            newItem.find('*').removeAttr('disabled');
            newItem.find('.spdx-delete-icon-sub').css('cursor', 'pointer');

            if ($(addBtn).hasClass('spdx-add-button-sub-creator')) {
                if ($('#creator-anonymous').is(':checked')) {
                    newItem.find('.creator-type').val('Tool');
                } else {
                    newItem.find('.creator-type').val('Organization');
                }
            }

            $(addBtn).before(newItem);
        }
    }

    function setAnonymous(checkbox) {
        let selectboxes = $('#creator-anonymous').parent().next().find('select');

        if ($('#creator-anonymous').is(':checked')) {
            selectboxes.each(function (index) {
                if ($(this).val() == 'Organization' || $(this).val() == 'Person') {
                    $(this).attr('disabled', 'true');
                    $(this).next().attr('disabled', 'true');
                    $(this).next().next().css('cursor', 'not-allowed');
                }
            });
        } else {
            selectboxes.each(function (index) {
                if ($(this).val() == 'Organization' || $(this).val() == 'Person') {
                    $(this).removeAttr('disabled');
                    $(this).next().removeAttr('disabled');
                    $(this).next().next().css('cursor', 'pointer');
                }
            });
        }
    }

    function updateRadioButton(button) {
        if ($(button).attr('id') == 'FilesAnalyzedFalse' && $(button).is(':checked')) {
            $('#verificationCodeValue').attr('disabled', 'true');
            $('#excludedFiles').attr('disabled', 'true');
            return;
        }

        if ($(button).attr('id') == 'FilesAnalyzedTrue' && $(button).is(':checked')) {
            $('#verificationCodeValue').removeAttr('disabled');
            $('#excludedFiles').removeAttr('disabled');
            return;
        }

        if (button.val() == 'NONE' || button.val() == 'NOASSERTION') {
            button.parent().parent().find('input[type=text]').attr('disabled', 'true');
            button.parent().parent().find('select').attr('disabled', 'true');
            button.parent().parent().find('textarea').attr('disabled', 'true');
        } else {
            button.parent().parent().find('input[type=text]').removeAttr('disabled');
            button.parent().parent().find('select').removeAttr('disabled');
            button.parent().parent().find('textarea').removeAttr('disabled');
        }
    }


    $('.spdx-radio').on('change', function () {
        updateRadioButton($(this));
    });

    function changeCreatorType(selectbox) {
        if ($('#creator-anonymous').is(':checked') &&
            ($(selectbox).val() == 'Organization' || $(selectbox).val() == 'Person')) {
            $(selectbox).attr('disabled', 'true');
            $(selectbox).next().attr('disabled', 'true');
            $(selectbox).next().next().css('cursor', 'not-allowed');
        }
    }

    const referenceCategories = {
        'SECURITY': ['cpe22Type', 'cpe23Type'],
        'PACKAGE-MANAGER': ['maven-central', 'npm', 'nuget', 'bower', 'purl'],
        'PERSISTENT-ID': [],
        'OTHER': []
    }

    $('#referenceCategory').on('change', function () {
        let category = $('#referenceCategory').val();
        let types = referenceCategories[category];

        if (types.length > 0) {
            $("#referenceType-1").css('display', 'block');
            $("#referenceType-1").val(types[0]);
            $("#referenceType-2").css('display', 'none');

            $("#referenceType-1").empty();

            for (let i = 0; i < types.length; i++) {
                let option = '<option>' + types[i] + '</option>';
                $("#referenceType-1").append(option);
            }
        } else {
            $("#referenceType-1").css('display', 'none');
            $("#referenceType-2").css('display', 'block');
            $("#referenceType-2").val('');
        }
    });

    $('.spdx-radio').each(function () {
        updateRadioButton($(this));
    })

    $(function () {
        // Expand/collapse section when click on the header
        $('thead').on('click', function () {
            if ($(this).next().css('display') == 'none') {
                $(this).next().css('display', '');
            } else {
                $(this).next().css('display', 'none');
            }
        })
    });

</script>