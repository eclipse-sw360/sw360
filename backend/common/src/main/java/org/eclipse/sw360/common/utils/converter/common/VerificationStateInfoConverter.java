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

import org.eclipse.sw360.datahandler.services.common.VerificationStateInfo;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class VerificationStateInfoConverter {

    private VerificationStateInfoConverter() {}

    public static VerificationStateInfo fromThrift(org.eclipse.sw360.datahandler.thrift.VerificationStateInfo thrift) {
        if (thrift == null) {
            return null;
        }
        VerificationStateInfo pojo = new VerificationStateInfo();
        if (thrift.isSetCheckedBy()) {
            pojo.setCheckedBy(thrift.getCheckedBy());
        }
        if (thrift.isSetCheckedOn()) {
            pojo.setCheckedOn(thrift.getCheckedOn());
        }
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetVerificationState()) {
            pojo.setVerificationState(EnumConverter.fromThrift(thrift.getVerificationState(), org.eclipse.sw360.datahandler.services.common.VerificationState.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.VerificationStateInfo toThrift(VerificationStateInfo pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.VerificationStateInfo thrift = new org.eclipse.sw360.datahandler.thrift.VerificationStateInfo();
        if (pojo.getCheckedBy() != null) {
            thrift.setCheckedBy(pojo.getCheckedBy());
        }
        if (pojo.getCheckedOn() != null) {
            thrift.setCheckedOn(pojo.getCheckedOn());
        }
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getVerificationState() != null) {
            thrift.setVerificationState(EnumConverter.toThrift(pojo.getVerificationState(), org.eclipse.sw360.datahandler.thrift.VerificationState.class));
        }
        return thrift;
    }
}
