/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenses;


import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.licenses.db.LicenseDatabaseHandler;
import org.apache.thrift.TException;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicenseHandler implements LicenseService.Iface {

    LicenseDatabaseHandler handler;

    LicenseHandler() throws MalformedURLException {
        handler = new LicenseDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE);
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////

    /**
     * Get a list of all obligations
     */
    @Override
    public List<Obligation> getObligations() throws TException {
        return handler.getObligations();
    }

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
    public List<License> getDetailedLicenseSummaryForExport(String organisation) throws TException {
        return handler.getDetailedLicenseSummaryForExport(organisation);
    }

    @Override
    public List<License> getDetailedLicenseSummary(String organisation, List<String> identifiers) throws TException {
        return handler.getDetailedLicenseSummaryForExport(organisation, identifiers);
    }

    @Override
    public List<RiskCategory> addRiskCategories(List<RiskCategory> riskCategories, User user) throws TException {
        return handler.addRiskCategories(riskCategories, user);
    }

    @Override
    public List<Risk> addRisks(List<Risk> risks, User user) throws TException {
        return handler.addRisks(risks, user);
    }

    @Override
    public List<Obligation> addObligations(List<Obligation> obligations, User user) throws TException {
        return handler.addObligations(obligations, user);

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
    public List<Todo> addTodos(List<Todo> todos, User user) throws TException {
        return handler.addTodos(todos, user);

    }

    @Override
    public List<RiskCategory> getRiskCategories() throws TException {
        return handler.getRiskCategories() ;
    }

    @Override
    public List<Risk> getRisks() throws TException {
        return handler.getRisks();
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
    public List<Todo> getTodos() throws TException {
        return handler.getTodos();
    }

    @Override
    public List<Risk> getRisksByIds(List<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getRisksByIds(ids);
    }

    @Override
    public List<RiskCategory> getRiskCategoriesByIds(List<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getRiskCategoriesByIds(ids);
    }

    @Override
    public List<Obligation> getObligationsByIds(List<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getObligationsByIds(ids);
    }

    @Override
    public List<LicenseType> getLicenseTypesByIds(List<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getLicenseTypesByIds(ids);
    }

    @Override
    public List<Todo> getTodosByIds(List<String> ids) throws TException {
        assertNotEmpty(ids);
        return handler.getTodosByIds(ids);
    }



    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    /**
     * Get a single license by providing its ID, with todos filtered for the given organisation
     */
    @Override
    public License getByID(String id, String organisation) throws TException {
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
    public Risk getRiskById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getRiskById(id);
    }

    @Override
    public RiskCategory getRiskCategoryById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getRiskCategoryById(id);
    }

    @Override
    public Obligation getObligationById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getObligationById(id);
    }

    @Override
    public LicenseType getLicenseTypeById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getLicenseTypeById(id);
    }

    @Override
    public Todo getTodoById(String id) throws TException {
        assertNotEmpty(id);
        return handler.getTodoById(id);
    }

    ////////////////////
    // BUSINESS LOGIC //
    ////////////////////

    /**
     * Add a new todo object
     */
    @Override
    public String addTodo(Todo todo, User user) throws TException {
        assertNotNull(todo);
        assertIdUnset(todo.getId());

        return handler.addTodo(todo, user);
    }

    /**
     * Add an existing todo to a license
     */
    @Override
    public RequestStatus addTodoToLicense(Todo todo, String licenseId, User user) throws TException {
        assertNotEmpty(licenseId);
       return  handler.addTodoToLicense(todo, licenseId, user);
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
    public RequestStatus deleteTodo(String id, User user) throws TException {
        assertId(id);
        assertUser(user);
        return handler.deleteTodo(id, user);
    }

}
