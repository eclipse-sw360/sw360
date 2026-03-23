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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.PagedUsersResult;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
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

/**
 * Spring service that acts as the REST client adapter for the Users backend service.
 *
 * <p>Previously this class created a Thrift {@code THttpClient} pointing at
 * {@code /users/thrift} for every operation.  It now uses a Spring
 * {@link RestTemplate} to call the {@code backend-users} Spring Boot application
 * (default {@code http://localhost:8090}), which exposes the same functionality
 * as a conventional REST API.
 *
 * <p><strong>Data model note:</strong> The {@link User} and related classes are
 * still the Thrift-generated POJOs (from {@code libraries/datahandler}).  Their
 * replacement with hand-written POJOs is tracked as a separate follow-up task.
 * Jackson can serialize/deserialize these classes correctly with
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false}.
 */
@Service
public class Sw360UserService {

    private static final Logger log = LogManager.getLogger(Sw360UserService.class);

    @Value("${sw360.users-service-url:http://localhost:8090}")
    private String usersServiceUrl;

    public static final String AUTHORITIES_READ = "READ";
    public static final String AUTHORITIES_WRITE = "WRITE";
    public static final String EXPIRATION_DATE_PROPERTY = "expirationDate";

    private final RestTemplate restTemplate;

    public Sw360UserService() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);

        RestTemplate template = new RestTemplate();
        template.setRequestFactory(factory);
        template.getMessageConverters().removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        template.getMessageConverters().add(0, new MappingJackson2HttpMessageConverter(mapper));
        this.restTemplate = template;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public List<User> getAllUsers() {
        User[] users = restTemplate.getForObject(usersUrl("/all"), User[].class);
        return users != null ? Arrays.asList(users) : Collections.emptyList();
    }

    public User getUserByEmail(String email) {
        try {
            return restTemplate.getForObject(usersUrl("/byEmail?email={email}"), User.class, email);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw new RuntimeException("Error fetching user by email: " + email, e);
        }
    }

    public User getUserByEmailOrExternalId(String userIdentifier) {
        try {
            return restTemplate.getForObject(
                    usersUrl("/byEmailOrExternalId?email={id}&externalId={id}"),
                    User.class, userIdentifier, userIdentifier);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw new RuntimeException("Error fetching user by email or external id: " + userIdentifier, e);
        }
    }

    public User getUser(String id) throws SW360Exception {
        try {
            return restTemplate.getForObject(usersUrl("/{id}"), User.class, id);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Requested User Not Found");
            }
            throw new RuntimeException("Error fetching user with id: " + id, e);
        }
    }

    public User getUserByApiToken(String token) {
        try {
            return restTemplate.getForObject(usersUrl("/byApiToken?token={token}"), User.class, token);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw new RuntimeException("Error fetching user by API token", e);
        }
    }

    public User getUserFromClientId(String clientId) {
        try {
            return restTemplate.getForObject(
                    usersUrl("/byOidcClientId?clientId={clientId}"), User.class, clientId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw new RuntimeException("Error fetching user by OIDC client id: " + clientId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    public User addUser(User user) {
        user.setUserGroup(UserGroup.USER);
        try {
            AddDocumentRequestSummary summary =
                    restTemplate.postForObject(usersUrl(""), user, AddDocumentRequestSummary.class);
            if (summary == null) {
                throw new BadRequestClientException("No response from users service when adding user");
            }
            if (summary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
                user.setId(summary.getId());
                return user;
            } else if (summary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
                throw new DataIntegrityViolationException("sw360 user with name '" + user.getEmail()
                        + "' already exists, having database identifier " + summary.getId());
            } else if (summary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
                throw new BadRequestClientException(summary.getMessage());
            }
        } catch (HttpClientErrorException e) {
            throw new BadRequestClientException(e.getMessage());
        }
        return null;
    }

    public void updateUser(User sw360User) {
        try {
            restTemplate.put(usersUrl(""), sw360User);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error updating user: " + sw360User.getEmail(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Paginated / filtered searches
    // -------------------------------------------------------------------------

    public Map<PaginationData, List<User>> refineSearch(
            Map<String, Set<String>> filterMap, Pageable pageable) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", null);
        body.put("filterMap", filterMap);
        body.put("pageData", pageableToPaginationData(pageable));

        PagedUsersResult result = restTemplate.postForObject(usersUrl("/search"), body, PagedUsersResult.class);
        return toMap(result);
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(Pageable pageable) {
        PaginationData pageData = pageableToPaginationData(pageable);
        PagedUsersResult result = restTemplate.postForObject(usersUrl("/page"), pageData, PagedUsersResult.class);
        return toMap(result);
    }

    public Map<PaginationData, List<User>> searchUsersByExactValues(
            Map<String, Set<String>> filterMap, Pageable pageable) {
        Map<String, Object> body = new HashMap<>();
        body.put("filterMap", filterMap);
        body.put("pageData", pageableToPaginationData(pageable));

        PagedUsersResult result = restTemplate.postForObject(usersUrl("/searchExact"), body, PagedUsersResult.class);
        return toMap(result);
    }

    // -------------------------------------------------------------------------
    // Department queries
    // -------------------------------------------------------------------------

    public Set<String> getAvailableDepartments() {
        return Sets.union(getExistingPrimaryDepartments(), getExistingSecondaryDepartments());
    }

    public Set<String> getExistingPrimaryDepartments() {
        try {
            ResponseEntity<Set<String>> response = restTemplate.exchange(
                    usersUrl("/departments"), HttpMethod.GET, null,
                    new ParameterizedTypeReference<Set<String>>() {});
            return response.getBody() != null ? response.getBody() : Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to fetch primary departments: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    public Set<String> getExistingSecondaryDepartments() {
        try {
            ResponseEntity<Set<String>> response = restTemplate.exchange(
                    usersUrl("/secondaryDepartments"), HttpMethod.GET, null,
                    new ParameterizedTypeReference<Set<String>>() {});
            return response.getBody() != null ? response.getBody() : Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to fetch secondary departments: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    // -------------------------------------------------------------------------
    // Token helpers (no service call — pure local logic)
    // -------------------------------------------------------------------------

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
            if (!PermissionUtils.isUserAtLeast(API_WRITE_ACCESS_USERGROUP, sw360User))
                throw new IllegalArgumentException("User permission [WRITE] is not allowed for user");
            if (!isValidExpireDays(restApiToken)) {
                throw new IllegalArgumentException("Token expiration days is not valid for user");
            }
        }
        Set<String> otherPermissions = restApiToken.getAuthorities().stream()
                .filter(p -> !p.equals(AUTHORITIES_READ) && !p.equals(AUTHORITIES_WRITE))
                .collect(Collectors.toSet());
        if (!otherPermissions.isEmpty()) {
            throw new IllegalArgumentException("Invalid permissions: " + String.join(", ", otherPermissions) + ".");
        }
    }

    // -------------------------------------------------------------------------
    // CSV / import helpers
    //
    // TODO: Replace ThriftClients usage below with a call to the users REST API
    //       once all callers (importers, exporters) have been migrated away from
    //       Thrift.  Tracked as part of the full Thrift removal project.
    // -------------------------------------------------------------------------

    /**
     * Sync a single UserCSV with the users database.
     *
     * @deprecated Use the REST API directly.  Will be removed once all callers
     *             have been migrated off {@link ThriftClients}.
     */
    @Deprecated(since = "20.0.0", forRemoval = true)
    public static boolean syncUser(UserCSV userRec, ThriftClients thriftClients) {
        User thriftUser = null;
        try {
            thriftUser = synchronizeUserWithDatabase(
                    userRec, thriftClients, userRec::getEmail, userRec::getGid,
                    Sw360UserService::fillThriftUserFromUserCSV);
        } catch (Exception e) {
            log.error("Error creating a new user", e);
            return false;
        }
        return thriftUser != null;
    }

    /**
     * @deprecated Use the REST API directly.  Will be removed once all callers
     *             have been migrated off {@link ThriftClients}.
     */
    @Deprecated(since = "20.0.0", forRemoval = true)
    public static <T> User synchronizeUserWithDatabase(
            T source, ThriftClients thriftClients, Supplier<String> emailSupplier,
            Supplier<String> extIdSupplier, BiConsumer<User, T> synchronizer) {
        UserService.Iface client = thriftClients.makeUserClient();
        User existingThriftUser = null;
        String email = emailSupplier.get();
        try {
            existingThriftUser = client.getByEmailOrExternalId(email, extIdSupplier.get());
        } catch (Exception e) {
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
                if (!existingThriftUser.getEmail().equals(email)) {
                    resultUser.setFormerEmailAddresses(prepareFormerEmailAddresses(existingThriftUser, email));
                }
                synchronizer.accept(resultUser, source);
                client.updateUser(resultUser);
            }
        } catch (Exception e) {
            log.error("Exception when saving the user", e);
        }
        return resultUser;
    }

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

    @NotNull
    public static Set<String> prepareFormerEmailAddresses(@NotNull User thriftUser, String email) {
        Set<String> formerEmailAddresses = nullToEmptySet(thriftUser.getFormerEmailAddresses()).stream()
                .filter(e -> !e.equals(email))
                .collect(Collectors.toCollection(HashSet::new));
        formerEmailAddresses.add(thriftUser.getEmail());
        return formerEmailAddresses;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Prepends the base URL of the users service to the given path. */
    private String usersUrl(String path) {
        return usersServiceUrl + "/users" + path;
    }

    /** Converts a {@link PagedUsersResult} to the {@code Map<PaginationData, List<User>>} form
     *  still expected by the controllers that have not yet been updated. */
    private static Map<PaginationData, List<User>> toMap(PagedUsersResult result) {
        if (result == null) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(
                result.getPaginationData(),
                result.getUsers() != null ? result.getUsers() : Collections.emptyList());
    }

    /**
     * Converts a Spring {@link Pageable} to the {@link PaginationData} used by
     * the users service REST API.
     */
    private static PaginationData pageableToPaginationData(@NotNull Pageable pageable) {
        UserSortColumn column = UserSortColumn.BY_GIVENNAME;
        boolean ascending = true;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            column = switch (order.getProperty()) {
                case "lastname"    -> UserSortColumn.BY_LASTNAME;
                case "email"       -> UserSortColumn.BY_EMAIL;
                case "deactivated" -> UserSortColumn.BY_STATUS;
                case "department"  -> UserSortColumn.BY_DEPARTMENT;
                case "primaryRoles"-> UserSortColumn.BY_ROLE;
                default            -> column;
            };
            ascending = order.isAscending();
        }
        return new PaginationData()
                .setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize())
                .setSortColumnNumber(column.getValue())
                .setAscending(ascending);
    }
}
