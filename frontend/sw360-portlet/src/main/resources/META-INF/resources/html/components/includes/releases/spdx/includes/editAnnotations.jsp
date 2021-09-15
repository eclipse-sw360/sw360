<table class="table spdx-table spdx-full" id="editAnnotations">
  <thead>
    <tr>
      <th colspan="3">8. Annotations</th>
    </tr>
  </thead>
  <tbody class="section section-annotation">
    <tr>
      <td>
        <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem; padding-left: 1rem;">
          <label for="selectAnnotationSource" style="text-decoration: underline;" class="sub-title">Select Source</label>
          <select id="selectAnnotationSource" type="text" class="form-control spdx-select always-enable" style="margin-right: 4rem;">
            <option>SPDX Document</option>
            <option>Package</option>
          </select>
        </div>
        <div style="display: flex; flex-direction: column; padding-left: 1rem;">
          <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
            <label for="selectAnnotation" style="text-decoration: underline;" class="sub-title">Select Annotation</label>
            <select id="selectAnnotation" type="text" class="form-control spdx-select"></select>
            <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-annotation" data-row-id="" viewBox="0 0 512 512">
              <title><liferay-ui:message key="delete" /></title>
              <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
            </svg>
          </div>
          <button class="spdx-add-button-main" name="add-annotation">Add new Annotation</button>
        </div>
      </td>
    </tr>
    <tr>
      <td style="display: flex">
        <div class="form-group" style="flex: 3">
          <label class="mandatory" for="annotator">8.1 Annotator</label>
          <div style="display: flex">
            <select id="annotatorType" style="flex: 2; margin-right: 1rem;" type="text" class="form-control" placeholder="Enter Type">
              <option value="Organization" selected="">Organization</option>
              <option value="Person">Person</option>
              <option value="Tool">Tool</option>
            </select>
            <input style="flex: 6; margin-right: 1rem;" id="annotatorValue" type="text" class="form-control"
              placeholder="Enter Value" >
          </div>
        </div>
        <div class="form-group" style="flex: 1">
          <label class="mandatory" for="annotationCreatedDate">8.2 Annotation Date </label>
          <div style="display: flex">
            <input id="annotationCreatedDate" style="width: 12rem; text-align: center;" type="date"
              class="form-control" placeholder="creation.date.yyyy.mm.dd" >
            <input id="annotationCreatedTime" style="width: 12rem; text-align: center; margin-left: 10px;"
              type="time" step="1" class="form-control" placeholder="creation.time.hh.mm.ss" >
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td style="display: flex">
        <div class="form-group" style="flex: 1">
          <label class="mandatory" for="annotationType">8.3 Annotation Type</label>
          <input id="annotationType" class="form-control" type="text" placeholder="Enter Annotation Type" >
        </div>
        <div class="form-group" style="flex: 1">
          <label class="mandatory" for="spdxIdRef">8.4 SPDX Identifier Reference</label>
          <input id="spdxIdRef" class="form-control" type="text"
            placeholder="Enter SPDX Identifier Reference" >
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory" for="annotationComment">8.5 Annotation Comment</label>
          <textarea class="form-control" id="annotationComment" rows="5"
            placeholder="Enter License Comment"></textarea>
        </div>
      </td>
    </tr>
  </tbody>
</table>