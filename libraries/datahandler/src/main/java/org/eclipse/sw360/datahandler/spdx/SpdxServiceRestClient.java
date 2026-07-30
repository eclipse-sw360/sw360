/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.spdx;

import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class SpdxServiceRestClient implements SpdxClient {

    private static final String SPDX_DOCUMENT_URI = "/spdxdocument/api/spdx-documents";
    private static final String DOCUMENT_CREATION_INFO_URI =
            "/spdxdocumentcreationinfo/api/document-creation-information";
    private static final String PACKAGE_INFORMATION_URI = "/spdxpackageinfo/api/package-information";

    private final RestClient restClient;

    public SpdxServiceRestClient(RestClient restClient) {
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
        try {
            runnable.run();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
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
    public SPDXDocument getSPDXDocumentById(String id, User user) {
        return call(() -> restClient.get()
                .uri(SPDX_DOCUMENT_URI + "/" + id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(SPDXDocument.class));
    }

    @Override
    public String addSPDXDocument(SPDXDocument spdx, User user) {
        AddDocumentRequestSummary summary = call(() -> restClient.post()
                .uri(SPDX_DOCUMENT_URI)
                .headers(h -> addUser(h, user))
                .body(spdx)
                .retrieve()
                .body(AddDocumentRequestSummary.class));
        return summary != null ? summary.getId() : null;
    }

    @Override
    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) {
        RequestStatus status = call(() -> restClient.put()
                .uri(SPDX_DOCUMENT_URI)
                .headers(h -> addUser(h, user))
                .body(spdx)
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public RequestStatus updateSPDXDocumentFromModerationRequest(
            SPDXDocument additions, SPDXDocument deletions, User user) {
        ModerationUpdate<SPDXDocument> update = new ModerationUpdate<SPDXDocument>()
                .setAdditions(additions)
                .setDeletions(deletions);
        RequestStatus status = call(() -> restClient.put()
                .uri(SPDX_DOCUMENT_URI + "/moderation")
                .headers(h -> addUser(h, user))
                .body(update)
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public RequestStatus deleteSPDXDocument(String id, User user) {
        RequestStatus status = call(() -> restClient.delete()
                .uri(SPDX_DOCUMENT_URI + "/" + id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public boolean isValidSbomFile(byte[] file, String type, String extension) {
        Boolean result = call(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(SPDX_DOCUMENT_URI + "/validate-sbom")
                        .queryParam("type", type)
                        .queryParam("extension", extension)
                        .build())
                .body(file)
                .retrieve()
                .body(Boolean.class));
        return Boolean.TRUE.equals(result);
    }

    @Override
    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) {
        return call(() -> restClient.get()
                .uri(DOCUMENT_CREATION_INFO_URI + "/" + id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(DocumentCreationInformation.class));
    }

    @Override
    public void addDocumentCreationInformation(DocumentCreationInformation document, User user) {
        callVoid(() -> restClient.post()
                .uri(DOCUMENT_CREATION_INFO_URI)
                .headers(h -> addUser(h, user))
                .body(document)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation document, User user) {
        RequestStatus status = call(() -> restClient.put()
                .uri(DOCUMENT_CREATION_INFO_URI)
                .headers(h -> addUser(h, user))
                .body(document)
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public RequestStatus updateDocumentCreationInfomationFromModerationRequest(
            DocumentCreationInformation additions, DocumentCreationInformation deletions, User user) {
        ModerationUpdate<DocumentCreationInformation> update = new ModerationUpdate<DocumentCreationInformation>()
                .setAdditions(additions)
                .setDeletions(deletions);
        RequestStatus status = call(() -> restClient.put()
                .uri(DOCUMENT_CREATION_INFO_URI + "/moderation")
                .headers(h -> addUser(h, user))
                .body(update)
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public RequestStatus deleteDocumentCreationInformation(String id, User user) {
        RequestStatus status = call(() -> restClient.delete()
                .uri(DOCUMENT_CREATION_INFO_URI + "/" + id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public PackageInformation getPackageInformationById(String id, User user) {
        return call(() -> restClient.get()
                .uri(PACKAGE_INFORMATION_URI + "/" + id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(PackageInformation.class));
    }

    @Override
    public void addPackageInformation(PackageInformation packageInformation, User user) {
        callVoid(() -> restClient.post()
                .uri(PACKAGE_INFORMATION_URI)
                .headers(h -> addUser(h, user))
                .body(packageInformation)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public RequestStatus updatePackageInformation(PackageInformation packageInformation, User user) {
        RequestStatus status = call(() -> restClient.put()
                .uri(PACKAGE_INFORMATION_URI)
                .headers(h -> addUser(h, user))
                .body(packageInformation)
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public RequestStatus updatePackageInfomationFromModerationRequest(
            PackageInformation additions, PackageInformation deletions, User user) {
        ModerationUpdate<PackageInformation> update = new ModerationUpdate<PackageInformation>()
                .setAdditions(additions)
                .setDeletions(deletions);
        RequestStatus status = call(() -> restClient.put()
                .uri(PACKAGE_INFORMATION_URI + "/moderation")
                .headers(h -> addUser(h, user))
                .body(update)
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }

    @Override
    public RequestStatus deletePackageInformation(String id, User user) {
        RequestStatus status = call(() -> restClient.delete()
                .uri(PACKAGE_INFORMATION_URI + "/" + id)
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
        return status != null ? status : RequestStatus.FAILURE;
    }
}
