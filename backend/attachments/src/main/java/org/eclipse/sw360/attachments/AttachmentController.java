/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.attachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentContentConverter;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentUsageConverter;
import org.eclipse.sw360.common.utils.converter.attachments.UsageDataConverter;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.SourceConverter;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentOwnerContentIds;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsageCountEntry;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsageCountRequest;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsagesQueryRequest;
import org.eclipse.sw360.datahandler.services.attachments.DeleteAttachmentUsagesByTypeRequest;
import org.eclipse.sw360.datahandler.services.attachments.ReplaceAttachmentUsagesRequest;
import org.eclipse.sw360.datahandler.services.attachments.UsedAttachmentsRequest;
import org.eclipse.sw360.datahandler.services.attachments.UsageData;
import org.eclipse.sw360.datahandler.services.attachments.VacuumAttachmentRequest;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.Source;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentHandler attachmentHandler;

    public AttachmentController(AttachmentHandler attachmentHandler) {
        this.attachmentHandler = attachmentHandler;
    }

    @PostMapping("/contents")
    public AttachmentContent makeAttachmentContent(@RequestBody AttachmentContent attachmentContent) throws TException {
        return AttachmentContentConverter.fromThrift(
                attachmentHandler.makeAttachmentContent(AttachmentContentConverter.toThrift(attachmentContent)));
    }

    @PostMapping("/contents/bulk")
    public List<AttachmentContent> makeAttachmentContents(@RequestBody List<AttachmentContent> attachmentContents)
            throws TException {
        List<org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent> thriftContents = attachmentContents
                .stream()
                .map(AttachmentContentConverter::toThrift)
                .collect(Collectors.toList());
        return attachmentHandler.makeAttachmentContents(thriftContents).stream()
                .map(AttachmentContentConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @GetMapping("/contents/{id}")
    public AttachmentContent getAttachmentContent(@PathVariable String id) throws SW360Exception {
        return AttachmentContentConverter.fromThrift(attachmentHandler.getAttachmentContent(id));
    }

    @GetMapping("/contents/by-content-id/{attachmentContentId}")
    public AttachmentContent getAttachmentContentById(@PathVariable String attachmentContentId) throws TException {
        return AttachmentContentConverter.fromThrift(attachmentHandler.getAttachmentContentById(attachmentContentId));
    }

    @GetMapping("/contents/{id}/sha1")
    public String getSha1FromAttachmentContentId(@PathVariable String id) throws TException {
        return attachmentHandler.getSha1FromAttachmentContentId(id);
    }

    @DeleteMapping("/contents/bulk")
    public RequestSummary bulkDelete(@RequestBody List<String> ids) throws TException {
        return RequestSummaryConverter.fromThrift(attachmentHandler.bulkDelete(ids));
    }

    @DeleteMapping("/contents/{id}")
    public RequestStatus deleteAttachmentContent(@PathVariable String id) throws TException {
        return EnumConverter.fromThrift(attachmentHandler.deleteAttachmentContent(id), RequestStatus.class);
    }

    @PostMapping("/vacuum")
    public RequestSummary vacuumAttachmentDB(
            @RequestBody VacuumAttachmentRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return RequestSummaryConverter.fromThrift(
                attachmentHandler.vacuumAttachmentDB(user, request.getUsedIds()));
    }

    @PostMapping("/usages")
    public AttachmentUsage makeAttachmentUsage(@RequestBody AttachmentUsage attachmentUsage) throws TException {
        return AttachmentUsageConverter.fromThrift(
                attachmentHandler.makeAttachmentUsage(AttachmentUsageConverter.toThrift(attachmentUsage)));
    }

    @PostMapping("/usages/bulk")
    public void makeAttachmentUsages(@RequestBody List<AttachmentUsage> attachmentUsages) throws TException {
        attachmentHandler.makeAttachmentUsages(attachmentUsages.stream()
                .map(AttachmentUsageConverter::toThrift)
                .collect(Collectors.toList()));
    }

    @GetMapping("/usages/{id}")
    public AttachmentUsage getAttachmentUsage(@PathVariable String id) throws TException {
        return AttachmentUsageConverter.fromThrift(attachmentHandler.getAttachmentUsage(id));
    }

    @PutMapping("/usages/{id}")
    public AttachmentUsage updateAttachmentUsage(
            @PathVariable String id,
            @RequestBody AttachmentUsage attachmentUsage) throws TException {
        org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage thriftUsage =
                AttachmentUsageConverter.toThrift(attachmentUsage);
        thriftUsage.setId(id);
        return AttachmentUsageConverter.fromThrift(attachmentHandler.updateAttachmentUsage(thriftUsage));
    }

    @PutMapping("/usages/bulk")
    public void updateAttachmentUsages(@RequestBody List<AttachmentUsage> attachmentUsages) throws TException {
        attachmentHandler.updateAttachmentUsages(attachmentUsages.stream()
                .map(AttachmentUsageConverter::toThrift)
                .collect(Collectors.toList()));
    }

    @PutMapping("/usages/replace")
    public void replaceAttachmentUsages(@RequestBody ReplaceAttachmentUsagesRequest request) throws TException {
        attachmentHandler.replaceAttachmentUsages(
                SourceConverter.toThrift(request.getUsedBy()),
                request.getAttachmentUsages().stream()
                        .map(AttachmentUsageConverter::toThrift)
                        .collect(Collectors.toList()));
    }

    @DeleteMapping("/usages")
    public void deleteAttachmentUsage(@RequestBody AttachmentUsage attachmentUsage) throws TException {
        attachmentHandler.deleteAttachmentUsage(AttachmentUsageConverter.toThrift(attachmentUsage));
    }

    @DeleteMapping("/usages/bulk")
    public void deleteAttachmentUsages(@RequestBody List<AttachmentUsage> attachmentUsages) throws TException {
        attachmentHandler.deleteAttachmentUsages(attachmentUsages.stream()
                .map(AttachmentUsageConverter::toThrift)
                .collect(Collectors.toList()));
    }

    @DeleteMapping("/usages/by-type")
    public void deleteAttachmentUsagesByUsageDataType(@RequestBody DeleteAttachmentUsagesByTypeRequest request)
            throws TException {
        attachmentHandler.deleteAttachmentUsagesByUsageDataType(
                SourceConverter.toThrift(request.getUsedBy()),
                UsageDataConverter.toThrift(request.getUsageData()));
    }

    @PostMapping("/usages/query")
    public List<AttachmentUsage> getAttachmentUsages(@RequestBody AttachmentUsagesQueryRequest request)
            throws TException {
        org.eclipse.sw360.datahandler.thrift.Source owner = SourceConverter.toThrift(request.getOwner());
        org.eclipse.sw360.datahandler.thrift.attachments.UsageData filter =
                UsageDataConverter.toThrift(request.getFilter());
        if (request.getAttachmentContentIds() != null && !request.getAttachmentContentIds().isEmpty()) {
            return attachmentHandler.getAttachmentsUsages(owner, request.getAttachmentContentIds(), filter).stream()
                    .map(AttachmentUsageConverter::fromThrift)
                    .collect(Collectors.toList());
        }
        return attachmentHandler.getAttachmentUsages(owner, request.getAttachmentContentId(), filter).stream()
                .map(AttachmentUsageConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @PostMapping("/usages/count")
    public List<AttachmentUsageCountEntry> getAttachmentUsageCount(@RequestBody AttachmentUsageCountRequest request)
            throws TException {
        Map<org.eclipse.sw360.datahandler.thrift.Source, Set<String>> attachments = new HashMap<>();
        for (AttachmentOwnerContentIds entry : request.getAttachments()) {
            attachments.put(SourceConverter.toThrift(entry.getOwner()), entry.getContentIds());
        }
        Map<Map<org.eclipse.sw360.datahandler.thrift.Source, String>, Integer> counts = attachmentHandler
                .getAttachmentUsageCount(attachments, UsageDataConverter.toThrift(request.getFilter()));

        List<AttachmentUsageCountEntry> result = new ArrayList<>();
        for (Map.Entry<Map<org.eclipse.sw360.datahandler.thrift.Source, String>, Integer> entry : counts.entrySet()) {
            Map.Entry<org.eclipse.sw360.datahandler.thrift.Source, String> key =
                    entry.getKey().entrySet().iterator().next();
            result.add(new AttachmentUsageCountEntry()
                    .setOwner(SourceConverter.fromThrift(key.getKey()))
                    .setAttachmentContentId(key.getValue())
                    .setCount(entry.getValue()));
        }
        return result;
    }

    @PostMapping("/usages/used")
    public List<AttachmentUsage> getUsedAttachments(@RequestBody UsedAttachmentsRequest request) throws TException {
        return attachmentHandler.getUsedAttachments(
                SourceConverter.toThrift(request.getUsedBy()),
                UsageDataConverter.toThrift(request.getFilter())).stream()
                .map(AttachmentUsageConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @GetMapping("/usages/used-by-content/{attachmentContentId}")
    public List<AttachmentUsage> getUsedAttachmentsById(@PathVariable String attachmentContentId) throws TException {
        return attachmentHandler.getUsedAttachmentsById(attachmentContentId).stream()
                .map(AttachmentUsageConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @GetMapping("/usages/by-release/{releaseId}")
    public List<AttachmentUsage> getAttachmentUsagesByReleaseId(@PathVariable String releaseId) throws TException {
        return attachmentHandler.getAttachmentUsagesByReleaseId(releaseId).stream()
                .map(AttachmentUsageConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @PostMapping("/by-ids")
    public List<Attachment> getAttachmentsByIds(@RequestBody Set<String> ids) throws TException {
        return attachmentHandler.getAttachmentsByIds(ids).stream()
                .map(AttachmentConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @PostMapping("/by-sha1s")
    public List<Attachment> getAttachmentsBySha1s(@RequestBody Set<String> sha1s) throws TException {
        return attachmentHandler.getAttachmentsBySha1s(sha1s).stream()
                .map(AttachmentConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @PostMapping("/owners/by-ids")
    public List<Source> getAttachmentOwnersByIds(@RequestBody Set<String> ids) throws TException {
        return attachmentHandler.getAttachmentOwnersByIds(ids).stream()
                .map(SourceConverter::fromThrift)
                .collect(Collectors.toList());
    }

    @PostMapping("/cleanup/filesystem")
    public RequestStatus deleteOldAttachmentFromFileSystem() throws TException {
        return EnumConverter.fromThrift(attachmentHandler.deleteOldAttachmentFromFileSystem(), RequestStatus.class);
    }
}
