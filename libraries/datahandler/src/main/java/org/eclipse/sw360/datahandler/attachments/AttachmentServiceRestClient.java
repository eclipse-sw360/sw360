/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.attachments;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.common.Source;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link AttachmentClient}.
 *
 * Maps to {@code AttachmentController} under {@code /attachments/api/attachments}.
 */
public class AttachmentServiceRestClient implements AttachmentClient {

    private static final String BASE = "/attachments/api/attachments";

    private static final ParameterizedTypeReference<List<AttachmentContent>> ATTACHMENT_CONTENT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<AttachmentUsage>> ATTACHMENT_USAGE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<AttachmentUsageCountEntry>> USAGE_COUNT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Attachment>> ATTACHMENT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Source>> SOURCE_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public AttachmentServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void callVoid(Runnable runnable) {
        call(() -> {
            runnable.run();
            return null;
        });
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            headers.set("X-User-Email", user.getEmail());
        }
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        if (user.getUserGroup() != null) {
            headers.set("X-User-Group", user.getUserGroup().name());
        }
    }

    @Override
    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) {
        AttachmentContent result = call(() -> restClient.post()
                .uri(BASE + "/contents")
                .body(attachmentContent)
                .retrieve()
                .body(AttachmentContent.class));
        if (result == null) {
            throw new SW360Exception("Failed to create attachment content");
        }
        return result;
    }

    @Override
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) {
        List<AttachmentContent> result = call(() -> restClient.post()
                .uri(BASE + "/contents/bulk")
                .body(attachmentContents)
                .retrieve()
                .body(ATTACHMENT_CONTENT_LIST));
        return result == null ? List.of() : result;
    }

    @Override
    public AttachmentContent getAttachmentContent(String id) {
        return call(() -> restClient.get()
                .uri(BASE + "/contents/{id}", id)
                .retrieve()
                .body(AttachmentContent.class));
    }

    @Override
    public AttachmentContent getAttachmentContentById(String attachmentContentId) {
        return call(() -> restClient.get()
                .uri(BASE + "/contents/by-content-id/{id}", attachmentContentId)
                .retrieve()
                .body(AttachmentContent.class));
    }

    @Override
    public String getSha1FromAttachmentContentId(String attachmentContentId) {
        return call(() -> restClient.get()
                .uri(BASE + "/contents/{id}/sha1", attachmentContentId)
                .retrieve()
                .body(String.class));
    }

    @Override
    public RequestSummary bulkDelete(List<String> ids) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(BASE + "/contents/bulk")
                .body(ids)
                .retrieve()
                .body(RequestSummary.class));
    }

    @Override
    public RequestStatus deleteAttachmentContent(String attachmentId) {
        return call(() -> restClient.delete()
                .uri(BASE + "/contents/{id}", attachmentId)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds) {
        VacuumAttachmentRequest request = new VacuumAttachmentRequest().setUsedIds(usedIds);
        return call(() -> restClient.post()
                .uri(BASE + "/vacuum")
                .headers(h -> addUser(h, user))
                .body(request)
                .retrieve()
                .body(RequestSummary.class));
    }

    @Override
    public AttachmentUsage makeAttachmentUsage(AttachmentUsage attachmentUsage) {
        return call(() -> restClient.post()
                .uri(BASE + "/usages")
                .body(attachmentUsage)
                .retrieve()
                .body(AttachmentUsage.class));
    }

    @Override
    public void makeAttachmentUsages(List<AttachmentUsage> attachmentUsages) {
        callVoid(() -> restClient.post()
                .uri(BASE + "/usages/bulk")
                .body(attachmentUsages)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public AttachmentUsage getAttachmentUsage(String id) {
        return call(() -> restClient.get()
                .uri(BASE + "/usages/{id}", id)
                .retrieve()
                .body(AttachmentUsage.class));
    }

    @Override
    public AttachmentUsage updateAttachmentUsage(AttachmentUsage attachmentUsage) {
        return call(() -> restClient.put()
                .uri(BASE + "/usages/{id}", attachmentUsage.getId())
                .body(attachmentUsage)
                .retrieve()
                .body(AttachmentUsage.class));
    }

    @Override
    public void updateAttachmentUsages(List<AttachmentUsage> attachmentUsages) {
        callVoid(() -> restClient.put()
                .uri(BASE + "/usages/bulk")
                .body(attachmentUsages)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void replaceAttachmentUsages(Source usedBy, List<AttachmentUsage> attachmentUsages) {
        ReplaceAttachmentUsagesRequest request = new ReplaceAttachmentUsagesRequest()
                .setUsedBy(usedBy)
                .setAttachmentUsages(attachmentUsages);
        callVoid(() -> restClient.put()
                .uri(BASE + "/usages/replace")
                .body(request)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void deleteAttachmentUsage(AttachmentUsage attachmentUsage) {
        callVoid(() -> restClient.method(HttpMethod.DELETE)
                .uri(BASE + "/usages")
                .body(attachmentUsage)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void deleteAttachmentUsages(List<AttachmentUsage> attachmentUsages) {
        callVoid(() -> restClient.method(HttpMethod.DELETE)
                .uri(BASE + "/usages/bulk")
                .body(attachmentUsages)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void deleteAttachmentUsagesByUsageDataType(Source usedBy, UsageData usageData) {
        DeleteAttachmentUsagesByTypeRequest request = new DeleteAttachmentUsagesByTypeRequest()
                .setUsedBy(usedBy)
                .setUsageData(usageData);
        callVoid(() -> restClient.method(HttpMethod.DELETE)
                .uri(BASE + "/usages/by-type")
                .body(request)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public List<AttachmentUsage> getAttachmentUsages(Source owner, String attachmentContentId, UsageData filter) {
        AttachmentUsagesQueryRequest request = new AttachmentUsagesQueryRequest()
                .setOwner(owner)
                .setAttachmentContentId(attachmentContentId)
                .setFilter(filter);
        return queryAttachmentUsages(request);
    }

    @Override
    public List<AttachmentUsage> getAttachmentsUsages(Source owner, Set<String> attachmentContentIds,
            UsageData filter) {
        AttachmentUsagesQueryRequest request = new AttachmentUsagesQueryRequest()
                .setOwner(owner)
                .setAttachmentContentIds(attachmentContentIds)
                .setFilter(filter);
        return queryAttachmentUsages(request);
    }

    private List<AttachmentUsage> queryAttachmentUsages(AttachmentUsagesQueryRequest request) {
        List<AttachmentUsage> pojos = call(() -> restClient.post()
                .uri(BASE + "/usages/query")
                .body(request)
                .retrieve()
                .body(ATTACHMENT_USAGE_LIST));
        return pojos == null ? List.of() : pojos;
    }

    @Override
    public List<AttachmentUsageCountEntry> getAttachmentUsageCount(Map<Source, Set<String>> attachments,
            UsageData filter) {
        List<AttachmentOwnerContentIds> ownerContentIds = attachments.entrySet().stream()
                .map(e -> new AttachmentOwnerContentIds()
                        .setOwner(e.getKey())
                        .setContentIds(e.getValue()))
                .collect(Collectors.toList());
        AttachmentUsageCountRequest request = new AttachmentUsageCountRequest()
                .setAttachments(ownerContentIds)
                .setFilter(filter);
        List<AttachmentUsageCountEntry> entries = call(() -> restClient.post()
                .uri(BASE + "/usages/count")
                .body(request)
                .retrieve()
                .body(USAGE_COUNT_LIST));
        return entries == null ? List.of() : entries;
    }

    @Override
    public List<AttachmentUsage> getUsedAttachments(Source usedBy, UsageData filter) {
        UsedAttachmentsRequest request = new UsedAttachmentsRequest()
                .setUsedBy(usedBy)
                .setFilter(filter);
        List<AttachmentUsage> pojos = call(() -> restClient.post()
                .uri(BASE + "/usages/used")
                .body(request)
                .retrieve()
                .body(ATTACHMENT_USAGE_LIST));
        return pojos == null ? List.of() : pojos;
    }

    @Override
    public List<AttachmentUsage> getUsedAttachmentsById(String attachmentId) {
        List<AttachmentUsage> pojos = call(() -> restClient.get()
                .uri(BASE + "/usages/used-by-content/{id}", attachmentId)
                .retrieve()
                .body(ATTACHMENT_USAGE_LIST));
        return pojos == null ? List.of() : pojos;
    }

    @Override
    public List<AttachmentUsage> getAttachmentUsagesByReleaseId(String releaseId) {
        List<AttachmentUsage> pojos = call(() -> restClient.get()
                .uri(BASE + "/usages/by-release/{releaseId}", releaseId)
                .retrieve()
                .body(ATTACHMENT_USAGE_LIST));
        return pojos == null ? List.of() : pojos;
    }

    @Override
    public List<Attachment> getAttachmentsByIds(Set<String> ids) {
        List<Attachment> pojos = call(() -> restClient.post()
                .uri(BASE + "/by-ids")
                .body(ids)
                .retrieve()
                .body(ATTACHMENT_LIST));
        return pojos == null ? List.of() : pojos;
    }

    @Override
    public List<Attachment> getAttachmentsBySha1s(Set<String> sha1s) {
        List<Attachment> pojos = call(() -> restClient.post()
                .uri(BASE + "/by-sha1s")
                .body(sha1s)
                .retrieve()
                .body(ATTACHMENT_LIST));
        return pojos == null ? List.of() : pojos;
    }

    @Override
    public List<Source> getAttachmentOwnersByIds(Set<String> ids) {
        List<Source> pojos = call(() -> restClient.post()
                .uri(BASE + "/owners/by-ids")
                .body(ids)
                .retrieve()
                .body(SOURCE_LIST));
        return pojos == null ? List.of() : pojos;
    }

    @Override
    public RequestStatus deleteOldAttachmentFromFileSystem() {
        return call(() -> restClient.post()
                .uri(BASE + "/cleanup/filesystem")
                .retrieve()
                .body(RequestStatus.class));
    }
}
