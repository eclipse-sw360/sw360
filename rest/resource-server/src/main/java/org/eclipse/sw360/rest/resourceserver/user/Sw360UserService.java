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
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.users.UserSortColumn;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_USER;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_WRITE_ACCESS_USERGROUP;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_WRITE_TOKEN_GENERATOR_ENABLED;

@Service
public class Sw360UserService {
    private static final Logger log = LogManager.getLogger(Sw360UserService.class);
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;
    public static final String AUTHORITIES_READ = "READ";
    public static final String AUTHORITIES_WRITE = "WRITE";
    public static final String EXPIRATION_DATE_PROPERTY = "expirationDate";

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

    public Map<PaginationData, List<User>> refineSearch(Map<String, Set<String>> filterMap, Pageable pageable) throws TException {
        UserService.Iface sw360UserClient = getThriftUserClient();
        PaginationData pageData = pageableToPaginationData(pageable);
        return sw360UserClient.refineSearch(null, filterMap, pageData);
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(Pageable pageable) throws TException {
        UserService.Iface sw360UserClient = getThriftUserClient();
        PaginationData pageData = pageableToPaginationData(pageable);
        return sw360UserClient.getUsersWithPagination(null, pageData);
    }

    public Map<PaginationData, List<User>> searchUsersByExactValues(Map<String, Set<String>> filterMap, Pageable pageable) throws TException {
        UserService.Iface sw360UserClient = getThriftUserClient();
        PaginationData pageData = pageableToPaginationData(pageable);
        return sw360UserClient.searchUsersByExactValues(filterMap, pageData);
    }

    public RestApiToken convertToRestApiToken(Map<String, Object> requestBody, User sw360User) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (!requestBody.containsKey(EXPIRATION_DATE_PROPERTY)
                || CommonUtils.isNullEmptyOrWhitespace(requestBody.get(EXPIRATION_DATE_PROPERTY).toString())) {
            throw new IllegalArgumentException(EXPIRATION_DATE_PROPERTY + " is a required field.");
        }
        if (!(requestBody.get(EXPIRATION_DATE_PROPERTY) instanceof String)) {
            throw new IllegalArgumentException(EXPIRATION_DATE_PROPERTY + " must be a string.");
        }

        RestApiToken restApiToken = mapper.convertValue(requestBody, RestApiToken.class);
        if (!API_WRITE_TOKEN_GENERATOR_ENABLED && restApiToken.authorities.contains(AUTHORITIES_WRITE)) {
            throw new AccessDeniedException("Token requested with '" +
                    AUTHORITIES_WRITE + "' authority, but not allowed.");
        }
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

    /**
     * Converts a Pageable object to a PaginationData object.
     *
     * @param pageable the Pageable object to convert
     * @return a PaginationData object representing the pagination information
     */
    private static PaginationData pageableToPaginationData(@NotNull Pageable pageable) {
        UserSortColumn column = UserSortColumn.BY_GIVENNAME;
        boolean ascending = true;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            column = switch (property) {
                case "lastname" -> UserSortColumn.BY_LASTNAME;
                case "email" -> UserSortColumn.BY_EMAIL;
                case "deactivated" -> UserSortColumn.BY_STATUS;
                case "department" -> UserSortColumn.BY_DEPARTMENT;
                case "primaryRoles" -> UserSortColumn.BY_ROLE;
                default -> column; // Default to BY_GIVENNAME if no match
            };
            ascending = order.isAscending();
        }
        return new PaginationData().setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize()).setSortColumnNumber(column.getValue()).setAscending(ascending);
    }

    /**
     * Sync a single UserCSV with Thrift Database
     *
     * @param userRec       UserCSV record to sync
     * @param thriftClients Thrift object to create clients
     * @return True if the user was synced successfully, false otherwise.
     */
    public static boolean syncUser(UserCSV userRec, ThriftClients thriftClients) {
        User thriftUser = null;
        try {
            thriftUser = synchronizeUserWithDatabase(userRec, thriftClients, userRec::getEmail, userRec::getGid, Sw360UserService::fillThriftUserFromUserCSV);
        } catch (Exception e) {
            log.error("Error creating a new user", e);
            return false;
        }

        return thriftUser != null;
    }

    /**
     * Sync a UserCSV with thrift client by first checking if the user already exists by matching email or external id,
     * if found the user is updated. Otherwise, the user is inserted in the database.
     *
     * @param source        User record to be synced.
     * @param thriftClients Thrift clients.
     * @param emailSupplier Function to get user's email.
     * @param extIdSupplier Function to get user's external id.
     * @param synchronizer  Function to transfer properties from source to thrift object.
     * @param <T>           Usually UserCSV
     * @return Object of newly created or updated user on success or null on failure.
     */
    public static <T> User synchronizeUserWithDatabase(
            T source, ThriftClients thriftClients, Supplier<String> emailSupplier,
            Supplier<String> extIdSupplier, BiConsumer<User, T> synchronizer) {
        UserService.Iface client = thriftClients.makeUserClient();

        User existingThriftUser = null;

        String email = emailSupplier.get();
        try {
            existingThriftUser = client.getByEmailOrExternalId(email, extIdSupplier.get());
        } catch (TException e) {
            //This occurs for every new user, so there is not necessarily something wrong
            log.trace("User not found by email or external ID");
        }

        User resultUser = null;
        try {
            if (existingThriftUser == null) {
                log.info("Creating new user.");
                resultUser = new User();
                synchronizer.accept(resultUser, source);
                client.addUser(resultUser);
            } else {
                resultUser = existingThriftUser;
                if (!existingThriftUser.getEmail().equals(email)) { // email has changed
                    resultUser.setFormerEmailAddresses(prepareFormerEmailAddresses(existingThriftUser, email));
                }
                synchronizer.accept(resultUser, source);
                client.updateUser(resultUser);
            }
        } catch (TException e) {
            log.error("Thrift exception when saving the user", e);
        }
        return resultUser;
    }

    /**
     * Set the fields from CSV to the thrift user object
     *
     * @param thriftUser Thrift user object to be updated
     * @param userCsv    CSV user to read the properties from
     */
    public static void fillThriftUserFromUserCSV(final @NotNull User thriftUser, final @NotNull UserCSV userCsv) {
        thriftUser.setEmail(userCsv.getEmail());
        thriftUser.setType(TYPE_USER);
        thriftUser.setUserGroup(UserGroup.valueOf(userCsv.getGroup()));
        thriftUser.setExternalid(userCsv.getGid());
        thriftUser.setFullname(userCsv.getGivenname() + " " + userCsv.getLastname());
        thriftUser.setGivenname(userCsv.getGivenname());
        thriftUser.setLastname(userCsv.getLastname());
        thriftUser.setDepartment(userCsv.getDepartment());
        thriftUser.setWantsMailNotification(userCsv.isWantsMailNotification());
    }

    /**
     * Get the list of former email addresses of the user (if they change)
     */
    @NotNull
    public static Set<String> prepareFormerEmailAddresses(@NotNull User thriftUser, String email) {
        Set<String> formerEmailAddresses = nullToEmptySet(thriftUser.getFormerEmailAddresses()).stream()
                .filter(e -> !e.equals(email)) // make sure the current email is not in the former addresses
                .collect(Collectors.toCollection(HashSet::new));
        formerEmailAddresses.add(thriftUser.getEmail());
        return formerEmailAddresses;
    }
}
