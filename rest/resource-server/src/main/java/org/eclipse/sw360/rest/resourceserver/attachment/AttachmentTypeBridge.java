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

import org.eclipse.sw360.common.utils.converter.attachments.AttachmentContentConverter;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentUsageConverter;
import org.eclipse.sw360.common.utils.converter.attachments.UsageDataConverter;
import org.eclipse.sw360.common.utils.converter.common.SourceConverter;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

/**
 * Maps between Thrift types (still used in HAL / controller layer) and service-api POJOs.
 * Thrift unions ({@code Source}, {@code UsageData}) must use check-and-copy converters —
 * Jackson {@code convertValue} reads every getter and throws on unset union arms.
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
        return AttachmentContentConverter.fromThrift(thrift);
    }

    public org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent toThrift(
            org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojo) {
        return AttachmentContentConverter.toThrift(pojo);
    }

    public org.eclipse.sw360.datahandler.services.attachments.Attachment toPojoAttachment(
            org.eclipse.sw360.datahandler.thrift.attachments.Attachment thrift) {
        return AttachmentConverter.fromThrift(thrift);
    }

    public org.eclipse.sw360.datahandler.thrift.attachments.Attachment toThriftAttachment(
            org.eclipse.sw360.datahandler.services.attachments.Attachment pojo) {
        return AttachmentConverter.toThrift(pojo);
    }

    public org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage toPojo(
            org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage thrift) {
        return AttachmentUsageConverter.fromThrift(thrift);
    }

    public org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage toThrift(
            org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage pojo) {
        return AttachmentUsageConverter.toThrift(pojo);
    }

    public org.eclipse.sw360.datahandler.services.attachments.UsageData toPojo(
            org.eclipse.sw360.datahandler.thrift.attachments.UsageData thrift) {
        return UsageDataConverter.fromThrift(thrift);
    }

    public org.eclipse.sw360.datahandler.thrift.attachments.UsageData toThrift(
            org.eclipse.sw360.datahandler.services.attachments.UsageData pojo) {
        return UsageDataConverter.toThrift(pojo);
    }

    public org.eclipse.sw360.datahandler.services.common.Source toPojoSource(
            org.eclipse.sw360.datahandler.thrift.Source thrift) {
        return SourceConverter.fromThrift(thrift);
    }

    public org.eclipse.sw360.datahandler.thrift.Source toThriftSource(
            org.eclipse.sw360.datahandler.services.common.Source pojo) {
        return SourceConverter.toThrift(pojo);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus toThriftRequestStatus(RequestStatus pojo) {
        return pojo == null ? null : org.eclipse.sw360.datahandler.thrift.RequestStatus.valueOf(pojo.name());
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary toThriftRequestSummary(RequestSummary pojo) {
        return pojo == null ? null : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.RequestSummary.class);
    }
}
