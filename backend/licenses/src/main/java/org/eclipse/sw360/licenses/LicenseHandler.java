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

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ObligationSearchHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.licenses.db.LicenseDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ObligationElementSearchHandler;

import org.apache.thrift.TException;
import java.nio.ByteBuffer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicenseHandler implements LicenseService.Iface {

    LicenseDatabaseHandler handler;
    ObligationElementSearchHandler searchHandler;
    ObligationSearchHandler obligationSearchHandler;

    LicenseHandler() throws IOException {
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


    /**
     * Get an list of id/identifier/fullname for all licenses. The other fields will be set to null.
     */
    @Override
    public List<License> getLicenseSummary() throws TException {
        return handler.getLicenseSummary();
    }

    /**
     * Get an list of license details for Excel export.
     */
    @Override
    public List<License> getLicenseSummaryForExport() throws TException {
        return handler.getLicenseSummaryForExport();
    }

    @Override
    public ByteBuffer downloadExcel(String token) throws TException {
        return handler.downloadExcel(token);
    }

    @Override
    public ByteBuffer getLicenseReportDataStream() throws TException {
        return handler.getLicenseReportDataStream();
    }

    @Override
    public List<License> getDetailedLicenseSummaryForExport(String organisation) throws TException {
        return handler.getDetailedLicenseSummaryForExport(organisation);
    }

    @Override
    public List<License> getDetailedLicenseSummary(String organisation, List<String> identifiers) throws TException {
        return handler.getDetailedLicenseSummaryForExport(organisation, identifiers);
    }

    @Override
    public RequestStatus addLicenseType(LicenseType licenseType, User user) throws TException {
        assertNotNull(licenseType);

        return handler.addLicenseType(licenseType, user);
    }

    @Override
    public List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user) throws TException {
        return handler.addLicenseTypes(licenseTypes, user);
    }

    @Override
    public List<License> addLicenses(List<License> licenses, User user) throws TException {
        return handler.addOrOverwriteLicenses(licenses, user, false);
    }

    @Override
    public List<License> addOrOverwriteLicenses(List<License> licenses, User user) throws TException {
        return handler.addOrOverwriteLicenses(licenses, user, true);
    }

    @Override
    public List<Obligation> addListOfObligations(List<Obligation> ListOfObligations, User user) throws TException {
        return handler.addListOfObligations(ListOfObligations, user);

    }

    @Override
    public List<LicenseType> getLicenseTypes() throws TException {
        return handler.getLicenseTypes();
    }

    @Override
    public List<License> getLicenses() throws TException {
        return handler.getLicenses();
    }

    @Override
    public List<Obligation> getObligations() throws TException {
        return handler.getObligations();
    }

    @Override
    public List<ObligationNode> getObligationNodes() throws TException {
        return handler.getObligationNodes();
    }

    @Override
    public List<ObligationElement> getObligationElements() throws TException {
        return handler.getObligationElements();
    }

    @Override
    public List<LicenseType> getLicenseTypesByIds(List<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getLicenseTypesByIds(ids);
    }

    @Override
    public List<Obligation> getObligationsByIds(List<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getObligationsByIds(ids);
    }

    @Override
    public List<Obligation> getObligationsByLicenseId(String id) throws TException {
        assertNotEmpty(id);
        return handler.getObligationsByLicenseId(id);
    }



    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    /**
     * Get a single license by providing its ID, with obligations filtered for the given organisation
     */
    @Override
    public License getByID(String id, String organisation) throws SW360Exception {
        assertNotEmpty(id);
        assertNotEmpty(organisation);

        return handler.getLicenseForOrganisation(id, organisation);
    }

    @Override
    public License getByIDWithOwnModerationRequests(String id, String organisation, User user) throws TException {
        assertNotEmpty(id);
        assertNotEmpty(organisation);
        assertUser(user);

        return handler.getLicenseForOrganisationWithOwnModerationRequests(id, organisation, user);
    }

    @Override
    public List<License> getByIds(Set<String> ids, String organisation) throws TException {
        assertNotNull(ids);
        assertNotEmpty(organisation);

        return handler.getLicenses(ids, organisation);
    }

    @Override
    public LicenseType getLicenseTypeById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getLicenseTypeById(id);
    }

    @Override
    public Obligation getObligationsById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getObligationsById(id);
    }

    @Override
    public ObligationNode getObligationNodeById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getObligationNodeById(id);
    }

    @Override
    public ObligationElement getObligationElementById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getObligationElementById(id);
    }

    ////////////////////
    // BUSINESS LOGIC //
    ////////////////////

    /**
     * Add a new obligation object
     */
    @Override
    public String addObligations(Obligation obligs, User user) throws TException {
        assertNotNull(obligs);
        assertIdUnset(obligs.getId());

        return handler.addObligations(obligs, user);
    }

    /**
     * Add a new obligation element object
     */
    @Override
    public String addObligationElements(ObligationElement obligationElement, User user) throws TException {
        assertNotNull(obligationElement);
        assertIdUnset(obligationElement.getId());

        return handler.addObligationElements(obligationElement, user);
    }

    /**
     * Add a new obligation node object
     */
    @Override
    public String addObligationNodes(ObligationNode obligationNode, User user) throws TException {
        assertNotNull(obligationNode);
        assertIdUnset(obligationNode.getId());

        return handler.addObligationNodes(obligationNode, user);
    }

    /**
     * Add an existing oblig to a license
     */
    @Override
    public RequestStatus addObligationsToLicense(Set<Obligation> obligs, License license, User user) throws TException {
       assertNotNull(license);
       return  handler.addObligationsToLicense(obligs, license, user);
    }

    @Override
    public RequestStatus updateLicense(License license, User user, User requestingUser) throws TException {
        return handler.updateLicense(license, user, requestingUser);
    }

    @Override
    public RequestStatus updateLicenseFromModerationRequest(License licenseAdditions,
                                                            License licenseDeletions,
                                                            User user,
                                                            User requestingUser){
        return handler.updateLicenseFromAdditionsAndDeletions(licenseAdditions,
                licenseDeletions, user, requestingUser);
    }

    @Override
    public RequestStatus updateWhitelist(String licenceId, Set<String> whitelist, User user) throws TException {
        assertNotEmpty(licenceId);
        assertUser(user);

        return handler.updateWhitelist(licenceId, whitelist, user);
    }

    @Override
    public RequestStatus deleteLicense(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.deleteLicense(id, user);
    }

    @Override
    public List<CustomProperties> getCustomProperties(String documentType) {
        return handler.getCustomProperties(documentType);
    }

    @Override
    public RequestStatus updateCustomProperties(CustomProperties customProperties, User user){
        if(! PermissionUtils.isAdmin(user)){
            return RequestStatus.FAILURE;
        }
        return handler.addOrUpdateCustomProperties(customProperties);
    }

    @Override
    public RequestSummary deleteAllLicenseInformation(User user) {
        if(! PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)){
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
        return handler.deleteAllLicenseInformation();
    }

    @Override
    public RequestSummary importAllSpdxLicenses(User user) throws TException {
        if(! PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
        return handler.importAllSpdxLicenses(user);
    }

    @Override
    public RequestSummary importAllOSADLLicenses(User user) throws TException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)) {
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
        return handler.importAllOSADLLicenses(user);
    }

    @Override
    public RequestStatus deleteObligations(String id, User user) throws TException {
        assertId(id);
        assertUser(user);
        return handler.deleteObligations(id, user);
    }

    @Override
    public RequestStatus deleteLicenseType(String id, User user) throws TException {
        assertId(id);
        assertUser(user);
        return handler.deleteLicenseType(id, user);
    }

    @Override
    public int checkLicenseTypeInUse(String id) throws TException {
        assertId(id);
        return handler.checkLicenseTypeInUse(id);
    }

    public String addNodes(String jsonString, User user) throws TException {
        return handler.addNodes(jsonString, user);
    }

    @Override
    public String buildObligationText(String nodes, String level) throws TException {
        return handler.buildObligationText(nodes, Integer.parseInt(level));
    }

    @Override
    public List<ObligationElement> searchObligationElement(String text) throws TException {
        return searchHandler.search(text);
    }

    @Override
    public String convertTextToNode(Obligation obligation, User user) throws TException {
        String node= handler.convertTextToNodes(obligation,user);
        return node;
    }

    @Override
    public Obligation getWithTextNodes(Obligation obligation, User user) throws TException {
        return handler.getWithTextNodes(obligation, user);
    }

    @Override
    public String updateObligation(Obligation oblig, User user) throws TException {
        return handler.updateObligation(oblig, user);
    }

    @Override
    public List<LicenseType> searchByLicenseType(String licenseType) {
        return handler.searchByLicenseType(licenseType);
    }

    @Override
    public List<License> searchLicense(String searchText) {
        if (CommonUtils.isNullEmptyOrWhitespace(searchText)) {
            return handler.getLicenseSummary();
        }
        return handler.searchLicense(searchText);
    }

    @Override
    public Map<PaginationData, List<Obligation>> searchObligationTextPaginated(
            String searchText, ObligationLevel obligationLevel, PaginationData pageData
    ) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(searchText) || obligationLevel != null) {
            return obligationSearchHandler.searchWithPagination(searchText, obligationLevel, pageData);
        }
        return handler.getObligationsPaginated(pageData);
    }
}
