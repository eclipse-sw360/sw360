/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.licenseinfo;

import org.eclipse.sw360.datahandler.services.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ObligationParsingResultConverter {

    private ObligationParsingResultConverter() {}

    public static ObligationParsingResult fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult thrift) {
        if (thrift == null) {
            return null;
        }
        ObligationParsingResult pojo = new ObligationParsingResult();
        if (thrift.isSetAttachmentContentId()) {
            pojo.setAttachmentContentId(thrift.getAttachmentContentId());
        }
        if (thrift.isSetMessage()) {
            pojo.setMessage(thrift.getMessage());
        }
        if (thrift.isSetObligationsAtProject()) {
            pojo.setObligationsAtProject(ThriftCollectionConverter.mapList(thrift.getObligationsAtProject(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.ObligationAtProjectConverter.fromThrift(e)));
        }
        if (thrift.isSetRelease()) {
            pojo.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(thrift.getRelease()));
        }
        if (thrift.isSetSha1Hash()) {
            pojo.setSha1Hash(thrift.getSha1Hash());
        }
        if (thrift.isSetStatus()) {
            pojo.setStatus(EnumConverter.fromThrift(thrift.getStatus(), org.eclipse.sw360.datahandler.services.licenseinfo.ObligationInfoRequestStatus.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult toThrift(ObligationParsingResult pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult();
        if (pojo.getAttachmentContentId() != null) {
            thrift.setAttachmentContentId(pojo.getAttachmentContentId());
        }
        if (pojo.getMessage() != null) {
            thrift.setMessage(pojo.getMessage());
        }
        if (pojo.getObligationsAtProject() != null) {
            thrift.setObligationsAtProject(ThriftCollectionConverter.mapList(pojo.getObligationsAtProject(), e -> org.eclipse.sw360.common.utils.converter.licenseinfo.ObligationAtProjectConverter.toThrift(e)));
        }
        if (pojo.getRelease() != null) {
            thrift.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(pojo.getRelease()));
        }
        if (pojo.getSha1Hash() != null) {
            thrift.setSha1Hash(pojo.getSha1Hash());
        }
        if (pojo.getStatus() != null) {
            thrift.setStatus(EnumConverter.toThrift(pojo.getStatus(), org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationInfoRequestStatus.class));
        }
        return thrift;
    }
}
