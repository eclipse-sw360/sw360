/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils.converter.common;

import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.datahandler.services.common.DocumentState;

public final class DocumentStateConverter {

    private DocumentStateConverter() {}

    public static DocumentState fromThrift(org.eclipse.sw360.datahandler.thrift.DocumentState thrift) {
        if (thrift == null) {
            return null;
        }
        DocumentState pojo = new DocumentState();
        if (thrift.isSetIsOriginalDocument()) {
            pojo.setOriginalDocument(thrift.isIsOriginalDocument());
        }
        if (thrift.isSetModerationState()) {
            pojo.setModerationState(EnumConverter.fromThrift(
                    thrift.getModerationState(), org.eclipse.sw360.datahandler.services.common.ModerationState.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.DocumentState toThrift(DocumentState pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.DocumentState thrift = new org.eclipse.sw360.datahandler.thrift.DocumentState();
        thrift.setIsOriginalDocument(pojo.isOriginalDocument());
        if (pojo.getModerationState() != null) {
            thrift.setModerationState(EnumConverter.toThrift(
                    pojo.getModerationState(), org.eclipse.sw360.datahandler.thrift.ModerationState.class));
        }
        return thrift;
    }
}
