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

import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.spdx.SpdxClient;
import org.eclipse.sw360.datahandler.spdx.SpdxClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

@Service
public class Sw360SpdxServices {

    private SpdxClient client() {
        return SpdxClients.get();
    }

    public SPDXDocument getSPDXDocumentById(String id, User user) {
        return client().getSPDXDocumentById(id, UserConverter.fromThrift(user));
    }

    public String addSPDXDocument(SPDXDocument spdx, User user) {
        return client().addSPDXDocument(spdx, UserConverter.fromThrift(user));
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) {
        return client().updateSPDXDocument(spdx, UserConverter.fromThrift(user));
    }

    public RequestStatus updateSPDXDocumentFromModerationRequest(
            SPDXDocument additions, SPDXDocument deletions, User user) {
        return client().updateSPDXDocumentFromModerationRequest(
                additions, deletions, UserConverter.fromThrift(user));
    }

    public RequestStatus deleteSPDXDocument(String id, User user) {
        return client().deleteSPDXDocument(id, UserConverter.fromThrift(user));
    }

    public boolean isValidSbomFile(byte[] file, String type, String extension) {
        return client().isValidSbomFile(file, type, extension);
    }

    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) {
        return client().getDocumentCreationInformationById(id, UserConverter.fromThrift(user));
    }

    public void addDocumentCreationInformation(DocumentCreationInformation document, User user) {
        client().addDocumentCreationInformation(document, UserConverter.fromThrift(user));
    }

    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation document, User user) {
        return client().updateDocumentCreationInformation(document, UserConverter.fromThrift(user));
    }

    public RequestStatus updateDocumentCreationInfomationFromModerationRequest(
            DocumentCreationInformation additions, DocumentCreationInformation deletions, User user) {
        return client().updateDocumentCreationInfomationFromModerationRequest(
                additions, deletions, UserConverter.fromThrift(user));
    }

    public RequestStatus deleteDocumentCreationInformation(String id, User user) {
        return client().deleteDocumentCreationInformation(id, UserConverter.fromThrift(user));
    }

    public PackageInformation getPackageInformationById(String id, User user) {
        return client().getPackageInformationById(id, UserConverter.fromThrift(user));
    }

    public void addPackageInformation(PackageInformation packageInformation, User user) {
        client().addPackageInformation(packageInformation, UserConverter.fromThrift(user));
    }

    public RequestStatus updatePackageInformation(PackageInformation packageInformation, User user) {
        return client().updatePackageInformation(packageInformation, UserConverter.fromThrift(user));
    }

    public RequestStatus updatePackageInfomationFromModerationRequest(
            PackageInformation additions, PackageInformation deletions, User user) {
        return client().updatePackageInfomationFromModerationRequest(
                additions, deletions, UserConverter.fromThrift(user));
    }

    public RequestStatus deletePackageInformation(String id, User user) {
        return client().deletePackageInformation(id, UserConverter.fromThrift(user));
    }
}
