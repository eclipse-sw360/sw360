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

define('components/includes/releases/spdxjs', ['jquery',"components/includes/releases/validateLib"] , function($,validateLib) {
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

        section.find('.always-enable').removeAttr('disabled');

        section.find('.spdx-radio').each(function () {
            $(this).parent().parent().find('input[type=text]').attr('disabled', 'true');

            $(this).parent().parent().find('select').attr('disabled', 'true');

            $(this).parent().parent().find('textarea').attr('disabled', 'true');
        });

        section.find('.spdx-add-button-main').removeAttr('disabled');
    }

    function clearSection(section) {
        section.find('input[type=text]').val('');

        section.find('textarea').val('');

        section.find('select').not('.spdx-select').prop("selectedIndex", 0).change();

        section.find('input[type=radio]').prop('checked', false);
    }

    function deleteMain(deleteBtn) {
        if ($(deleteBtn).css('cursor') == 'not-allowed') {
          return;
        }

        let selectbox = $(deleteBtn).prev('select');

        selectbox.find('option:selected').remove();

        if (selectbox.find('option').length == 0) {
          selectbox.attr('disabled', 'true');

          $(deleteBtn).css('cursor', 'not-allowed')
        }

        let newItem = selectbox.find('option:selected').val();

        if (typeof (newItem) == 'undefined') {
          section = selectbox.closest('.section');

          enableSection(section, false);

          clearSection(section);
        } else {
          selectbox.change();
        }
    }

    function deleteSub(deleteBtn) {
        if ($(deleteBtn).css('cursor') == 'not-allowed') {
          return;
        }

        let section = $(deleteBtn).parent().parent();

        if (section.find('.spdx-delete-icon-sub').length == 1) {
          $(deleteBtn).parent().css('display', 'none');

          $(deleteBtn).addClass('hidden');
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

        selectbox.append('<option>' + newIndex + '</option>');

        section = selectbox.closest('.section');

        enableSection(section, true);

        clearSection(section);

        selectbox.val(newIndex);
    }

    function addSub(addBtn)  {
        if ($(addBtn).prev().css('display') == 'none') {
            $(addBtn).prev().css('display', 'flex');

            $(addBtn).prev().find('[name=delete-snippetRange]').removeClass('hidden');

            $(addBtn).prev().find('[name=checksum-delete]').removeClass('hidden');

            clearSection($(addBtn).prev());

            $(addBtn).prev().find('*').removeAttr('disabled');

            $(addBtn).prev().find('.spdx-delete-icon-sub').css('cursor', 'pointer');

            if ($(addBtn).hasClass('spdx-add-button-sub-creator')) {
                if ($('#creator-anonymous').is(':checked')) {
                    $(addBtn).prev().find('.creator-type').val('Tool');
                } else {
                    $(addBtn).prev().find('.creator-type').val('Organization');
                }
            }
        } else {
            let newItem = $(addBtn).prev().clone();

            clearSection(newItem)

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

    function updateRadioButton(button) {
        if ($(button).attr('id') == 'FilesAnalyzedFalse' && $(button).is(':checked')) {
            $('#verificationCodeValue').attr('disabled', 'true');

            $('#excludedFiles').attr('disabled', 'true');

            $('#licenseInfoFromFilesExist').attr('disabled', 'true');

            $('#licenseInfoFromFilesValue').attr('disabled', 'true');

            $('#licenseInfoFromFilesNone').attr('disabled', 'true');

            $('#licenseInfoFromFilesNoAssertion').attr('disabled', 'true');

            return;
        }

        if ($(button).attr('id') == 'FilesAnalyzedTrue' && $(button).is(':checked')) {
            $('#verificationCodeValue').removeAttr('disabled');

            $('#excludedFiles').removeAttr('disabled');

            $('#licenseInfoFromFilesExist').removeAttr('disabled');

            $('#licenseInfoFromFilesNone').removeAttr('disabled');

            $('#licenseInfoFromFilesNoAssertion').removeAttr('disabled');

            if (!$('#licenseInfoFromFilesNone').is(':checked') && !$('#licenseInfoFromFilesNoAssertion').is(':checked')) {
                $('#licenseInfoFromFilesExist').click();
            }

            if ($('#licenseInfoFromFilesExist').is(':checked')) {
                $('#licenseInfoFromFilesValue').removeAttr('disabled');
            }

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

    function readDocumentCreator() {
        let creators = [];

        let index = 0;

        $('[name=creatorRow]').each(function () {
            if ($(this).css('display') == 'none') {
                return;
            }

            if ($(this).find('.creator-type').first().attr('disabled')) {
                return;
            }

            let creatorType = $(this).find('.creator-type').first().val().trim();

            let creatorValue = $(this).find('.creator-value').first().val().trim();

            if (creatorValue != '') {
                creators.push({ 'type': creatorType, 'value': creatorValue, 'index': index });
                index += 1;
            }
        });

        return creators;
    }

    function fillDateTime(datePicker, timePicker, value) {
        let timeStamp = Date.parse(value);

        let date = new Date(timeStamp);

        let localTimeStamp = timeStamp - date.getTimezoneOffset();

        let localDate = new Date(localTimeStamp);

        $(datePicker).val(localDate.getFullYear()
                  + '-' + (localDate.getMonth() + 1).toString().padStart(2, '0')
                  + '-' + localDate.getDate().toString().padStart(2, '0'));

        $(timePicker).val(date.getHours().toString().padStart(2, '0')
                  + ':' + date.getMinutes().toString().padStart(2, '0')
                  + ':' + date.getSeconds().toString().padStart(2, '0'));
    }

    function fillMultiOptionsField(inputTag, value, type = 'text') {
        if (type == 'array' && value.length == 1) {
            if (value[0].toUpperCase() == 'NONE' || value[0].toUpperCase() == 'NOASSERTION') {
                $(inputTag).val('');

                $(inputTag).parent().parent().find('input[value=' + value[0].toUpperCase() + ']').click();

                return;
            }
        }

        if (type != 'array' && (value.toUpperCase() == 'NONE' || value.toUpperCase() == 'NOASSERTION')) {
            $(inputTag)[0].selectedIndex = 0;

            $(inputTag).parent().find('input').val('');

            $(inputTag).parent().find('textarea').val('');

            $(inputTag).parent().parent().find('input[value=' + value.toUpperCase() + ']').click();
        } else {
            switch (type) {
                case 'array':
                    fillArray(inputTag, value);
                    break;
                case 'annotator':
                    fillAnnotator(inputTag, value);
                    break;
                case 'text':
                default:
                    $(inputTag).val(value);
                    $(inputTag).prev().click();
            }

            $(inputTag).parent().parent().find('input[value=EXIST]').click();
        }
    }

    function fillArray(textarea, value) {
        if (Array.isArray(value)) {
            $(textarea).val(value.sort().join('\n'));
        } else {
            $(textarea).val('');
        }
    }

    function fillAnnotator(typeTag, value) {
        if (value.startsWith('Organization: ')) {
            $(typeTag).val('Organization');

            $(typeTag).next().val(value.substr(14));
        } else if (value.startsWith('Person: ')) {
            $(typeTag).val('Person');

            $(typeTag).next().val(value.substr(8));
        } else if (value.startsWith('Tool: ')) {
            $(typeTag).val('Tool');

            $(typeTag).next().val(value.substr(6));
        } else {
            $(typeTag).val('Organization');

            $(typeTag).next().val('');
        }
    }

    function readMultiOptionField(inputTag, type = 'text') {
        if ($(inputTag).attr('disabled')) {
            if (type == 'array') {
                return [$(inputTag).parent().parent().find('[type=radio]:checked').val()];
            } else {
                return $(inputTag).parent().parent().find('[type=radio]:checked').val();
            }
        } else {
            switch (type) {
                case 'array':
                    return readArray(inputTag);
                case 'annotator':
                    return readAnnotator(inputTag);
                case 'text':
                default:
                    let result = $(inputTag).val().trim();

                    if (result.toUpperCase() == 'NONE' || result.toUpperCase() == 'NOASSERTION') {
                        return result.toUpperCase();
                    } else {
                        return result;
                    }
            }
        }
    }

    function readArray(textarea) {
        let result = $(textarea).val().split('\n');

        for (let i = 0; i < result.length; i++) {
          result[i] = result[i].trim();
        }

        result.filter(function(e) { return e !== '' }).sort();

        if (result.length == 1 && (result[0].toUpperCase() == 'NONE' || result[0].toUpperCase() == 'NOASSERTION')) {
            return [result[0].toUpperCase()];
        }

        return result.filter(function(v) { return v !=='' } );
    }

    function readAnnotator(typeTag) {
        let val = $(typeTag).parent().parent().find('.spdx-radio:checked').val();

        if (val != 'EXIST') {
            $(typeTag).parent().parent().find('[type=radio]:checked').val();
        }

        if ($(typeTag).next().val().trim() != '') {
            val = $(typeTag).val() + ': ' + $(typeTag).next().val().trim();
        } else {
            val = '';
        }

        return val;
    }

    function readDateTime(datePicker, timePicker) {
        if ($(datePicker).val() == '' || $(timePicker).val() == '') {
            return '';
        }

        let localDate = new Date($(datePicker).val() + ' ' + $(timePicker).val());

        return localDate.toISOString().slice(0, -5) + 'Z';
    }

    function fillSelectbox(selectbox, num) {
        $(selectbox).find('option').remove();

        for (let i = 0; i < num; i++) {
            $(selectbox).append('<option>' + (i + 1).toString() + '</option>');
        }

        if (num > 0) {
            $(selectbox).val(1);
        }
    }

    // --------------------------------- Document Creation ---------------------------------

    function initDocumentCreation(userDisplay) {
        if (documentCreationInformationObj['spdxVersion'].startsWith('SPDX-')) {
            $('#spdxVersion').val(documentCreationInformationObj['spdxVersion'].substr(5).trim());
        }
        else {
              $('#spdxVersion').val('');
        }
        if (documentCreationInformationObj['dataLicense'] == '') {
                $('#dataLicense').val('');
        }
        if (documentCreationInformationObj['SPDXID'].startsWith('SPDXRef-')) {
            $('#spdxIdentifier').val(documentCreationInformationObj['SPDXID'].substr(8).trim());
        }
        else {
             $('#spdxIdentifier').val('');
        }

        if (documentCreationInformationObj.externalDocumentRefs.length == 0) {
            enableSection($('.section-external-doc-ref'), false);
        } else {
            fillSelectbox('#externalDocumentRefs', documentCreationInformationObj.externalDocumentRefs.length);

            fillExternalDocRef(0);
        }

        if (documentCreationInformationObj.creator.length == 0) {
            $('.spdx-add-button-sub-creator').first().click();
            $('.creator-type').last().val('Person');
            $('.creator-value').last().val(userDisplay);
        } else {
            for (let i = 0; i < documentCreationInformationObj.creator.length; i++) {
                addSub($('.spdx-add-button-sub-creator').first());
                $('.creator-type').last().val(documentCreationInformationObj.creator[i].type);
                $('.creator-value').last().val(documentCreationInformationObj.creator[i].value);
            }
        }

        $('[name=delete-spdx-creator]').bind('click', function() {
            deleteSub($(this));
        });

        if (documentCreationInformationObj.created == '') {
            fillDateTime('#createdDate', '#createdTime', (new Date().toISOString()));
        } else {
            fillDateTime('#createdDate', '#createdTime', documentCreationInformationObj.created);
        }

        $('#creatorComment').val(documentCreationInformationObj['creatorComment'].trim());
        $('#documentComment').val(documentCreationInformationObj['documentComment'].trim());
    }

    function storeDocumentCreation() {
        if ($('#spdxVersion').val().trim() == '') {
              documentCreationInformationObj['spdxVersion'] = '';
        } else {
            documentCreationInformationObj['spdxVersion'] = 'SPDX-' + $('#spdxVersion').val().trim();
        }

        if ($('#dataLicense').val().trim() == '') {
            documentCreationInformationObj['dataLicense'] = '';
        } else {
            documentCreationInformationObj['dataLicense'] = $('#dataLicense').val().trim();
        }

        if ($('#spdxIdentifier').val().trim() == '') {
            documentCreationInformationObj['SPDXID'] = '';
        } else {
            documentCreationInformationObj['SPDXID'] = 'SPDXRef-' + $('#spdxIdentifier').val().trim();
        }

        documentCreationInformationObj['name'] = $('#documentName').val().trim();

        documentCreationInformationObj['documentNamespace'] = $('#documentNamespace').val().trim();

        documentCreationInformationObj['licenseListVersion'] = $('#licenseListVersion').val().trim();

        documentCreationInformationObj.creator = readDocumentCreator();

        documentCreationInformationObj['created'] = readDateTime('#createdDate', '#createdTime');

        documentCreationInformationObj['creatorComment'] = $('#creatorComment').val().trim();

        documentCreationInformationObj['documentComment'] = $('#documentComment').val().trim();

        if (documentCreationInformationObj['created'] == '') {
            documentCreationInformationObj['created'] = (new Date()).toISOString();
        }
    }

    // --------------------------------- External Document Reference ---------------------------------

    function fillExternalDocRef(index) {
        index = $('#externalDocumentRefs')[0].selectedIndex;

        let obj = documentCreationInformationObj.externalDocumentRefs[index];

        $('#externalDocumentId').val(obj['externalDocumentId']);

        $('#externalDocument').val(obj['spdxDocument']);

        $('#checksumAlgorithm').val(obj['checksum']['algorithm']);

        $('#checksumValue').val(obj['checksum']['checksumValue']);
    }

    function storeExternalDocRef(index) {
        if (index < 0 || index > documentCreationInformationObj.externalDocumentRefs.length - 1) {
            return;
        }

        let obj = documentCreationInformationObj.externalDocumentRefs[index];

        obj['externalDocumentId'] = $('#externalDocumentId').val().trim();

        obj['spdxDocument'] = $('#externalDocument').val().trim();

        let algorithm = $('#checksumAlgorithm').val().trim();

        let checksumValue = $('#checksumValue').val().trim();

        if (algorithm == '' || checksumValue == '') {
            obj['checksum']['algorithm'] = '';
            obj['checksum']['checksumValue'] = '';
        } else {
            obj['checksum']['algorithm'] = algorithm;
            obj['checksum']['checksumValue'] = checksumValue;
        }
    }

    // --------------------------------- Package Information ---------------------------------

    function initPackageInfo() {
        if (packagesInformationObj.length == 0) {
          enableSection($('.section-package'), false);
        } else {
          fillSelectbox('#selectPackage', packagesInformationObj.length);

          fillPackage(0);
        }
    }

    function fillPackage(index) {
        const packageInformationObj = packagesInformationObj[index]

        $('#packageName').val(packageInformationObj['name']);

        if (packageInformationObj.SPDXID.startsWith('SPDXRef-')) {
        $('#packageSPDXId').val(packageInformationObj.SPDXID.substr(8));
        } else {
            $('#packageSPDXId').val('Package-' + packageInformationObj['name']);
        }

        $('#versionInfo').val(packageInformationObj['versionInfo']);
        $('#packageFileName').val(packageInformationObj['packageFileName']);
        $('#sourceInfo').val(packageInformationObj['sourceInfo']);
        $('#licenseComments').val(packageInformationObj['licenseComments']);
        $('#summary').val(packageInformationObj['summary']);
        $('#description').val(packageInformationObj['description']);
        $('#spdxPackageComment').val(packageInformationObj['packageComment']);



        fillMultiOptionsField('#supplierType', packageInformationObj.supplier, 'annotator');

        fillMultiOptionsField('#originatorType', packageInformationObj.originator, 'annotator');

        fillMultiOptionsField('#downloadLocationValue', packageInformationObj.downloadLocation);

        if (packageInformationObj.filesAnalyzed) {
            $('#FilesAnalyzedTrue').click();

            $('#verificationCodeValue').val(packageInformationObj.packageVerificationCode.value);

            fillArray('#excludedFiles', packageInformationObj.packageVerificationCode.excludedFiles);
        } else {
            $('#FilesAnalyzedFalse').click();

            $('#verificationCodeValue').val('');

            $('#excludedFiles').val('');
        }

        if ($('[name=checksum-delete].hidden').length == 0) {
            const checksumsNum = $('[name=checksumRow]').length;

            for (let i = 0; i < checksumsNum; i++) {
                if (i == 0) {
                  $($('[name=checksumRow]')[i]).css('display', 'none');

                  $($('[name=checksumRow]')[i]).find('[name=checksum-delete]').addClass('hidden');

                  clearSection($($('[name=checksumRow]')[i]));
                } else {
                  $('[name=checksumRow]').last().remove();
                }
            }
        }

        for (let i = 0; i < packageInformationObj.checksums.length; i++) {
            addSub($('.spdx-add-button-sub-checksum').first());

            $('.checksum-delete').last().bind('click', function() {
                deleteSub($(this));
            });

            let algorithm   = packageInformationObj.checksums[i].algorithm;

            let checksumValue = packageInformationObj.checksums[i].checksumValue;

            $('.checksum-algorithm').last().val(algorithm);

            $('.checksum-value').last().val(checksumValue);
        }

        $('.checksum-algorithm, .checksum-value').bind('change keyup', function() {
            let selectedPackage = $('#selectPackage')[0].selectedIndex;
            if ($(this).is(":focus")) {
                //storePackageInfo(packageInformationObj.index);
                storePackageInfo(selectedPackage);
            }
        });

        $('.checksum-delete').bind('click', function() {
            let selectedPackage = $('#selectPackage')[0].selectedIndex;
            deleteSub($(this));
            storePackageInfo(selectedPackage);
            //storePackageInfo(packageInformationObj.index);
        });

        fillMultiOptionsField('#packageHomepageValue', packageInformationObj.homepage);

        fillMultiOptionsField('#licenseConcludedValue', packageInformationObj.licenseConcluded);

        fillMultiOptionsField('#licenseInfoFromFilesValue', packageInformationObj.licenseInfoFromFiles, 'array');

        fillMultiOptionsField('#licenseDeclaredValue', packageInformationObj.licenseDeclared);

        fillMultiOptionsField('#copyrightTextValue', packageInformationObj.copyrightText);

        if (packageInformationObj.externalRefs.length == 0) {
            enableSection($('.section-external-ref'), false);
            $('#externalReferences').empty();
        } else {
            fillSelectbox('#externalReferences', packageInformationObj.externalRefs.length);

            fillExternalRef(packageInformationObj, 0);
        }

        fillArray('#spdxPackageAttributionText', packageInformationObj.attributionText);

    }


    function storePackageInfo(packageIndex) {
        let packageInformationObj = packagesInformationObj[packageIndex];
        packageInformationObj['name'] = $('#packageName').val().trim();

        if ($('#packageSPDXId').val().trim() == '') {
            packageInformationObj['SPDXID'] = 'SPDXRef-Package-' + packageInformationObj['name'];
        } else {
            packageInformationObj['SPDXID'] = 'SPDXRef-' + $('#packageSPDXId').val().trim();
        }

        packageInformationObj['versionInfo'] = $('#versionInfo').val().trim();

        packageInformationObj['packageFileName'] = $('#packageFileName').val().trim();

        packageInformationObj['supplier'] = readMultiOptionField('#supplierType', 'annotator');

        packageInformationObj['originator'] = readMultiOptionField('#originatorType', 'annotator');

        packageInformationObj['downloadLocation'] = readMultiOptionField('#downloadLocationValue');

        packageInformationObj['filesAnalyzed'] = $('[name=_sw360_portlet_components_FILES_ANALYZED]:checked').val();

        if (packageInformationObj['filesAnalyzed'] == 'true') {
            packageInformationObj['packageVerificationCode']['value'] = $('#verificationCodeValue').val().trim();

            packageInformationObj['packageVerificationCode']['excludedFiles'] = readArray('#excludedFiles');
        } else {
            packageInformationObj['packageVerificationCode']['value'] = '';

            packageInformationObj['packageVerificationCode']['excludedFiles'] = '';
        }

        packageInformationObj['checksums'] = [];

        let index = 0;

        $('[name=checksumRow]').each(function() {
            let algorithm = $(this).find('.checksum-algorithm').first().val().trim();

            let checksumValue = $(this).find('.checksum-value').first().val().trim();

            if (algorithm !='' && checksumValue != '') {
                packageInformationObj['checksums'].push({ 'algorithm': algorithm, 'checksumValue': checksumValue, 'index': index });
                index += 1;
            }
        });

        packageInformationObj['homepage'] = readMultiOptionField('#packageHomepageValue');

        packageInformationObj['sourceInfo'] = $('#sourceInfo').val().trim();

        packageInformationObj['licenseConcluded'] = readMultiOptionField('#licenseConcludedValue');

        if (packageInformationObj['filesAnalyzed'] == 'true') {
            packageInformationObj['licenseInfoFromFiles'] = readMultiOptionField('#licenseInfoFromFilesValue', 'array');
        } else {
            packageInformationObj['licenseInfoFromFiles'] = [];
        }

        packageInformationObj['licenseDeclared'] = readMultiOptionField('#licenseDeclaredValue');

        packageInformationObj['licenseComments'] = $('#licenseComments').val().trim();

        packageInformationObj['copyrightText'] = readMultiOptionField('#copyrightTextValue');

        packageInformationObj['summary'] = $('#summary').val().trim();

        packageInformationObj['description'] = $('#description').val().trim();

        packageInformationObj['packageComment'] = $('#spdxPackageComment').val().trim();

        packageInformationObj['attributionText'] = readMultiOptionField('#spdxPackageAttributionText', 'array');
    }

    // --------------------------------- External Reference ---------------------------------

    function fillExternalRef(packageInformationObj, index) {
        let obj = packageInformationObj.externalRefs[index];
        $('#externalReferences').removeAttr('disabled');

        $('#referenceCategory').val(obj['referenceCategory']);

        $('#referenceCategory').change();
        $('#referenceCategory').removeAttr('disabled');

        if (obj['referenceCategory'] == 'SECURITY' || obj['referenceCategory'] == 'PACKAGE-MANAGER') {
            $('#referenceType-1').val(obj['referenceType']);
            $('#referenceType-1').removeAttr('disabled');

        } else {
            $('#referenceType-2').val(obj['referenceType']);
            $('#referenceType-2').removeAttr('disabled');

        }

        $('#externalReferencesLocator').val(obj['referenceLocator']);
        $('#externalReferencesLocator').removeAttr('disabled');


        $('#externalReferencesComment').val(obj['comment']);
        $('#externalReferencesComment').removeAttr('disabled');
    }

    function storeExternalRef(packageInformationObj, index) {
        if (index < 0 || index > packageInformationObj.externalRefs.length - 1) {
            return;
        }

        let obj = packageInformationObj.externalRefs[index];

        obj['referenceCategory'] = $('#referenceCategory').val().trim();

        if (obj['referenceCategory'] == 'SECURITY' || obj['referenceCategory'] == 'PACKAGE-MANAGER') {
            obj['referenceType'] = $('#referenceType-1').val().trim();
        } else {
            obj['referenceType'] = $('#referenceType-2').val().trim();
        }

        obj['referenceLocator'] = $('#externalReferencesLocator').val().trim();

        obj['comment'] = $('#externalReferencesComment').val().trim();
    }

    // --------------------------------- Snippet Information ---------------------------------

    function initSnippetInfo() {
        if (spdxDocumentObj.snippets.length == 0) {
          enableSection($('.section-snippet'), false);
        } else {
          fillSelectbox('#selectSnippet', spdxDocumentObj.snippets.length);

          fillSnippet(0);
        }
    }

    function fillSnippet(index) {
        const obj = spdxDocumentObj.snippets[index];

        if (obj['SPDXID'].startsWith('SPDXRef-')) {
            $('#snippetSpdxIdentifier').val(obj['SPDXID'].substr(8));
        } else {
            $('#snippetSpdxIdentifier').val('Snippet-' + obj['name']);
        }

        if (obj['snippetFromFile'].startsWith('SPDXRef-')) {
            $('#snippetFromFile').val('SPDXRef');

            $('#snippetFromFileValue').val(obj['snippetFromFile'].substr(8));
        } else if (obj['snippetFromFile'].startsWith('DocumentRef-')) {
            $('#snippetFromFile').val('DocumentRef');

            $('#snippetFromFileValue').val(obj['snippetFromFile'].substr(12));
        } else {
            $('#snippetFromFile').val('SPDXRef');

            $('#snippetFromFileValue').val('');
        }

        if ($('[name=delete-snippetRange].hidden').length == 0) {
            const rangesNum = $('[name=snippetRange]').length;

            for (let i = 0; i < rangesNum; i++) {
                if (i == 0) {
                  $($('[name=snippetRange]')[i]).css('display', 'none');

                  $($('[name=snippetRange]')[i]).find('[name=delete-snippetRange]').addClass('hidden');

                  clearSection($($('[name=snippetRange]')[i]));
                } else {
                  $('[name=snippetRange]').last().remove();
                }
            }
        }

        for (let i = 0; i < obj.snippetRanges.length; i++) {
            addSub('#addNewRange');

            $('.range-type').last().val(obj.snippetRanges[i].rangeType);

            $('.start-pointer').last().val(obj.snippetRanges[i].startPointer);

            $('.end-pointer').last().val(obj.snippetRanges[i].endPointer);

            $('.reference').last().val(obj.snippetRanges[i].reference);
        }

        $('.range-type, .start-pointer, .end-pointer, .reference').bind('change keyup', function() {
            if ($(this).is(":focus")) {
                storeSnippet();
            }
        });

        $('[name=delete-snippetRange]').bind('click', function() {
            deleteSub($(this));

            storeSnippet();
        });

        fillMultiOptionsField('#spdxConcludedLicenseValue', obj.licenseConcluded);

        fillMultiOptionsField('#licenseInfoInFileValue', obj.licenseInfoInSnippets, 'array');

        $('#snippetLicenseComments').val(obj.licenseComments);

        fillMultiOptionsField('#copyrightTextValueSnippet', obj.copyrightText);

        $('#snippetComment').val(obj.comment);

        $('#snippetName').val(obj.name);

        $('#snippetAttributionText').val(obj.snippetAttributionText);
    }

    function storeSnippet(index) {
        if (typeof(index) == 'undefined') {
            index = $('#selectSnippet')[0].selectedIndex;
        }

        if (index < 0 || index > spdxDocumentObj.snippets - 1) {
            return;
        }

        let obj = spdxDocumentObj.snippets[index];

        if ($('#snippetSpdxIdentifier').val().trim() != '') {
            obj['SPDXID'] = 'SPDXRef-' + $('#snippetSpdxIdentifier').val().trim();
        } else {
            obj['SPDXID'] = 'SPDXRef-Snippet-' + $('#snippetName').val().trim();
        }

        if ($('#snippetFromFileValue').val().trim() != '') {
            obj['snippetFromFile'] = $('#snippetFromFile').val() + '-' + $('#snippetFromFileValue').val().trim();
        } else {
            obj['snippetFromFile'] = '';
        }

        obj['snippetRanges'] = [];

        if ($('[name=snippetRange]').first().css('display') != 'none') {
            obj['snippetRanges'] = [];

            let index = 0;

            $('[name=snippetRange]').each(function() {
                let range = {'rangeType': '', 'startPointer': '', 'endPointer': '', 'reference': ''};

                range['rangeType'] = $(this).find('.range-type').first().val().trim();

                range['startPointer'] = $(this).find('.start-pointer').first().val().trim();

                range['endPointer'] = $(this).find('.end-pointer').first().val().trim();

                range['reference'] = $(this).find('.reference').first().val().trim();

                range['index'] = index;

                index += 1;

                if (range['startPointer'] != '' && range['endPointer'] != '' && range['reference'] != '') {
                    obj['snippetRanges'].push(range);
                }
            })
        }

        obj['licenseConcluded'] = readMultiOptionField('#spdxConcludedLicenseValue');

        obj['licenseInfoInSnippets']  = readMultiOptionField('#licenseInfoInFileValue', 'array');

        obj['licenseComments'] = $('#snippetLicenseComments').val().trim();

        obj['copyrightText'] = readMultiOptionField('#copyrightTextValueSnippet');

        obj['comment'] = $('#snippetComment').val().trim();

        obj['name'] = $('#snippetName').val().trim();

        obj['snippetAttributionText'] = $('#snippetAttributionText').val().trim();
    }

    // --------------------------------- Other Licensing ---------------------------------

    function initOtherLicensing() {
        if (spdxDocumentObj.otherLicensingInformationDetecteds.length == 0) {
            enableSection($('.section-other-licensing'), false);
        } else {
            fillSelectbox('#selectOtherLicensing', spdxDocumentObj.otherLicensingInformationDetecteds.length);

            fillOtherLicensing(0);
        }
    }

    function fillOtherLicensing(index) {
        let obj = spdxDocumentObj.otherLicensingInformationDetecteds[index];

        if (obj.licenseId.startsWith('LicenseRef-')) {
            $('#licenseId').val(obj.licenseId.substr(11));
        } else {
            $('#licenseId').val(obj.licenseName);
        }

        $('#extractedText').val(obj.extractedText);

        fillMultiOptionsField('#licenseName', obj.licenseName);

        fillArray('#licenseCrossRefs', obj.licenseCrossRefs);

        $('#licenseCommentOnOtherLicensing').val(obj.licenseComment);
    }

    function storeOtherLicensing(index) {
        if (index < 0 || index > spdxDocumentObj.otherLicensingInformationDetecteds - 1) {
            return;
        }

        let obj = spdxDocumentObj.otherLicensingInformationDetecteds[index];

        if ($('#licenseId').val().trim() != '') {
            obj['licenseId'] = 'LicenseRef-' + $('#licenseId').val().trim();
        } else {
            obj['licenseId'] = 'LicenseRef-' + readMultiOptionField('#licenseName');
        }

        obj['extractedText'] = $('#extractedText').val().trim();

        obj['licenseName'] = readMultiOptionField('#licenseName');

        obj['licenseCrossRefs'] = readArray('#licenseCrossRefs');

        obj['licenseComment'] = $('#licenseCommentOnOtherLicensing').val().trim();
    }

    // --------------------------------- Relationship ---------------------------------
    function getRelationshipsSource() {
        if ($('#selectRelationshipSource').val() == 'Package') {
            return packagesInformationObj[0].relationships;
        }

        return spdxDocumentObj.relationships;
    }


    function initRelationships() {
        let source = getRelationshipsSource();
        if (source.length == 0) {
            enableSection($('.section-relationship'), false);
        } else {
            fillSelectbox('#selectRelationship', source.length);

            fillRelationship(source, 0);
        }
    }

    function fillRelationship(sourceRelationship, index) {
        let obj = sourceRelationship[index];

        $('#spdxElement').val(obj.spdxElementId);

        $('#relationshipType').val(obj.relationshipType.toUpperCase());

        $('#relatedSPDXElement').val(obj.relatedSpdxElement);

        $('#relationshipComment').val(obj.relationshipComment);
    }

    function storeRelationship(index) {
        let source = getRelationshipsSource()
        if (index < 0 || index > source - 1) {
            return;
        }

        let obj = source[index];

        obj['spdxElementId'] = $('#spdxElement').val().trim();

        obj['relationshipType'] = $('#relationshipType').val().toUpperCase().trim();

        obj['relatedSpdxElement'] = $('#relatedSPDXElement').val().trim();

        obj['relationshipComment'] = $('#relationshipComment').val().trim();
    }

    // --------------------------------- Annotation ---------------------------------

    function getAnnotationsSource() {
        if ($('#selectAnnotationSource').val() == 'Package') {
            return packagesInformationObj[0].annotations;
        }

        return spdxDocumentObj.annotations;
    }

    function initAnnotations() {
        let source = getAnnotationsSource();

        if (source.length == 0) {
            enableSection($('.section-annotation'), false);

            $('#selectAnnotation').find('option').remove();
        } else {
            enableSection($('.section-annotation'), true);

            fillSelectbox('#selectAnnotation', source.length);

            fillAnnotation(source, 0);
        }
    }

    function fillAnnotation(source, index) {
        let obj = source[index];

        fillAnnotator('#annotatorType', obj['annotator']);

        fillDateTime('#annotationCreatedDate', '#annotationCreatedTime', obj['annotationDate']);

        $('#annotationType').val(obj['annotationType']);

        $('#spdxIdRef').val(obj['spdxIdRef']);

        $('#annotationComment').val(obj['annotationComment']);
    }

    function storeAnnotation(index) {
        let source = getAnnotationsSource();

        if (index < 0 || index > source.length - 1) {
            return;
        }

        let obj = source[index];

        if ($('#annotatorValue').val().trim() != '') {
            obj['annotator'] = $('#annotatorType').val() + ': ' + $('#annotatorValue').val().trim();
        } else {
            obj['annotator'] = '';
        }

        obj['annotationDate'] = readDateTime('#annotationCreatedDate', '#annotationCreatedTime');

        obj['annotationType'] = $('#annotationType').val().trim();

        obj['spdxIdRef'] = $('#spdxIdRef').val().trim();

        obj['annotationComment'] = $('#annotationComment').val().trim();
    }


    return {
        addMain: addMain,
        addSub: addSub,
        deleteMain: deleteMain,
        deleteSub: deleteSub,

        updateRadioButton: updateRadioButton,

        initDocumentCreation: initDocumentCreation,
        storeDocumentCreation: storeDocumentCreation,

        readDocumentCreator: readDocumentCreator,

        fillExternalDocRef: fillExternalDocRef,
        storeExternalDocRef: storeExternalDocRef,

        initPackageInfo: initPackageInfo,
        fillPackage: fillPackage,
        storePackageInfo: storePackageInfo,

        fillExternalRef: fillExternalRef,
        storeExternalRef: storeExternalRef,

        initSnippetInfo: initSnippetInfo,
        fillSnippet: fillSnippet,
        storeSnippet: storeSnippet,

        initOtherLicensing: initOtherLicensing,
        fillOtherLicensing: fillOtherLicensing,
        storeOtherLicensing: storeOtherLicensing,

        getRelationshipsSource: getRelationshipsSource,
        initRelationships: initRelationships,
        fillRelationship: fillRelationship,
        storeRelationship: storeRelationship,

        getAnnotationsSource: getAnnotationsSource,
        initAnnotations: initAnnotations,
        fillAnnotation: fillAnnotation,
        storeAnnotation: storeAnnotation
    };
});
