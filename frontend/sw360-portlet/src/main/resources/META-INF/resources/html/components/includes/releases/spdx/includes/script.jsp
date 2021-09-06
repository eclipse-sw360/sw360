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
            console.log($(this));
            $(this).parent().parent().find('input[type=text]').attr('disabled', 'true');
            $(this).parent().parent().find('select').attr('disabled', 'true');
            $(this).parent().parent().find('textarea').attr('disabled', 'true');
        });

        // Keep the main button Add enable
        section.find('.spdx-add-button-main').removeAttr('disabled');
    }

    function clearSection(section) {
        section.find('input[type=text]').val('');
        section.find('textarea').val('');

        //select box: first value
        section.find('select').not('.spdx-select').prop("selectedIndex", 0).change();

        //radio button: no selection
        section.find('input[type=radio]').prop('checked', false);
    }

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

        if (typeof (newItem) == 'undefined') {
            //Clear all textboxes and disable all textboxes/selectboxes/radio buttons/buttons

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

    function setAnonymous() {
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


    function changeCreatorType(selectbox) {
        if ($('#creator-anonymous').is(':checked') &&
            ($(selectbox).val() == 'Organization' || $(selectbox).val() == 'Person')) {
            $(selectbox).attr('disabled', 'true');
            $(selectbox).next().attr('disabled', 'true');
            $(selectbox).next().next().css('cursor', 'not-allowed');
        }
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
        if (!Array.isArray(value) && (value.toUpperCase() == 'NONE' || value.toUpperCase() == 'NOASSERTION')) {
            $(inputTag).parent().parent().find('input[value=' + value + ']').click();
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
            $(textarea).val(value.join('\n'));
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
            return $(inputTag).parent().parent().find(':checked').val()
        } else {
            switch (type) {
                case 'array':
                    return readArray(inputTag);
                case 'annotator':
                    return readAnnotator(inputTag);
                case 'text':
                default:
                    return $(inputTag).val().trim();
            }
        }
    }

    function readArray(textarea) {
        let result = $(textarea).val().split('\n');

        for (let i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }

        return result.filter(function(e) { return e !== '' });
    }

    function readAnnotator(typeTag) {
        let val = $(typeTag).parent().parent().find('.spdx-radio:checked').val();

        if (val == 'NONE' || val == 'NOASSERTION') {
            return val;
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

        return localDate.toISOString();
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

    let spdxDocument = '{  "_id": "686670907de6c5756666467f8f0021d4",  "_rev": "3-891816d105ebc9cf721a3b14d57a893f",  "type": "SPDXDocument",  "releaseId": "9dac73b43b334687bdbc6a3350a5212b",  "spdxDocumentCreationInfoId": "686670907de6c5756666467f8f00412a",  "spdxPackageInfoIds": [    "686670907de6c5756666467f8f006abb"  ],  "snippets": [    {      "SPDXID": "SPDXRef-Snippet",      "snippetFromFile": "SPDXRef-DoapSource",      "snippetRanges": [        {          "rangeType": "LINE",          "startPointer": "5",          "endPointer": "23",          "reference": "./src/org/spdx/parser/DOAPProject.java"        },        {          "rangeType": "BYTE",          "startPointer": "210",          "endPointer": "520",          "reference": "./src/org/spdx/parser/DOAPProject.java"        }      ],      "licenseConcluded": "GPL-2.0",      "licenseInfoInSnippets": [        "GPL-2.0"      ],      "licenseComments": "The concluded license was taken from package xyz, from which the snippet was copied into the current file. The concluded license information was found in the COPYING.txt file in package xyz.",      "copyrightText": "Copyright 2008-2010 John Smith",      "comment": "This snippet was identified as significant and highlighted in this Apache-2.0 file, when a commercial scanner identified it as being derived from file foo.c in package xyz which is licensed under GPL-2.0.",      "name": "from linux kernel",      "snippetAttributionText": "AAAAAAAAA"    },    {      "SPDXID": "SPDXRef-sdasdSnippet",      "snippetFromFile": "DocumentRef-SSL",      "snippetRanges": [        {          "rangeType": "BYTE",          "startPointer": "dsadas5",          "endPointer": "2dasda3",          "reference": "./src/org/spdx/parser/DdasdasdOAPProject.java"        },        {          "rangeType": "LINE",          "startPointer": "310",          "endPointer": "420",          "reference": "./src/org/spdx/pdasdarser/DOAPProject.java"        }      ],      "licenseConcluded": "GPL-2.0",      "licenseInfoInSnippets": [        "GPL-2.0", "GPL-2.1"      ],      "licenseComments": "The concluded licensedasdas was taken from package xyz, from which the snippet was copied into the current file. The concluded license information was found in the COPYING.txt file in package xyz.",      "copyrightText": "Copyright 2008-2010 John Smith",      "comment": "This snippet was identified adasdas significant and highlighted in this Apache-2.0 file, when a commercial scanner identified it as being derived from file foo.c in package xyz which is licensed under GPL-2.0.",      "name": "from linux kernel",      "snippetAttributionText": "AAAAdasdaAAAAA"    }  ],  "relationships": [    {      "spdxElementId": "SPDXRef-File",      "relationshipType": "relationshipType_describes",      "relatedSpdxElement": "./package/foo.c",      "relationshipComment": "AAAAAÂÂÂAÂAA"    },    {      "spdxElementId": "SPDXRef-Package",      "relationshipType": "relationshipType_contains",      "relatedSpdxElement": "glibc"    },    {      "spdxElementId": "SPDXRef-Package",      "relationshipType": "relationshipType_describes",      "relatedSpdxElement": "glibc"    }  ],  "annotations": [    {      "annotator": "Organization: TSDV (mail.com)",      "annotationDate": "2010-01-29T17:30:22Z",      "annotationType": "PERSON",      "annotationComment": "Document level annotation",      "spdxRef": "spdxRef"    },    {      "annotator": "Person: Jane ddsdsDoe ()",      "annotationDate": "2011-01-29T18:30:22Z",      "annotationType": "OTHER",      "annotationComment": "Document sdsdlevel annotation",      "spdxRef": "sdsdpdxRef"    }  ],  "hasExtractedLicensingInfos": [    {      "licenseId": "LicenseRef-Beerware-4.2",      "extractedText": "2010-01-29T18:30:22Z",      "licenseName": "Beerware-4.2",      "licenseCrossRefs": [        "Document level annotation",        "AAAAAAA"      ],      "licenseComment": "spdxRef"    },    {      "licenseId": "LicenseRef-3",      "extractedText": "2010-01-2sdasda9T18:30:22Z",      "licenseName": "NOASSERTION",      "licenseCrossRefs": [        "Document level annosdsadasdtation",        "AAAdasdasdAAAA"      ],      "licenseComment": "spdxdsdRef"    }  ]}';

    let packageInformation = '{  "_id": "686670907de6c5756666467f8f006abb",  "_rev": "1-043aec01acd64ca318827fae0e19928d",  "type": "packageInformation",  "spdxDocumentId": "8363de6a29aa4658b38a1016671761aa",  "name": "glibc",  "SPDXID": "SPDXRef-Package",  "versionInfo": "2.11.1",  "packageFileName": "glibc-2.11.1.tar.gz",  "supplier": "Person: Jane Doe (jane.doe@example.com)",  "originator": "Organization: ExampleCodeInspect (contact@example.com)",  "downloadLocation": "http://ftp.gnu.org/gnu/glibc/glibc-ports-2.15.tar.gz",  "filesAnalyzed": true,  "packageVerificationCode": {    "excludedFiles": [      "excludes: ./package.spdx",      "AAAAAAAAAAAAAAAAAAAAAAAAA",      "SSSSSSSSSSSSSSSSSSSSSSSSS"    ],    "value": "d6a770ba38583ed4bb4525bd96e50461655d2758"  },  "checksums": [    {      "algorithm": "checksumAlgorithm_md5",      "checksumValue": "111111111111111"    },    {      "algorithm": "checksumAlgorithm_sha512",      "checksumValue": "2222222222222"    }  ],  "homepage": "http://ftp.gnu.org/gnu/glibc",  "sourceInfo": "uses glibc-2_11-branch from git://sourceware.org/git/glibc.git.",  "licenseConcluded": "ewqewqeeeeee",  "licenseInfoFromFiles": [    "GPL-2.0",    "LicenseRef-2",    "LicenseRef-1"  ],  "licenseDeclared": "(LicenseRef-3 AND LGPL-2.0)",  "licenseComments": "The license for this project changed with the release of version x.y.  The version of the project included here post-dates the license change.",  "copyrightText": "Copyright 2008-2010 John Smith",  "summary": "GNU C library.",  "description": "The GNU C Library defines functions that are specified by the ISO C standard, as well as additional features specific to POSIX and other derivatives of the Unix operating system, and extensions specific to GNU systems.", "packageComment": "this is package comment", "externalRefs": [    {      "referenceCategory": "referenceCategory_other",      "referenceLocator": "acmecorp/acmenator/4.1.3-alpha",      "referenceType": "http://spdx.org/spdxdocs/spdx-example-444504E0-4F89-41D3-9A0C-0305E82C3301#LocationRef-acmeforge",      "comment": "This is the external ref for Acme"    },    {      "referenceCategory": "referenceCategory_security",      "referenceLocator": "cpe:2.3:a:pivotal_software:spring_framework:4.1.0:*:*:*:*:*:*:*",      "referenceType": "cpe23Type","comment":""    }  ],  "attributionText": "",  "annotations": [    {      "annotator": "Person: Package Commenter",      "annotationDate": "2011-01-29T18:30:22Z",      "annotationType": "OTHER",      "annotationComment": "Package level annotation" , "spdxRef": "spdxRef"   },    {      "annotator": "Person: Packdsdsdsge Commenter",      "annotationDate": "2011-11-29T18:30:22Z",      "annotationType": "OTHER",      "annotationComment": "Package levedsadasdaddasl annotation"    }  ],  "issetBitfield": "1"}';

    let documentCreationInformation = '{  "_id": "686670907de6c5756666467f8f00412a",  "_rev": "1-29bfb0d62c91a5edfd7a06977c9ea3ee",  "type": "documentCreationInformation",  "spdxDocumentId": "8363de6a29aa4658b38a1016671761aa",  "spdxVersion": "SPDX-2.0",  "SPDXID": "SPDXRef-Sample",  "dataLicense": "CC0-1.0",  "name": "SPDX-Tools-v2.0",  "documentNamespace": "http://spdx.org/spdxdocs/spdx-example-444504E0-4F89-41D3-9A0C-0305E82C3301#",  "externalDocumentRefs": [    {      "externalDocumentId": "DocumentRef-spdx-tool-1.2",      "checksum": {        "algorithm": "checksumAlgorithm_sha1",        "checksumValue": "d6a770ba38583ed4bb4525bd96e50461655d2759"      },      "spdxDocument": "http://spdx.org/spdxdocs/spdx-tools-v1.2-3F2504E0-4F89-41D3-9A0C-0305E82C3301"    },    {      "externalDocumentId": "DocumentRgsdgsdgef-spdx-tool-1.2",      "checksum": {        "algorithm": "checksumAlgorithm_md5",        "checksumValue": "d6a770ba38583edsdgsdg4bb4525bd96e50461655d2759"      },      "spdxDocument": "http://spdx.org/spdxdocs/spgdgsdgdx-tools-v1.2-3F2504E0-4F89-41D3-9A0C-0305E82C3302"    }  ],  "licenseListVersion": "1.19",  "creator": [    {      "type": "Organization",      "value": "ExampleCodeInspect ()"    },    {      "type": "Tool",      "value": "LicenseFind-1.0"    },    {      "type": "Person",      "value": "Jane Doe ()"    }  ],  "created": "2010-01-29T18:30:22Z",  "creatorComment": "This package has been shipped in source and binary form.The binaries were created with gcc 4.5.1 and expect to link tocompatible system run time libraries.",  "documentComment": "This document was created using SPDX 2.0 using licenses from the web site.",  "createdBy": "admin@sw360.org"}';

</script>