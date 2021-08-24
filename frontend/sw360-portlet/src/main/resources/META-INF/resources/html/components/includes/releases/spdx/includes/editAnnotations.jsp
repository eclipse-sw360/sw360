<core_rt:set var="annotations" value="${spdxDocument.annotations}" />
<table class="table three-columns" id="editAnnotations">
    <thead>
        <tr>
            <th colspan="3">8. Annotations</th>
        </tr>
    </thead>
    <tbody class="section">
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectAnnotation" style="text-decoration: underline;" class="sub-title">Select
                            Annotation</label>
                        <select id="selectAnnotation" type="text" class="form-control spdx-select" onclick="generateAnnotationsTable($(this).find('option:selected').text())">
                            <option>1</option>
                        </select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-spdxCreatorType-Person"
                            data-row-id="" onclick="deleteMain(this)" viewBox="0 0 512 512">
                            <title><liferay-ui:message key="delete" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main" onclick="addMain(this)">Add new Annotation</button>
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
        fillValueToId("annotatorType", "");
        fillValueToId("annotatorValue", "");
        setCreatedDateTime("");
        fillValueToId("annotationType", "");
        fillValueToId("spdxIdRef", "");
        fillValueToId("annotationComment", "");
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