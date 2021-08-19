<core_rt:set var="package" value="${spdxPackageInfo.iterator().next()}"/>
<table class="table spdx-table three-columns" id="editPackageInformation">
    <thead>
        <tr>
            <th colspan="3">3. Package Information</th>
        </tr>
    </thead>
    <tbody></tbody>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="packageName">
                        3.1 Package Name
                    </label>
                    <div style="display: flex">
                        <input id="packageName" class="form-control needs-validation" type="text"
                            placeholder="Enter Package Name" name="_sw360_portlet_components_PACKAGE_NAME"
                            value="${package.name}">
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="packageSPDXId">
                        3.2 Package SPDX Identifier
                    </label>
                    <div style="display: flex">
                        <label class="sub-label">SPDXRef-</label>
                        <input id="packageSPDXId" class="form-control needs-validation" type="text"
                            placeholder="Enter Package SPDX Identifier"
                            name="_sw360_portlet_components_PACKAGE_SPDX_ID" value="${package.SPDXID}">
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label for="versionInfo">
                        3.3 Package Version
                    </label>
                    <div style="display: flex">
                        <input id="versionInfo" class="form-control" type="text" placeholder="Enter Package Version"
                            name="_sw360_portlet_components_VERSION_INFO" value="${package.versionInfo}">
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label for="packageFileName">
                        3.4 Package File Name
                    </label>
                    <div style="display: flex">
                        <input id="packageFileName" class="form-control" type="text"
                            placeholder="Enter Package File Name" name="_sw360_portlet_components_PACKAGE_FILE_NAME" value="${package.packageFileName}">
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label>3.5 Package Supplier</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" type="radio" name="_sw360_portlet_components_SUPPLIER"
                                value="EXIST">
                            <select id="supplierType" style="flex: 2; margin-right: 1rem;" class="form-control">
                                <option>Person</option>
                                <option>Organization</option>
                            </select>
                            <input style="flex: 6; margin-right: 1rem;" id="supplierValue"
                                class="form-control needs-validation" rule="isDownloadUrl" type="text"
                                name="_sw360_portlet_components_SUPPLIER_VALUE" placeholder="Enter Package Supplier" value="${package.supplier}">
                        </div>
                        <div style="flex: 2">
                            <input class="spdx-radio" id="supplierNoAssertion" type="radio"
                                name="_sw360_portlet_components_SUPPLIER" value="NOASSERTION">
                            <label class="form-check-label radio-label" for="supplierNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label>3.6 Package Originator</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" type="radio" name="_sw360_portlet_components_ORIGINATOR"
                                value="EXIST">
                            <select id="originatorType" style="flex: 2; margin-right: 1rem;" class="form-control">
                                <option>Person</option>
                                <option>Organization</option>
                            </select>
                            <input style="flex: 6; margin-right: 1rem;" class="form-control needs-validation"
                                rule="isDownloadUrl" type="text" name="_sw360_portlet_components_ORIGINATOR_VALUE"
                                placeholder="Enter Package Originator" value="${package.originator}">
                        </div>
                        <div style="flex: 2">
                            <input class="spdx-radio" id="originatorNoAssertion" type="radio"
                                name="_sw360_portlet_components_ORIGINATOR" value="NOASSERTION">
                            <label class="form-check-label radio-label" for="originatorNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label class="mandatory">3.7 Package Download Location</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" type="radio" id="downloadLocationExist"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION" value="EXIST"
                                onchange="setInputValue('downloadLocation', value);">
                            <input style="flex: 6; margin-right: 1rem;" class="form-control needs-validation"
                                id="downloadLocationValue" rule="isDownloadUrl" type="text" required=""
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION_VALUE"
                                placeholder="Enter Package Download Location" value="${package.downloadLocation}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="downloadLocationNone" type="radio"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION" value="NONE"
                                onchange="setInputValue('downloadLocation', value);">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="downloadLocationNone">NONE</label>
                            <input class="spdx-radio" id="downloadLocationNoAssertion" type="radio"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION" value="NOASSERTION"
                                onchange="setInputValue('downloadLocation', value);">
                            <label class="form-check-label radio-label"
                                for="downloadLocationNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label>3.8 Files Analyzed</label>
                    <div style="display: flex; flex-direction: row;">
                        <div>
                            <input class="spdx-radio" id="FilesAnalyzedTrue" type="radio"
                                name="_sw360_portlet_components_FILES_ANALYZED" value="TRUE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="FilesAnalyzedTrue">TRUE</label>
                            <input class="spdx-radio" id="FilesAnalyzedFalse" type="radio"
                                name="_sw360_portlet_components_FILES_ANALYZED" value="FALSE">
                            <label class="form-check-label radio-label" for="FilesAnalyzedFalse">FALSE</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">3.11 Package Homepage</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="packageHomepageExist" type="radio"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE" value="EXIST"
                                onchange="setInputValue('packageHomepage', value);">
                            <input style="flex: 6; margin-right: 1rem;" id="packageHomepageValue"
                                class="form-control needs-validation" rule="isDownloadUrl" type="text" required=""
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE_VALUE"
                                placeholder="Enter Package Homepage" value="${package.homepage}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="packageHomepageNone" type="radio"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE" value="NONE"
                                onchange="setInputValue('packageHomepage', value);">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="packageHomepageNone">NONE</label>
                            <input class="spdx-radio" id="packageHomepageNoAssertion" type="radio"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE" value="NOASSERTION"
                                onchange="setInputValue('packageHomepage', value);">
                            <label class="form-check-label radio-label"
                                for="packageHomepageNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="sourceInfo">3.12 Source Information</label>
                    <textarea class="form-control" id="sourceInfo" rows="5" name="_sw360_portlet_components_SOURCE_INFO"
                        placeholder="Enter Source Information"> ${package.sourceInfo}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">3.13 Concluded License</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseConcludedExist" type="radio"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED" value="EXIST"
                                onchange="setInputValue('licenseConcluded', value);">
                            <input style="flex: 6; margin-right: 1rem;" class="form-control needs-validation"
                                id="licenseConcludedValue" type="text" required=""
                                name="_sw360_portlet_components_LICENSE_CONCLUDED_VALUE"
                                placeholder="Enter Concluded License" value="${package.licenseConcluded}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseConcludedNone" type="radio"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED" value="NONE"
                                onchange="setInputValue('licenseConcluded', value);">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="licenseConcludedNone">NONE</label>
                            <input class="spdx-radio" id="licenseConcludedNoAssertion" type="radio"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED" value="NOASSERTION"
                                onchange="setInputValue('licenseConcluded', value);">
                            <label class="form-check-label radio-label"
                                for="licenseConcludedNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">3.14 All Licenses Information from Files</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseInfoFromFilesExist" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES" value="EXIST"
                                onchange="setInputValue('licenseInfoFromFiles', value);">
                            <textarea style="flex: 6; margin-right: 1rem;" id="licenseInfoFromFilesValue" rows="5"
                                class="form-control needs-validation" type="text" required=""
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES_VALUE"
                                placeholder="Enter All Licenses Information from Files">${package.licenseInfoFromFiles}</textarea>
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseInfoFromFilesNone" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES" value="NONE"
                                onchange="setInputValue('licenseInfoFromFiles', value);">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="licenseInfoFromFilesNone">NONE</label>
                            <input class="spdx-radio" id="licenseInfoFromFilesNoAssertion" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES" value="NOASSERTION"
                                onchange="setInputValue('licenseInfoFromFiles', value);">
                            <label class="form-check-label radio-label"
                                for="licenseInfoFromFilesNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">3.15 Declared License</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseDeclaredExist" type="radio"
                                name="_sw360_portlet_components_DECLARED_LICENSE" value="EXIST"
                                onchange="setInputValue('licenseDeclared', value);">
                            <input style="flex: 6; margin-right: 1rem;" id="licenseDeclaredValue"
                                class="form-control needs-validation" type="text" required=""
                                name="_sw360_portlet_components_DECLARED_LICENSE_VALUE"
                                placeholder="Enter Declared License" value="${package.licenseDeclared}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseDeclaredNone" type="radio"
                                name="_sw360_portlet_components_DECLARED_LICENSE" value="NONE"
                                onchange="setInputValue('licenseDeclared', value);">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="licenseDeclaredNone">NONE</label>
                            <input class="spdx-radio" id="licenseDeclaredNoAssertion" type="radio"
                                name="_sw360_portlet_components_DECLARED_LICENSE" value="NOASSERTION"
                                onchange="setInputValue('licenseDeclared', value);">
                            <label class="form-check-label radio-label"
                                for="licenseDeclaredNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="licenseComments">3.16 Comments On License</label>
                    <textarea class="form-control" id="licenseComments" rows="5"
                        name="_sw360_portlet_components_LICENSE_COMMENTS"
                        placeholder="Enter Comments On License">${package.licenseComments}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">3.17 Copyright Text</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="copyrightTextExist" type="radio"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT" value="EXIST"
                                onchange="setInputValue('copyrightText', value);">
                            <textarea style="flex: 6; margin-right: 1rem;" id="copyrightTextValue" rows="5"
                                class="form-control needs-validation" type="text" required=""
                                name="_sw360_portlet_components_COPYRIGHT_TEXT_VALUE"
                                placeholder="Enter Copyright Text">${package.copyrightText}</textarea>
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="copyrightTextNone" type="radio"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT" value="NONE"
                                onchange="setInputValue('copyrightText', value);">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="copyrightTextNone">NONE</label>
                            <input class="spdx-radio" id="copyrightTextNoAssertion" type="radio"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT" value="NOASSERTION"
                                onchange="setInputValue('copyrightText', value);">
                            <label class="form-check-label radio-label"
                                for="copyrightTextNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="summary">3.18 Package Summary Description</label>
                    <textarea class="form-control" id="summary" rows="5"
                        name="_sw360_portlet_components_PACKAGE_SUMMARY"
                        placeholder="Enter Package Summary Description">${package.summary}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="description">3.19 Package Detailed Description</label>
                    <textarea class="form-control" id="description" rows="5"
                        name="_sw360_portlet_components_PACKAGE_DESCRIPTION"
                        placeholder="Enter Package Detailed Description">${package.description}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="spdxPackageComment">3.20 Package Comment</label>
                    <textarea class="form-control" id="spdxPackageComment" rows="5"
                        name="_sw360_portlet_components_PACKAGE_COMMENT" placeholder="Enter Package Comment">${package.packageComment}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label>
                        3.21 External References
                    </label>
                    <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                        <div style="display: flex; flex-direction: row; margin-bottom: 1.5rem;">
                            <label style="text-decoration: underline;" class="sub-title">Select Reference</label>
                            <select style="width: auto; flex: auto;" type="text" class="form-control">
                                <option>1</option>
                                <option>2</option>
                            </select>
                        </div>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Category</label>
                            <select style="width: auto; flex: auto;" type="text" class="form-control"
                            id="externalReferencesCategory"
                                placeholder="Enter Category" name="_sw360_portlet_components_REFERENCE_CATEGORY">
                                <option value="referenceCategory_security">SECURITY</option>
                                <option>PACKAGE-MANAGER</option>
                                <option>PERSISTENT-ID</option>
                                <option value="referenceCategory_other">OTHER</option>
                            </select>
                        </div>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Type</label>
                            <select style="width: auto; flex: auto;" type="text" class="form-control"
                                id="externalReferencesType"
                                placeholder="Enter Type" name="_sw360_portlet_components_REFERENCE_TYPE">
                                <option>cpe22Type</option>
                                <option>cpe23Type</option>
                                <option>maven-central</option>
                                <option>npm</option>
                                <option>nuget</option>
                                <option>bower</option>
                                <option>purl</option>
                                <option>swh</option>
                                <option>[idstring]</option>
                            </select>
                        </div>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Locator</label>
                            <input style="width: auto; flex: auto;" type="text" class="form-control"
                                id="externalReferencesLocator"
                                placeholder="Enter Locator" name="_sw360_portlet_components_REFERENCE_LOCATOR">
                        </div>
                        <div style="display: flex; flex-direction: row;">
                            <label class="sub-title">3.22 Comment</label>
                            <textarea style="width: auto; flex: auto;" type="text" rows="5" class="form-control"
                                id="externalReferencesComment"
                                placeholder="Enter Comment"
                                name="_sw360_portlet_components_REFERENCE_COMMENT"></textarea>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>

    setInputValue('downloadLocation', "${package.downloadLocation}");
    setInputValue('packageHomepage', "${package.homepage}");
    setInputValue('licenseConcluded', "${package.licenseConcluded}");
    setInputValue('licenseDeclared', "${package.licenseDeclared}");
    setInputValue('copyrightText', "${package.copyrightText}");

    function setInputValue(id, type) {
        switch (type) {
            case '':
                setDisabled(id);
                break;
            case 'NONE':
                document.getElementById(id + 'None').checked = 'true';
                setDisabled(id);
                break;
            case 'NOASSERTION':
                document.getElementById(id + 'NoAssertion').checked = 'true';
                setDisabled(id);
                break;
            default:
                document.getElementById(id + 'Exist').checked = 'true';
                setEnabled(id);
                break;
        }
    }

    function setDisabled(id) {
        $('#' + id + 'Value').prop('disabled', true);
    }

    function setEnabled(id) {
        $('#' + id + 'Value').prop('disabled', false);
    }

    generateExternalDocumentRefsTable('1');
    function generateExternalDocumentRefsTable(index){
        <core_rt:if test="${not package.externalRefs.isEmpty()}">
            var i = 0;
            <core_rt:forEach items="${package.externalRefs}" var="externalRefsData" varStatus="loop">
                i++;
                if (i == index) {
                    fillValueToId("externalReferencesCategory", "${externalRefsData.referenceCategory}");
                    fillValueToId("externalReferencesType", "${externalRefsData.referenceType}");
                    fillValueToId("externalReferencesLocator", "${externalRefsData.referenceLocator}");
                    fillValueToId("externalReferencesComment", "${externalRefsData.comment}");
                }
            </core_rt:forEach>
        </core_rt:if>
    }


</script>