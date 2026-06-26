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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentOwnerContentIds;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsageCountEntry;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsageCountRequest;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentUsagesQueryRequest;
import org.eclipse.sw360.datahandler.services.attachments.DeleteAttachmentUsagesByTypeRequest;
import org.eclipse.sw360.datahandler.services.attachments.ReplaceAttachmentUsagesRequest;
import org.eclipse.sw360.datahandler.services.attachments.UsedAttachmentsRequest;
import org.eclipse.sw360.datahandler.services.attachments.VacuumAttachmentRequest;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360AttachmentBackendService {

    private static final String ATTACHMENTS_URI = "/attachments/api/attachments";

    @NonNull
    private final RestClient restClient;

    @NonNull
    private final AttachmentTypeBridge attachmentTypeBridge;

    private void addUserHeaders(RestClient.RequestHeadersSpec<?> spec, User user) {
        spec.header("X-User-Email", user.getEmail())
            .header("X-User-Department", user.getDepartment())
            .header("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        var pojo = attachmentTypeBridge.toPojo(attachmentContent);
        AttachmentContent result = attachmentTypeBridge.toThrift(
                restClient.post().uri(ATTACHMENTS_URI + "/contents").body(pojo).retrieve().body(
                        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent.class));
        if (result == null) {
            throw new TException("Failed to create attachment content");
        }
        return result;
    }

    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentContent> pojos = attachmentContents.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentContent> result = restClient.post()
                .uri(ATTACHMENTS_URI + "/contents/bulk")
                .body(pojos)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.attachments.AttachmentContent>>() {});
        return result == null ? Collections.emptyList()
                : result.stream().map(attachmentTypeBridge::toThrift).collect(Collectors.toList());
    }

    public AttachmentContent getAttachmentContent(String id) throws SW360Exception {
        try {
            org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojo = restClient.get()
                    .uri(ATTACHMENTS_URI + "/contents/{id}", id)
                    .retrieve()
                    .body(org.eclipse.sw360.datahandler.services.attachments.AttachmentContent.class);
            return attachmentTypeBridge.toThrift(pojo);
        } catch (HttpClientErrorException.NotFound e) {
            throw new SW360Exception(e.getResponseBodyAsString()).setErrorCode(404);
        }
    }

    public AttachmentContent getAttachmentContentById(String attachmentContentId) throws TException {
        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojo = restClient.get()
                .uri(ATTACHMENTS_URI + "/contents/by-content-id/{id}", attachmentContentId)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.attachments.AttachmentContent.class);
        return attachmentTypeBridge.toThrift(pojo);
    }

    public String getSha1FromAttachmentContentId(String attachmentContentId) throws TException {
        return restClient.get()
                .uri(ATTACHMENTS_URI + "/contents/{id}/sha1", attachmentContentId)
                .retrieve()
                .body(String.class);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary bulkDelete(List<String> ids) throws TException {
        RequestSummary pojo = restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(ATTACHMENTS_URI + "/contents/bulk")
                .body(ids)
                .retrieve()
                .body(RequestSummary.class);
        return attachmentTypeBridge.toThriftRequestSummary(pojo);
    }

    public RequestStatus deleteAttachmentContent(String attachmentId) throws TException {
        org.eclipse.sw360.datahandler.services.common.RequestStatus pojo = restClient.delete()
                .uri(ATTACHMENTS_URI + "/contents/{id}", attachmentId)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class);
        return attachmentTypeBridge.toThriftRequestStatus(pojo);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds)
            throws TException {
        var request = restClient.post()
                .uri(ATTACHMENTS_URI + "/vacuum")
                .body(new VacuumAttachmentRequest().setUsedIds(usedIds));
        addUserHeaders(request, user);
        RequestSummary pojo = request.retrieve().body(RequestSummary.class);
        return attachmentTypeBridge.toThriftRequestSummary(pojo);
    }

    public AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage pojo = restClient.post()
                .uri(ATTACHMENTS_URI + "/usages")
                .body(attachmentTypeBridge.toPojo(attachmentUsage))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage.class);
        return attachmentTypeBridge.toThrift(pojo);
    }

    public void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = attachmentUsages.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        restClient.post().uri(ATTACHMENTS_URI + "/usages/bulk").body(pojos).retrieve().toBodilessEntity();
    }

    public AttachmentUsage getAttachmentUsage(String id) throws TException {
        org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage pojo = restClient.get()
                .uri(ATTACHMENTS_URI + "/usages/{id}", id)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage.class);
        return attachmentTypeBridge.toThrift(pojo);
    }

    public AttachmentUsage updateAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage pojo = restClient.put()
                .uri(ATTACHMENTS_URI + "/usages/{id}", attachmentUsage.getId())
                .body(attachmentTypeBridge.toPojo(attachmentUsage))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage.class);
        return attachmentTypeBridge.toThrift(pojo);
    }

    public void updateAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = attachmentUsages.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        restClient.put().uri(ATTACHMENTS_URI + "/usages/bulk").body(pojos).retrieve().toBodilessEntity();
    }

    public void replaceAttachmentUsages(Source usedBy, List<AttachmentUsage> attachmentUsages) throws TException {
        ReplaceAttachmentUsagesRequest request = new ReplaceAttachmentUsagesRequest()
                .setUsedBy(attachmentTypeBridge.toPojoSource(usedBy))
                .setAttachmentUsages(attachmentUsages.stream()
                        .map(attachmentTypeBridge::toPojo)
                        .collect(Collectors.toList()));
        restClient.put().uri(ATTACHMENTS_URI + "/usages/replace").body(request).retrieve().toBodilessEntity();
    }

    public void deleteAttachmentUsage(AttachmentUsage attachmentUsage) throws TException {
        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(ATTACHMENTS_URI + "/usages")
                .body(attachmentTypeBridge.toPojo(attachmentUsage))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = attachmentUsages.stream()
                .map(attachmentTypeBridge::toPojo)
                .collect(Collectors.toList());
        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(ATTACHMENTS_URI + "/usages/bulk")
                .body(pojos)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteAttachmentUsagesByUsageDataType(Source usedBy, UsageData usageData) throws TException {
        DeleteAttachmentUsagesByTypeRequest request = new DeleteAttachmentUsagesByTypeRequest()
                .setUsedBy(attachmentTypeBridge.toPojoSource(usedBy))
                .setUsageData(attachmentTypeBridge.toPojo(usageData));
        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(ATTACHMENTS_URI + "/usages/by-type")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public List<AttachmentUsage> getAttachmentUsages(Source owner, String attachmentContentId, UsageData filter)
            throws TException {
        AttachmentUsagesQueryRequest request = new AttachmentUsagesQueryRequest()
                .setOwner(attachmentTypeBridge.toPojoSource(owner))
                .setAttachmentContentId(attachmentContentId)
                .setFilter(attachmentTypeBridge.toPojo(filter));
        return queryAttachmentUsages(request);
    }

    public List<AttachmentUsage> getAttachmentsUsages(Source owner, Set<String> attachmentContentIds, UsageData filter)
            throws TException {
        AttachmentUsagesQueryRequest request = new AttachmentUsagesQueryRequest()
                .setOwner(attachmentTypeBridge.toPojoSource(owner))
                .setAttachmentContentIds(attachmentContentIds)
                .setFilter(attachmentTypeBridge.toPojo(filter));
        return queryAttachmentUsages(request);
    }

    private List<AttachmentUsage> queryAttachmentUsages(AttachmentUsagesQueryRequest request) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = restClient.post()
                .uri(ATTACHMENTS_URI + "/usages/query")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage>>() {});
        return pojos == null ? Collections.emptyList()
                : pojos.stream().map(attachmentTypeBridge::toThrift).collect(Collectors.toList());
    }

    public Map<Map<Source, String>, Integer> getAttachmentUsageCount(
            Map<Source, Set<String>> attachments, UsageData filter) throws TException {
        List<AttachmentOwnerContentIds> ownerContentIds = attachments.entrySet().stream()
                .map(e -> new AttachmentOwnerContentIds()
                        .setOwner(attachmentTypeBridge.toPojoSource(e.getKey()))
                        .setContentIds(e.getValue()))
                .collect(Collectors.toList());
        AttachmentUsageCountRequest request = new AttachmentUsageCountRequest()
                .setAttachments(ownerContentIds)
                .setFilter(attachmentTypeBridge.toPojo(filter));
        List<AttachmentUsageCountEntry> entries = restClient.post()
                .uri(ATTACHMENTS_URI + "/usages/count")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AttachmentUsageCountEntry>>() {});

        Map<Map<Source, String>, Integer> result = new HashMap<>();
        if (entries != null) {
            for (AttachmentUsageCountEntry entry : entries) {
                Source thriftSource = attachmentTypeBridge.toThriftSource(entry.getOwner());
                Map<Source, String> key = Collections.singletonMap(thriftSource, entry.getAttachmentContentId());
                result.put(key, entry.getCount());
            }
        }
        return result;
    }

    public List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter) throws TException {
        UsedAttachmentsRequest request = new UsedAttachmentsRequest()
                .setUsedBy(attachmentTypeBridge.toPojoSource(usedBy))
                .setFilter(attachmentTypeBridge.toPojo(filter));
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = restClient.post()
                .uri(ATTACHMENTS_URI + "/usages/used")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage>>() {});
        return pojos == null ? Collections.emptyList()
                : pojos.stream().map(attachmentTypeBridge::toThrift).collect(Collectors.toList());
    }

    public List<AttachmentUsage> getUsedAttachmentsById(String attachmentId) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = restClient.get()
                .uri(ATTACHMENTS_URI + "/usages/used-by-content/{id}", attachmentId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage>>() {});
        return pojos == null ? Collections.emptyList()
                : pojos.stream().map(attachmentTypeBridge::toThrift).collect(Collectors.toList());
    }

    public List<AttachmentUsage> getAttachmentUsagesByReleaseId(String releaseId) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage> pojos = restClient.get()
                .uri(ATTACHMENTS_URI + "/usages/by-release/{releaseId}", releaseId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.attachments.AttachmentUsage>>() {});
        return pojos == null ? Collections.emptyList()
                : pojos.stream().map(attachmentTypeBridge::toThrift).collect(Collectors.toList());
    }

    public List<Attachment> getAttachmentsByIds(Set<String> ids) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.Attachment> pojos = restClient.post()
                .uri(ATTACHMENTS_URI + "/by-ids")
                .body(ids)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.attachments.Attachment>>() {});
        return pojos == null ? Collections.emptyList()
                : pojos.stream().map(attachmentTypeBridge::toThriftAttachment).collect(Collectors.toList());
    }

    public List<Attachment> getAttachmentsBySha1s(Set<String> sha1s) throws TException {
        List<org.eclipse.sw360.datahandler.services.attachments.Attachment> pojos = restClient.post()
                .uri(ATTACHMENTS_URI + "/by-sha1s")
                .body(sha1s)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.attachments.Attachment>>() {});
        return pojos == null ? Collections.emptyList()
                : pojos.stream().map(attachmentTypeBridge::toThriftAttachment).collect(Collectors.toList());
    }

    public List<Source> getAttachmentOwnersByIds(Set<String> ids) throws TException {
        List<org.eclipse.sw360.datahandler.services.common.Source> pojos = restClient.post()
                .uri(ATTACHMENTS_URI + "/owners/by-ids")
                .body(ids)
                .retrieve()
                .body(new ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.common.Source>>() {});
        return pojos == null ? Collections.emptyList()
                : pojos.stream().map(attachmentTypeBridge::toThriftSource).collect(Collectors.toList());
    }

    public RequestStatus deleteOldAttachmentFromFileSystem() throws TException {
        org.eclipse.sw360.datahandler.services.common.RequestStatus pojo = restClient.post()
                .uri(ATTACHMENTS_URI + "/cleanup/filesystem")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class);
        return attachmentTypeBridge.toThriftRequestStatus(pojo);
    }
}
