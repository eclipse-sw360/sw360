/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;


import com.ibm.cloud.cloudant.v1.Cloudant;

import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ObligationSearchHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.services.common.CustomProperties;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.licenses.LicenseType;
import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.datahandler.services.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.services.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.services.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.licenses.db.LicenseDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ObligationElementSearchHandler;

import org.apache.thrift.TException;
import org.eclipse.sw360.exporter.LicenseImportExportGateway;
import org.eclipse.sw360.exporter.LicsExporter;
import org.eclipse.sw360.exporter.utils.ZipTools;
import org.eclipse.sw360.importer.LicsImporter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * Business logic for license management.
 *
 * @author cedric.bodet@tngtech.com
 */
@Service
public class LicenseHandler implements LicenseImportExportGateway {

    LicenseDatabaseHandler handler;
    ObligationElementSearchHandler searchHandler;
    ObligationSearchHandler obligationSearchHandler;

    public LicenseHandler() throws IOException {
        handler = new LicenseDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
        searchHandler = new ObligationElementSearchHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
        obligationSearchHandler = new ObligationSearchHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
    }

    LicenseHandler(Cloudant client, String dbName) throws IOException {
        handler = new LicenseDatabaseHandler(client, dbName);
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////

    public List<License> getLicenseSummary() {
        return handler.getLicenseSummary();
    }

    public List<License> getLicenseSummaryForExport() {
        return handler.getLicenseSummaryForExport();
    }

    public ByteBuffer downloadExcel(String token) {
        return handler.downloadExcel(token);
    }

    public ByteBuffer getLicenseReportDataStream() {
        return handler.getLicenseReportDataStream();
    }

    public List<License> getDetailedLicenseSummaryForExport(String organisation) {
        return handler.getDetailedLicenseSummaryForExport(organisation);
    }

    public List<License> getDetailedLicenseSummary(String organisation, List<String> identifiers) {
        return handler.getDetailedLicenseSummaryForExport(organisation, identifiers);
    }

    public RequestStatus addLicenseType(LicenseType licenseType, User user) {
        try {
            assertNotNull(licenseType);
            return handler.addLicenseType(licenseType, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user) {
        return handler.addLicenseTypes(licenseTypes, user);
    }

    public List<License> addLicenses(List<License> licenses, User user) {
        return handler.addOrOverwriteLicenses(licenses, user, false);
    }

    public List<License> addOrOverwriteLicenses(List<License> licenses, User user) {
        return handler.addOrOverwriteLicenses(licenses, user, true);
    }

    public List<Obligation> addListOfObligations(List<Obligation> listOfObligations, User user) {
        return handler.addListOfObligations(listOfObligations, user);
    }

    public List<LicenseType> getLicenseTypes() {
        return handler.getLicenseTypes();
    }

    public List<License> getLicenses() {
        return handler.getLicenses();
    }

    public List<Obligation> getObligations() {
        return handler.getObligations();
    }

    public List<ObligationNode> getObligationNodes() {
        return handler.getObligationNodes();
    }

    public List<ObligationElement> getObligationElements() {
        return handler.getObligationElements();
    }

    public List<LicenseType> getLicenseTypesByIds(List<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getLicenseTypesByIds(ids);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Obligation> getObligationsByIds(List<String> ids) {
        try {
            assertNotEmpty(ids);
            return handler.getObligationsByIds(ids);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<Obligation> getObligationsByLicenseId(String id) {
        try {
            assertNotEmpty(id);
            return handler.getObligationsByLicenseId(id);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    public License getByID(String id, String organisation) {
        try {
            assertNotEmpty(id);
            assertNotEmpty(organisation);
            return handler.getLicenseForOrganisation(id, organisation);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public License getByIDWithOwnModerationRequests(String id, String organisation, User user) {
        try {
            assertNotEmpty(id);
            assertNotEmpty(organisation);
            assertUser(user);
            return handler.getLicenseForOrganisationWithOwnModerationRequests(id, organisation, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public List<License> getByIds(Set<String> ids, String organisation) {
        try {
            assertNotNull(ids);
            assertNotEmpty(organisation);
            return handler.getLicenses(ids, organisation);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public LicenseType getLicenseTypeById(String id) {
        try {
            assertNotEmpty(id);
            return handler.getLicenseTypeById(id);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public Obligation getObligationsById(String id) {
        try {
            assertNotEmpty(id);
            return handler.getObligationsById(id);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public ObligationNode getObligationNodeById(String id) {
        try {
            assertNotEmpty(id);
            return handler.getObligationNodeById(id);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public ObligationElement getObligationElementById(String id) {
        try {
            assertNotEmpty(id);
            return handler.getObligationElementById(id);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    ////////////////////
    // BUSINESS LOGIC //
    ////////////////////

    public String addObligations(Obligation obligs, User user) {
        try {
            assertNotNull(obligs);
            assertIdUnset(obligs.getId());
            return handler.addObligations(obligs, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public String addObligationElements(ObligationElement obligationElement, User user) {
        try {
            assertNotNull(obligationElement);
            assertIdUnset(obligationElement.getId());
            return handler.addObligationElements(obligationElement, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public String addObligationNodes(ObligationNode obligationNode, User user) {
        try {
            assertNotNull(obligationNode);
            assertIdUnset(obligationNode.getId());
            return handler.addObligationNodes(obligationNode, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus addObligationsToLicense(Set<Obligation> obligs, License license, User user) {
        try {
            assertNotNull(license);
            return handler.addObligationsToLicense(obligs, license, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus updateLicense(License license, User user, User requestingUser) {
        return handler.updateLicense(license, user, requestingUser);
    }

    public RequestStatus updateLicenseFromModerationRequest(License licenseAdditions,
                                                            License licenseDeletions,
                                                            User user,
                                                            User requestingUser) {
        return handler.updateLicenseFromAdditionsAndDeletions(licenseAdditions,
                licenseDeletions, user, requestingUser);
    }

    public RequestStatus updateWhitelist(String licenceId, Set<String> whitelist, User user) {
        try {
            assertNotEmpty(licenceId);
            assertUser(user);
            return handler.updateWhitelist(licenceId, whitelist, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus deleteLicense(String id, User user) {
        try {
            assertId(id);
            assertUser(user);
            return handler.deleteLicense(id, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    @Override
    public List<CustomProperties> getCustomProperties(String documentType) {
        return handler.getCustomProperties(documentType);
    }

    @Override
    public RequestStatus updateCustomProperties(CustomProperties customProperties, User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return RequestStatus.FAILURE;
        }
        return handler.addOrUpdateCustomProperties(customProperties);
    }

    public RequestSummary deleteAllLicenseInformation(User user) {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
        return handler.deleteAllLicenseInformation();
    }

    public RequestSummary importAllSpdxLicenses(User user) {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)) {
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
        return handler.importAllSpdxLicenses(user);
    }

    public RequestSummary importAllOSADLLicenses(User user) {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)) {
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
        return handler.importAllOSADLLicenses(user);
    }

    public RequestStatus deleteObligations(String id, User user) {
        try {
            assertId(id);
            assertUser(user);
            return handler.deleteObligations(id, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public RequestStatus deleteLicenseType(String id, User user) {
        try {
            assertId(id);
            assertUser(user);
            return handler.deleteLicenseType(id, user);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public int checkLicenseTypeInUse(String id) {
        try {
            assertId(id);
            return handler.checkLicenseTypeInUse(id);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public String addNodes(String jsonString, User user) {
        return handler.addNodes(jsonString, user);
    }

    public String buildObligationText(String nodes, String level) {
        return handler.buildObligationText(nodes, Integer.parseInt(level));
    }

    public List<ObligationElement> searchObligationElement(String text) {
        return searchHandler.search(text);
    }

    public String convertTextToNode(Obligation obligation, User user) {
        return handler.convertTextToNodes(obligation, user);
    }

    public Obligation getWithTextNodes(Obligation obligation, User user) {
        return handler.getWithTextNodes(obligation, user);
    }

    public String updateObligation(Obligation oblig, User user) {
        return handler.updateObligation(oblig, user);
    }

    public List<LicenseType> searchByLicenseType(String licenseType) {
        return handler.searchByLicenseType(licenseType);
    }

    public List<License> searchLicense(String searchText) {
        if (CommonUtils.isNullEmptyOrWhitespace(searchText)) {
            return handler.getLicenseSummary();
        }
        return handler.searchLicense(searchText);
    }

    public Map<PaginationData, List<Obligation>> searchObligationTextPaginated(
            String searchText, ObligationLevel obligationLevel, PaginationData pageData
    ) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(searchText)) {
            return obligationSearchHandler.searchWithPagination(searchText, obligationLevel, pageData);
        }
        if (obligationLevel != null) {
            Map<PaginationData, List<Obligation>> result = handler.getObligationsPaginated(pageData);
            if (result == null || result.isEmpty()) {
                return result;
            }
            Map.Entry<PaginationData, List<Obligation>> entry = result.entrySet().iterator().next();
            List<Obligation> filtered = entry.getValue().stream()
                    .filter(o -> obligationLevel.equals(o.getObligationLevel()))
                    .collect(Collectors.toList());
            PaginationData pd = entry.getKey();
            pd.setTotalRowCount(filtered.size());
            return java.util.Collections.singletonMap(pd, filtered);
        }
        return handler.getObligationsPaginated(pageData);
    }

    public void importArchive(User user, Map<String, InputStream> inputMap,
            boolean overwriteIfExternalIdMatches, boolean overwriteIfIdMatchesEvenWithoutExternalIdMatch) {
        try {
            LicsImporter importer = new LicsImporter(this, overwriteIfExternalIdMatches,
                    overwriteIfIdMatchesEvenWithoutExternalIdMatch);
            importer.importLics(user, inputMap);
        } catch (TException e) {
            throw new org.eclipse.sw360.datahandler.services.common.SW360Exception(e.getMessage(), e);
        }
    }

    public byte[] exportArchive() throws IOException {
        try {
            LicsExporter exporter = new LicsExporter(this);
            Map<String, InputStream> fileNameToStreams = exporter.getFilenameToCSVStreams();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                for (Map.Entry<String, InputStream> entry : fileNameToStreams.entrySet()) {
                    try (InputStream in = entry.getValue()) {
                        ZipTools.addToZip(zipOutputStream, entry.getKey(), in);
                    }
                }
                zipOutputStream.finish();
            }
            return byteArrayOutputStream.toByteArray();
        } catch (TException e) {
            throw new org.eclipse.sw360.datahandler.services.common.SW360Exception(e.getMessage(), e);
        }
    }
}
