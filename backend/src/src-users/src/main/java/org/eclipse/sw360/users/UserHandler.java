/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.users.db.UserDatabaseHandler;
import org.ektorp.http.HttpClient;

import com.cloudant.client.api.CloudantClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserHandler implements UserService.Iface {

    private static final Logger log = LogManager.getLogger(UserHandler.class);

    private UserDatabaseHandler db;

    public UserHandler() throws IOException {
        db = new UserDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_USERS);
    }

    public UserHandler(Supplier<CloudantClient> client, Supplier<HttpClient> httpclient, String userDbName) throws IOException {
        db = new UserDatabaseHandler(client, httpclient, userDbName);
    }

    @Override
    public User getUser(String id) {
        return db.getUser(id);
    }

    @Override
    public User getByEmail(String email) throws TException {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
        assertNotEmpty(email, "Invalid empty email " + stackTraceElement.getFileName() + ": "  + stackTraceElement.getLineNumber());

        if (log.isTraceEnabled()) log.trace("getByEmail: " + email);

        return db.getByEmail(email);
    }

    @Override
    public User getByEmailOrExternalId(String email, String externalId) throws TException {
        User user = getByEmail(email);
        if (user == null) {
            user = db.getByExternalId(externalId);
        }
        if (user != null && user.isDeactivated()) {
            return null;
        }
        return user;
    }

    @Override
    public User getByApiToken(String token) throws TException {
        assertNotEmpty(token);
        return db.getByApiToken(token);
    }


    @Override
    public User getByOidcClientId(String clientId) throws TException {
        assertNotEmpty(clientId);
        return db.getByOidcClientId(clientId);
    }

    @Override
    public List<User> searchUsers(String searchText) {
        return db.searchUsers(searchText);
    }

    @Override
    public List<User> getAllUsers() {
        return db.getAll();
    }

    @Override
    public AddDocumentRequestSummary addUser(User user) throws TException {
        assertUser(user);
        return db.addUser(user);
    }

    @Override
    public RequestStatus updateUser(User user) throws TException {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.updateUser(user);
    }

    @Override
    public RequestStatus deleteUser(User user, User adminUser) throws TException {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.deleteUser(user, adminUser);
    }

    @Override
    public String getDepartmentByEmail(String email) throws TException {
        User user = getByEmail(email);
        return user != null ? user.getDepartment() : null;
    }

    @Override
    public Map<PaginationData, List<User>> getUsersWithPagination(User user, PaginationData pageData)
            throws TException {
        return db.getUsersWithPagination(pageData);
    }

    @Override
    public List<User> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return db.search(text, subQueryRestrictions);
    }

    @Override
    public Set<String> getUserDepartments() throws TException {
        return db.getUserDepartments();
    }

    @Override
    public Set<String> getUserEmails() throws TException {
        return db.getUserEmails();
    }
}
