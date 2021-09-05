<core_rt:set var="snippets" value="${spdxDocument.snippets}" />
<table class="table" id="editSnippetInformation">
    <thead>
        <tr>
            <th>5. Snippet Information</th>
        </tr>
    </thead>
    <tbody class="section">
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectSnippet" style="text-decoration: underline;" class="sub-title">Select
                            Snippet</label>
                        <select id="selectSnippet" type="text" class="form-control spdx-select"
                            onchange="generateSnippetTable($(this).find('option:selected').text())">
                        </select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-spdxCreatorType-Person"
                            data-row-id="" onclick="deleteMain(this)" viewBox="0 0 512 512">
                            <title><liferay-ui:message key="delete" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main" onclick="addMain(this)">Add new Snippet</button>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="snippetSpdxIdentifier">
                        5.1 Snippet SPDX Identifier
                    </label>
                    <div style="display: flex">
                        <label class="sub-label">SPDXRef-</label>
                        <input id="snippetSpdxIdentifier" class="form-control needs-validation"
                            rule="regex:^[0-9a-zA-Z.-]+$" name="_sw360_portlet_components_SPDXSPDX_IDENTIFIER"
                            type="text" placeholder="Enter Snippet SPDX Identifier" value="">
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="snippetFromFile">
                        5.2 Snippet from File SPDX Identifier
                    </label>
                    <div style="display: flex">
                        <select id="snippetFromFile" class="form-control" style="flex: 1;">
                            <option>SPDXRef</option>
                            <option>DocumentRef</option>
                        </select>
                        <div style="margin: 0.5rem;">-</div>
                        <input style="flex: 3;" id="snippetFromFileValue" class="form-control needs-validation"
                            rule="regex:^[0-9]+\.[0-9]+$" name="_sw360_portlet_components_SPDXSPDX_VERSION" type="text"
                            placeholder="Enter Snippet from File SPDX Identifier" value="">
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">
                        5.3 & 5.4 Snippet Ranges
                    </label>
                    <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                        <div style="display: flex; margin-bottom: 0.75rem;" name="snippetRanges">
                            <select style="flex: 1; margin-right: 1rem;" type="text" class="form-control"
                                placeholder="Enter Type">
                                <option value="BYTE" selected>Byte</option>
                                <option value="LINE">Line</option>
                            </select>
                            <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                                placeholder="Enter Start Pointer">
                            <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                                placeholder="Enter End Pointer">
                            <input style="flex: 4; margin-right: 2rem;" type="text" class="form-control"
                                placeholder="Enter Reference">
                            <svg class="lexicon-icon spdx-delete-icon-sub" name="delete-snippetRange" data-row-id=""
                                onclick="deleteSub(this)" viewBox="0 0 512 512">
                                <title><liferay-ui:message key="delete" /></title>
                                <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                            </svg>
                        </div>
                        <button class="spdx-add-button-sub" onclick="addSub(this)">Add new Range</button>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">5.5 Snippet Concluded License</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="spdxConcludedLicenseExist" type="radio"
                                name="_sw360_portlet_components_CONCLUDED_LICENSE" value="exist">
                            <input style="flex: 6; margin-right: 1rem;" id="spdxConcludedLicenseValue"
                                class="form-control needs-validation" type="text"
                                name="_sw360_portlet_components_CONCLUDED_LICENSE_VALUE"
                                placeholder="Enter Snippet Concluded License">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="spdxConcludedLicenseNone" type="radio"
                                name="_sw360_portlet_components_CONCLUDED_LICENSE" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="spdxConcludedLicenseNone">NONE</label>
                            <input class="spdx-radio" id="spdxConcludedLicenseNoAssertion" type="radio"
                                name="_sw360_portlet_components_CONCLUDED_LICENSE" value="NOASSERTION">
                            <label class="form-check-label radio-label"
                                for="spdxConcludedLicenseNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">5.6 License Information in Snippet</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseInfoInFile" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="exist">
                            <textarea style="flex: 6; margin-right: 1rem;" id="licenseInfoInFileValue" rows="5"
                                class="form-control needs-validation" type="text"
                                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE_SOURCE"
                                placeholder="Enter License Information in Snippet"></textarea>
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseInfoInFileNone" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="licenseInfoInFileNone">NONE</label>
                            <input class="spdx-radio" id="licenseInfoInFileNoAssertion" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="NOASSERTION">
                            <label class="form-check-label radio-label"
                                for="licenseInfoInFileNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="snippetLicenseComments">5.7 Snippet Comments on License</label>
                    <textarea class="form-control" id="snippetLicenseComments" rows="5"
                        name="_sw360_portlet_components_SNIPPET_LICENSE_COMMENTS"
                        placeholder="Enter Snippet Comments on License"></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">5.8 Copyright Text</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="snippetCopyrightText" type="radio"
                                name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="exist">
                            <textarea style="flex: 6; margin-right: 1rem;" id="copyrightTextValueSnippet" rows="5"
                                class="form-control needs-validation" type="text"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT_VALUE"
                                placeholder="Enter Copyright Text"></textarea>
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="snippetCopyrightTextNone" type="radio"
                                name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="snippetCopyrightTextNone">NONE</label>
                            <input class="spdx-radio" id="snippetCopyrightTextNoAssertion" type="radio"
                                name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NOASSERTION">
                            <label class="form-check-label radio-label"
                                for="snippetCopyrightTextNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="snippetComment">5.9 Snippet Comments</label>
                    <textarea class="form-control" id="snippetComment" rows="5"
                        name="_sw360_portlet_components_SNIPPET_COMMENT"
                        placeholder="Enter Snippet Comments"></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="snippetName">
                        5.10 Snippet Name
                    </label>
                    <input id="snippetName" name="_sw360_portlet_components_SPDXSPDX_NAME" type="text"
                        class="form-control" placeholder="Enter Snippet Name" value="">
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="snippetAttributionText">5.11 Snippet Attribution Text</label>
                    <textarea class="form-control" id="snippetAttributionText" rows="5"
                        name="_sw360_portlet_components_SNIPPET_ATTRIBUTION_TEXT"
                        placeholder="Enter Snippet Attribution Text"></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>
   function initSnippetInfo() {
        if (spdxDocumentObj.snippets.length == 0) {
            enableSection($('.section-snippet'), false);
        } else {
            fillSelectbox('#selectSnippet', spdxDocumentObj.snippets.length);
            fillSnippet(0);
        }
    }

    // ------------------------- 5 Snippet Information
    // Add data
    $('[name=add-snippet]').on('click', function(e) {
        let newObj = { 'SPDXID': '', 'snippetFromFile': '', 'snippetRanges': [], 'licenseConcluded': [], 'licenseInfoInSnippets': [], 'licenseComments': '', 'copyrightText': '', 'comment': '', 'name': '', 'snippetAttributionText': ''};
        spdxDocumentObj.snippets.push(newObj);
        addMain($(this));
        $('#selectSnippet').change();
    });

    // Delete data
    $('[name=delete-snippet').on('click', function(e) {
        let selectedIndex = $('#selectSnippet')[0].selectedIndex;
        spdxDocumentObj.snippets.splice(selectedIndex, 1);
        deleteMain($(this));
    });

    // Change data
    $('#selectSnippet').on('change', function(e) {
        let selectedIndex = $('#selectSnippet')[0].selectedIndex;
        fillSnippet(selectedIndex);
    });

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

        // Check to clear all current ranges
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

    $('#addNewRange').on('click', function() {
        addSub($(this));

        $('[name=delete-snippetRange]').bind('click', function() {
            deleteSub($(this));

            storeSnippet();
        });

        $('.range-type, .start-pointer, .end-pointer, .reference').bind('change keyup', function() {
            if ($(this).is(":focus")) {
                storeSnippet();
            }
        });
    });

    function storeSnippet(index) {
        if (typeof(index) == 'undefined') {
            index = $('#selectSnippet')[0].selectedIndex;
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

            $('[name=snippetRange]').each(function() {
                let range = {'rangeType': '', 'startPointer': '', 'endPointer': '', 'reference': ''};

                range['rangeType'] = $(this).find('.range-type').first().val().trim();
                range['startPointer'] = $(this).find('.start-pointer').first().val().trim();
                range['endPointer'] = $(this).find('.end-pointer').first().val().trim();
                range['reference'] = $(this).find('.reference').first().val().trim();

                if (range['startPointer'] != '' || range['endPointer'] != '' || range['reference'] != '') {
                    obj['snippetRanges'].push(range);
                }
            })
        }

        obj['licenseConcluded']       = readMultiOptionField('#spdxConcludedLicenseValue');
        obj['licenseInfoInSnippets']  = readMultiOptionField('#licenseInfoInFileValue', 'array');
        obj['licenseComments']        = $('#snippetLicenseComments').val().trim();
        obj['copyrightText']          = readMultiOptionField('#copyrightTextValueSnippet');
        obj['comment']                = $('#snippetComment').val().trim();
        obj['name']                   = $('#snippetName').val().trim();
        obj['snippetAttributionText'] = $('#snippetAttributionText').val().trim();
    }
</script>