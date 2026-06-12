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

import org.eclipse.sw360.datahandler.services.spdx.CheckSum;

public final class CheckSumConverter {

    private CheckSumConverter() {}

    public static CheckSum fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.CheckSum thrift) {
        if (thrift == null) {
            return null;
        }
        CheckSum pojo = new CheckSum();
        if (thrift.isSetAlgorithm()) {
            pojo.setAlgorithm(thrift.getAlgorithm());
        }
        if (thrift.isSetChecksumValue()) {
            pojo.setChecksumValue(thrift.getChecksumValue());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.CheckSum toThrift(CheckSum pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.CheckSum thrift = new org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.CheckSum();
        if (pojo.getAlgorithm() != null) {
            thrift.setAlgorithm(pojo.getAlgorithm());
        }
        if (pojo.getChecksumValue() != null) {
            thrift.setChecksumValue(pojo.getChecksumValue());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        return thrift;
    }
}
