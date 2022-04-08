<%--
    ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
    ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.

    ~ This program and the accompanying materials are made
    ~ available under the terms of the Eclipse Public License 2.0
    ~ which is available at https://www.eclipse.org/legal/epl-2.0/

    ~ SPDX-License-Identifier: EPL-2.0
--%>

<core_rt:if test="${not spdxPackageInfo.isEmpty()}">
    <core_rt:set var="package" value="${spdxPackageInfo.iterator().next()}" />
</core_rt:if>
<core_rt:if test="${not spdxPackageInfo.isEmpty()}">
    <core_rt:set var="spdxPackages" value="${spdxPackageInfo}" />
</core_rt:if>
<table class="table spdx-table" id="editPackageInformation">
    <thead>
        <tr>
            <th colspan="3">7. Package Information</th>
        </tr>
    </thead>
    <tbody id="sectionPackageInformation" class="section section-package">
        <tr>
            <td style="display:none;">
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectPackage" style="text-decoration: underline;" class="sub-title">Select Package</label>
                        <select id="selectPackage" type="text" class="form-control spdx-select"></select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-package" data-row-id="" viewBox="0 0 512 512">
                            <title><liferay-ui:message key="delete" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main" name="add-package">Add new Package</button>
                </div>
            </td>
        </tr>
 
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="packageName">7.1 Package name</label>
                    <div style="display: flex">
                        <input id="packageName" class="form-control needs-validation" rule="required" type="text"
                            placeholder="Enter package name" name="_sw360_portlet_components_PACKAGE_NAME"
                            value="<sw360:out value="${package.name}" />">
                    </div>
                    <div id="packageName-error-messages">
                        <div class="invalid-feedback" rule="required">
                            <liferay-ui:message key="this.field.must.be.not.empty" />
                        </div>
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="packageSPDXId">7.2 Package SPDX identifier</label>
                    <div style="display: flex">
                        <label class="sub-label">SPDXRef-</label>
                        <input id="packageSPDXId" class="form-control needs-validation" rule="required" type="text"
                            placeholder="Enter package SPDX identifier" name="_sw360_portlet_components_PACKAGE_SPDX_ID"
                            value="<sw360:out value="${package.SPDXID}" />">
                    </div>
                    <div id="packageSPDXId-error-messages">
                        <div class="invalid-feedback" rule="required">
                            <liferay-ui:message key="this.field.must.be.not.empty" />
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label for="versionInfo">7.3 Package version</label>
                    <div style="display: flex">
                        <input id="versionInfo" class="form-control" type="text" placeholder="Enter package version"
                            name="_sw360_portlet_components_VERSION_INFO" value="<sw360:out value="${package.versionInfo}" />">
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label for="packageFileName">7.4 Package file name</label>
                    <div style="display: flex">
                        <input id="packageFileName" class="form-control" type="text"
                            placeholder="Enter package file name" name="_sw360_portlet_components_PACKAGE_FILE_NAME"
                            value="<sw360:out value="${package.packageFileName}" />">
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label>7.5 Package supplier</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" type="radio" name="_sw360_portlet_components_SUPPLIER"
                                value="EXIST">
                            <select id="supplierType" style="flex: 2; margin-right: 1rem;" class="form-control">
                                <option>Organization</option>
                                <option>Person</option>
                            </select>
                            <input style="flex: 6; margin-right: 1rem;" id="supplierValue"
                                class="form-control" type="text"
                                name="_sw360_portlet_components_SUPPLIER_VALUE" placeholder="Enter package supplier">
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
                    <label>7.6 Package originator</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" type="radio" name="_sw360_portlet_components_ORIGINATOR"
                                value="EXIST">
                            <select id="originatorType" style="flex: 2; margin-right: 1rem;" class="form-control">
                                <option>Organization</option>
                                <option>Person</option>
                            </select>
                            <input style="flex: 6; margin-right: 1rem;" class="form-control" id="originatorValue"
                                type="text" name="_sw360_portlet_components_ORIGINATOR_VALUE"
                                placeholder="Enter package originator">
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
                    <label class="mandatory">7.7 Package download location</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" type="radio" id="downloadLocationExist"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" class="form-control needs-validation"
                                id="downloadLocationValue" rule="required" type="text"
                                name="_sw360_portlet_components_DOWNLOAD_LOCATION_VALUE"
                                placeholder="Enter package download location">
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
                    <div id="downloadLocationValue-error-messages">
                        <div class="invalid-feedback" rule="required">
                            <liferay-ui:message key="this.field.must.be.not.empty" />
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label>7.8 Files analyzed</label>
                    <div style="display: flex; flex-direction: row;">
                        <div>
                            <input class="spdx-radio" id="FilesAnalyzedTrue" type="radio"
                                name="_sw360_portlet_components_FILES_ANALYZED" checked value="true">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="FilesAnalyzedTrue">TRUE</label>
                            <input class="spdx-radio" id="FilesAnalyzedFalse" type="radio"
                                name="_sw360_portlet_components_FILES_ANALYZED" value="false">
                            <label class="form-check-label radio-label" for="FilesAnalyzedFalse">FALSE</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="verificationCodeValue" class="mandatory" >7.9 Package verification code</label>
                    <div>
                        <input style="margin-bottom: 0.75rem;" class="form-control" id="verificationCodeValue"
                            name="_sw360_portlet_components_VERIFICATION_CODE_VALUE"
                            placeholder="Enter verification code value"></input>
                        <textarea class="form-control" id="excludedFiles" rows="5"
                            name="_sw360_portlet_components_EXCLUDED_FILES"
                            placeholder="Enter excluded files"><sw360:out value="${package.packageVerificationCode.excludedFiles.toString()}" /></textarea>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label>7.10 Package checksum</label>
                    <div style="display: flex;">
                        <label class="sub-title">Checksum</label>
                        <div style="display: flex; flex-direction: column; flex: 7">
                            <div style="display: none; margin-bottom: 0.75rem;" name="checksumRow">
                                <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control checksum-algorithm"
                                    placeholder="Enter algorithm">
                                <input style="flex: 6; margin-right: 2rem;" type="text" class="form-control checksum-value"
                                    placeholder="Enter value">
                                <svg class="disabled lexicon-icon spdx-delete-icon-sub checksum-delete" data-row-id=""
                                    viewBox="0 0 512 512">
                                    <title><liferay-ui:message key="delete" /></title>
                                    <path class="lexicon-icon-outline lx-trash-body-border" d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z"></path>
                                    <polygon class="lexicon-icon-outline lx-trash-lid" points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 "></polygon>
                                    <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6" width="63.9" height="191.6"></rect>
                                    <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6" width="63.9" height="191.6"></rect>
                                </svg>
                            </div>
                            <button id="addNewAlgorithm" class="spdx-add-button-sub spdx-add-button-sub-checksum" >Add new algorithm</button>
                        </div>
                    </div>
                </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label>7.11 Package home page</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="packageHomepageExist" type="radio"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" id="packageHomepageValue"
                                class="form-control" type="text"
                                name="_sw360_portlet_components_PACKAGE_HOMEPAGE_VALUE"
                                placeholder="Enter package home page">
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
                    <label for="sourceInfo">7.12 Source information</label>
                    <textarea class="form-control" id="sourceInfo" rows="5" name="_sw360_portlet_components_SOURCE_INFO"
                        placeholder="Enter source information">${package.sourceInfo}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">7.13 Concluded license</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseConcludedExist" type="radio"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" class="form-control needs-validation"
                                rule="required" id="licenseConcludedValue" type="text"
                                name="_sw360_portlet_components_LICENSE_CONCLUDED_VALUE"
                                placeholder="Enter concluded license">
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
                    <div id="licenseConcludedValue-error-messages">
                        <div class="invalid-feedback" rule="required">
                            <liferay-ui:message key="this.field.must.be.not.empty" />
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label class="mandatory">7.14 All licenses information from files</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseInfoFromFilesExist" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES" value="EXIST">
                            <textarea style="flex: 6; margin-right: 1rem;" id="licenseInfoFromFilesValue" rows="5"
                                class="form-control" type="text"
                                name="_sw360_portlet_components_LICENSE_INFO_FROM_FILES_VALUE"
                                placeholder="Enter all licenses information from files"><sw360:out value="${package.licenseInfoFromFiles.toString()}" hashSet="true"/></textarea>
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
                    <label class="mandatory">7.15 Declared license</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseDeclaredExist" type="radio"
                                name="_sw360_portlet_components_DECLARED_LICENSE" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" id="licenseDeclaredValue"
                                class="form-control needs-validation" type="text" rule="required"
                                name="_sw360_portlet_components_DECLARED_LICENSE_VALUE"
                                placeholder="Enter declared license">
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
                    <div id="licenseDeclaredValue-error-messages">
                        <div class="invalid-feedback" rule="required">
                            <liferay-ui:message key="this.field.must.be.not.empty" />
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="licenseComments">7.16 Comments on license</label>
                    <textarea class="form-control" id="licenseComments" rows="5"
                        name="_sw360_portlet_components_LICENSE_COMMENTS"
                        placeholder="Enter comments on license">${package.licenseComments}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory">7.17 Copyright text</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="copyrightTextExist" type="radio"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT" value="EXIST">
                            <textarea style="flex: 6; margin-right: 1rem;" id="copyrightTextValue" rows="5"
                                class="form-control needs-validation" rule="required" type="text"
                                name="_sw360_portlet_components_COPYRIGHT_TEXT_VALUE"
                                placeholder="Enter copyright text">${package.copyrightText}</textarea>
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
                    <div id="copyrightTextValue-error-messages">
                        <div class="invalid-feedback" rule="required">
                            <liferay-ui:message key="this.field.must.be.not.empty" />
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="summary">7.18 Package summary description</label>
                    <textarea class="form-control" id="summary" rows="5"
                        name="_sw360_portlet_components_PACKAGE_SUMMARY"
                        placeholder="Enter package summary description">${package.summary}</textarea>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="description">7.19 Package detailed description</label>
                    <textarea class="form-control" id="description" rows="5"
                        name="_sw360_portlet_components_PACKAGE_DESCRIPTION"
                        placeholder="Enter package detailed description">${package.description}</textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="spdxPackageComment">7.20 Package comment</label>
                    <textarea class="form-control" id="spdxPackageComment" rows="5"
                        name="_sw360_portlet_components_PACKAGE_COMMENT"
                        placeholder="Enter package comment">${package.packageComment}</textarea>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group section section-external-ref">
                    <label>7.21 External references</label>
                    <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label style="text-decoration: underline;" class="sub-title">Select Reference</label>
                            <select type="text" class="form-control spdx-select" id="externalReferences"></select>
                            <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-externalRef" data-row-id="" viewBox="0 0 512 512">
                                <title><liferay-ui:message key="delete" /></title>
                                <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash" />
                            </svg>
                        </div>
                        <button class="spdx-add-button-main" name="add-externalRef">Add new Reference</button>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Category</label>
                            <select style="width: auto; flex: auto;" id="referenceCategory" type="text"
                                class="form-control" placeholder="Enter category"
                                name="_sw360_portlet_components_REFERENCE_CATEGORY">
                                <option value="SECURITY">SECURITY</option>
                                <option value="PACKAGE-MANAGER">PACKAGE-MANAGER</option>
                                <option value="PERSISTENT-ID">PERSISTENT-ID</option>
                                <option value="OTHER">OTHER</option>
                            </select>
                        </div>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Type</label>
                            <select style="width: auto; flex: auto;" id="referenceType-1" type="text"
                                class="form-control" placeholder="Enter type"
                                name="_sw360_portlet_components_REFERENCE_TYPE-1">
                                <option>cpe22Type</option>
                                <option>cpe23Type</option>
                            </select>
                            <input style="width: auto; flex: auto; display: none;" id="referenceType-2" type="text"
                                class="form-control" placeholder="Enter type"
                                name="_sw360_portlet_components_REFERENCE_TYPE-2">
                            </select>
                        </div>
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title">Locator</label>
                            <input style="width: auto; flex: auto;" type="text" class="form-control"
                                id="externalReferencesLocator" placeholder="Enter locator"
                                name="_sw360_portlet_components_REFERENCE_LOCATOR">
                        </div>
                        <div style="display: flex; flex-direction: row;">
                            <label class="sub-title">7.22 Comment</label>
                            <textarea style="width: auto; flex: auto;" type="text" rows="5" class="form-control"
                                id="externalReferencesComment" placeholder="Enter comment"
                                name="_sw360_portlet_components_REFERENCE_COMMENT"></textarea>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td colspan="3">
                <div class="form-group">
                    <label for="spdxPackageComment">7.23 Package attribution text</label>
                    <textarea class="form-control" id="spdxPackageAttributionText" rows="5"
                        name="_sw360_portlet_components_PACKAGE_COMMENT"
                        placeholder="Enter package attribution text"><sw360:out value="${package.attributionText.toString()}" stripNewlines="false" hashSet="true"/></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>