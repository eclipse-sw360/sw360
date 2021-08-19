<core_rt:set var="snippets" value="${spdxDocument.snippets}"/>
<table class="table" id="editSnippetInformation">
    <thead>
        <tr>
            <th>5. Snippet Information</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectSnippet" style="text-decoration: underline;" class="sub-title">Select Snippet</label>
                        <select id="selectSnippet" type="text" class="form-control spdx-select">
                            <option>1</option>
                        </select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-spdxCreatorType-Person" data-row-id="" onclick="removeRow(this);" viewBox="0 0 512 512">
                            <title>Delete</title>
                            <path class="lexicon-icon-outline lx-trash-body-border" d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z"></path>
                            <polygon class="lexicon-icon-outline lx-trash-lid" points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 "></polygon>
                            <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6" width="63.9" height="191.6"></rect>
                            <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6" width="63.9" height="191.6"></rect>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main">Add new Snippet</button>
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
                        <input id="snippetSpdxIdentifier" class="form-control needs-validation" rule="regex:^[0-9a-zA-Z.-]+$" name="_sw360_portlet_components_SPDXSPDX_IDENTIFIER" type="text" placeholder="Enter Snippet SPDX Identifier" value="">
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
                        <input style="flex: 3;" id="snippetFromFileValue" class="form-control needs-validation" rule="regex:^[0-9]+\.[0-9]+$" name="_sw360_portlet_components_SPDXSPDX_VERSION" type="text" placeholder="Enter Snippet from File SPDX Identifier" value="">
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
                    <div style="display: flex; margin-bottom: 0.75rem;">
                        <select style="flex: 1; margin-right: 1rem;" type="text" class="form-control" placeholder="Enter Type" value="Line">
                            <option value="Organization" selected>Byte</option>
                            <option value="Person">Line</option>
                        </select>
                        <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control" placeholder="Enter Start Pointer">
                        <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control" placeholder="Enter End Pointer">
                        <input style="flex: 4; margin-right: 2rem;" type="text" class="form-control" placeholder="Enter Reference">
                        <svg class="disabled lexicon-icon spdx-delete-icon-sub" name="delete-spdxCreatorType-Person" data-row-id="" onclick="removeRow(this);" viewBox="0 0 512 512">
                            <title>Delete</title>
                            <path class="lexicon-icon-outline lx-trash-body-border" d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z"></path>
                            <polygon class="lexicon-icon-outline lx-trash-lid" points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 "></polygon>
                            <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6" width="63.9" height="191.6"></rect>
                            <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6" width="63.9" height="191.6"></rect>
                        </svg>
                    </div>
                    <button class="spdx-add-button-sub">Add new range</button>
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
                        <input class="spdx-radio" id="spdxConcludedLicenseExist" type="radio" name="_sw360_portlet_components_CONCLUDED_LICENSE" value="exist">
                        <input style="flex: 6; margin-right: 1rem;" id="spdxConcludedLicenseValue" class="form-control needs-validation" type="text" required="" name="_sw360_portlet_components_CONCLUDED_LICENSE_VALUE" placeholder="Enter Snippet Concluded License">
                    </div>
                    <div style="flex: 2;">
                        <input class="spdx-radio" id="spdxConcludedLicenseNone" type="radio" name="_sw360_portlet_components_CONCLUDED_LICENSE" value="NONE">
                        <label style="margin-right: 2rem;" class="form-check-label radio-label" for="spdxConcludedLicenseNone">NONE</label>
                        <input class="spdx-radio" id="spdxConcludedLicenseNoAssertion" type="radio" name="_sw360_portlet_components_CONCLUDED_LICENSE" value="NOASSERTION">
                        <label class="form-check-label radio-label" for="spdxConcludedLicenseNoAssertion">NOASSERTION</label>
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
                        <input class="spdx-radio" id="licenseInfoInFile" type="radio" name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="exist">
                        <textarea style="flex: 6; margin-right: 1rem;" id="licenseInfoInFileValue" rows="5" class="form-control needs-validation" type="text" required="" name="_sw360_portlet_components_LICENSE_INFO_IN_FILE_SOURCE" placeholder="Enter License Information in Snippet">
