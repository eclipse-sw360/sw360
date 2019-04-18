/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016-2017.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.apache.log4j.Logger;
import org.ektorp.DocumentOperationResult;
import org.ektorp.http.HttpClient;
import org.jetbrains.annotations.NotNull;

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
    private final ObligationRepository obligationRepository;
    private final TodoRepository todoRepository;
    private final RiskRepository riskRepository;
    private final RiskCategoryRepository riskCategoryRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final LicenseModerator moderator;
    private final CustomPropertiesRepository customPropertiesRepository;
    private final DatabaseRepository[] repositories;

    private final Logger log = Logger.getLogger(LicenseDatabaseHandler.class);

    public LicenseDatabaseHandler(Supplier<HttpClient> httpClient, String dbName) throws MalformedURLException {
        // Create the connector
        db = new DatabaseConnector(httpClient, dbName);

        // Create the repository
        licenseRepository = new LicenseRepository(db);
        obligationRepository = new ObligationRepository(db);
        todoRepository = new TodoRepository(db);
        riskRepository = new RiskRepository(db);
        riskCategoryRepository = new RiskCategoryRepository(db);
        licenseTypeRepository = new LicenseTypeRepository(db);
        customPropertiesRepository = new CustomPropertiesRepository(db);

        repositories = new DatabaseRepository[]{
                licenseRepository,
                licenseTypeRepository,
                todoRepository,
                obligationRepository,
                riskRepository,
                riskCategoryRepository,
                customPropertiesRepository
        };

        moderator = new LicenseModerator();
    }


    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////

    /**
     * Get all obligations from database
     */
    public List<Obligation> getObligations() {
        return obligationRepository.getAll();
    }

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

                for (Todo todo : license.getTodos()) {
                    //remove other organisations from whitelist of todo
                    todo.setWhitelist(SW360Utils.filterBUSet(organisation, todo.whitelist));
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
        if (license.isSetTodoDatabaseIds()) {
            license.setTodos(getTodosByIds(license.todoDatabaseIds));
        }

        if (license.isSetTodos()) {
            for (Todo todo : license.getTodos()) {
                //remove other organisations from whitelist of todo
                todo.setWhitelist(SW360Utils.filterBUSet(organisation, todo.whitelist));
                if(todo.isSetObligationDatabaseIds()) {
                    todo.setObligations(getObligationsByIds(todo.obligationDatabaseIds));
                }
            }
        }

        if (license.isSetLicenseTypeDatabaseId()) {
            final LicenseType licenseType = licenseTypeRepository.get(license.getLicenseTypeDatabaseId());
            license.setLicenseType(licenseType);
        }
        if(license.isSetRiskDatabaseIds()) {
            license.setRisks(getRisksByIds(license.riskDatabaseIds));
            license.unsetRiskDatabaseIds();
        }

        license.setShortname(license.getId());
    }

    ////////////////////
    // BUSINESS LOGIC //
    ////////////////////

    /**
     * Adds a new todo to the database.
     *
     * @return ID of the added todo.
     */
    public String addTodo(@NotNull Todo todo, User user) throws SW360Exception {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        prepareTodo(todo);
        todoRepository.add(todo);

        return todo.getId();
    }

    /**
     * Add todo id to a given license
     */
    public RequestStatus addTodoToLicense(Todo todo, String licenseId, User user) throws SW360Exception {
        assertNotNull(todo);
        License license = licenseRepository.get(licenseId);
        if (makePermission(license, user).isActionAllowed(RequestedAction.WRITE)) {
            assertNotNull(license);
            if(isTemporaryTodo(todo)){
                todo.unsetId();
            }
            todo.unsetObligations();
            String todoId = addTodo(todo, user);
            license.addToTodoDatabaseIds(todoId);
            licenseRepository.update(license);
            return RequestStatus.SUCCESS;
        } else {
            License licenseForModerationRequest = getLicenseForOrganisationWithOwnModerationRequests(licenseId, user.getDepartment(),user);
            assertNotNull(licenseForModerationRequest);
            if(todo.isSetObligationDatabaseIds()){
                for(String obligationDatabaseId: todo.obligationDatabaseIds){
                    todo.addToObligations(obligationRepository.get(obligationDatabaseId));
                }
            }
            licenseForModerationRequest.addToTodos(todo);
            return moderator.updateLicense(licenseForModerationRequest, user); // Only moderators can change licenses!
        }
    }

    /**
     * Update the whitelisted todos for an organisation
     */
    public RequestStatus updateWhitelist(String licenseId, Set<String> whitelistTodos, User user) throws SW360Exception {
        License license = licenseRepository.get(licenseId);
        assertNotNull(license);

        String organisation = user.getDepartment();
        String businessUnit = SW360Utils.getBUFromOrganisation(organisation);

        if (makePermission(license, user).isActionAllowed(RequestedAction.WRITE)) {

            List<Todo> todos = todoRepository.get(license.todoDatabaseIds);
            for (Todo todo : todos) {
                String todoId = todo.getId();
                Set<String> currentWhitelist = todo.whitelist != null ? todo.whitelist : new HashSet<>();

                // Add to whitelist if necessary
                if (whitelistTodos.contains(todoId) && !currentWhitelist.contains(businessUnit)) {
                    todo.addToWhitelist(businessUnit);
                    todoRepository.update(todo);
                }

                // Remove from whitelist if necessary
                if (!whitelistTodos.contains(todoId) && currentWhitelist.contains(businessUnit)) {
                    currentWhitelist.remove(businessUnit);
                    todoRepository.update(todo);
                }

            }
            return RequestStatus.SUCCESS;
        } else {
            //add updated whitelists to todos in moderation request, not yet in database
            License licenseForModerationRequest = getLicenseForOrganisationWithOwnModerationRequests(licenseId, user.getDepartment(),user);
            List<Todo> todos = licenseForModerationRequest.getTodos();
            for (Todo todo : todos) {
                String todoId = todo.getId();
                Set<String> currentWhitelist = todo.whitelist != null ? todo.whitelist : new HashSet<>();

                // Add to whitelist if necessary
                if (whitelistTodos.contains(todoId) && !currentWhitelist.contains(businessUnit)) {
                    todo.addToWhitelist(businessUnit);
                }

                // Remove from whitelist if necessary
                if (!whitelistTodos.contains(todoId) && currentWhitelist.contains(businessUnit)) {
                    currentWhitelist.remove(businessUnit);
                }

            }
            return moderator.updateLicense(licenseForModerationRequest, user); // Only moderators can edit whitelists!
        }
    }

    public List<License> getLicenses(Set<String> ids, String organisation) {
        final List<License> licenses = licenseRepository.get(ids);
        final List<Todo> todosFromLicenses = getTodosFromLicenses(licenses);
        final List<LicenseType> licenseTypes = getLicenseTypesFromLicenses(licenses);
        final List<Risk> risks = getRisksFromLicenses(licenses);
        filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(organisation, licenses, todosFromLicenses, risks, licenseTypes);
        return licenses;
    }

    public List<License> getDetailedLicenseSummaryForExport(String organisation) {

        final List<License> licenses = licenseRepository.getAll();
        final List<Todo> todos = todoRepository.getAll();
        final List<LicenseType> licenseTypes = licenseTypeRepository.getAll();
        final List<Risk> risks = riskRepository.getAll();
        return filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(organisation, licenses, todos, risks, licenseTypes);
    }

    @NotNull
    private List<License> filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(String organisation, List<License> licenses, List<Todo> todos, List<Risk> risks, List<LicenseType> licenseTypes) {
        filterTodoWhiteList(organisation, todos);
        fillTodosRisksAndLicenseTypes(licenses, todos, risks, licenseTypes);
        return licenses;
    }

    private void putLicenseTypesInLicenses(List<License> licenses, List<LicenseType> licenseTypes) {
        final Map<String, LicenseType> licenseTypesById = ThriftUtils.getIdMap(licenseTypes);

        for (License license : licenses) {
            license.setLicenseType(licenseTypesById.get(license.getLicenseTypeDatabaseId()));
            license.unsetLicenseTypeDatabaseId();
        }
    }

    private void putTodosInLicenses(List<License> licenses, List<Todo> todos) {
        final Map<String, Todo> todosById = ThriftUtils.getIdMap(todos);

        for (License license : licenses) {
            license.setTodos(getEntriesFromIds(todosById, CommonUtils.nullToEmptySet(license.getTodoDatabaseIds())));
            license.unsetTodoDatabaseIds();
        }
    }

    private void putRisksInLicenses(List<License> licenses, List<Risk> risks) {
        final Map<String, Risk> risksById = ThriftUtils.getIdMap(risks);

        for (License license : licenses) {
            license.setRisks(getEntriesFromIds(risksById, CommonUtils.nullToEmptySet(license.getRiskDatabaseIds())));
            license.unsetRiskDatabaseIds();
        }
    }

    private void filterTodoWhiteList(String organisation, List<Todo> todos) {
        for (Todo todo : todos) {
            todo.setWhitelist(SW360Utils.filterBUSet(organisation, todo.getWhitelist()));
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
        if(inputLicense.isSetTodos()) {
            for (Todo todo : inputLicense.getTodos()) {
                if (isTemporaryTodo(todo)) {
                    todo.unsetId();
                    try {
                        String todoDatabaseId = addTodo(todo, user);
                        license.addToTodoDatabaseIds(todoDatabaseId);
                    } catch (SW360Exception e) {
                        log.error("Error adding todo to database.");
                    }
                } else if (todo.isSetId()) {
                    Todo dbTodo = todoRepository.get(todo.id);
                    if (todo.whitelist.contains(businessUnit) && !dbTodo.whitelist.contains(businessUnit)) {
                        dbTodo.addToWhitelist(businessUnit);
                        todoRepository.update(dbTodo);
                    }
                    if (!todo.whitelist.contains(businessUnit) && dbTodo.whitelist.contains(businessUnit)) {
                        dbTodo.whitelist.remove(businessUnit);
                        todoRepository.update(dbTodo);
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
        List<Todo> todos = getTodosFromLicenses(licenses);
        final List<LicenseType> licenseTypes = getLicenseTypesFromLicenses(licenses);
        final List<Risk> risks = getRisksFromLicenses(licenses);
        return filterTodoWhiteListAndFillTodosRisksAndLicenseTypeInLicense(organisation, licenses, todos, risks, licenseTypes);
    }

    private List<Todo> getTodosFromLicenses(List<License> licenses) {
        List<Todo> todos;
        final Set<String> todoIds = new HashSet<>();
        for (License license : licenses) {
            todoIds.addAll(CommonUtils.nullToEmptySet(license.getTodoDatabaseIds()));
        }

        if (todoIds.isEmpty()) {
            todos = Collections.emptyList();
        } else {
            todos = CommonUtils.nullToEmptyList(getTodosByIds(todoIds));
        }
        return todos;
    }

    private List<Risk> getRisksFromLicenses(List<License> licenses) {
        List<Risk> risks;
        final Set<String> riskIds = new HashSet<>();
        for (License license : licenses) {
            riskIds.addAll(CommonUtils.nullToEmptySet(license.getRiskDatabaseIds()));
        }

        if (riskIds.isEmpty()) {
            risks = Collections.emptyList();
        } else {
            risks = CommonUtils.nullToEmptyList(getRisksByIds(riskIds));
        }
        return risks;
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

    public List<RiskCategory> addRiskCategories(List<RiskCategory> riskCategories, User user) throws SW360Exception {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        for (RiskCategory riskCategory : riskCategories) {
            prepareRiskCategory(riskCategory);
        }

        final List<DocumentOperationResult> documentOperationResults = riskCategoryRepository.executeBulk(riskCategories);
        if (documentOperationResults.isEmpty()) {
            return riskCategories;
        } else return null;
    }

    public List<Risk> addRisks(List<Risk> risks, User user) throws SW360Exception {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        for (Risk risk : risks) {
            prepareRisk(risk);
        }

        final List<DocumentOperationResult> documentOperationResults = riskRepository.executeBulk(risks);
        if (documentOperationResults.isEmpty()) {
            return risks;
        } else return null;
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

    public List<Obligation> addObligations(List<Obligation> obligations, User user) throws SW360Exception {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        for (Obligation obligation : obligations) {
            prepareObligation(obligation);
        }

        final List<DocumentOperationResult> documentOperationResults = obligationRepository.executeBulk(obligations);
        if (documentOperationResults.isEmpty()) {
            return obligations;
        } else return null;
    }

    public List<Todo> addTodos(List<Todo> todos, User user) throws SW360Exception {
        if (!PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user)){
            return null;
        }
        for (Todo todo : todos) {
            prepareTodo(todo);
        }

        final List<DocumentOperationResult> documentOperationResults = todoRepository.executeBulk(todos);
        if (documentOperationResults.isEmpty()) {
            return todos;
        } else return null;
    }

    public List<License> getLicenses() {
        final List<License> licenses = licenseRepository.getAll();
        if (licenses == null) {
            return Collections.emptyList();
        }
        final List<Todo> todos = getTodosFromLicenses(licenses);
        final List<LicenseType> licenseTypes = getLicenseTypesFromLicenses(licenses);
        final List<Risk> risks = getRisksFromLicenses(licenses);
        fillTodosRisksAndLicenseTypes(licenses, todos, risks, licenseTypes);
        return licenses;
    }

    private void fillTodosRisksAndLicenseTypes(List<License> licenses, List<Todo> todos, List<Risk> risks, List<LicenseType> licenseTypes) {
        putTodosInLicenses(licenses, todos);
        putRisksInLicenses(licenses, risks);
        putLicenseTypesInLicenses(licenses, licenseTypes);
    }

    public List<LicenseType> getLicenseTypes() {
        return licenseTypeRepository.getAll();
    }

    public List<Risk> getRisks() {
        final List<Risk> risks = riskRepository.getAll();
        fillRisks(risks);
        return risks;
    }

    public List<RiskCategory> getRiskCategories() {
        return riskCategoryRepository.getAll();
    }


    public List<Todo> getTodos() {
        final List<Todo> todos = todoRepository.getAll();
        fillTodos(todos);
        return todos;
    }

    public List<Risk> getRisksByIds(Collection<String> ids) {
        final List<Risk> risks = riskRepository.get(ids);
        fillRisks(risks);
        return risks;
    }

    private void fillRisks(List<Risk> risks) {
        final List<RiskCategory> riskCategories = riskCategoryRepository.get(risks.stream()
                .map(Risk::getRiskCategoryDatabaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        final Map<String, RiskCategory> idMap = ThriftUtils.getIdMap(riskCategories);
        for (Risk risk : risks) {
            if (risk.isSetRiskCategoryDatabaseId()) {
                risk.setCategory(idMap.get(risk.getRiskCategoryDatabaseId()));
                risk.unsetRiskCategoryDatabaseId();
            }
        }
    }

    public List<RiskCategory> getRiskCategoriesByIds(Collection<String> ids) {
        return riskCategoryRepository.get(ids);
    }

    public List<Obligation> getObligationsByIds(Collection<String> ids) {
        return obligationRepository.get(ids);
    }

    public List<LicenseType> getLicenseTypesByIds(Collection<String> ids) {
        return licenseTypeRepository.get(ids);
    }

    public List<Todo> getTodosByIds(Collection<String> ids) {
        final List<Todo> todos = todoRepository.get(ids);
        fillTodos(todos);
        return todos;
    }

    private void fillTodos(List<Todo> todos) {
        Set<String> obligationIdsToFetch = new HashSet<>();
        for (Todo todo : todos) {
            obligationIdsToFetch.addAll(CommonUtils.nullToEmptySet(todo.getObligationDatabaseIds()));
        }

        Map<String, Obligation> obligationIdMap = null;
        if (!obligationIdsToFetch.isEmpty()) {
            List<Obligation> obligations = getObligationsByIds(obligationIdsToFetch);
            if (obligations != null && !obligations.isEmpty()) {
                obligationIdMap = ThriftUtils.getIdMap(obligations);
            }
        }
        if (obligationIdMap == null) {
            obligationIdMap = Collections.emptyMap();
        }

        for (Todo todo : todos) {
            if (todo.isSetObligationDatabaseIds()) {
                for (String id : todo.getObligationDatabaseIds()) {
                    final Obligation obligation = obligationIdMap.get(id);
                    if (obligation != null) {
                        todo.addToObligations(obligation);
                    }
                }
            }
            todo.setDevelopmentString(todo.isDevelopment()?"True":"False");
            todo.setDistributionString(todo.isDistribution()?"True":"False");
            todo.unsetObligationDatabaseIds();
        }

        for (Todo todo : todos) {
            if(! todo.isSetWhitelist()){
                todo.setWhitelist(Collections.emptySet());
            }
        }
    }

    public Risk getRiskById(String id) {
        final Risk risk = riskRepository.get(id);
        fillRisk(risk);
        return risk;
    }

    private void fillRisk(Risk risk) {
        if (risk.isSetRiskCategoryDatabaseId()) {
            final RiskCategory riskCategory = riskCategoryRepository.get(risk.getRiskCategoryDatabaseId());
            risk.setCategory(riskCategory);
            risk.unsetRiskCategoryDatabaseId();
        }
    }

    public RiskCategory getRiskCategoryById(String id) {
        return riskCategoryRepository.get(id);
    }

    public Obligation getObligationById(String id) {
        return obligationRepository.get(id);
    }

    public LicenseType getLicenseTypeById(String id) {
        return licenseTypeRepository.get(id);
    }

    public Todo getTodoById(String id) {
        final Todo todo = todoRepository.get(id);

        fillTodo(todo);

        return todo;
    }

    private void fillTodo(Todo todo) {
        if (todo.isSetObligationDatabaseIds()) {
            final List<Obligation> obligations = obligationRepository.get(todo.getObligationDatabaseIds());
            todo.setObligations(obligations);
            todo.unsetObligationDatabaseIds();

            todo.setDevelopmentString(todo.isDevelopment()?"True":"False");
            todo.setDistributionString(todo.isDistribution()?"True":"False");
        }
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

    public RequestStatus deleteTodo(String id, User user) throws SW360Exception {
        Todo todo = todoRepository.get(id);
        assertNotNull(todo);

        // Remove the license if the user is allowed to do it by himself
        if (PermissionUtils.isUserAtLeast(UserGroup.SW360_ADMIN, user)) {
            todoRepository.remove(todo);
            return RequestStatus.SUCCESS;
        } else {
            log.error(user + " does not have the permission to delete todo.");
            return RequestStatus.ACCESS_DENIED;
        }
    }
}
