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

import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class RequestSummaryConverter {

    private RequestSummaryConverter() {}

    public static RequestSummary fromThrift(org.eclipse.sw360.datahandler.thrift.RequestSummary thrift) {
        if (thrift == null) {
            return null;
        }
        RequestSummary pojo = new RequestSummary();
        if (thrift.isSetMessage()) {
            pojo.setMessage(thrift.getMessage());
        }
        if (thrift.isSetRequestStatus()) {
            pojo.setRequestStatus(EnumConverter.fromThrift(thrift.getRequestStatus(), org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
        }
        if (thrift.isSetTotalAffectedElements()) {
            pojo.setTotalAffectedElements(thrift.getTotalAffectedElements());
        }
        if (thrift.isSetTotalElements()) {
            pojo.setTotalElements(thrift.getTotalElements());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.RequestSummary toThrift(RequestSummary pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.RequestSummary thrift = new org.eclipse.sw360.datahandler.thrift.RequestSummary();
        if (pojo.getMessage() != null) {
            thrift.setMessage(pojo.getMessage());
        }
        if (pojo.getRequestStatus() != null) {
            thrift.setRequestStatus(EnumConverter.toThrift(pojo.getRequestStatus(), org.eclipse.sw360.datahandler.thrift.RequestStatus.class));
        }
        if (pojo.getTotalAffectedElements() != null) {
            thrift.setTotalAffectedElements(pojo.getTotalAffectedElements());
        }
        if (pojo.getTotalElements() != null) {
            thrift.setTotalElements(pojo.getTotalElements());
        }
        return thrift;
    }
}