https://spdx.org/licenses/GPL-2.0-only
#LicenseRef-1
#LicenseRef-2</textarea>
                    </div>
                    <div style="flex: 2;">
                        <input class="spdx-radio" id="licenseInfoInFileNone" type="radio" name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="NONE">
                        <label style="margin-right: 2rem;" class="form-check-label radio-label" for="licenseInfoInFileNone">NONE</label>
                        <input class="spdx-radio" id="licenseInfoInFileNoAssertion" type="radio" name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="NOASSERTION">
                        <label class="form-check-label radio-label" for="licenseInfoInFileNoAssertion">NOASSERTION</label>
                    </div>
                </div>
            </div>
        </td>
    </tr>
    <tr>
        <td colspan="3">
            <div class="form-group">
                <label for="snippetLicenseComments">5.7 Snippet Comments on License</label>
                <textarea class="form-control" id="snippetLicenseComments" rows="5" name="_sw360_portlet_components_SNIPPET_LICENSE_COMMENTS" placeholder="Enter Snippet Comments on License"></textarea>
            </div>
        </td>
    </tr>
    <tr>
        <td>
            <div class="form-group">
                <label class="mandatory">5.8 Copyright Text</label>
                <div style="display: flex; flex-direction: row;">
                    <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                        <input class="spdx-radio" id="snippetCopyrightText" type="radio" name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="exist">
                        <textarea style="flex: 6; margin-right: 1rem;" id="copyrightTextValue" rows="5" class="form-control needs-validation" type="text" required="" name="_sw360_portlet_components_COPYRIGHT_TEXT_VALUE" placeholder="Enter Copyright Text"></textarea>
                    </div>
                    <div style="flex: 2;">
                        <input class="spdx-radio" id="snippetCopyrightTextNone" type="radio" name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NONE">
                        <label style="margin-right: 2rem;" class="form-check-label radio-label" for="snippetCopyrightTextNone">NONE</label>
                        <input class="spdx-radio" id="snippetCopyrightTextNoAssertion" type="radio" name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NOASSERTION">
                        <label class="form-check-label radio-label" for="snippetCopyrightTextNoAssertion">NOASSERTION</label>
                    </div>
                </div>
            </div>
        </td>
    </tr>
    <tr>
        <td colspan="3">
            <div class="form-group">
                <label for="snippetComment">5.9 Snippet Comments</label>
                <textarea class="form-control" id="snippetComment" rows="5" name="_sw360_portlet_components_SNIPPET_COMMENT" placeholder="Enter Snippet Comments"></textarea>
            </div>
        </td>
    </tr>
    <tr>
        <td>
            <div class="form-group">
                <label class="mandatory" for="snippetName">
                    5.10 Snippet Name
                </label>
                <input id="snippetName" name="_sw360_portlet_components_SPDXSPDX_NAME" type="text" class="form-control" placeholder="Enter Snippet Name" value="">
            </div>
        </td>
    </tr>
    <tr>
        <td colspan="3">
            <div class="form-group">
                <label for="snippetAttributionText">5.11 Snippet Attribution Text</label>
                <textarea class="form-control" id="snippetAttributionText" rows="5" name="_sw360_portlet_components_SNIPPET_ATTRIBUTION_TEXT" placeholder="Enter Snippet Attribution Text"></textarea>
            </div>
        </td>
    </tr>
    </tbody>
</table>

<script>
    generateSnippetTable('1');
    function generateSnippetTable(index){
    <core_rt:if test="${not snippets.isEmpty()}">
        var i = 0;
        <core_rt:forEach items="${snippets}" var="snippetData" varStatus="loop">
            i++;
            if (i == index) {
                fillValueToId("snippetSpdxIdentifier", "${snippetData.SPDXID}");
                fillValueToId("snippetFromFileValue", "${snippetData.snippetFromFile}");
                fillValueToId("spdxConcludedLicenseValue", "${snippetData.licenseConcluded}");
                fillValueToId("licenseInfoInFileValue", "${snippetData.licenseInfoInSnippets}");
                fillValueToId("snippetLicenseComments", "${snippetData.licenseComments}");
                fillValueToId("copyrightTextValue", "${snippetData.copyrightText}");
                fillValueToId("snippetComment", "${snippetData.comment}");
                fillValueToId("snippetName", "${snippetData.name}");
                fillValueToId("snippetAttributionText", "${snippetData.snippetAttributionText}");
            }
        </core_rt:forEach>
    </core_rt:if>
    }
</script>