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

import org.eclipse.sw360.datahandler.services.spdx.Creator;

public final class CreatorConverter {

    private CreatorConverter() {}

    public static Creator fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator thrift) {
        if (thrift == null) {
            return null;
        }
        Creator pojo = new Creator();
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetValue()) {
            pojo.setValue(thrift.getValue());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator toThrift(Creator pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator thrift = new org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator();
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getValue() != null) {
            thrift.setValue(pojo.getValue());
        }
        return thrift;
    }
}
