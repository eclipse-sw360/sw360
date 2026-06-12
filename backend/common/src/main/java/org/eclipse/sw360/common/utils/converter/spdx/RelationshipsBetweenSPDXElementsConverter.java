/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.spdx;

import org.eclipse.sw360.datahandler.services.spdx.RelationshipsBetweenSPDXElements;

public final class RelationshipsBetweenSPDXElementsConverter {

    private RelationshipsBetweenSPDXElementsConverter() {}

    public static RelationshipsBetweenSPDXElements fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements thrift) {
        if (thrift == null) {
            return null;
        }
        RelationshipsBetweenSPDXElements pojo = new RelationshipsBetweenSPDXElements();
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetRelatedSpdxElement()) {
            pojo.setRelatedSpdxElement(thrift.getRelatedSpdxElement());
        }
        if (thrift.isSetRelationshipComment()) {
            pojo.setRelationshipComment(thrift.getRelationshipComment());
        }
        if (thrift.isSetRelationshipType()) {
            pojo.setRelationshipType(thrift.getRelationshipType());
        }
        if (thrift.isSetSpdxElementId()) {
            pojo.setSpdxElementId(thrift.getSpdxElementId());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements toThrift(RelationshipsBetweenSPDXElements pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements thrift = new org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements();
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getRelatedSpdxElement() != null) {
            thrift.setRelatedSpdxElement(pojo.getRelatedSpdxElement());
        }
        if (pojo.getRelationshipComment() != null) {
            thrift.setRelationshipComment(pojo.getRelationshipComment());
        }
        if (pojo.getRelationshipType() != null) {
            thrift.setRelationshipType(pojo.getRelationshipType());
        }
        if (pojo.getSpdxElementId() != null) {
            thrift.setSpdxElementId(pojo.getSpdxElementId());
        }
        return thrift;
    }
}
