<table class="table spdx-table" id="editRelationshipsBetweenSPDXElements">
  <thead>
    <tr>
      <th colspan="3">7. Relationships between SPDX Elements</th>
    </tr>
  </thead>
  <tbody class="section section-relationship">
    <tr>
      <td>
        <div style="display: flex; flex-direction: column; padding-left: 1rem;">
          <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
            <label for="selectRelationship" style="text-decoration: underline;" class="sub-title">Select Relationship</label>
            <select id="selectRelationship" type="text" class="form-control spdx-select"></select>
            <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-relationship" data-row-id="" viewBox="0 0 512 512">
              <title><liferay-ui:message key="delete" /></title>
              <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
            </svg>
          </div>
          <button class="spdx-add-button-main" name="add-relationship">Add new Relationship</button>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label class="mandatory" for="spdxElement">7.1 Relationship</label>
          <div style="display: flex">
            <input style="margin-right: 1rem;" id="spdxElement" class="form-control"
              name="_sw360_portlet_components_LICENSE_ID" type="text" placeholder="Enter SPDX Element"
             >
            <input style="margin-right: 1rem;" id="relationshipType" class="form-control"
              name="_sw360_portlet_components_LICENSE_ID" type="text"
              placeholder="Enter Relationship Type">
            <input id="relatedSPDXElement" class="form-control" name="_sw360_portlet_components_LICENSE_ID"
              type="text" placeholder="Enter Related SPDX Element">
          </div>
        </div>
      </td>
    </tr>
    <tr>
      <td>
        <div class="form-group">
          <label for="relationshipComment">7.2 Relationship Comment</label>
          <textarea class="form-control" id="relationshipComment" rows="5"
            name="_sw360_portlet_components_RELATIONSHIP_COMMENT"
            placeholder="Enter License Comment"></textarea>
        </div>
      </td>
    </tr>
  </tbody>
</table>