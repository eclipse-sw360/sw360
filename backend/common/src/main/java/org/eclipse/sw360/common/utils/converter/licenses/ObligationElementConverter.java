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

import org.eclipse.sw360.datahandler.services.licenses.ObligationElement;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class ObligationElementConverter {

    private ObligationElementConverter() {}

    public static ObligationElement fromThrift(org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement thrift) {
        if (thrift == null) {
            return null;
        }
        ObligationElement pojo = new ObligationElement();
        if (thrift.isSetAction()) {
            pojo.setAction(thrift.getAction());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLangElement()) {
            pojo.setLangElement(thrift.getLangElement());
        }
        if (thrift.isSetObject()) {
            pojo.setObject(thrift.getObject());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetStatus()) {
            pojo.setStatus(EnumConverter.fromThrift(thrift.getStatus(), org.eclipse.sw360.datahandler.services.licenses.ObligationElementStatus.class));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement toThrift(ObligationElement pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement thrift = new org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement();
        if (pojo.getAction() != null) {
            thrift.setAction(pojo.getAction());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLangElement() != null) {
            thrift.setLangElement(pojo.getLangElement());
        }
        if (pojo.getObject() != null) {
            thrift.setObject(pojo.getObject());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getStatus() != null) {
            thrift.setStatus(EnumConverter.toThrift(pojo.getStatus(), org.eclipse.sw360.datahandler.thrift.licenses.ObligationElementStatus.class));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
