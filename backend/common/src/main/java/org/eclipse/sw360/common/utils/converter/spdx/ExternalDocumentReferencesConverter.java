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

import org.eclipse.sw360.datahandler.services.spdx.ExternalDocumentReferences;

public final class ExternalDocumentReferencesConverter {

    private ExternalDocumentReferencesConverter() {}

    public static ExternalDocumentReferences fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences thrift) {
        if (thrift == null) {
            return null;
        }
        ExternalDocumentReferences pojo = new ExternalDocumentReferences();
        if (thrift.isSetChecksum()) {
            pojo.setChecksum(org.eclipse.sw360.common.utils.converter.spdx.CheckSumConverter.fromThrift(thrift.getChecksum()));
        }
        if (thrift.isSetExternalDocumentId()) {
            pojo.setExternalDocumentId(thrift.getExternalDocumentId());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetSpdxDocument()) {
            pojo.setSpdxDocument(thrift.getSpdxDocument());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences toThrift(ExternalDocumentReferences pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences thrift = new org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences();
        if (pojo.getChecksum() != null) {
            thrift.setChecksum(org.eclipse.sw360.common.utils.converter.spdx.CheckSumConverter.toThrift(pojo.getChecksum()));
        }
        if (pojo.getExternalDocumentId() != null) {
            thrift.setExternalDocumentId(pojo.getExternalDocumentId());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getSpdxDocument() != null) {
            thrift.setSpdxDocument(pojo.getSpdxDocument());
        }
        return thrift;
    }
}
