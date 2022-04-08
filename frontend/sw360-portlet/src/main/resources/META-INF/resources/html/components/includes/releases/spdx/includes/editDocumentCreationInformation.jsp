<%--
    ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
    ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.

    ~ This program and the accompanying materials are made
    ~ available under the terms of the Eclipse Public License 2.0
    ~ which is available at https://www.eclipse.org/legal/epl-2.0/

    ~ SPDX-License-Identifier: EPL-2.0
--%>

<table class="table spdx-table" id="editDocumentCreationInformation">
    <thead>
        <tr>
            <th colspan="3">6. Document Creation Information</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label  for="spdxVersion">6.1 SPDX version</label>
                    <div style="display: flex">
                        <label class="sub-label">SPDX-</label>
                        <input id="spdxVersion" class="form-control needs-validation"
                          type="text" placeholder="Enter SPDX version"
                          value="<sw360:out value="${spdxDocumentCreationInfo.spdxVersion}" />">
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label for="dataLicense">6.2 Data license</label>
                    <input id="dataLicense" class="form-control needs-validation" type="text"
                        placeholder="Enter data license"
                        value="<sw360:out value="${spdxDocumentCreationInfo.dataLicense}" />">
                </div>
                <div class="form-group" style="flex: 1">
                    <label for="spdxIdentifier">6.3 SPDX identifier</label>
                        <div style="display: flex">
                            <label class="sub-label">SPDXRef-</label>
                            <input id="spdxIdentifier" class="form-control needs-validation" type="text"
                            placeholder="Enter SPDX identifier" value="<sw360:out value="${spdxDocumentCreationInfo.SPDXID}" />">
                        </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="documentName">6.4 Document name</label>
                    <input id="documentName" type="text"
                    class="form-control needs-validation"
                    placeholder="Enter document name" value="<sw360:out value="${spdxDocumentCreationInfo.name}" />">
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="documentNamespace">6.5 SPDX document namespace</label>
                    <input id="documentNamespace" class="form-control needs-validation" type="text"
                        placeholder="Enter SPDX document namespace"
                        value="<sw360:out value="${spdxDocumentCreationInfo.documentNamespace}" />">
                </div>
            </td>
        </tr>
        <tr>
            <td class="spdx-full">
                <div class="form-group section section-external-doc-ref">
                    <label for="externalDocumentRefs">6.6 External document references</label>
                    <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label for="externalDocumentRefs" style="text-decoration: underline;" class="sub-title">Select Reference</label>
                            <select id="externalDocumentRefs" type="text" class="form-control spdx-select"></select>
                            <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-externalDocRef" data-row-id="" viewBox="0 0 512 512">
                                <title><liferay-ui:message key="delete" /></title>
                                <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                            </svg>
                        </div>
                        <button class="spdx-add-button-main" name="add-externalDocRef">Add new Reference</button>
                    </div>
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label class="sub-title" for="externalDocumentId">External document ID</label>
                        <input id="externalDocumentId" style="width: auto; flex: auto;" type="text" class="form-control"
                          placeholder="Enter external document ID">
                    </div>
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label class="sub-title" for="externalDocument">External document</label>
                        <input id="externalDocument" style="width: auto; flex: auto;" type="text" class="form-control"
                          placeholder="Enter external document">
                    </div>
                    <div style="display: flex;">
                        <label class="sub-title">Checksum</label>
                        <div style="display: flex; flex-direction: column; flex: 7">
                            <div style="display: flex; margin-bottom: 0.75rem;">
                                <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                                id="checksumAlgorithm" placeholder="Enter algorithm">
                                <input style="flex: 6;" type="text" class="form-control" id="checksumValue"
                                placeholder="Enter value">
                            </div>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="licenseListVersion">6.7 License list version</label>
                    <input id="licenseListVersion" class="form-control" type="text"
                        placeholder="Enter license list version" value="<sw360:out value="${spdxDocumentCreationInfo.licenseListVersion}" />">
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="creator">6.8 Creators</label>
                    <div style="display: flex; flex-direction: column;">
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title" for="creator-anonymous">Anonymous</label>
                            <input id="creator-anonymous" class="spdx-checkbox" type="checkbox" onclick="setAnonymous()" >
                        </div>
                        <div style="display: flex;">
                            <label class="sub-title">List</label>
                            <div style="display: flex; flex-direction: column; flex: 7">
                                <div style="display: none; margin-bottom: 0.75rem;" name="creatorRow">
                                    <select style="flex: 2; margin-right: 1rem;" type="text"
                                        class="form-control creator-type" placeholder="Enter type"
                                        onchange="changeCreatorType(this)">
                                        <option value="Organization" selected>Organization</option>
                                        <option value="Person">Person</option>
                                        <option value="Tool">Tool</option>
                                    </select>
                                    <input style="flex: 6; margin-right: 2rem;" type="text"
                                        class="form-control creator-value" placeholder="Enter creator">
                                    <svg class="disabled lexicon-icon spdx-delete-icon-sub"
                                        name="delete-spdx-creator" data-row-id="" viewBox="0 0 512 512">
                                        <title><liferay-ui:message key="delete" /></title>
                                        <path class="lexicon-icon-outline lx-trash-body-border" d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z"></path>
                                        <polygon class="lexicon-icon-outline lx-trash-lid" points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 "></polygon>
                                        <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6" width="63.9" height="191.6"></rect>
                                        <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6" width="63.9" height="191.6"></rect>
                                    </svg>
                                </div>
                                <button class="spdx-add-button-sub spdx-add-button-sub-creator" name="add-spdx-creator">Add new creator</button>
                            </div>
                        </div>
                        <input id="spdxCreator" class="form-control" style="display: none" rule="required" type="text">
                        <div id="spdxCreator-error-messages">
                            <div class="invalid-feedback" rule="required">
                                <liferay-ui:message key="this.field.must.be.not.empty" />
                            </div>
                        </div>
                    </div>
                </div>
          </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label for="createdDate">6.9 Created</label>
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <div>
                            <input id="createdDate" type="date" class="form-control spdx-date needs-validation"
                                placeholder="created.date.yyyy.mm.dd">
                        </div>
                        <div>
                            <input id="createdTime" type="time" step="1" class="form-control spdx-time needs-validation"
                               placeholder="created.time.hh.mm.ss">
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td colspan="3">
                <div class="form-group">
                    <label for="creatorComment">6.10 Creator comment</label>
                    <textarea class="form-control" id="creatorComment" rows="5"
                        placeholder="Enter creator comment"></textarea>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td colspan="3">
                <div class="form-group">
                    <label for="documentComment">6.11 Document comment</label>
                    <textarea class="form-control" id="documentComment" rows="5"
                        placeholder="Enter document comment"></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>
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
    function changeCreatorType(selectbox) {
        if ($('#creator-anonymous').is(':checked') &&
            ($(selectbox).val() == 'Organization' || $(selectbox).val() == 'Person')) {
            $(selectbox).attr('disabled', 'true');
            $(selectbox).next().attr('disabled', 'true');
            $(selectbox).next().next().css('cursor', 'not-allowed');
        }
    }
</script>