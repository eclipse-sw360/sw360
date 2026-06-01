/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.importstatus;

import org.eclipse.sw360.datahandler.services.importstatus.ImportStatus;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ImportStatusConverter {

    private ImportStatusConverter() {}

    public static ImportStatus fromThrift(org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus thrift) {
        if (thrift == null) {
            return null;
        }
        ImportStatus pojo = new ImportStatus();
        if (thrift.isSetFailedIds()) {
            pojo.setFailedIds(thrift.getFailedIds());
        }
        if (thrift.isSetRequestStatus()) {
            pojo.setRequestStatus(EnumConverter.fromThrift(thrift.getRequestStatus(), org.eclipse.sw360.datahandler.services.common.RequestStatus.class));
        }
        if (thrift.isSetSuccessfulIds()) {
            pojo.setSuccessfulIds(ThriftCollectionConverter.mapList(thrift.getSuccessfulIds(), e -> e));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus toThrift(ImportStatus pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus thrift = new org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus();
        if (pojo.getFailedIds() != null) {
            thrift.setFailedIds(pojo.getFailedIds());
        }
        if (pojo.getRequestStatus() != null) {
            thrift.setRequestStatus(EnumConverter.toThrift(pojo.getRequestStatus(), org.eclipse.sw360.datahandler.thrift.RequestStatus.class));
        }
        if (pojo.getSuccessfulIds() != null) {
            thrift.setSuccessfulIds(ThriftCollectionConverter.mapList(pojo.getSuccessfulIds(), e -> e));
        }
        return thrift;
    }
}
