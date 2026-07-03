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
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.users.RestApiToken;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.datahandler.services.users.UserSortColumn;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.clients.users.UsersClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
@RequiredArgsConstructor
public class Sw360UserService {
    private static final Logger log = LogManager.getLogger(Sw360UserService.class);
    public static final String AUTHORITIES_READ = "READ";
    public static final String AUTHORITIES_WRITE = "WRITE";
    public static final String EXPIRATION_DATE_PROPERTY = "expirationDate";

    private final UsersClient usersClient;

    public List<User> getAllUsers() {
        return usersClient.getAllUsers();
    }

    public User getUserByEmail(String email) {
        return usersClient.getByEmail(email);
    }

    public User getUserByEmailOrExternalId(String userIdentifier) {
        return usersClient.getByEmailOrExternalId(userIdentifier, userIdentifier);
    }

    public User getUser(String id) {
        try {
            return usersClient.getUser(id);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Requested User Not Found");
        }
    }

    public User getUserByApiToken(String token) {
        return usersClient.getByApiToken(token);
    }

    public User getUserFromClientId(String clientId) {
        return usersClient.getByOidcClientId(clientId);
    }

    public User addUser(User user) {
        user.setUserGroup(UserGroup.USER);
        AddDocumentRequestSummary documentRequestSummary = usersClient.addUser(user);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            user.setId(documentRequestSummary.getId());
            return user;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 user with name '" + user.getEmail()
                    + "' already exists, having database identifier " + documentRequestSummary.getId());
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
            throw new BadRequestClientException(documentRequestSummary.getMessage());
        }
        throw new BadRequestClientException("Failed to add user");
    }

    public void updateUser(User user) {
        usersClient.updateUser(user);
    }

    public Map<PaginationData, List<User>> refineSearch(Map<String, Set<String>> filterMap, Pageable pageable) {
        return toPaginatedMap(usersClient.refineSearch(null, filterMap, pageableToPaginationData(pageable)));
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(Pageable pageable) {
        return toPaginatedMap(usersClient.getUsersWithPagination(pageableToPaginationData(pageable)));
    }

    public Map<PaginationData, List<User>> searchUsersByExactValues(Map<String, Set<String>> filterMap,
            Pageable pageable) {
        return toPaginatedMap(usersClient.searchUsersByExactValues(filterMap, pageableToPaginationData(pageable)));
    }

    public Map<PaginationData, List<User>> searchUsersByNameOrEmail(String searchTerm, Pageable pageable) {
        return toPaginatedMap(usersClient.refineSearch(searchTerm, Collections.emptyMap(),
                pageableToPaginationData(pageable)));
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
        if (!API_WRITE_TOKEN_GENERATOR_ENABLED && restApiToken.getAuthorities().contains(AUTHORITIES_WRITE)) {
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
        return CommonUtils.nullToEmptyList(user.getRestApiTokens()).stream()
                .anyMatch(t -> t.getName().equals(tokenName));
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
            if (!PermissionUtils.isUserAtLeast(API_WRITE_ACCESS_USERGROUP, UserConverter.toThrift(sw360User))) {
                throw new IllegalArgumentException("User permission [WRITE] is not allowed for user");
            }
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
            return usersClient.getUserDepartments();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Collections.emptySet();
        }
    }

    public Set<String> getExistingSecondaryDepartments() {
        try {
            return usersClient.getUserSecondaryDepartments();
        } catch (Exception e) {
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
                case "score" -> UserSortColumn.BY_SCORE;
                default -> column;
            };
            ascending = order.isAscending();
        }
        return new PaginationData().setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize()).setSortColumnNumber(column.getValue()).setAscending(ascending);
    }

    /**
     * Sync a single UserCSV with the users database.
     *
     * @param userRec UserCSV record to sync
     * @return true if the user was synced successfully, false otherwise
     */
    public boolean syncUser(UserCSV userRec) {
        User user;
        try {
            user = synchronizeUserWithDatabase(userRec, userRec::getEmail, userRec::getGid,
                    Sw360UserService::fillUserFromUserCSV);
        } catch (Exception e) {
            log.error("Error creating a new user", e);
            return false;
        }
        return user != null;
    }

    /**
     * Sync a user record by first checking if the user already exists by matching email or external id.
     * If found, the user is updated; otherwise, the user is inserted in the database.
     *
     * @param source        user record to be synced
     * @param emailSupplier function to get the user's email
     * @param extIdSupplier function to get the user's external id
     * @param synchronizer  function to transfer properties from source to user object
     * @param <T>           usually UserCSV
     * @return newly created or updated user on success, or null on failure
     */
    public <T> User synchronizeUserWithDatabase(
            T source, Supplier<String> emailSupplier,
            Supplier<String> extIdSupplier, BiConsumer<User, T> synchronizer) {
        User existingUser = null;

        String email = emailSupplier.get();
        try {
            existingUser = usersClient.getByEmailOrExternalId(email, extIdSupplier.get());
        } catch (Exception e) {
            // This occurs for every new user, so there is not necessarily something wrong
            log.trace("User not found by email or external ID");
        }

        User resultUser = null;
        try {
            if (existingUser == null) {
                log.info("Creating new user.");
                resultUser = new User();
                synchronizer.accept(resultUser, source);
                usersClient.addUser(resultUser);
            } else {
                resultUser = existingUser;
                if (!existingUser.getEmail().equals(email)) { // email has changed
                    resultUser.setFormerEmailAddresses(prepareFormerEmailAddresses(existingUser, email));
                }
                synchronizer.accept(resultUser, source);
                usersClient.updateUser(resultUser);
            }
        } catch (Exception e) {
            log.error("Exception when saving the user", e);
        }
        return resultUser;
    }

    public static void fillUserFromUserCSV(final @NotNull User user, final @NotNull UserCSV userCsv) {
        user.setEmail(userCsv.getEmail());
        user.setType(TYPE_USER);
        user.setUserGroup(UserGroup.valueOf(userCsv.getGroup()));
        user.setExternalid(userCsv.getGid());
        user.setFullname(userCsv.getGivenname() + " " + userCsv.getLastname());
        user.setGivenname(userCsv.getGivenname());
        user.setLastname(userCsv.getLastname());
        user.setDepartment(userCsv.getDepartment());
        user.setWantsMailNotification(userCsv.isWantsMailNotification());
    }

    /**
     * Get the list of former email addresses of the user (if they change).
     */
    @NotNull
    public static Set<String> prepareFormerEmailAddresses(@NotNull User user, String email) {
        Set<String> formerEmailAddresses = nullToEmptySet(user.getFormerEmailAddresses()).stream()
                .filter(e -> !e.equals(email)) // make sure the current email is not in the former addresses
                .collect(Collectors.toCollection(HashSet::new));
        formerEmailAddresses.add(user.getEmail());
        return formerEmailAddresses;
    }

    private static Map<PaginationData, List<User>> toPaginatedMap(PaginatedResult<User> result) {
        if (result == null) {
            return Map.of();
        }
        return Map.of(result.getPaginationData(), result.getData());
    }
}
