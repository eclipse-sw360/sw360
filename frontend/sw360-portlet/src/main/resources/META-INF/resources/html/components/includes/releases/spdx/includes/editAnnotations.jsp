<core_rt:set var="annotations" value="${spdxDocument.annotations}" />
<table class="table three-columns" id="editAnnotations">
    <thead>
        <tr>
            <th colspan="3">8. Annotations</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectAnnotation" style="text-decoration: underline;" class="sub-title">Select
                            Annotation</label>
                        <select id="selectAnnotation" type="text" class="form-control spdx-select">
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
                    <button class="spdx-add-button-main">Add new Annotation</button>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 3">
                    <label class="mandatory" for="annotator">
                        8.1 Annotator
                    </label>
                    <div style="display: flex">
                        <select id="annotatorType" style="flex: 2; margin-right: 1rem;" type="text" class="form-control"
                            placeholder="Enter Type"
                            onchange="generateSelecterOption($(this).find('option:selected').text())">
                            <option value="Organization" selected>Organization</option>
                            <option value="Person">Person</option>
                            <option value="Tool">Tool</option>
                        </select>
                        <input style="flex: 6; margin-right: 1rem;" id="annotatorValue" type="text" class="form-control"
                            placeholder="Enter Value" value="">
                    </div>
                </div>
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="annotationCreatedDate">
                        8.2 Annotation Date
                    </label>
                    <div style="display: flex">
                        <input id="annotationCreatedDate" style="width: 12rem; text-align: center;" type="date"
                            class="form-control" name="_sw360_portlet_components_SPDX_CREATED_DATE"
                            placeholder="creation.date.yyyy.mm.dd" value="">
                        <input id="annotationCreatedTime" style="width: 12rem; text-align: center; margin-left: 10px;"
                            type="time" step="1" class="form-control" name="_sw360_portlet_components_SPDX_CREATED_TIME"
                            placeholder="creation.time.hh.mm.ss" value="">
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td style="display: flex">
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="annotationType">
                        8.3 Annotation Type
                    </label>
                    <input id="annotationType" class="form-control needs-validation"
                        name="_sw360_portlet_components_ANNOTATION_TYPE" type="text" placeholder="Enter Annotation Type"
                        value="">
                </div>
                <div class="form-group" style="flex: 1">
                    <label class="mandatory" for="spdxIdRef">
                        8.4 SPDX Identifier Reference
                    </label>
                    <input id="spdxIdRef" class="form-control needs-validation"
                        name="_sw360_portlet_components_SPDX_IDENTIFIER_REF" type="text"
                        placeholder="Enter SPDX Identifier Reference" value="">
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="annotationComment">8.5 Annotation Comment</label>
                    <textarea class="form-control" id="annotationComment" rows="5"
                        name="_sw360_portlet_components_ANNOTATION_COMMENT"
                        placeholder="Enter License Comment"></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>

<script>
    generateAnnotationsTable('1');
    function generateAnnotationsTable(index) {
        <core_rt:if test="${not annotations.isEmpty()}">
            var i = 0;
        <core_rt:forEach items="${annotations}" var="annotationsData" varStatus="loop">
                i++;
            if (i == index) {
                var annotator = "${annotationsData.annotator}";
                annotatorType = annotator.replace(/:.*/, '');
                annotatorValue = annotator.replace(annotatorType + ':', '')
                fillValueToId("annotatorType", annotatorType);
                fillValueToId("annotatorValue", annotatorValue);
                setCreatedDateTime("${annotationsData.annotationDate}");
                fillValueToId("annotationType", "${annotationsData.annotationType}");
                fillValueToId("spdxIdRef", "${annotationsData.spdxRef}");
                fillValueToId("annotationComment", "${annotationsData.annotationComment}");
            }
        </core_rt:forEach>
        </core_rt:if>
    }

    function setCreatedDateTime(created) {
        var createdDate = created.replace(/T.*/i, '');
        var createdTime = created.replace(createdDate, '');
        createdTime = createdTime.replace(/[A-Z]/g, '');
        $('#annotationCreatedDate').prop('value', createdDate);
        $('#annotationCreatedTime').prop('value', createdTime);
    }

    generateSelecterOption('selectAnnotation', "${annotations.size()}");

</script>