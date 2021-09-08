<core_rt:set var="otherLicensing" value="${spdxDocument.otherLicensingInformationDetecteds}" />
<table class="table" id="editOtherLicensingInformationDetected">
  <thead>
    <tr>
      <th colspan="3">6. Other Licensing Information Detected</th>
    </tr>
  </thead>
  <tbody class="section section-other-licensing">
    <tr>
      <td>
        <div style="display: flex; flex-direction: column; padding-left: 1rem;">
          <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
            <label for="selectOtherLicensing" style="text-decoration: underline;" class="sub-title">Select
              Other Licensing</label>
            <select id="selectOtherLicensing" type="text" class="form-control spdx-select">
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
          <label class="mandatory" for="licenseId">
            6.1 License Identifier
          </label>
          <div style="display: flex">
            <label class="sub-label">LicenseRef-</label>
            <input id="licenseId" class="form-control needs-validation"
              name="_sw360_portlet_components_LICENSE_ID" type="text"
              placeholder="Enter License Identifier" value="">
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory" for="extractedText">6.2 Extracted Text</label>
          <textarea class="form-control" id="extractedText" rows="5"
            name="_sw360_portlet_components_EXTRACTED_TEXT" placeholder="Enter Extracted Text"></textarea>
        </div>
      </td>
    </tr>
    <tr>
      <td style="display: flex; flex-direction: column;">
        <div class="form-group">
          <label class="mandatory">6.3 License Name</label>
          <div style="display: flex; flex-direction: row;">
            <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
              <input class="spdx-radio" id="licenseNameExist" type="radio"
                name="_sw360_portlet_components_LICENSE_NAME" value="EXIST">
              <input style="flex: 6; margin-right: 1rem;" id="licenseName"
                class="form-control needs-validation" rule="isDownloadUrl" type="text"
                name="_sw360_portlet_components_LICENSE_NAME_VALUE" placeholder="Enter License Name">
            </div>
            <div style="flex: 2;">
              <input class="spdx-radio" id="licenseNameNoAssertion" type="radio"
                name="_sw360_portlet_components_LICENSE_NAME" value="NOASSERTION">
              <label class="form-check-label radio-label" for="licenseNameNoAssertion">NOASSERTION</label>
            </div>
          </div>
        </div>
      </td>
    </tr>
    <tr class="spdx-full">
      <td>
        <div class="form-group">
          <label for="licenseCrossRefs">6.4 License Cross Reference</label>
          <textarea class="form-control" id="licenseCrossRefs" rows="5"
            name="_sw360_portlet_components_LICENSE_CROSS_REFERENCE"
            placeholder="Enter License Cross Reference"></textarea>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label for="licenseComment">6.5 License Comment</label>
          <textarea class="form-control" id="licenseCommentOnOtherLicensing" rows="5"
            name="_sw360_portlet_components_LICENSE_COMMENT" placeholder="Enter License Comment"></textarea>
        </div>
      </td>
    </tr>
  </tbody>
</table>