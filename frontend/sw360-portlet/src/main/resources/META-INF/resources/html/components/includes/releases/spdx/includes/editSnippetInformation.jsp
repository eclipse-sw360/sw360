<%--
    ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
    ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.

    ~ This program and the accompanying materials are made
    ~ available under the terms of the Eclipse Public License 2.0
    ~ which is available at https://www.eclipse.org/legal/epl-2.0/

    ~ SPDX-License-Identifier: EPL-2.0
--%>

<table class="table spdx-table spdx-full" id="editSnippetInformation">
    <thead>
        <tr>
            <th>9. Snippet Information</th>
        </tr>
    </thead>
    <tbody id ="sectionSnippet" class="section section-snippet">
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectSnippet" style="text-decoration: underline;" class="sub-title">Select Snippet</label>
                        <select id="selectSnippet" type="text" class="form-control spdx-select"></select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-snippet" data-row-id="" viewBox="0 0 512 512">
                            <title><liferay-ui:message key="delete" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main" name="add-snippet">Add new Snippet</button>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label for="snippetSpdxIdentifier">9.1 Snippet SPDX identifier</label>
                    <div style="display: flex">
                        <label class="sub-label">SPDXRef-</label>
                        <input id="snippetSpdxIdentifier" class="form-control"
                            name="_sw360_portlet_components_SPDXSPDX_IDENTIFIER"
                            type="text" placeholder="Enter snippet SPDX identifier" >
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label  for="snippetFromFile">9.2 Snippet from file SPDX identifier</label>
                    <div style="display: flex">
                        <select id="snippetFromFile" class="form-control" style="flex: 1;">
                            <option>SPDXRef</option>
                            <option>DocumentRef</option>
                        </select>
                        <div style="margin: 0.5rem;">-</div>
                        <input style="flex: 3;" id="snippetFromFileValue" class="form-control"
                            name="_sw360_portlet_components_SPDXSPDX_VERSION" type="text"
                            placeholder="Enter snippet from file SPDX identifier" >
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label >9.3 & 9.4 Snippet ranges</label>
                    <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                        <div style="display: none; margin-bottom: 0.75rem;" name="snippetRange">
                            <select style="flex: 1; margin-right: 1rem;" type="text" class="form-control range-type" placeholder="Enter type">
                                <option value="BYTE" selected>BYTE</option>
                                <option value="LINE">LINE</option>
                            </select>
                            <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control start-pointer" placeholder="Enter start pointer">
                            <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control end-pointer" placeholder="Enter end pointer">
                            <input style="flex: 4; margin-right: 2rem;" type="text" class="form-control reference" placeholder="Enter reference">
                            <svg class="lexicon-icon spdx-delete-icon-sub hidden" name="delete-snippetRange" data-row-id="" viewBox="0 0 512 512">
                                <title><liferay-ui:message key="delete" /></title>
                                <path class="lexicon-icon-outline lx-trash-body-border" d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z"></path>
                                <polygon class="lexicon-icon-outline lx-trash-lid" points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 "></polygon>
                                <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6" width="63.9" height="191.6"></rect>
                                <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6" width="63.9" height="191.6"></rect>
                            </svg>
                        </div>
                        <button id="addNewRange" class="spdx-add-button-sub">Add new Range</button>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label >9.5 Snippet concluded license</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="spdxConcludedLicenseExist" type="radio"
                                name="_sw360_portlet_components_CONCLUDED_LICENSE" value="EXIST">
                            <input style="flex: 6; margin-right: 1rem;" id="spdxConcludedLicenseValue"
                                class="form-control" type="text"
                                name="_sw360_portlet_components_CONCLUDED_LICENSE_VALUE"
                                placeholder="Enter snippet concluded license">
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
                    <label>9.6 License information in snippet</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="licenseInfoInFile" type="radio"
                                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="EXIST">
                            <textarea style="flex: 6; margin-right: 1rem;" id="licenseInfoInFileValue" rows="5"
                                class="form-control" type="text"
                                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE_SOURCE"
                                placeholder="Enter license information in snippet"></textarea>
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
                    <label for="snippetLicenseComments">9.7 Snippet comments on license</label>
                    <textarea class="form-control" id="snippetLicenseComments" rows="5"
                        placeholder="Enter snippet comments on license"></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label >9.8 Snippet copyright text</label>
                    <div style="display: flex; flex-direction: row;">
                        <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
                            <input class="spdx-radio" id="snippetCopyrightText" type="radio" name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="EXIST">
                            <textarea style="flex: 6; margin-right: 1rem;" id="copyrightTextValueSnippet" rows="5"
                                class="form-control" type="text"
                                placeholder="Enter snippet copyright text"></textarea>
                        </div>
                        <div style="flex: 2;">
                            <input class="spdx-radio" id="snippetCopyrightTextNone" type="radio" name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NONE">
                            <label style="margin-right: 2rem;" class="form-check-label radio-label"
                                for="snippetCopyrightTextNone">NONE</label>
                            <input class="spdx-radio" id="snippetCopyrightTextNoAssertion" type="radio" name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NOASSERTION">
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
                    <label for="snippetComment">9.9 Snippet comment</label>
                    <textarea class="form-control" id="snippetComment" rows="5"
                        placeholder="Enter snippet comment"></textarea>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="snippetName">9.10 Snippet name</label>
                    <input id="snippetName" type="text"
                        class="form-control" placeholder="Enter snippet name" >
                </div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="form-group">
                    <label for="snippetAttributionText">9.11 Snippet attribution text</label>
                    <textarea class="form-control" id="snippetAttributionText" rows="5"
                        placeholder="Enter snippet attribution text"></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>