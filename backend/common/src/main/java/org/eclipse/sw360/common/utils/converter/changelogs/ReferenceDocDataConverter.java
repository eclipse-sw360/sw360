/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.changelogs;

import org.eclipse.sw360.datahandler.services.changelogs.ReferenceDocData;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class ReferenceDocDataConverter {

    private ReferenceDocDataConverter() {}

    public static ReferenceDocData fromThrift(org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData thrift) {
        if (thrift == null) {
            return null;
        }
        ReferenceDocData pojo = new ReferenceDocData();
        if (thrift.isSetDbName()) {
            pojo.setDbName(thrift.getDbName());
        }
        if (thrift.isSetRefDocId()) {
            pojo.setRefDocId(thrift.getRefDocId());
        }
        if (thrift.isSetRefDocOperation()) {
            pojo.setRefDocOperation(EnumConverter.fromThrift(thrift.getRefDocOperation(), org.eclipse.sw360.datahandler.services.changelogs.Operation.class));
        }
        if (thrift.isSetRefDocType()) {
            pojo.setRefDocType(thrift.getRefDocType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData toThrift(ReferenceDocData pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData thrift = new org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData();
        if (pojo.getDbName() != null) {
            thrift.setDbName(pojo.getDbName());
        }
        if (pojo.getRefDocId() != null) {
            thrift.setRefDocId(pojo.getRefDocId());
        }
        if (pojo.getRefDocOperation() != null) {
            thrift.setRefDocOperation(EnumConverter.toThrift(pojo.getRefDocOperation(), org.eclipse.sw360.datahandler.thrift.changelogs.Operation.class));
        }
        if (pojo.getRefDocType() != null) {
            thrift.setRefDocType(pojo.getRefDocType());
        }
        return thrift;
    }
}
