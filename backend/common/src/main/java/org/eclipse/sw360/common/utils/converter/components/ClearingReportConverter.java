/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.components;

import org.eclipse.sw360.datahandler.services.components.ClearingReport;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ClearingReportConverter {

    private ClearingReportConverter() {}

    public static ClearingReport fromThrift(org.eclipse.sw360.datahandler.thrift.components.ClearingReport thrift) {
        if (thrift == null) {
            return null;
        }
        ClearingReport pojo = new ClearingReport();
        if (thrift.isSetAttachments()) {
            pojo.setAttachments(ThriftCollectionConverter.mapSet(thrift.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.fromThrift(e)));
        }
        if (thrift.isSetClearingReportStatus()) {
            pojo.setClearingReportStatus(EnumConverter.fromThrift(thrift.getClearingReportStatus(), org.eclipse.sw360.datahandler.services.common.ClearingReportStatus.class));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ClearingReport toThrift(ClearingReport pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ClearingReport thrift = new org.eclipse.sw360.datahandler.thrift.components.ClearingReport();
        if (pojo.getAttachments() != null) {
            thrift.setAttachments(ThriftCollectionConverter.mapSet(pojo.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.toThrift(e)));
        }
        if (pojo.getClearingReportStatus() != null) {
            thrift.setClearingReportStatus(EnumConverter.toThrift(pojo.getClearingReportStatus(), org.eclipse.sw360.datahandler.thrift.ClearingReportStatus.class));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        return thrift;
    }
}
