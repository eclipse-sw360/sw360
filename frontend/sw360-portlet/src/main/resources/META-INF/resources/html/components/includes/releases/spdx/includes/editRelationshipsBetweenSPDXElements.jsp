<core_rt:set var="relationships" value="${spdxDocument.relationships}" />
<table class="table three-columns" id="editRelationshipsBetweenSPDXElements">
    <thead>
        <tr>
            <th colspan="3">7. Relationships between SPDX Elements</th>
        </tr>
    </thead>
    <tbody class="section">
        <tr>
            <td>
                <div style="display: flex; flex-direction: column; padding-left: 1rem;">
                    <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem;">
                        <label for="selectRelationship" style="text-decoration: underline;" class="sub-title">Select
                            Relationship</label>
                        <select id="selectRelationship" type="text" class="form-control spdx-select"
                            onchange="generateRelationshipTable($(this).find('option:selected').text())">
                        </select>
                        <svg class="disabled lexicon-icon spdx-delete-icon-main" name="delete-spdxCreatorType-Person"
                            data-row-id="" onclick="deleteMain(this)" viewBox="0 0 512 512">
                            <title><liferay-ui:message key="delete" /></title>
                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                        </svg>
                    </div>
                    <button class="spdx-add-button-main" onclick="addMain(this)">Add new Relationship</button>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label class="mandatory" for="spdxElement">
                        7.1 Relationship
                    </label>
                    <div style="display: flex">
                        <input style="margin-right: 1rem;" id="spdxElement" class="form-control"
                            name="_sw360_portlet_components_LICENSE_ID" type="text" placeholder="Enter SPDX Element"
                            value="">
                        <input style="margin-right: 1rem;" id="relationshipType" class="form-control"
                            name="_sw360_portlet_components_LICENSE_ID" type="text"
                            placeholder="Enter Relationship Type" value="">
                        <input id="relatedSPDXElement" class="form-control" name="_sw360_portlet_components_LICENSE_ID"
                            type="text" placeholder="Enter Related SPDX Element" value="">
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
<script>
    $(function () {
        // ------------------------- 7 Relationships between SPDX Elements
        // Add data
        $('[name=add-relationship]').on('click', function(e) {
            let newObj = { 'spdxElementId': '', 'relationshipType': '', 'relatedSpdxElement': '', 'relationshipComment': '' };
            spdxDocumentObj.relationships.push(newObj);
            addMain($(this));
            $('#selectRelationship').change();
        });

        // Delete data
        $('[name=delete-relationship').on('click', function(e) {
            let selectedIndex = $('#selectRelationship')[0].selectedIndex;
            spdxDocumentObj.relationships.splice(selectedIndex, 1);
            deleteMain($(this));
        });

        // Change data
        $('#selectRelationship').on('change', function(e) {
            let selectedIndex = $('#selectRelationship')[0].selectedIndex;
            fillRelationship(selectedIndex);
        });

        function fillRelationship(index) {
            let obj = spdxDocumentObj.relationships[index];
            $('#spdxElement').val(obj.spdxElementId);
            if (obj.relationshipType.startsWith('relationshipType_')) {
                $('#relationshipType').val(obj.relationshipType.substr(17).toUpperCase());
            } else {
                $('#relationshipType').val('');
            }

            $('#relatedSPDXElement').val(obj.relatedSpdxElement);
            $('#relationshipComment').val(obj.relationshipComment);
        }

        function storeRelationship(index) {
            let obj = spdxDocumentObj.relationships[index];

            obj['spdxElementId'] = $('#spdxElement').val().trim();
            obj['relationshipType'] = 'relationshipType_' + $('#relationshipType').val().toLowerCase().trim();
            obj['relatedSpdxElement'] = $('#relatedSPDXElement').val().trim();
            obj['relationshipComment'] = $('#relationshipComment').val().trim();
        }
    });
</script>