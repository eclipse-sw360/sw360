<core_rt:set var="otherLicensing" value="${spdxDocument.otherLicensingInformationDetecteds}" />
<table class="table" id="editOtherLicensingInformationDetected">
    <thead>
        <tr>
            <th colspan="3">6. Other Licensing Information Detected</th>
        </tr>
    </thead>
    <tbody class="section">
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectOtherLicensing" style="text-decoration: underline;" class="sub-title">Select
                            Other Licensing</label>
                        <select id="selectOtherLicensing" type="text" class="form-control spdx-select"
                            onchange="generateOtherLicensingTable($(this).find('option:selected').text())">
                        </select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-spdxCreatorType-Person"
                            data-row-id="" onclick="deleteMain(this)" viewBox="0 0 512 512">
                            <title><liferay-ui:message key="delete" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main" onclick="addMain(this)">Add new Licensing</button>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="licenseId">
                        6.1 License Identifier
                    </label>
                    <div style="display: flex">
                        <label class="sub-label">LicenseRef-</label>
                        <input id="licenseId" class="form-control needs-validation"
                            name="_sw360_portlet_components_LICENSE_ID" type="text"
                            placeholder="Enter License Identifier" value="">
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="extractedText">6.2 Extracted Text</label>
                    <textarea class="form-control" id="extractedText" rows="5"
                        name="_sw360_portlet_components_EXTRACTED_TEXT" placeholder="Enter Extracted Text"></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label class="mandatory">6.3 License Name</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseNameExist" type="radio"
                                name="_sw360_portlet_components_LICENSE_NAME" value="exist">
                            <input style="flex: 6; margin-right: 1rem;" id="licenseName"
                                class="form-control needs-validation" rule="isDownloadUrl" type="text"
                                name="_sw360_portlet_components_LICENSE_NAME_VALUE" placeholder="Enter License Name">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseNameNoAssertion" type="radio"
                                name="_sw360_portlet_components_LICENSE_NAME" value="NOASSERTION">
                            <label class="form-check-label radio-label" for="licenseNameNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="licenseCrossRefs">6.4 License Cross Reference</label>
                    <textarea class="form-control" id="licenseCrossRefs" rows="5"
                        name="_sw360_portlet_components_LICENSE_CROSS_REFERENCE"
                        placeholder="Enter License Cross Reference"></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="licenseComment">6.5 License Comment</label>
                    <textarea class="form-control" id="licenseCommentOnOtherLicensing" rows="5"
                        name="_sw360_portlet_components_LICENSE_COMMENT" placeholder="Enter License Comment"></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>
    function initOtherLicensing() {
        if (spdxDocumentObj.hasExtractedLicensingInfos.length == 0) {
            enableSection($('.section-other-licensing'), false);
        } else {
            fillSelectbox('#selectOtherLicensing', spdxDocumentObj.hasExtractedLicensingInfos.length);
            fillOtherLicensing(0);
        }
    }

    // ------------------------- 6 Other Licensing Information Detected
    // Add data
    $('[name=add-otherLicensing]').on('click', function(e) {
        let newObj = { 'licenseId': '', 'extractedText': '', 'licenseName': '', 'licenseCrossRefs': [] };
        spdxDocumentObj.hasExtractedLicensingInfos.push(newObj);
        addMain($(this));
        $('#selectOtherLicensing').change();
    });

    // Delete data
    $('[name=delete-otherLicensing').on('click', function(e) {
        let selectedIndex = $('#selectOtherLicensing')[0].selectedIndex;
        spdxDocumentObj.hasExtractedLicensingInfos.splice(selectedIndex, 1);
        deleteMain($(this));
    });

    // Change data
    $('#selectOtherLicensing').on('change', function(e) {
        let selectedIndex = $('#selectOtherLicensing')[0].selectedIndex;
        fillOtherLicensing(selectedIndex);
    });

    function fillOtherLicensing(index) {
        let obj = spdxDocumentObj.hasExtractedLicensingInfos[index];

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
        let obj = spdxDocumentObj.hasExtractedLicensingInfos[index];

        if ($('#licenseId').val().trim() != '') {
            obj['licenseId'] = 'LicenseRef-' + $('#licenseId').val().trim();
        } else {
            obj['licenseId'] = 'LicenseRef-' + readMultiOptionField('#licenseName');
        }

        obj['extractedText'] = $('#extractedText').val().trim();

        obj['licenseName'] = readMultiOptionField('#licenseName');
    }
</script>