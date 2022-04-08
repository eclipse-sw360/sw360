<%--
    ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
    ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.

    ~ This program and the accompanying materials are made
    ~ available under the terms of the Eclipse Public License 2.0
    ~ which is available at https://www.eclipse.org/legal/epl-2.0/

    ~ SPDX-License-Identifier: EPL-2.0
--%>

<table class="table spdx-table" id="editOtherLicensingInformationDetected">
    <thead>
        <tr>
            <th colspan="3">10. Other Licensing Information Detected</th>
        </tr>
    </thead>
    <tbody id="sectionOtherLicensing" class="section section-other-licensing">
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectOtherLicensing" style="text-decoration: underline;" class="sub-title">Select Other Licensing</label>
                        <select id="selectOtherLicensing" type="text" class="form-control spdx-select"></select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-otherLicensing" data-row-id="" viewBox="0 0 512 512">
                            <title><liferay-ui:message key="delete" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main" name="add-otherLicensing">Add new Licensing</button>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="licenseId">10.1 License identifier</label>
                    <div style="display: flex">
                        <label class="sub-label">LicenseRef-</label>
                        <input id="licenseId" class="form-control needs-validation" rule="required"
                            type="text" placeholder="Enter license identifier">
                    </div>
                    <div id="licenseId-error-messages">
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
                    <label class="mandatory" for="extractedText">10.2 Extracted text</label>
                    <textarea class="form-control needs-validation" rule="required" id="extractedText" rows="5"
                        name="_sw360_portlet_components_EXTRACTED_TEXT" placeholder="Enter extracted text"></textarea>
                </div>
                <div id="extractedText-error-messages">
                    <div class="invalid-feedback" rule="required">
                        <liferay-ui:message key="this.field.must.be.not.empty" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label >10.3 License name</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseNameExist" type="radio" name="_sw360_portlet_components_LICENSE_NAME" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" id="licenseName"
                                class="form-control needs-validation" type="text"
                                placeholder="Enter license name">
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="licenseNameNoAssertion" type="radio" name="_sw360_portlet_components_LICENSE_NAME" value="NOASSERTION">
                            <label class="form-check-label radio-label" for="licenseNameNoAssertion">NOASSERTION</label>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="licenseCrossRefs">10.4 License cross reference</label>
                    <textarea class="form-control" id="licenseCrossRefs" rows="5"
                        placeholder="Enter license cross reference"></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="licenseComment">10.5 License comment</label>
                    <textarea class="form-control" id="licenseCommentOnOtherLicensing" rows="5"
                        placeholder="Enter license comment"></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>