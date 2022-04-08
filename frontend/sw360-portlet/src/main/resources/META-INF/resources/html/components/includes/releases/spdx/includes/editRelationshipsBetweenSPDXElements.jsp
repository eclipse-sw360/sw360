<%--
    ~ Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
    ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.

    ~ This program and the accompanying materials are made
    ~ available under the terms of the Eclipse Public License 2.0
    ~ which is available at https://www.eclipse.org/legal/epl-2.0/

    ~ SPDX-License-Identifier: EPL-2.0
--%>

<table class="table spdx-table spdx-full" id="editRelationshipsBetweenSPDXElements">
    <thead>
        <tr>
            <th colspan="3">11. Relationship between SPDX Elements Information</th>
        </tr>
    </thead>
    <tbody class="section section-relationship">
        <tr>
            <td>
                <div style="display: flex; flex-direction: row; margin-bottom: 0.75rem; padding-left: 1rem;">
                    <label for="selectRelationshipSource" style="text-decoration: underline;" class="sub-title">Select Source</label>
                    <select id="selectRelationshipSource" type="text" class="form-control spdx-select always-enable" style="margin-right: 4rem;">
                        <option>SPDX Document</option>
                        <option>Package</option>
                    </select>
                </div>
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
                    <label for="spdxElement">11.1 Relationship</label>
                    <div style="display: flex">
                        <input style="margin-right: 1rem;" id="spdxElement" class="form-control"
                            name="_sw360_portlet_components_LICENSE_ID" type="text" placeholder="Enter SPDX element">
                        <select class="form-control" id="relationshipType" style="margin-right: 1rem;" >
                            <option value="" selected=""></option>
                            <core_rt:forEach items="${setRelationshipType}" var="entry">
                                <option value="${entry}" class="textlabel stackedLabel">${entry}</option>
                            </core_rt:forEach>
                        </select>
                        <input id="relatedSPDXElement" class="form-control" name="_sw360_portlet_components_LICENSE_ID"
                            type="text" placeholder="Enter related SPDX element">
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="form-group">
                    <label for="relationshipComment">11.2 Relationship comment</label>
                    <textarea class="form-control" id="relationshipComment" rows="5"
                        name="_sw360_portlet_components_RELATIONSHIP_COMMENT"
                        placeholder="Enter relationship comment"></textarea>
                </div>
            </td>
        </tr>
    </tbody>
</table>