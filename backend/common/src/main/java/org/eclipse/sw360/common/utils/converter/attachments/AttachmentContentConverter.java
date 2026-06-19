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

import org.eclipse.sw360.datahandler.services.attachments.AttachmentContent;

public final class AttachmentContentConverter {

    private AttachmentContentConverter() {}

    public static AttachmentContent fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent thrift) {
        if (thrift == null) {
            return null;
        }
        AttachmentContent pojo = new AttachmentContent();
        if (thrift.isSetContentType()) {
            pojo.setContentType(thrift.getContentType());
        }
        if (thrift.isSetFilename()) {
            pojo.setFilename(thrift.getFilename());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetOnlyRemote()) {
            pojo.setOnlyRemote(thrift.isOnlyRemote());
        }
        if (thrift.isSetPartsCount()) {
            pojo.setPartsCount(thrift.getPartsCount());
        }
        if (thrift.isSetRemoteUrl()) {
            pojo.setRemoteUrl(thrift.getRemoteUrl());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent toThrift(AttachmentContent pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent thrift = new org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent();
        if (pojo.getContentType() != null) {
            thrift.setContentType(pojo.getContentType());
        }
        if (pojo.getFilename() != null) {
            thrift.setFilename(pojo.getFilename());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getOnlyRemote() != null) {
            thrift.setOnlyRemote(pojo.getOnlyRemote());
        }
        if (pojo.getPartsCount() != null) {
            thrift.setPartsCount(pojo.getPartsCount());
        }
        if (pojo.getRemoteUrl() != null) {
            thrift.setRemoteUrl(pojo.getRemoteUrl());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
