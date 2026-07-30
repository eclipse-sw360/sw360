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

import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the SPDX backend services.
 */
public interface SpdxClient {

    SPDXDocument getSPDXDocumentById(String id, User user);

    String addSPDXDocument(SPDXDocument spdx, User user);

    RequestStatus updateSPDXDocument(SPDXDocument spdx, User user);

    RequestStatus updateSPDXDocumentFromModerationRequest(SPDXDocument additions, SPDXDocument deletions, User user);

    RequestStatus deleteSPDXDocument(String id, User user);

    boolean isValidSbomFile(byte[] file, String type, String extension);

    DocumentCreationInformation getDocumentCreationInformationById(String id, User user);

    void addDocumentCreationInformation(DocumentCreationInformation document, User user);

    RequestStatus updateDocumentCreationInformation(DocumentCreationInformation document, User user);

    RequestStatus updateDocumentCreationInfomationFromModerationRequest(
            DocumentCreationInformation additions, DocumentCreationInformation deletions, User user);

    RequestStatus deleteDocumentCreationInformation(String id, User user);

    PackageInformation getPackageInformationById(String id, User user);

    void addPackageInformation(PackageInformation packageInformation, User user);

    RequestStatus updatePackageInformation(PackageInformation packageInformation, User user);

    RequestStatus updatePackageInfomationFromModerationRequest(
            PackageInformation additions, PackageInformation deletions, User user);

    RequestStatus deletePackageInformation(String id, User user);
}
