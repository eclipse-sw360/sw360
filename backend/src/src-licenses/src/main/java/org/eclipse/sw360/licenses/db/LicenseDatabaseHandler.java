/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016-2017.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.db.CustomPropertiesRepository;
import org.eclipse.sw360.datahandler.db.ReleaseRepository;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.entitlement.LicenseModerator;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.licenses.tools.SpdxConnector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ektorp.DocumentOperationResult;
import org.ektorp.http.HttpClient;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Sets;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.*;

/**
 * Class for accessing the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicenseDatabaseHandler {

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnector db;

    /**
     * License Repository
     */
    private final LicenseRepository licenseRepository;
    private final TodoRepository obligRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final LicenseModerator moderator;
    private final CustomPropertiesRepository customPropertiesRepository;
    private final DatabaseRepository[] repositories;

    private final Logger log = LogManager.getLogger(LicenseDatabaseHandler.class);

    public LicenseDatabaseHandler(Supplier<HttpClient> httpClient, String dbName) throws MalformedURLException {
        // Create the connector
        db = new DatabaseConnector(httpClient, dbName);

        // Create the repository
        licenseRepository = new LicenseRepository(db);
        obligRepository = new TodoRepository(db);
        licenseTypeRepository = new LicenseTypeRepository(db);
        customPropertiesRepository = new CustomPropertiesRepository(db);

        repositories = new DatabaseRepository[]{
                licenseRepository,
                licenseTypeRepository,
                obligRepository,
                customPropertiesRepository
        };

        moderator = new LicenseModerator();
    }


    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////


    /**
     * Get a summary of all licenses from the database
     */
    public List<License> getLicenseSummary() {
        final List<License> licenses = licenseRepository.getAll();
        final List<LicenseType> licenseTypes = licenseTypeRepository.getAll();
        putLicenseTypesInLicenses(licenses, licenseTypes);
        /*Note that risks are not set here*/
        return licenseRepository.makeSummaryFromFullDocs(SummaryType.SUMMARY, licenses);

    }

    /**
     * Get a summary of all licenses from the database for Excel export
     */
    public List<License> getLicenseSummaryForExport() {
        return licenseRepository.getLicenseSummaryForExport();
    }
    
    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    /**
     * Get license from the database and fill its obligations
     */

    public License getLicenseForOrganisation(String id, String organisation) throws SW360Exception {
        License license = licenseRepository.get(id);

        if (license == null) {
            throw new SW360Exception("No license details found in the database for id " + id + ".");
        }

        fillLicenseForOrganisation(organisation, license);

        return license;
    }

    public License getLicenseForOrganisationWithOwnModerationRequests(String id, String organisation, User user) throws SW360Exception {
        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        License license = getLicenseForOrganisation(id, organisation);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())) {
                ModerationRequest moderationRequest = moderationRequestOptional.get();

                license = moderator.updateLicenseFromModerationRequest(
                        license,
                        moderationRequest.getLicenseAdditions(),
                        moderationRequest.getLicenseDeletions(),
                        organisation);

                for (Obligation oblig : license.getObligations()) {
                    //remove other organisations from whitelist of oblig
                    oblig.setWhitelist(SW360Utils.filterBUSet(organisation, oblig.whitelist));
                }

                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        license.setPermissions(makePermission(license, user).getPermissionMap());
        license.setDocumentState(documentState);
        return license;
    }

    private void fillLicenseForOrganisation(String organisation, License license) {
        if (license.isSetObligationDatabaseIds()) {
            license.setObligations(getObligationsByIds(license.obligationDatabaseIds));
        }


        if (license.isSetLicenseTypeDatabaseId()) {
            final LicenseType licenseType = licenseTypeRepository.get(license.getLicenseTypeDatabaseId());
            license.setLicenseType(licenseType);
        }

        license.setShortname(license.getId());
    }

    ////////////////////
    // BUSINESS LOGIC //
    ////////////////////

    /**
     * Adds a new obligation to the database.
     *
     * @return ID of the added obligations.
     */
    public String addObligations(@NotNull Obligation obligs, User user) throws SW360Exception {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        prepareTodo(obligs);
        obligRepository.add(obligs);

        return obligs.getId();
    }

    /**
     * Add oblig id to a given license
     */
    public RequestStatus addObligationsToLicense(Set<Obligation> obligs, License license, User user) throws SW360Exception {
        license.setObligationDatabaseIds(Sets.newHashSet());
        if (makePermission(license, user).isActionAllowed(RequestedAction.WRITE)) {
            assertNotNull(license);
            for (Obligation oblig : obligs) {
                obligRepository.update(oblig);
                license.addToObligationDatabaseIds(oblig.getId());
            }
            licenseRepository.update(license);
            return RequestStatus.SUCCESS;
        } else {
            License licenseForModerationRequest = getLicenseForOrganisationWithOwnModerationRequests(license.getId(), user.getDepartment(),user);
            assertNotNull(licenseForModerationRequest);
            for (Obligation oblig : obligs) {
                licenseForModerationRequest.addToObligations(oblig);
            }
            return moderator.updateLicense(licenseForModerationRequest, user); // Only moderators can change licenses!
        }
    }

    /**
     * Update the whitelisted obligations for an organisation
     */
    public RequestStatus updateWhitelist(String licenseId, Set<String> whitelistTodos, User user) throws SW360Exception {
        License license = licenseRepository.get(licenseId);
        assertNotNull(license);

        String organisation = user.getDepartment();
        String businessUnit = SW360Utils.getBUFromOrganisation(organisation);

        if (makePermission(license, user).isActionAllowed(RequestedAction.WRITE)) {

            List<Obligation> obligations = obligRepository.get(license.obligationDatabaseIds);
            for (Obligation oblig : obligations) {
                String obligId = oblig.getId();
                Set<String> currentWhitelist = oblig.whitelist != null ? oblig.whitelist : new HashSet<>();

                // Add to whitelist if necessary
                if (whitelistTodos.contains(obligId) && !currentWhitelist.contains(businessUnit)) {
                    oblig.addToWhitelist(businessUnit);
                    obligRepository.update(oblig);
                }

                // Remove from whitelist if necessary
                if (!whitelistTodos.contains(obligId) && currentWhitelist.contains(businessUnit)) {
                    currentWhitelist.remove(businessUnit);
                    obligRepository.update(oblig);
                }

            }
            return RequestStatus.SUCCESS;
        } else {
            //add updated whitelists to obligations in moderation request, not yet in database
            License licenseForModerationRequest = getLicenseForOrganisationWithOwnModerationRequests(licenseId, user.getDepartment(),user);
            List<Obligation> obligations = licenseForModerationRequest.getObligations();
            for (Obligation oblig : obligations) {
                String obligId = oblig.getId();
                Set<String> currentWhitelist = oblig.whitelist != null ? oblig.whitelist : new HashSet<>();

                // Add to whitelist if necessary
                if (whitelistTodos.contains(obligId) && !currentWhitelist.contains(businessUnit)) {
                    oblig.addToWhitelist(businessUnit);
                }

                // Remove from whitelist if necessary
                if (!whitelistTodos.contains(obligId) && currentWhitelist.contains(businessUnit)) {
                    currentWhitelist.remove(businessUnit);
                }

            }
            return moderator.updateLicense(licenseForModerationRequest, user); // Only moderators can edit whitelists!
        }
    }

    public List<License> getLicenses(Set<String> ids, String organisation) {
        final List<License> licenses = licenseRepository.get(ids);
        final List<Obligation> obligationsFromLicenses = getTodosFromLicenses(licenses);
        final List<LicenseType> licenseTypes = getLicenseTypesFromLicenses(licenses);
        filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(organisation, licenses, obligationsFromLicenses, licenseTypes);
        return licenses;
    }

    public List<License> getDetailedLicenseSummaryForExport(String organisation) {

        final List<License> licenses = licenseRepository.getAll();
        final List<Obligation> obligations = obligRepository.getAll();
        final List<LicenseType> licenseTypes = licenseTypeRepository.getAll();
        return filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(organisation, licenses, obligations, licenseTypes);
    }

    @NotNull
    private List<License> filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(String organisation, List<License> licenses, List<Obligation> obligations, List<LicenseType> licenseTypes) {
        filterTodoWhiteList(organisation, obligations);
        fillTodosRisksAndLicenseTypes(licenses, obligations, licenseTypes);
        return licenses;
    }

    private void putLicenseTypesInLicenses(List<License> licenses, List<LicenseType> licenseTypes) {
        final Map<String, LicenseType> licenseTypesById = ThriftUtils.getIdMap(licenseTypes);

        for (License license : licenses) {
            license.setLicenseType(licenseTypesById.get(license.getLicenseTypeDatabaseId()));
            license.unsetLicenseTypeDatabaseId();
        }
    }

    private void putTodosInLicenses(List<License> licenses, List<Obligation> obligations) {
        final Map<String, Obligation> obligationsById = ThriftUtils.getIdMap(obligations);

        for (License license : licenses) {
            license.setObligations(getEntriesFromIds(obligationsById, CommonUtils.nullToEmptySet(license.getObligationDatabaseIds())));
            license.unsetObligationDatabaseIds();
        }
    }

    private void filterTodoWhiteList(String organisation, List<Obligation> obligations) {
        for (Obligation oblig : obligations) {
            oblig.setWhitelist(SW360Utils.filterBUSet(organisation, oblig.getWhitelist()));
        }
    }


    private static <T> List<T> getEntriesFromIds(final Map<String, T> map, Set<String> ids) {
        return ids
                .stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public RequestStatus updateLicense(License inputLicense, User user, User requestingUser) throws SW360Exception {
        if (! makePermission(inputLicense, user).isActionAllowed(RequestedAction.CLEARING)) {
            inputLicense.setChecked(false);
        }
        if (makePermission(inputLicense, user).isActionAllowed(RequestedAction.WRITE)) {

            String businessUnit = SW360Utils.getBUFromOrganisation(requestingUser.getDepartment());

            Optional<License> oldLicense = Optional.ofNullable(inputLicense.getId())
                    .map(id -> licenseRepository.get(inputLicense.getId()));
            boolean isNewLicense = ! oldLicense.isPresent();

            if(isNewLicense){
                validateNewLicense(inputLicense);
            } else {
                validateExistingLicense(inputLicense);
            }

            boolean oldLicenseWasChecked = oldLicense.map(License::isChecked).orElse(false);

            License resultLicense = updateLicenseFromInputLicense(oldLicense, inputLicense, businessUnit, user);

            if (oldLicenseWasChecked && ! resultLicense.isChecked()){
                log.debug("reject license update due to: an already checked license is not allowed to become unchecked again");
                return RequestStatus.FAILURE;
            }

            if(isNewLicense) {
                licenseRepository.add(resultLicense);
            } else {
                licenseRepository.update(resultLicense);
            }
            return RequestStatus.SUCCESS;
        }
        return RequestStatus.FAILURE;
    }

    private License updateLicenseFromInputLicense(Optional<License> oldLicense, License inputLicense, String businessUnit, User user){
        License license = oldLicense.orElse(new License());
        if(inputLicense.isSetObligations()) {
            for (Obligation oblig : inputLicense.getObligations()) {
                if (isTemporaryObligation(oblig)) {
                    oblig.unsetId();
                    try {
                        String obligDatabaseId = addObligations(oblig, user);
                        license.addToObligationDatabaseIds(obligDatabaseId);
                    } catch (SW360Exception e) {
                        log.error("Error adding oblig to database.");
                    }
                } else if (oblig.isSetId()) {
                    Obligation dbTodo = obligRepository.get(oblig.id);
                    if (oblig.whitelist.contains(businessUnit) && !dbTodo.whitelist.contains(businessUnit)) {
                        dbTodo.addToWhitelist(businessUnit);
                        obligRepository.update(dbTodo);
                    }
                    if (!oblig.whitelist.contains(businessUnit) && dbTodo.whitelist.contains(businessUnit)) {
                        dbTodo.whitelist.remove(businessUnit);
                        obligRepository.update(dbTodo);
                    }
                }
            }
        }
        license.setText(inputLicense.getText());
        license.setFullname(inputLicense.getFullname());
        // only a new license gets its id from the shortname. Id of an existing license isn't supposed to be changed anyway
        if (!license.isSetId()) license.setId(inputLicense.getShortname());
        license.unsetShortname();
        license.setLicenseTypeDatabaseId(inputLicense.getLicenseTypeDatabaseId());
        license.unsetLicenseType();
        license.setGPLv2Compat(Optional.ofNullable(inputLicense.getGPLv2Compat())
                .orElse(Ternary.UNDEFINED));
        license.setGPLv3Compat(Optional.ofNullable(inputLicense.getGPLv3Compat())
                .orElse(Ternary.UNDEFINED));
        license.setExternalLicenseLink(inputLicense.getExternalLicenseLink());
        license.setChecked(inputLicense.isChecked());
        license.setObligationDatabaseIds(inputLicense.getObligationDatabaseIds());

        return license;
    }

    public RequestStatus updateLicenseFromAdditionsAndDeletions(License licenseAdditions,
                                                                License licenseDeletions,
                                                                User user,
                                                                User requestingUser){
        try {
            License license = getLicenseForOrganisation(licenseAdditions.getId(), requestingUser.getDepartment());
            license = moderator.updateLicenseFromModerationRequest(license,
                    licenseAdditions,
                    licenseDeletions,
                    requestingUser.getDepartment());
            return updateLicense(license, user, requestingUser);
        } catch (SW360Exception e) {
            log.error("Could not get original license when updating from moderation request.");
            return RequestStatus.FAILURE;
        }
    }

    public List<License> getDetailedLicenseSummaryForExport(String organisation, List<String> identifiers) {
        final List<License> licenses = CommonUtils.nullToEmptyList(licenseRepository.searchByShortName(identifiers));
        List<Obligation> obligations = getTodosFromLicenses(licenses);
        final List<LicenseType> licenseTypes = getLicenseTypesFromLicenses(licenses);
        return filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(organisation, licenses, obligations, licenseTypes);
    }

    private List<Obligation> getTodosFromLicenses(List<License> licenses) {
        List<Obligation> obligations;
        final Set<String> obligIds = new HashSet<>();
        for (License license : licenses) {
            obligIds.addAll(CommonUtils.nullToEmptySet(license.getObligationDatabaseIds()));
        }

        if (obligIds.isEmpty()) {
            obligations = Collections.emptyList();
        } else {
            obligations = CommonUtils.nullToEmptyList(getObligationsByIds(obligIds));
        }
        return obligations;
    }

    private List<LicenseType> getLicenseTypesFromLicenses(List<License> licenses) {
        List<LicenseType> licenseTypes;
        final Set<String> licenseTypeIds = new HashSet<>();
        for (License license : licenses) {
            if (license.isSetLicenseTypeDatabaseId()) {
                licenseTypeIds.add(license.getLicenseTypeDatabaseId());
            }
        }

        if (licenseTypeIds.isEmpty()) {
            licenseTypes = Collections.emptyList();
        } else {
            licenseTypes = CommonUtils.nullToEmptyList(getLicenseTypesByIds(licenseTypeIds));
        }
        return licenseTypes;
    }

    public List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user) {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        final List<DocumentOperationResult> documentOperationResults = licenseTypeRepository.executeBulk(licenseTypes);
        if (documentOperationResults.isEmpty()) {
            return licenseTypes;
        } else return null;
    }

    public List<License> addOrOverwriteLicenses(List<License> licenses, User user, boolean allowOverwriting) throws SW360Exception {
        final List<License> knownLicenses = licenseRepository.getAll();
        for (License license : licenses) {
            if(! makePermission(license, user).isActionAllowed(RequestedAction.CLEARING)){
                license.setChecked(false);
            }

            if(! license.isSetId()){
                validateNewLicense(license);
            } else {
                if(allowOverwriting) {
                    knownLicenses.stream()
                            .filter(kl -> license.getId().equals(kl.getId()))
                            .findFirst()
                            .map(License::getRevision)
                            .ifPresent(license::setRevision);
                } else {
                    license.unsetRevision();
                }
                validateExistingLicense(license);
            }
            prepareLicense(license);
        }

        final List<DocumentOperationResult> documentOperationResults = licenseRepository.executeBulk(licenses);
        if (documentOperationResults.isEmpty()) {
            return licenses;
        } else {
            documentOperationResults.forEach(dor ->
                    log.error("Adding license=[" + dor.getId() + "] produced an [" + dor.getError() + "] due to: " + dor.getReason()));
            return null;
        }
    }

    public List<Obligation> addListOfObligations(List<Obligation> listOfObligations, User user) throws SW360Exception {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        for (Obligation Oblig : listOfObligations) {
            prepareTodo(Oblig);
        }

        final List<DocumentOperationResult> documentOperationResults = obligRepository.executeBulk(listOfObligations);
        if (documentOperationResults.isEmpty()) {
            return listOfObligations;
        } else return null;
    }

    public List<License> getLicenses() {
        final List<License> licenses = licenseRepository.getAll();
        if (licenses == null) {
            return Collections.emptyList();
        }
        final List<Obligation> obligations = getTodosFromLicenses(licenses);
        final List<LicenseType> licenseTypes = getLicenseTypesFromLicenses(licenses);
        fillTodosRisksAndLicenseTypes(licenses, obligations, licenseTypes);
        return licenses;
    }

    private void fillTodosRisksAndLicenseTypes(List<License> licenses, List<Obligation> obligations, List<LicenseType> licenseTypes) {
        putTodosInLicenses(licenses, obligations);
        putLicenseTypesInLicenses(licenses, licenseTypes);
    }

    public List<LicenseType> getLicenseTypes() {
        return licenseTypeRepository.getAll();
    }


    public List<Obligation> getObligations() {
        final List<Obligation> obligations = obligRepository.getAll();
        return obligations;
    }

    public List<LicenseType> getLicenseTypesByIds(Collection<String> ids) {
        return licenseTypeRepository.get(ids);
    }

    public List<Obligation> getObligationsByIds(Collection<String> ids) {
        final List<Obligation> obligations = obligRepository.get(ids);
        for (Obligation oblig : obligations) {
            if(! oblig.isSetWhitelist()){
                oblig.setWhitelist(Collections.emptySet());
            }
        }
        obligations.stream().forEach(obl -> {
            obl.setDevelopmentString(obl.isDevelopment() ? "True" : "False");
            obl.setDistributionString(obl.isDistribution() ? "True" : "False");
        });
        return obligations;
    }

    public LicenseType getLicenseTypeById(String id) {
        return licenseTypeRepository.get(id);
    }

    public Obligation getObligationsById(String id) {
        return obligRepository.get(id);
    }

    public RequestStatus deleteLicense(String id, User user) throws SW360Exception {
        License license = licenseRepository.get(id);
        assertNotNull(license);

        if (checkIfInUse(id)) {
            return RequestStatus.IN_USE;
        }

        // Remove the license if the user is allowed to do it by himself
        if (makePermission(license, user).isActionAllowed(RequestedAction.DELETE)) {
            licenseRepository.remove(license);
            moderator.notifyModeratorOnDelete(license.getId());
            return RequestStatus.SUCCESS;
        } else {
            log.error(user + " does not have the permission to delete the license.");
            return RequestStatus.FAILURE;
        }
    }

    public boolean checkIfInUse(String licenseId) {
        ReleaseRepository releaseRepository = new ReleaseRepository(db,new VendorRepository(db));
        final List<Release> usingReleases = releaseRepository.searchReleasesByUsingLicenseId(licenseId);
        return !usingReleases.isEmpty();
    }

    public List<CustomProperties> getCustomProperties(String documentType){
        return customPropertiesRepository.getCustomProperties(documentType);
    }

    public RequestStatus addOrUpdateCustomProperties(CustomProperties customProperties){
        if(customProperties.isSetId()){
            customPropertiesRepository.update(customProperties);
        } else {
            customPropertiesRepository.add(customProperties);
        }
        return RequestStatus.SUCCESS;
    }

    public RequestSummary deleteAllLicenseInformation() {
        RequestSummary result = new RequestSummary()
                .setRequestStatus(RequestStatus.SUCCESS)
                .setTotalElements(0)
                .setTotalAffectedElements(0);
        for(DatabaseRepository repository : repositories) {
            result = addRequestSummaries(result, deleteAllDocuments(repository));
        }
        return result;
    }

    private RequestSummary deleteAllDocuments(DatabaseRepository repository) {
        Set<String> allIds = repository.getAllIds();
        List<DocumentOperationResult> operationResults = repository.deleteIds(allIds);
        return getRequestSummary(allIds.size(), operationResults.size());
    }

    public RequestSummary importAllSpdxLicenses(User user) {
        RequestSummary requestSummary = new RequestSummary()
                .setTotalAffectedElements(0)
                .setMessage("");
        List<String> spdxIds = SpdxConnector.getAllSpdxLicenseIds();
        Map<String,License> sw360Licenses = ThriftUtils.getIdMap(getLicenses());

        List<License> newLicenses = new ArrayList<>();
        List<String> mismatchedLicenses = new ArrayList<>();

        for(String spdxId : spdxIds){
            License sw360license = sw360Licenses.get(spdxId);

            if(sw360license == null) {

                final Optional<License> spdxLicenseAsSW360License = SpdxConnector.getSpdxLicenseAsSW360License(spdxId);
                if(spdxLicenseAsSW360License.isPresent()){
                    newLicenses.add(spdxLicenseAsSW360License.get());
                }else{
                    log.error("Failed to find SpdxListedLicense with id=" + spdxId);
                }
            }else{
                boolean matches = SpdxConnector.matchesSpdxLicenseText(sw360license,spdxId);
                if (matches) {
                    log.info("The SPDX license with id=" + spdxId + " is already in the DB");
                }else {
                    log.warn("There is a license with id=" + spdxId + " which does not match the SPDX license");
                    mismatchedLicenses.add(spdxId);
                }
            }
        }

        try {
            addOrOverwriteLicenses(newLicenses,user, false);

            if (mismatchedLicenses.size() > 0){
                requestSummary.setMessage("The following licenses did not match their SPDX equivalent: " + COMMA_JOINER.join(mismatchedLicenses));
            }
            requestSummary.setTotalAffectedElements(newLicenses.size());
        } catch (SW360Exception e) {
            String msg = "Failed to import all SPDX licenses";
            requestSummary.setMessage(msg);
            log.error(msg, e);
        }

        return requestSummary
                .setTotalElements(spdxIds.size())
                .setRequestStatus(RequestStatus.SUCCESS);
    }

    public RequestStatus deleteObligations(String id, User user) throws SW360Exception {
        Obligation oblig = obligRepository.get(id);
        assertNotNull(oblig);

        // Remove the license if the user is allowed to do it by himself
        if (PermissionUtils.isUserAtLeast(UserGroup.SW360_ADMIN, user)) {
            obligRepository.remove(oblig);
            return RequestStatus.SUCCESS;
        } else {
            log.error(user + " does not have the permission to delete oblig.");
            return RequestStatus.ACCESS_DENIED;
        }
    }
}
