/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.attachments;

import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage;

public final class AttachmentUsageConverter {

    private AttachmentUsageConverter() {}

    public static AttachmentUsage fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage thrift) {
        if (thrift == null) {
            return null;
        }
        AttachmentUsage pojo = new AttachmentUsage();
        if (thrift.isSetAttachmentContentId()) {
            pojo.setAttachmentContentId(thrift.getAttachmentContentId());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetOwner()) {
            pojo.setOwner(org.eclipse.sw360.common.utils.converter.common.SourceConverter.fromThrift(thrift.getOwner()));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUsageData()) {
            pojo.setUsageData(org.eclipse.sw360.common.utils.converter.attachments.UsageDataConverter.fromThrift(thrift.getUsageData()));
        }
        if (thrift.isSetUsedBy()) {
            pojo.setUsedBy(org.eclipse.sw360.common.utils.converter.common.SourceConverter.fromThrift(thrift.getUsedBy()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage toThrift(AttachmentUsage pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage thrift = new org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage();
        if (pojo.getAttachmentContentId() != null) {
            thrift.setAttachmentContentId(pojo.getAttachmentContentId());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getOwner() != null) {
            thrift.setOwner(org.eclipse.sw360.common.utils.converter.common.SourceConverter.toThrift(pojo.getOwner()));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUsageData() != null) {
            thrift.setUsageData(org.eclipse.sw360.common.utils.converter.attachments.UsageDataConverter.toThrift(pojo.getUsageData()));
        }
        if (pojo.getUsedBy() != null) {
            thrift.setUsedBy(org.eclipse.sw360.common.utils.converter.common.SourceConverter.toThrift(pojo.getUsedBy()));
        }
        return thrift;
    }
}
