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

import org.eclipse.sw360.datahandler.services.common.RequestStatusWithBoolean;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class RequestStatusWithBooleanConverter {

    private RequestStatusWithBooleanConverter() {}

    public static RequestStatusWithBoolean fromThrift(org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean thrift) {
        if (thrift == null) {
            return null;
        }
        RequestStatusWithBoolean pojo = new RequestStatusWithBoolean();
        if (thrift.isSetAnswerPositive()) {
            pojo.setAnswerPositive(thrift.isAnswerPositive());
        }
        if (thrift.isSetRequestStatus()) {
            pojo.setRequestStatus(EnumConverter.fromThrift(thrift.getRequestStatus(), org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean toThrift(RequestStatusWithBoolean pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean thrift = new org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean();
        if (pojo.getAnswerPositive() != null) {
            thrift.setAnswerPositive(pojo.getAnswerPositive());
        }
        if (pojo.getRequestStatus() != null) {
            thrift.setRequestStatus(EnumConverter.toThrift(pojo.getRequestStatus(), org.eclipse.sw360.datahandler.thrift.RequestStatus.class));
        }
        return thrift;
    }
}
