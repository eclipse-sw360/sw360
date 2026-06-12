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

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class AddDocumentRequestSummaryConverter {

    private AddDocumentRequestSummaryConverter() {}

    public static AddDocumentRequestSummary fromThrift(org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary thrift) {
        if (thrift == null) {
            return null;
        }
        AddDocumentRequestSummary pojo = new AddDocumentRequestSummary();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetMessage()) {
            pojo.setMessage(thrift.getMessage());
        }
        if (thrift.isSetRequestStatus()) {
            pojo.setRequestStatus(EnumConverter.fromThrift(thrift.getRequestStatus(), org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary toThrift(AddDocumentRequestSummary pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary thrift = new org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getMessage() != null) {
            thrift.setMessage(pojo.getMessage());
        }
        if (pojo.getRequestStatus() != null) {
            thrift.setRequestStatus(EnumConverter.toThrift(pojo.getRequestStatus(), org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus.class));
        }
        return thrift;
    }
}
