/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.licenses;

import org.eclipse.sw360.datahandler.services.licenses.ObligationNode;

public final class ObligationNodeConverter {

    private ObligationNodeConverter() {}

    public static ObligationNode fromThrift(org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode thrift) {
        if (thrift == null) {
            return null;
        }
        ObligationNode pojo = new ObligationNode();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetNodeText()) {
            pojo.setNodeText(thrift.getNodeText());
        }
        if (thrift.isSetNodeType()) {
            pojo.setNodeType(thrift.getNodeType());
        }
        if (thrift.isSetOblElementId()) {
            pojo.setOblElementId(thrift.getOblElementId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode toThrift(ObligationNode pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode thrift = new org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getNodeText() != null) {
            thrift.setNodeText(pojo.getNodeText());
        }
        if (pojo.getNodeType() != null) {
            thrift.setNodeType(pojo.getNodeType());
        }
        if (pojo.getOblElementId() != null) {
            thrift.setOblElementId(pojo.getOblElementId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
