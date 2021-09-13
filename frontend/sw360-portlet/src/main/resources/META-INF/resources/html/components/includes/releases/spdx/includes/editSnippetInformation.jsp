<table class="table spdx-table" id="editSnippetInformation">
  <thead>
    <tr>
      <th>5. Snippet Information</th>
    </tr>
  </thead>
  <tbody class="section section-snippet">
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
          <label class="mandatory" for="snippetSpdxIdentifier">
            5.1 Snippet SPDX Identifier
          </label>
          <div style="display: flex">
            <label class="sub-label">SPDXRef-</label>
            <input id="snippetSpdxIdentifier" class="form-control needs-validation"
              rule="regex:^[0-9a-zA-Z.-]+$" name="_sw360_portlet_components_SPDXSPDX_IDENTIFIER"
              type="text" placeholder="Enter Snippet SPDX Identifier" >
          </div>
        </div>
        <div class="form-group" style="flex: 1">
          <label class="mandatory" for="snippetFromFile">
            5.2 Snippet from File SPDX Identifier
          </label>
          <div style="display: flex">
            <select id="snippetFromFile" class="form-control" style="flex: 1;">
              <option>SPDXRef</option>
              <option>DocumentRef</option>
            </select>
            <div style="margin: 0.5rem;">-</div>
            <input style="flex: 3;" id="snippetFromFileValue" class="form-control needs-validation"
              rule="regex:^[0-9]+\.[0-9]+$" name="_sw360_portlet_components_SPDXSPDX_VERSION" type="text"
              placeholder="Enter Snippet from File SPDX Identifier" >
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory">
            5.3 & 5.4 Snippet Ranges
          </label>
          <div style="display: flex; flex-direction: column; padding-left: 1rem;">
            <div style="display: none; margin-bottom: 0.75rem;" name="snippetRange">
              <select style="flex: 1; margin-right: 1rem;" type="text" class="form-control range-type" placeholder="Enter Type">
                <option value="BYTE" selected>BYTE</option>
                <option value="LINE">LINE</option>
              </select>
              <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control start-pointer" placeholder="Enter Start Pointer">
              <input style="flex: 2; margin-right: 1rem;" type="text" class="form-control end-pointer" placeholder="Enter End Pointer">
              <input style="flex: 4; margin-right: 2rem;" type="text" class="form-control reference" placeholder="Enter Reference">
              <svg class="lexicon-icon spdx-delete-icon-sub hidden" name="delete-snippetRange" data-row-id="" viewBox="0 0 512 512">
                <title><liferay-ui:message key="delete" /></title>
                <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
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
          <label class="mandatory">5.5 Snippet Concluded License</label>
          <div style="display: flex; flex-direction: row;">
            <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
              <input class="spdx-radio" id="spdxConcludedLicenseExist" type="radio"
                name="_sw360_portlet_components_CONCLUDED_LICENSE" value="EXIST">
              <input style="flex: 6; margin-right: 1rem;" id="spdxConcludedLicenseValue"
                class="form-control needs-validation" type="text"
                name="_sw360_portlet_components_CONCLUDED_LICENSE_VALUE"
                placeholder="Enter Snippet Concluded License">
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
          <label class="mandatory">5.6 License Information in Snippet</label>
          <div style="display: flex; flex-direction: row;">
            <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
              <input class="spdx-radio" id="licenseInfoInFile" type="radio"
                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE" value="EXIST">
              <textarea style="flex: 6; margin-right: 1rem;" id="licenseInfoInFileValue" rows="5"
                class="form-control needs-validation" type="text"
                name="_sw360_portlet_components_LICENSE_INFO_IN_FILE_SOURCE"
                placeholder="Enter License Information in Snippet"></textarea>
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
          <label for="snippetLicenseComments">5.7 Snippet Comments on License</label>
          <textarea class="form-control" id="snippetLicenseComments" rows="5"
            name="_sw360_portlet_components_SNIPPET_LICENSE_COMMENTS"
            placeholder="Enter Snippet Comments on License"></textarea>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory">5.8 Copyright Text</label>
          <div style="display: flex; flex-direction: row;">
            <div style="display: inline-flex; flex: 3; margin-right: 1rem;">
              <input class="spdx-radio" id="snippetCopyrightText" type="radio"
                name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="EXIST">
              <textarea style="flex: 6; margin-right: 1rem;" id="copyrightTextValueSnippet" rows="5"
                class="form-control needs-validation" type="text"
                name="_sw360_portlet_components_COPYRIGHT_TEXT_VALUE"
                placeholder="Enter Copyright Text"></textarea>
            </div>
            <div style="flex: 2;">
              <input class="spdx-radio" id="snippetCopyrightTextNone" type="radio"
                name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NONE">
              <label style="margin-right: 2rem;" class="form-check-label radio-label"
                for="snippetCopyrightTextNone">NONE</label>
              <input class="spdx-radio" id="snippetCopyrightTextNoAssertion" type="radio"
                name="_sw360_portlet_components_SNIPPET_COPYRIGHT_TEXT" value="NOASSERTION">
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
          <label for="snippetComment">5.9 Snippet Comments</label>
          <textarea class="form-control" id="snippetComment" rows="5"
            name="_sw360_portlet_components_SNIPPET_COMMENT"
            placeholder="Enter Snippet Comments"></textarea>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory" for="snippetName">
            5.10 Snippet Name
          </label>
          <input id="snippetName" name="_sw360_portlet_components_SPDXSPDX_NAME" type="text"
            class="form-control" placeholder="Enter Snippet Name" >
        </div>
      </td>
    </tr>
    <tr>
      <td colspan="3">
        <div class="form-group">
          <label for="snippetAttributionText">5.11 Snippet Attribution Text</label>
          <textarea class="form-control" id="snippetAttributionText" rows="5"
            name="_sw360_portlet_components_SNIPPET_ATTRIBUTION_TEXT"
            placeholder="Enter Snippet Attribution Text"></textarea>
        </div>
      </td>
    </tr>
  </tbody>
</table>