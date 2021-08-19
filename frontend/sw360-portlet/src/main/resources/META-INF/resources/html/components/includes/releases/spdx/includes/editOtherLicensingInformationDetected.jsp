<core_rt:set var="otherLicensing" value="${spdxDocument.otherLicensingInformationDetecteds}" />
<table class="table" id="editOtherLicensingInformationDetected">
    <thead>
        <tr>
            <th colspan="3">6. Other Licensing Information Detected</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectOtherLicensing" style="text-decoration: underline;" class="sub-title">Select
                            Other Licensing</label>
                        <select id="selectOtherLicensing" type="text" class="form-control spdx-select"
                            onchange="generateOtherLicensingTable($(this).find('option:selected').text())">
                            <option>1</option>
                        </select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-spdxCreatorType-Person"
                            data-row-id="" onclick="removeRow(this);" viewBox="0 0 512 512">
                            <title>Delete</title>
                            <path class="lexicon-icon-outline lx-trash-body-border"
                                d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z">
                            </path>
                            <polygon class="lexicon-icon-outline lx-trash-lid"
                                points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 ">
                            </polygon>
                            <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6" width="63.9"
                                height="191.6"></rect>
                            <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6" width="63.9"
                                height="191.6"></rect>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main">Add new Licensing</button>
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
        <tr>
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
    generateOtherLicensingTable('1');
    function generateOtherLicensingTable(index) {
        <core_rt:if test="${not otherLicensing.isEmpty()}">
            var i = 0;
        <core_rt:forEach items="${otherLicensing}" var="otherLicensingData" varStatus="loop">
                i++;
            if (i == index) {
                fillValueToId("licenseId", "${otherLicensingData.licenseId}");
                fillValueToId("licenseName", "${otherLicensingData.licenseName}");
                $('#extractedText').val("${otherLicensingData.extractedText}");
                $('#licenseCrossRefs').val("${otherLicensingData.licenseCrossRefs}");
                $('#licenseCommentOnOtherLicensing').val("${otherLicensingData.licenseComment}");
            }
        </core_rt:forEach>
        </core_rt:if>
    }

    generateSelecterOption('selectOtherLicensing', "${otherLicensing.size()}");

</script>