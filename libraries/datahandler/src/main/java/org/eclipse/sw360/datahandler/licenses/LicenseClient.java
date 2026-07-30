/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.licenses;

import java.util.List;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.CustomProperties;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.licenses.LicenseType;
import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.datahandler.services.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.services.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.services.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the licenses backend service.
 *
 * Callers use this instead of the former Thrift {@code LicenseService.Iface}.
 * Types are service-api POJOs. See {@link LicenseServiceRestClient} and {@link LicenseClients}.
 */
public interface LicenseClient {

    License getByID(String id, String organisation);

    License getByIDWithOwnModerationRequests(String id, String organisation, User user);

    List<License> getByIds(Set<String> ids, String organisation);

    List<License> getLicenseSummary();

    List<License> getLicenseSummaryForExport();

    List<License> getDetailedLicenseSummaryForExport(String organisation);

    List<License> getDetailedLicenseSummary(String organisation, List<String> identifiers);

    List<License> getLicenses();

    List<License> addLicenses(List<License> licenses, User user);

    List<License> addOrOverwriteLicenses(List<License> licenses, User user);

    RequestStatus updateLicense(License license, User user, User requestingUser);

    RequestStatus updateLicenseFromModerationRequest(License additions, License deletions, User user,
            User requestingUser);

    RequestStatus updateWhitelist(String licenseId, Set<String> obligationsDatabaseIds, User user);

    RequestStatus deleteLicense(String licenseId, User user);

    RequestSummary deleteAllLicenseInformation(User user);

    RequestSummary importAllSpdxLicenses(User user);

    RequestSummary importAllOSADLLicenses(User user);

    List<License> searchLicense(String searchText);

    byte[] downloadExcel(String token);

    byte[] getLicenseReportDataStream();

    RequestStatus addLicenseType(LicenseType licenseType, User user);

    List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user);

    List<LicenseType> getLicenseTypes();

    List<LicenseType> getLicenseTypesByIds(List<String> ids);

    LicenseType getLicenseTypeById(String id);

    RequestStatus deleteLicenseType(String id, User user);

    int checkLicenseTypeInUse(String id);

    List<LicenseType> searchByLicenseType(String licenseType);

    String addObligations(Obligation obligations, User user);

    String updateObligation(Obligation obligation, User user);

    RequestStatus addObligationsToLicense(Set<Obligation> obligations, License license, User user);

    List<Obligation> addListOfObligations(List<Obligation> obligations, User user);

    List<Obligation> getObligations();

    List<Obligation> getObligationsByIds(List<String> ids);

    List<Obligation> getObligationsByLicenseId(String id);

    Obligation getObligationsById(String id);

    RequestStatus deleteObligations(String id, User user);

    String convertTextToNode(Obligation obligation, User user);

    Obligation getWithTextNodes(Obligation obligation, User user);

    PaginatedResult<Obligation> searchObligationTextPaginated(String searchText, ObligationLevel obligationLevel,
            PaginationData pageData);

    String addObligationElements(ObligationElement obligationElement, User user);

    List<ObligationElement> getObligationElements();

    ObligationElement getObligationElementById(String id);

    List<ObligationElement> searchObligationElement(String text);

    String addObligationNodes(ObligationNode obligationNode, User user);

    List<ObligationNode> getObligationNodes();

    ObligationNode getObligationNodeById(String id);

    String addNodes(String jsonString, User user);

    String buildObligationText(String nodes, String level);

    List<CustomProperties> getCustomProperties(String documentType);

    RequestStatus updateCustomProperties(CustomProperties customProperties, User user);
}
