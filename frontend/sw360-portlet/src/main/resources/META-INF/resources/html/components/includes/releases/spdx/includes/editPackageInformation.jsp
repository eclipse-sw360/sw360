<core_rt:if test="${not spdxPackageInfo.isEmpty()}">
    <core_rt:set var="package" value="${spdxPackageInfo.iterator().next()}" />
</core_rt:if>
<table class="table spdx-table three-columns" id="editPackageInformation">
    <thead>
        <tr>
            <th colspan="3">3. Package Information</th>
        </tr>
    </thead>
    <tbody>
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
                            placeholder="Enter Package SPDX Identifier" name="_sw360_portlet_components_PACKAGE_SPDX_ID"
                            value="${package.SPDXID}">
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
                            placeholder="Enter Package File Name" name="_sw360_portlet_components_PACKAGE_FILE_NAME"
                            value="${package.packageFileName}">
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
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
                                name="_sw360_portlet_components_SUPPLIER_VALUE" placeholder="Enter Package Supplier"
                                value="${package.supplier}">
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
        <tr class="spdx-full">
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
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" class="form-control needs-validation"
                                id="downloadLocationValue" rule="isDownloadUrl" type="text"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION_VALUE"
                                placeholder="Enter Package Download Location" value="${package.downloadLocation}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="downloadLocationNone" type="radio"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="downloadLocationNone">NONE</label>
                            <input class="spdx-radio" id="downloadLocationNoAssertion" type="radio"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION" value="NOASSERTION">
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
                                name="_sw360_portlet_components_FILES_ANALYZED" checked value="TRUE">
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
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="verificationCodeValue">3.9 Package Verification Code</label>
                    <div>
                        <input style="margin-bottom: 0.75rem;" class="form-control" id="verificationCodeValue"
                            name="_sw360_portlet_components_VERIFICATION_CODE_VALUE"
                            placeholder="Enter Verification Code Value"></input>
                        <textarea class="form-control" id="excludedFiles" rows="5"
                            name="_sw360_portlet_components_EXCLUDED_FILES"
                            placeholder="Enter Excluded Files"><sw360:out value="${package.packageVerificationCode.excludedFiles.toString()}" hashSet="true"/></textarea>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label>3.10 Package Checksum</label>
                    <div style="display: flex;">
                        <label class="sub-title">Checksum</label>
                        <div style="display: flex; flex-direction: column; flex: 7">
                            <div style="display: flex; margin-bottom: 0.75rem;">
                                <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                                    placeholder="Enter Algorithm">
                                <input style="flex: 6; margin-right: 2rem;" type="text" class="form-control"
                                    placeholder="Enter Value">
                                <svg class="disabled lexicon-icon spdx-delete-icon-sub"
                                    name="delete-spdxCreatorType-Person" data-row-id="" onclick="deleteSub(this);"
                                    viewBox="0 0 512 512">
                                    <title><liferay-ui:message key="delete" /></title>
                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash" />
                                </svg>
                            </div>
                            <div style="display: flex; margin-bottom: 0.75rem;">
                                <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                                    placeholder="Enter Algorithm">
                                <input style="flex: 6; margin-right: 2rem;" type="text" class="form-control"
                                    placeholder="Enter Value">
                                <svg class="disabled lexicon-icon spdx-delete-icon-sub"
                                    name="delete-spdxCreatorType-Person" data-row-id="" onclick="deleteSub(this);"
                                    viewBox="0 0 512 512">
                                    <title><liferay-ui:message key="delete" /></title>
                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash" />
                                </svg>
                            </div>
                            <button class="spdx-add-button-sub" onclick="addSub(this)">Add new algorithm</button>
                        </div>
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
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" id="packageHomepageValue"
                                class="form-control needs-validation" rule="isDownloadUrl" type="text"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE_VALUE"
                                placeholder="Enter Package Homepage" value="${package.homepage}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="packageHomepageNone" type="radio"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="packageHomepageNone">NONE</label>
                            <input class="spdx-radio" id="packageHomepageNoAssertion" type="radio"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE" value="NOASSERTION">
                            <label class="form-check-label radio-label"
                                for="packageHomepageNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
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
                                name="_sw360_portlet_components_LICENSE_CONCLUDED" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" class="form-control needs-validation"
                                id="licenseConcludedValue" type="text"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED_VALUE"
                                placeholder="Enter Concluded License" value="${package.licenseConcluded}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseConcludedNone" type="radio"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="licenseConcludedNone">NONE</label>
                            <input class="spdx-radio" id="licenseConcludedNoAssertion" type="radio"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED" value="NOASSERTION">
                            <label class="form-check-label radio-label"
                                for="licenseConcludedNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label class="mandatory">3.14 All Licenses Information from Files</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseInfoFromFilesExist" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES" value="EXIST">
                            <textarea style="flex: 6; margin-right: 1rem;" id="licenseInfoFromFilesValue" rows="5"
                                class="form-control needs-validation" type="text"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES_VALUE"
                                placeholder="Enter All Licenses Information from Files"><sw360:out value="${package.licenseInfoFromFiles.toString()}" hashSet="true"/></textarea>
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseInfoFromFilesNone" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="licenseInfoFromFilesNone">NONE</label>
                            <input class="spdx-radio" id="licenseInfoFromFilesNoAssertion" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES" value="NOASSERTION">
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
                                name="_sw360_portlet_components_DECLARED_LICENSE" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" id="licenseDeclaredValue"
                                class="form-control needs-validation" type="text"
                                name="_sw360_portlet_components_DECLARED_LICENSE_VALUE"
                                placeholder="Enter Declared License" value="${package.licenseDeclared}">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseDeclaredNone" type="radio"
                                name="_sw360_portlet_components_DECLARED_LICENSE" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="licenseDeclaredNone">NONE</label>
                            <input class="spdx-radio" id="licenseDeclaredNoAssertion" type="radio"
                                name="_sw360_portlet_components_DECLARED_LICENSE" value="NOASSERTION">
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
                                name="_sw360_portlet_components_COPYRIGHT_TEXT" value="EXIST">
                            <textarea style="flex: 6; margin-right: 1rem;" id="copyrightTextValue" rows="5"
                                class="form-control needs-validation" type="text"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT_VALUE"
                                placeholder="Enter Copyright Text">${package.copyrightText}</textarea>
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="copyrightTextNone" type="radio"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="copyrightTextNone">NONE</label>
                            <input class="spdx-radio" id="copyrightTextNoAssertion" type="radio"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT" value="NOASSERTION">
                            <label class="form-check-label radio-label"
                                for="copyrightTextNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="summary">3.18 Package Summary Description</label>
                    <textarea class="form-control" id="summary" rows="5"
                        name="_sw360_portlet_components_PACKAGE_SUMMARY"
                        placeholder="Enter Package Summary Description">${package.summary}</textarea>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
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
                        name="_sw360_portlet_components_PACKAGE_COMMENT"
                        placeholder="Enter Package Comment">${package.packageComment}</textarea>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group section">
                    <label>
                        3.21 External References
                    </label>
                    <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label style="text-decoration: underline;" class="sub-title">Select Reference</label>
                            <select type="text" class="form-control spdx-select" id="externalReferences"
                                onchange="generateExternalRefsTable($(this).find('option:selected').text())">
                            </select>
                            <svg class="disabled lexicon-icon spdx-delete-icon-main" data-row-id=""
                                onclick="deleteMain(this);" viewBox="0 0 512 512">
                                <title><liferay-ui:message key="delete" /></title>
                                <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash" />
                            </svg>
                        </div>
                        <button class="spdx-add-button-main" onclick="addMain(this)">Add new Reference</button>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Category</label>
                            <select style="width: auto; flex: auto;" id="referenceCategory" type="text"
                                class="form-control" placeholder="Enter Category"
                                name="_sw360_portlet_components_REFERENCE_CATEGORY">
                                <option value="referenceCategory_security">SECURITY</option>
                                <option>PACKAGE-MANAGER</option>
                                <option>PERSISTENT-ID</option>
                                <option value="referenceCategory_other">OTHER</option>
                            </select>
                        </div>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Type</label>
                            <select style="width: auto; flex: auto;" id="referenceType-1" type="text"
                                class="form-control" placeholder="Enter Type"
                                name="_sw360_portlet_components_REFERENCE_TYPE-1">
                                <option>cpe22Type</option>
                                <option>cpe23Type</option>
                            </select>
                            <input style="width: auto; flex: auto; display: none;" id="referenceType-2" type="text"
                                class="form-control" placeholder="Enter Type"
                                name="_sw360_portlet_components_REFERENCE_TYPE-2">
                            </select>
                        </div>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Locator</label>
                            <input style="width: auto; flex: auto;" type="text" class="form-control"
                                id="externalReferencesLocator" placeholder="Enter Locator"
                                name="_sw360_portlet_components_REFERENCE_LOCATOR">
                        </div>
                        <div style="display: flex; flex-direction: row;">
                            <label class="sub-title">3.22 Comment</label>
                            <textarea style="width: auto; flex: auto;" type="text" rows="5" class="form-control"
                                id="externalReferencesComment" placeholder="Enter Comment"
                                name="_sw360_portlet_components_REFERENCE_COMMENT"></textarea>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="spdxPackageComment">3.23 Package Attribution Text</label>
                    <textarea class="form-control" id="spdxPackageAttributionText" rows="5"
                        name="_sw360_portlet_components_PACKAGE_COMMENT"
                        placeholder="Enter Package Comment"><sw360:out value="${package.attributionText.toString()}" stripNewlines="false" hashSet="true"/></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>
    $(function () {
        const referenceCategories = {
            'referenceCategory_security': ['cpe22Type', 'cpe23Type'],
            'referenceCategory_packageManager': ['maven-central', 'npm', 'nuget', 'bower', 'purl'],
            'referenceCategory_persistentId': [],
            'referenceCategory_other': []
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

            if ($('#referenceCategory').is(":focus")) {
                let index = $('#externalReferences')[0].selectedIndex;
                storeExternalRef(index);
            }
        });

        // $('#referenceType-1').on('change', function() {
        //     if ($('#referenceType-1').is(":focus")) {
        //         let index = $('#externalReferences')[0].selectedIndex;
        //         storeExternalRef(index);
        //     }
        // });

        // ------------------------- 3.21 External References
        // Add data
        $('[name=add-externalRef]').on('click', function(e) {
            let newObj = { 'referenceCategory': 'referenceCategory_security', 'referenceLocator': '', 'referenceType': 'cpe22Type', 'comment': '' };
            packageInformationObj.externalRefs.push(newObj);
            addMain($(this));
            $('#externalReferences').change();
        });

        // Delete data
        $('[name=delete-externalRef').on('click', function(e) {
            let selectedIndex = $('#externalReferences')[0].selectedIndex;
            packageInformationObj.externalRefs.splice(selectedIndex, 1);
            deleteMain($(this));
        });

        // // Change data
        $('#externalReferences').on('change', function(e) {
            let selectedIndex = $('#externalReferences')[0].selectedIndex;
            fillExternalRef(selectedIndex);
        });

        // // Fill data - index start from 0
        function fillExternalRef(index) {
            let obj = packageInformationObj.externalRefs[index];

            $('#referenceCategory').val(obj['referenceCategory']);
            $('#referenceCategory').change();

            if (obj['referenceCategory'] == 'referenceCategory_security' || obj['referenceCategory'] == 'referenceCategory_packageManager') {
                $('#referenceType-1').val(obj['referenceType']);
            } else {
                $('#referenceType-2').val(obj['referenceType']);
            }
            // alert(obj['referenceLocator']);
            $('#externalReferencesLocator').val(obj['referenceLocator']);
            $('#externalReferencesComment').val(obj['comment']);
        }

        function storeExternalDocRef(index) {
            let obj = documentCreationInformationObj.externalDocumentRefs[index];

            obj['externalDocumentId'] = $('#externalDocumentId').val();
            obj['spdxDocument'] = $('#externalDocument').val();
            obj['checksum']['algorithm'] = $('#checksumAlgorithm').val();
            obj['checksum']['checksumValue'] = $('#checksumValue').val();
        }
    });
</script>