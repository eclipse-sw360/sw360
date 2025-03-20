/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.user;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_WRITE_ACCESS_USERGROUP;

@Service
public class Sw360UserService {
    private static final Logger log = LogManager.getLogger(Sw360UserService.class);
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;
    private static final String AUTHORITIES_READ = "READ";
    private static final String AUTHORITIES_WRITE = "WRITE";
    private static final String EXPIRATION_DATE_PROPERTY = "expirationDate";

    public List<User> getAllUsers() {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getAllUsers();
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByEmail(String email) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByEmail(email);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByEmailOrExternalId(String userIdentifier) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByEmailOrExternalId(userIdentifier, userIdentifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User getUser(String id) throws TException {
        UserService.Iface sw360UserClient = getThriftUserClient();
        try {
            return sw360UserClient.getUser(id);
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested User Not Found");
            } else {
                throw sw360Exp;
            }
        }
    }

    public User getUserByApiToken(String token) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByApiToken(token);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserFromClientId(String clientId) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByOidcClientId(clientId);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User addUser(User user) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            user.setUserGroup(UserGroup.USER);
            AddDocumentRequestSummary documentRequestSummary = sw360UserClient.addUser(user);
            if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
                user.setId(documentRequestSummary.getId());
                return user;
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
                throw new DataIntegrityViolationException("sw360 user with name '" + user.getEmail()
                        + "' already exists, having database identifier " + documentRequestSummary.getId());
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
                throw new BadRequestClientException(documentRequestSummary.getMessage());
            }
        } catch (TException e) {
            throw new BadRequestClientException(e.getMessage());
        }
        return null;
    }

    private UserService.Iface getThriftUserClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new UserService.Client(protocol);
    }

    public void updateUser(User sw360User) throws TException {
        UserService.Iface sw360UserClient = getThriftUserClient();
        sw360UserClient.updateUser(sw360User);
    }

    public List<User> refineSearch(Map<String, Set<String>> filterMap) throws TException {
        UserService.Iface sw360UserClient = getThriftUserClient();
        return sw360UserClient.refineSearch(null, filterMap);
    }

    public List<User> searchUserByName(String givenname) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.searchUsers(givenname);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> searchUserByLastName(String lastname) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.searchUsers(lastname);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> searchUserByDepartment(String department) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.searchDepartmentUsers(department);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public List<User> searchUserByUserGroup(UserGroup usergroup) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.searchUsersGroup(usergroup);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public RestApiToken convertToRestApiToken(Map<String, Object> requestBody, User sw360User) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (!requestBody.containsKey(EXPIRATION_DATE_PROPERTY)
                || CommonUtils.isNullEmptyOrWhitespace(requestBody.get(EXPIRATION_DATE_PROPERTY).toString())) {
            throw new IllegalArgumentException("expirationDate is a required field.");
        }
        if (!(requestBody.get(EXPIRATION_DATE_PROPERTY) instanceof String)) {
            throw new IllegalArgumentException("expirationDate must be a string.");
        }

        RestApiToken restApiToken = mapper.convertValue(requestBody, RestApiToken.class);
        int numberOfExpireDay = getNumberOfExpireDays(requestBody.get(EXPIRATION_DATE_PROPERTY).toString());
        if (numberOfExpireDay < 0) {
            throw new IllegalArgumentException("Token expiration days is not valid for user");
        }
        restApiToken.setNumberOfDaysValid(numberOfExpireDay);
        restApiToken.setName(restApiToken.getName().trim());
        validateRestApiToken(restApiToken, sw360User);
        restApiToken.setCreatedOn(SW360Utils.getCreatedOnTime());


        return restApiToken;
    }

    private int getNumberOfExpireDays(String requestExpirationDate) {
        LocalDate expirationDate = LocalDate.parse(requestExpirationDate);
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    public boolean isTokenNameExisted(User user, String tokenName) {
        return CommonUtils.nullToEmptyList(user.getRestApiTokens()).stream().anyMatch(t -> t.getName().equals(tokenName));
    }

    private boolean isValidExpireDays(RestApiToken restApiToken) {
        String configExpireDays = restApiToken.getAuthorities().contains(AUTHORITIES_WRITE) ?
                API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS : API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;

        try {
            return restApiToken.getNumberOfDaysValid() >= 0 &&
                    restApiToken.getNumberOfDaysValid() <= Integer.parseInt(configExpireDays);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void validateRestApiToken(RestApiToken restApiToken, User sw360User) {
        if (CommonUtils.isNullEmptyOrWhitespace(restApiToken.getName())) {
            throw new IllegalArgumentException("Token name is required.");
        }

        if (isTokenNameExisted(sw360User, restApiToken.getName())) {
            throw new IllegalArgumentException("Duplicate token name.");
        }

        if (!restApiToken.getAuthorities().contains(AUTHORITIES_READ)) {
            throw new IllegalArgumentException("READ permission is required.");
        }


        if (restApiToken.getAuthorities().contains(AUTHORITIES_WRITE)) {
            // User needs at least the role which is defined in sw360.properties (default admin)
            if (!PermissionUtils.isUserAtLeast(API_WRITE_ACCESS_USERGROUP, sw360User))
                throw new IllegalArgumentException("User permission [WRITE] is not allowed for user");
            if (!isValidExpireDays(restApiToken)) {
                throw new IllegalArgumentException("Token expiration days is not valid for user");
            }
        }

        // Only READ and WRITE permission is allowed
        Set<String> otherPermissions = restApiToken.getAuthorities()
                .stream()
                .filter(permission -> !permission.equals(AUTHORITIES_READ) && !permission.equals(AUTHORITIES_WRITE))
                .collect(Collectors.toSet());
        if (!otherPermissions.isEmpty()) {
            throw new IllegalArgumentException("Invalid permissions: " + String.join(", ", otherPermissions) + ".");
        }
    }

    public Set<String> getAvailableDepartments() {
        Set<String> primaryDepartments = getExistingPrimaryDepartments();
        Set<String> secondaryDepartments = getExistingSecondaryDepartments();
        return Sets.union(primaryDepartments, secondaryDepartments);
    }

    public Set<String> getExistingPrimaryDepartments() {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getUserDepartments();
        } catch (TException e) {
            log.error(e.getMessage());
            return Collections.emptySet();
        }
    }

    public Set<String> getExistingSecondaryDepartments() {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getUserSecondaryDepartments();
        } catch (TException e) {
            log.error(e.getMessage());
            return Collections.emptySet();
        }
    }
}
