/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.spdx;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class Sw360SpdxServices {

    private static final String SPDX_DOCUMENT_URI = "/spdxdocument/api/spdx-documents";
    private static final String DOCUMENT_CREATION_INFO_URI =
            "/spdxdocumentcreationinfo/api/document-creation-information";
    private static final String PACKAGE_INFORMATION_URI = "/spdxpackageinfo/api/package-information";

    private final RestClient restClient;

    private RestClient.RequestHeadersSpec<?> withUser(RestClient.RequestHeadersSpec<?> spec, User user) {
        return spec.header("X-User-Email", user.getEmail())
                .header("X-User-Department", user.getDepartment())
                .header("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    private RestClient.RequestBodySpec withUser(RestClient.RequestBodySpec spec, User user) {
        return spec.header("X-User-Email", user.getEmail())
                .header("X-User-Department", user.getDepartment())
                .header("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    public SPDXDocument getSPDXDocumentById(String id, User user) {
        return withUser(restClient.get().uri(SPDX_DOCUMENT_URI + "/" + id), user)
                .retrieve()
                .body(SPDXDocument.class);
    }

    public String addSPDXDocument(SPDXDocument spdx, User user) {
        AddDocumentRequestSummary summary = withUser(restClient.post().uri(SPDX_DOCUMENT_URI), user)
                .body(spdx)
                .retrieve()
                .body(AddDocumentRequestSummary.class);
        return summary != null ? summary.getId() : null;
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) {
        RequestStatus status = withUser(restClient.put().uri(SPDX_DOCUMENT_URI), user)
                .body(spdx)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public RequestStatus updateSPDXDocumentFromModerationRequest(
            SPDXDocument additions, SPDXDocument deletions, User user) {
        ModerationUpdate<SPDXDocument> update = new ModerationUpdate<SPDXDocument>()
                .setAdditions(additions)
                .setDeletions(deletions);
        RequestStatus status = withUser(restClient.put().uri(SPDX_DOCUMENT_URI + "/moderation"), user)
                .body(update)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public RequestStatus deleteSPDXDocument(String id, User user) {
        RequestStatus status = withUser(restClient.delete().uri(SPDX_DOCUMENT_URI + "/" + id), user)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public boolean isValidSbomFile(byte[] file, String type, String extension) {
        Boolean result = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(SPDX_DOCUMENT_URI + "/validate-sbom")
                        .queryParam("type", type)
                        .queryParam("extension", extension)
                        .build())
                .body(file)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) {
        return withUser(restClient.get().uri(DOCUMENT_CREATION_INFO_URI + "/" + id), user)
                .retrieve()
                .body(DocumentCreationInformation.class);
    }

    public void addDocumentCreationInformation(DocumentCreationInformation document, User user) {
        withUser(restClient.post().uri(DOCUMENT_CREATION_INFO_URI), user)
                .body(document)
                .retrieve()
                .toBodilessEntity();
    }

    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation document, User user) {
        RequestStatus status = withUser(restClient.put().uri(DOCUMENT_CREATION_INFO_URI), user)
                .body(document)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public RequestStatus updateDocumentCreationInfomationFromModerationRequest(
            DocumentCreationInformation additions, DocumentCreationInformation deletions, User user) {
        ModerationUpdate<DocumentCreationInformation> update = new ModerationUpdate<DocumentCreationInformation>()
                .setAdditions(additions)
                .setDeletions(deletions);
        RequestStatus status = withUser(restClient.put().uri(DOCUMENT_CREATION_INFO_URI + "/moderation"), user)
                .body(update)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public RequestStatus deleteDocumentCreationInformation(String id, User user) {
        RequestStatus status = withUser(restClient.delete().uri(DOCUMENT_CREATION_INFO_URI + "/" + id), user)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public PackageInformation getPackageInformationById(String id, User user) {
        return withUser(restClient.get().uri(PACKAGE_INFORMATION_URI + "/" + id), user)
                .retrieve()
                .body(PackageInformation.class);
    }

    public void addPackageInformation(PackageInformation packageInformation, User user) {
        withUser(restClient.post().uri(PACKAGE_INFORMATION_URI), user)
                .body(packageInformation)
                .retrieve()
                .toBodilessEntity();
    }

    public RequestStatus updatePackageInformation(PackageInformation packageInformation, User user) {
        RequestStatus status = withUser(restClient.put().uri(PACKAGE_INFORMATION_URI), user)
                .body(packageInformation)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public RequestStatus updatePackageInfomationFromModerationRequest(
            PackageInformation additions, PackageInformation deletions, User user) {
        ModerationUpdate<PackageInformation> update = new ModerationUpdate<PackageInformation>()
                .setAdditions(additions)
                .setDeletions(deletions);
        RequestStatus status = withUser(restClient.put().uri(PACKAGE_INFORMATION_URI + "/moderation"), user)
                .body(update)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }

    public RequestStatus deletePackageInformation(String id, User user) {
        RequestStatus status = withUser(restClient.delete().uri(PACKAGE_INFORMATION_URI + "/" + id), user)
                .retrieve()
                .body(RequestStatus.class);
        return status != null ? status : RequestStatus.FAILURE;
    }
}
