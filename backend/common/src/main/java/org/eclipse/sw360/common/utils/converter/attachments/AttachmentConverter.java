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

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class AttachmentConverter {

    private AttachmentConverter() {}

    public static Attachment fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.Attachment thrift) {
        if (thrift == null) {
            return null;
        }
        Attachment pojo = new Attachment();
        if (thrift.isSetAttachmentContentId()) {
            pojo.setAttachmentContentId(thrift.getAttachmentContentId());
        }
        if (thrift.isSetAttachmentType()) {
            pojo.setAttachmentType(EnumConverter.fromThrift(thrift.getAttachmentType(), org.eclipse.sw360.datahandler.services.attachments.AttachmentType.class));
        }
        if (thrift.isSetCheckStatus()) {
            pojo.setCheckStatus(EnumConverter.fromThrift(thrift.getCheckStatus(), org.eclipse.sw360.datahandler.services.attachments.CheckStatus.class));
        }
        if (thrift.isSetCheckedBy()) {
            pojo.setCheckedBy(thrift.getCheckedBy());
        }
        if (thrift.isSetCheckedComment()) {
            pojo.setCheckedComment(thrift.getCheckedComment());
        }
        if (thrift.isSetCheckedOn()) {
            pojo.setCheckedOn(thrift.getCheckedOn());
        }
        if (thrift.isSetCheckedTeam()) {
            pojo.setCheckedTeam(thrift.getCheckedTeam());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedComment()) {
            pojo.setCreatedComment(thrift.getCreatedComment());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetCreatedTeam()) {
            pojo.setCreatedTeam(thrift.getCreatedTeam());
        }
        if (thrift.isSetFilename()) {
            pojo.setFilename(thrift.getFilename());
        }
        if (thrift.isSetProjectAttachmentUsage()) {
            pojo.setProjectAttachmentUsage(org.eclipse.sw360.common.utils.converter.attachments.ProjectAttachmentUsageConverter.fromThrift(thrift.getProjectAttachmentUsage()));
        }
        if (thrift.isSetSha1()) {
            pojo.setSha1(thrift.getSha1());
        }
        if (thrift.isSetSuperAttachmentFilename()) {
            pojo.setSuperAttachmentFilename(thrift.getSuperAttachmentFilename());
        }
        if (thrift.isSetSuperAttachmentId()) {
            pojo.setSuperAttachmentId(thrift.getSuperAttachmentId());
        }
        if (thrift.isSetUploadHistory()) {
            pojo.setUploadHistory(ThriftCollectionConverter.mapSet(thrift.getUploadHistory(), e -> e));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.Attachment toThrift(Attachment pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.Attachment thrift = new org.eclipse.sw360.datahandler.thrift.attachments.Attachment();
        if (pojo.getAttachmentContentId() != null) {
            thrift.setAttachmentContentId(pojo.getAttachmentContentId());
        }
        if (pojo.getAttachmentType() != null) {
            thrift.setAttachmentType(EnumConverter.toThrift(pojo.getAttachmentType(), org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType.class));
        }
        if (pojo.getCheckStatus() != null) {
            thrift.setCheckStatus(EnumConverter.toThrift(pojo.getCheckStatus(), org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus.class));
        }
        if (pojo.getCheckedBy() != null) {
            thrift.setCheckedBy(pojo.getCheckedBy());
        }
        if (pojo.getCheckedComment() != null) {
            thrift.setCheckedComment(pojo.getCheckedComment());
        }
        if (pojo.getCheckedOn() != null) {
            thrift.setCheckedOn(pojo.getCheckedOn());
        }
        if (pojo.getCheckedTeam() != null) {
            thrift.setCheckedTeam(pojo.getCheckedTeam());
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreatedComment() != null) {
            thrift.setCreatedComment(pojo.getCreatedComment());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getCreatedTeam() != null) {
            thrift.setCreatedTeam(pojo.getCreatedTeam());
        }
        if (pojo.getFilename() != null) {
            thrift.setFilename(pojo.getFilename());
        }
        if (pojo.getProjectAttachmentUsage() != null) {
            thrift.setProjectAttachmentUsage(org.eclipse.sw360.common.utils.converter.attachments.ProjectAttachmentUsageConverter.toThrift(pojo.getProjectAttachmentUsage()));
        }
        if (pojo.getSha1() != null) {
            thrift.setSha1(pojo.getSha1());
        }
        if (pojo.getSuperAttachmentFilename() != null) {
            thrift.setSuperAttachmentFilename(pojo.getSuperAttachmentFilename());
        }
        if (pojo.getSuperAttachmentId() != null) {
            thrift.setSuperAttachmentId(pojo.getSuperAttachmentId());
        }
        if (pojo.getUploadHistory() != null) {
            thrift.setUploadHistory(ThriftCollectionConverter.mapSet(pojo.getUploadHistory(), e -> e));
        }
        return thrift;
    }
}
