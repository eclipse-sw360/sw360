<core_rt:set var="created" value="${spdxDocumentCreationInfo.created}" />
<core_rt:set var="creator" value="${spdxDocumentCreationInfo.creator}" />
<core_rt:set var="externalDocumentRefs" value="${spdxDocumentCreationInfo.externalDocumentRefs}" />
<table class="table spdx-table three-columns" id="editDocumentCreationInformation">
    <thead>
        <tr>
            <th colspan="3">
                <liferay-ui:message key="2.document.creation.information" />
            </th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="spdxVersion">
                        2.1 SPDX Version
                    </label>
                    <div style="display: flex">
                        <label class="sub-label">SPDX-</label>
                        <input id="spdxVersion" class="form-control needs-validation" rule="regex:^[0-9]+\.[0-9]+$"
                            name="_sw360_portlet_components_SPDX_VERSION" type="text" placeholder="Enter SPDX Version"
                            value="${spdxDocumentCreationInfo.spdxVersion}">
                    </div>
                    <div id="spdxVersion-error-messages">
                        <div class="invalid-feedback" rule="regex">
                            <liferay-ui:message key="formatting.must.be.SPDX-M.N" />
                        </div>
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="dataLicense">
                        2.2 Data License
                    </label>
                    <input id="dataLicense" class="form-control needs-validation" rule="regex:^[0-9a-zA-Z.-]+$"
                        name="_sw360_portlet_components_DATA_LICENSE" type="text" placeholder="<liferay-ui:message key="
                        enter.data.license" />"
                    value="
                    <sw360:out value="${spdxDocumentCreationInfo.dataLicense}" />">
                    <div id="dataLicense-error-messages">
                        <div class="invalid-feedback" rule="regex">
                            <liferay-ui:message key="string.containing.letters.numbers.and/or.-" />
                        </div>
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="spdxIdentifier">
                        2.3 SPDX Identifier
                    </label>
                    <div style="display: flex">
                        <label class="sub-label">SPDXRef-</label>
                        <input id="spdxIdentifier" class="form-control needs-validation" rule="regex:^[0-9a-zA-Z.-]+$"
                            name="_sw360_portlet_components_SPDX_IDENTIFIER" type="text"
                            placeholder="Enter SPDX Identifier" value="${spdxDocumentCreationInfo.SPDXID}">
                    </div>
                    <div id="spdxIdentifier-error-messages">
                        <div class="invalid-feedback" rule="regex">
                            <liferay-ui:message key="formatting.must.be.SPDXRef.document" />
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="documentName">
                        2.4. Document Name
                    </label>
                    <input id="documentName" name="_sw360_portlet_components_DOCUMENT_NAME" type="text"
                        class="form-control" placeholder="<liferay-ui:message key=" enter.spdx.document.name" />"
                    value="${spdxDocumentCreationInfo.name}">
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="documentNamespace">
                        2.5 SPDX Document Namespace
                    </label>
                    <input id="documentNamespace" class="form-control needs-validation" rule="isUrl"
                        name="_sw360_portlet_components_DOCUMENT_NAMESPACE" type="text"
                        placeholder="<liferay-ui:message key=" enter.spdx.document.namespace" />"
                    value="${spdxDocumentCreationInfo.documentNamespace}">
                </div>
                <div id="documentNamespace-error-messages">
                    <div class="invalid-feedback" rule="isUrl">
                        <liferay-ui:message key="formatting.must.be.an.URI" />
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td class="spdx-full">
                <div class="form-group section">
                    <label for="externalDocumentRefs">
                        2.6 External Document References
                    </label>
                    <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label for="externalDocumentRefs" style="text-decoration: underline;"
                                class="sub-title">Select Reference</label>
                            <select id="externalDocumentRefs" type="text" class="form-control spdx-select"
                                onchange="generateExternalDocumentRefsTable($(this).find('option:selected').text())">
                                <option>1</option>
                            </select>
                            <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-externalDocumentRef"
                                data-row-id="" onclick="deleteMain(this);" viewBox="0 0 512 512">
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
                        <button class="spdx-add-button-main" id="addNewReferenceBtn" onclick="addMain(this)">Add new
                            Reference</button>
                    </div>
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label class="sub-title" for="externalDocumentId">External Document ID</label>
                        <input id="externalDocumentId" style="width: auto; flex: auto;" type="text" class="form-control"
                            placeholder="Enter External Document ID">
                    </div>
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label class="sub-title" for="externalDocument">External Document</label>
                        <input id="externalDocument" style="width: auto; flex: auto;" type="text" class="form-control"
                            placeholder="Enter External Document">
                    </div>
                    <div style="display: flex;">
                        <label class="sub-title">Checksum</label>
                        <div style="display: flex; flex-direction: column; flex: 7">
                            <div style="display: flex; margin-bottom: 0.75rem;">
                                <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                                    id="checksumAlgorithm" placeholder="Enter Algorithm">
                                <input style="flex: 6;" type="text" class="form-control" id="checksumValue"
                                    placeholder="Enter Value">
                            </div>
                            <!-- <button class="spdx-add-button-sub" onclick="addSub(this)">Add new algorithm</button> -->
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr class="spdx-full">
            <td>
                <div class="form-group">
                    <label for="licenseListVersion">
                        2.7 License List Version
                    </label>
                    <input id="licenseListVersion" class="form-control needs-validation"
                        name="_sw360_portlet_components_LICENSE_LIST_VERSION" type="text"
                        placeholder="Enter License List Version" value="${spdxDocumentCreationInfo.licenseListVersion}">
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="creator">
                        2.8 Creator
                    </label>
                    <div style="display: flex; flex-direction: column;">
                        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                            <label class="sub-title" for="creator-anonymous">Anonymous</label>
                            <input id="creator-anonymous" class="spdx-checkbox" type="checkbox"
                                name="_sw360_portlet_components_CREATOR_ANONYNOUS">
                        </div>
                        <div style="display: flex;">
                            <label class="sub-title">List</label>
                            <div style="display: flex; flex-direction: column; flex: 7">
                                <div style="display: flex; margin-bottom: 0.75rem;" name="creatorRow">
                                    <select style="flex: 2; margin-right: 1rem;" type="text"
                                        class="form-control creator-type" placeholder="Enter Type"
                                        onchange="changeCreatorType(this)">
                                        <option value="Organization" selected>Organization</option>
                                        <option value="Person">Person</option>
                                        <option value="Tool">Tool</option>
                                    </select>
                                    <input style="flex: 6; margin-right: 2rem;" type="text"
                                        class="form-control creator-value" placeholder="Enter Value"
                                        value="">
                                    <svg class="disabled lexicon-icon spdx-delete-icon-sub"
                                        name="delete-spdxCreatorType-Person" data-row-id="" onclick="deleteSub(this);"
                                        viewBox="0 0 512 512">
                                        <title>Delete</title>
                                        <path class="lexicon-icon-outline lx-trash-body-border"
                                            d="M64.4,440.7c0,39.3,31.9,71.3,71.3,71.3h240.6c39.3,0,71.3-31.9,71.3-71.3v-312H64.4V440.7z M128.2,192.6h255.5v231.7c0,13.1-10.7,23.8-23.8,23.8H152c-13.1,0-23.8-10.7-23.8-23.8V192.6z">
                                        </path>
                                        <polygon class="lexicon-icon-outline lx-trash-lid"
                                            points="351.8,32.9 351.8,0 160.2,0 160.2,32.9 64.4,32.9 64.4,96.1 447.6,96.1 447.6,32.9 ">
                                        </polygon>
                                        <rect class="lexicon-icon-outline lx-trash-line-2" x="287.9" y="223.6"
                                            width="63.9" height="191.6"></rect>
                                        <rect class="lexicon-icon-outline lx-trash-line-1" x="160.2" y="223.6"
                                            width="63.9" height="191.6"></rect>
                                    </svg>
                                </div>
                                <button class="spdx-add-button-sub spdx-add-button-sub-creator"
                                    onclick="addSub(this)">Add new creator</button>
                            </div>
                        </div>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex; flex-direction: column;">
                <div class="form-group">
                    <label class="mandatory" for="createdDate">
                        2.9 Created
                    </label>
                    <div style="display: flex; flex-direction: row; margin-bottom: 12px;">
                        <input id="createdDate" type="date" class="form-control spdx-date"
                            name="_sw360_portlet_components_CREATED_DATE" placeholder="created.date.yyyy.mm.dd">
                        <input id="createdTime" type="time" step="1" class="form-control spdx-time"
                            name="_sw360_portlet_components_CREATED_TIME" placeholder="created.time.hh.mm.ss">
                    </div>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>

    generateCreatorTable();
    //setCreatorInput();
    setCreatedTime("${created}");
    generateExternalDocumentRefsTable("1");
    function generateCreatorTable() {
        <core_rt:if test="${not creator.isEmpty()}">
            <core_rt:forEach items="${creator}" var="creatorData" varStatus="loop">
                addRow("creatorRow", "${creatorData.type}", "${creatorData.value}", "");
            </core_rt:forEach>
                deleteSub(document.getElementsByName('delete-spdxCreatorType-Person')[0]);
        </core_rt:if>
    }

    function generateExternalDocumentRefsTable(index) {
        fillValueToId("externalDocumentId", "");
        fillValueToId("externalDocument", "");
        fillValueToId("checksumAlgorithm", "");
        fillValueToId("checksumValue", "");
        <core_rt:if test="${not externalDocumentRefs.isEmpty()}">
            var i = 0;
            <core_rt:forEach items="${externalDocumentRefs}" var="externalDocumentRefsData" varStatus="loop">
                i++;
                if (i == index) {
                    fillValueToId("externalDocumentId", "${externalDocumentRefsData.externalDocumentId}");
                    fillValueToId("externalDocument", "${externalDocumentRefsData.spdxDocument}");
                    fillValueToId("checksumAlgorithm", "${externalDocumentRefsData.checksum.algorithm}");
                    fillValueToId("checksumValue", "${externalDocumentRefsData.checksum.checksumValue}");
                }
            </core_rt:forEach>
        </core_rt:if>
    }

    function fillValueToId(id, value) {
        $('#' + id).prop('value', value);
    }
    function addRow(name, value1, value2, lable) {
        if ($(document.getElementsByName(name)).hasClass('disabled')) {
            return;
        }
        var size = document.getElementsByName(name).length;
        var el = document.getElementsByName(name)[size - 1];

        var clone = el.cloneNode(true);
        clone.getElementsByTagName('input')[0].name = clone.getElementsByTagName('input')[0].name + Date.now();
        clone.getElementsByTagName('select')[0].name = clone.getElementsByTagName('select')[0].name + Date.now();
        clone.getElementsByTagName('input')[0].value = value2;
        clone.getElementsByTagName('select')[0].value = value1;
        $(clone).insertAfter(el);
        if (size == 1) {
            enableAction('delete-' + name);
        }
    }

    function removeRow(el) {
        if ($(el).hasClass('disabled')) {
            return;
        }
        var parent = $(el).parent();
        var name = $(parent)[0].getAttribute("name");
        $(parent).remove();
        if (document.getElementsByName(name).length < 2) {
            disableAction('delete-' + name);
        }
    }

    function disableAction(name) {
        var el = document.getElementsByName(name);
        $(el).removeClass('action');
        $(el).addClass('disabled');
    }

    function enableAction(name) {
        var el = document.getElementsByName(name);
        var size = $(el).length;
        if (size > 1 || name.includes("add")) {
            $(el).removeClass('disabled');
            $(el).addClass('action');
        }
    }




    function setCreatedTime(created) {
        var createdDate = created.replace(/T.*/i, '');
        var createdTime = created.replace(createdDate, '');
        createdTime = createdTime.replace(/[A-Z]/g, '');
        $('#createdDate').prop('value', createdDate);
        $('#createdTime').prop('value', createdTime);
    }

    generateSelecterOption('externalDocumentRefs', "${externalDocumentRefs.size()}");
    function generateSelecterOption(selectId, length) {
        for (var i = 2; i <= length; i++) {
            var option = document.createElement("option");
            option.text = i;
            document.getElementById(selectId).add(option);
        }
    }

</script>