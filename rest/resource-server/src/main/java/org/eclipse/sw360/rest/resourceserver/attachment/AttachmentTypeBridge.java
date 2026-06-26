/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.attachment;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

/**
 * Maps between Thrift types (still used in HAL / controller layer) and service-api POJOs
 * without pulling in {@code backend-common}.
 */
@Component
public class AttachmentTypeBridge {

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    private final ObjectMapper mapper;

    public AttachmentTypeBridge(com.fasterxml.jackson.databind.Module sw360Module) {
        this.sw360Module = sw360Module;
        this.mapper = new ObjectMapper().registerModule(sw360Module);
    }

    public org.eclipse.sw360.datahandler.services.attachments.AttachmentContent toPojo(
            org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojo =
                new org.eclipse.sw360.datahandler.services.attachments.AttachmentContent();
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

    public org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent toThrift(
            org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent thrift =
                new org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent();
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

    public org.eclipse.sw360.datahandler.services.attachments.Attachment toPojoAttachment(
            org.eclipse.sw360.datahandler.thrift.attachments.Attachment thrift) {
        return thrift == null ? null
                : mapper.convertValue(thrift, org.eclipse.sw360.datahandler.services.attachments.Attachment.class);
    }

    public org.eclipse.sw360.datahandler.thrift.attachments.Attachment toThriftAttachment(
            org.eclipse.sw360.datahandler.services.attachments.Attachment pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.attachments.Attachment.class);
    }

    public org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage toPojo(
            org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage thrift) {
        return thrift == null ? null
                : mapper.convertValue(thrift, org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage.class);
    }

    public org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage toThrift(
            org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage.class);
    }

    public org.eclipse.sw360.datahandler.services.attachments.UsageData toPojo(
            org.eclipse.sw360.datahandler.thrift.attachments.UsageData thrift) {
        return thrift == null ? null
                : mapper.convertValue(thrift, org.eclipse.sw360.datahandler.services.attachments.UsageData.class);
    }

    public org.eclipse.sw360.datahandler.thrift.attachments.UsageData toThrift(
            org.eclipse.sw360.datahandler.services.attachments.UsageData pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.attachments.UsageData.class);
    }

    public org.eclipse.sw360.datahandler.services.common.Source toPojoSource(
            org.eclipse.sw360.datahandler.thrift.Source thrift) {
        if (thrift == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.services.common.Source pojo =
                new org.eclipse.sw360.datahandler.services.common.Source();
        if (thrift.isSetProjectId()) {
            pojo.setProjectId(thrift.getProjectId());
        }
        if (thrift.isSetComponentId()) {
            pojo.setComponentId(thrift.getComponentId());
        }
        if (thrift.isSetReleaseId()) {
            pojo.setReleaseId(thrift.getReleaseId());
        }
        return pojo;
    }

    public org.eclipse.sw360.datahandler.thrift.Source toThriftSource(
            org.eclipse.sw360.datahandler.services.common.Source pojo) {
        if (pojo == null) {
            return null;
        }
        if (pojo.getProjectId() != null) {
            return org.eclipse.sw360.datahandler.thrift.Source.projectId(pojo.getProjectId());
        }
        if (pojo.getComponentId() != null) {
            return org.eclipse.sw360.datahandler.thrift.Source.componentId(pojo.getComponentId());
        }
        if (pojo.getReleaseId() != null) {
            return org.eclipse.sw360.datahandler.thrift.Source.releaseId(pojo.getReleaseId());
        }
        return new org.eclipse.sw360.datahandler.thrift.Source();
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus toThriftRequestStatus(RequestStatus pojo) {
        return pojo == null ? null : org.eclipse.sw360.datahandler.thrift.RequestStatus.valueOf(pojo.name());
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary toThriftRequestSummary(RequestSummary pojo) {
        return pojo == null ? null : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.RequestSummary.class);
    }
}
