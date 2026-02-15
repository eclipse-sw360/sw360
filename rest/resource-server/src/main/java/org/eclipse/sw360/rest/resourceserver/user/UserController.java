/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.user;

import com.google.common.collect.ImmutableSet;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.configuration.SW360ConfigurationsService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.data.domain.Pageable;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_TOKEN_HASH_SALT;
import static org.eclipse.sw360.rest.resourceserver.user.Sw360UserService.AUTHORITIES_READ;
import static org.eclipse.sw360.rest.resourceserver.user.Sw360UserService.AUTHORITIES_WRITE;
import static org.eclipse.sw360.rest.resourceserver.user.Sw360UserService.EXPIRATION_DATE_PROPERTY;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class UserController implements RepresentationModelProcessor<RepositoryLinksResource> {

    protected final EntityLinks entityLinks;

    static final String USERS_URL = "/users";

    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final PasswordEncoder passwordEncoder;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final SW360ConfigurationsService sw360ConfigurationsService;

    private static final ImmutableSet<User._Fields> setOfUserProfileFields =
            ImmutableSet.<User._Fields>builder().add(User._Fields.WANTS_MAIL_NOTIFICATION)
                    .add(User._Fields.NOTIFICATION_PREFERENCES).build();

    @Operation(summary = "List all of the service's users.",
            description = "List all of the service's users.", tags = {"Users"})
    @GetMapping(value = USERS_URL)
    public ResponseEntity<CollectionModel<EntityModel<User>>> getUsers(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request,
            @Parameter(description = "Given Name of the users")
            @RequestParam(value = "givenname", required = false) String givenname,
            @Parameter(description = "Last Name of the users")
            @RequestParam(value = "lastname", required = false) String lastname,
            @Parameter(description = "Email of the user")
            @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "Department of the users")
            @RequestParam(value = "department", required = false) String department,
            @Parameter(description = "Role of the users")
            @RequestParam(value = "usergroup", required = false) UserGroup usergroup,
            @Parameter(description = "luceneSearch parameter to filter the users.")
            @RequestParam(value = "luceneSearch", required = false) boolean luceneSearch
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(user);

        Map<PaginationData, List<User>> paginatedUsers = null;
        Map<String, Set<String>> filterMap = getFilterMap(givenname, lastname, email, department,
                usergroup, luceneSearch);
        if (luceneSearch) {
            paginatedUsers = userService.refineSearch(filterMap, pageable);
        } else {
            if (filterMap.isEmpty()) {
                paginatedUsers = userService.getUsersWithPagination(pageable);
            } else {
                paginatedUsers = userService.searchUsersByExactValues(filterMap, pageable);
            }
        }
        PaginationResult<User> paginationResult = null;
        List<User> allUsers = new ArrayList<>(paginatedUsers.values().iterator().next());
        int totalCount = Math.toIntExact(paginatedUsers.keySet().stream()
                .findFirst().map(PaginationData::getTotalRowCount).orElse(0L));

        paginationResult = restControllerHelper.paginationResultFromPaginatedList(
                request, pageable, allUsers, SW360Constants.TYPE_USER, totalCount);

        List<EntityModel<User>> userResources = new ArrayList<>();
        for (User sw360User : paginationResult.getResources()) {
            User embeddedUser = restControllerHelper.convertToEmbeddedGetUsers(sw360User);
            EntityModel<User> userResource = EntityModel.of(embeddedUser);
            userResources.add(userResource);
        }

        CollectionModel<EntityModel<User>> resources;
        if (userResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(User.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, userResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    // '/users/{xyz}' searches by email, as opposed to by id, as is customary,
    // for compatibility with older version of the REST API
    @Operation(summary = "Get a single user.", description = "Get a single user by email.",
            tags = {"Users"})
    @GetMapping(value = USERS_URL + "/{email:.+}")
    public ResponseEntity<EntityModel<User>> getUserByEmail(
            @Parameter(description = "The email of the user to be retrieved.")
            @PathVariable("email") String email
    ) {
        String decodedEmail;
        decodedEmail = URLDecoder.decode(email, StandardCharsets.UTF_8);

        User sw360User = userService.getUserByEmail(decodedEmail);
        HalResource<User> halResource = createHalUser(sw360User);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    // unusual URL mapping for compatibility with older version of the REST API (see
    // getUserByEmail())
    @Operation(summary = "Get a single user.", description = "Get a single user by id.",
            tags = {"Users"})
    @GetMapping(value = USERS_URL + "/byid/{id:.+}")
    public ResponseEntity<EntityModel<User>> getUser(
            @Parameter(description = "The id of the user to be retrieved.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = userService.getUser(id);
        HalResource<User> halResource = createHalUser(sw360User);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @Operation(summary = "Create a new user.", description = "Create a user (not in Liferay).",
            tags = {"Users"})
    @PostMapping(value = USERS_URL)
    public ResponseEntity<EntityModel<User>> createUser(
            @Parameter(description = "The user to be created.")
            @RequestBody User user
    ) {
        if (CommonUtils.isNullEmptyOrWhitespace(user.getPassword())) {
            throw new BadRequestClientException(
                    "Password can not be null or empty or whitespace!");
        }
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        User createdUser = userService.addUser(user);
        HalResource<User> halResource = createHalUser(createdUser);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdUser.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @Operation(summary = "Get current user's profile.", description = "Get current user's profile.",
            tags = {"Users"})
    @GetMapping(value = USERS_URL + "/profile")
    public ResponseEntity<HalResource<User>> getUserProfile() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        HalResource<User> halUserResource = new HalResource<>(sw360User);
        return ResponseEntity.ok(halUserResource);
    }

    @Operation(summary = "Update user's profile.", description = "Update user's profile.",
            tags = {"Users"})
    @PatchMapping(value = USERS_URL + "/profile")
    public ResponseEntity<HalResource<User>> updateUserProfile(
            @Parameter(description = "Updated user profile", schema = @Schema(example = """
                    {
                        "wantsMailNotification": true,
                        "notificationPreferences": {
                            "releaseCONTRIBUTORS": true,
                            "componentCREATED_BY": false,
                            "releaseCREATED_BY": false,
                            "moderationREQUESTING_USER": false,
                            "projectPROJECT_OWNER": true,
                            "moderationMODERATORS": false,
                            "releaseSUBSCRIBERS": true,
                            "componentMODERATORS": true,
                            "projectMODERATORS": false,
                            "projectROLES": false,
                            "releaseROLES": true,
                            "componentROLES": true,
                            "projectLEAD_ARCHITECT": false,
                            "componentCOMPONENT_OWNER": true,
                            "projectSECURITY_RESPONSIBLES": true,
                            "clearingREQUESTING_USER": true,
                            "projectCONTRIBUTORS": true,
                            "componentSUBSCRIBERS": true,
                            "projectPROJECT_RESPONSIBLE": false,
                            "releaseMODERATORS": false
                        }
                    }
                    """))
            @RequestBody Map<String, Object> userProfile
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        sw360User = restControllerHelper.updateUserProfile(sw360User, userProfile,
                setOfUserProfileFields);
        userService.updateUser(sw360User);
        HalResource<User> halUserResource = new HalResource<>(sw360User);
        return ResponseEntity.ok(halUserResource);
    }

    @Operation(summary = "List all of rest api tokens.",
            description = "List all of rest api tokens of current user.",
            responses = {@ApiResponse(responseCode = "200", description = "List of tokens.")},
            tags = {"Users"})
    @GetMapping(value = USERS_URL + "/tokens")
    public ResponseEntity<CollectionModel<EntityModel<RestApiToken>>> getUserRestApiTokens() {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<RestApiToken> restApiTokens = sw360User.getRestApiTokens();

        if (restApiTokens == null) {
            return new ResponseEntity<>(CollectionModel.of(Collections.emptyList()), HttpStatus.OK);
        }

        List<EntityModel<RestApiToken>> restApiResources =
                restApiTokens.stream().map(EntityModel::of).collect(Collectors.toList());
        return new ResponseEntity<>(CollectionModel.of(restApiResources), HttpStatus.OK);
    }

    @Operation(summary = "Create rest api token.",
            description = "Create rest api token for current user.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Create token successfully."),
                    @ApiResponse(responseCode = "403", description = "API token requested with write authority when not allowed"),
                    @ApiResponse(responseCode = "500", description = "Create token failure.")
            },
            tags = {"Users"})
    @PostMapping(value = USERS_URL + "/tokens")
    public ResponseEntity<String> createUserRestApiToken(
            @Parameter(description = "Token request",
                    schema = @Schema(
                            type = "object",
                            example = "{\n" +
                              "  \"name\": \"my-new-token\",\n" +
                              "  \"" + EXPIRATION_DATE_PROPERTY + "\": \"2025-12-31\",\n" +
                              "  \"authorities\": [\n" +
                              "  \"" + AUTHORITIES_READ + "\",\n" +
                              "  \"" + AUTHORITIES_WRITE + "\"\n" +
                              "  ]\n}",
                            requiredProperties = {"name", "authorities", EXPIRATION_DATE_PROPERTY}
                    ))
            @RequestBody Map<String, Object> requestBody
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RestApiToken restApiToken = userService.convertToRestApiToken(requestBody, sw360User);
        String tokenLengthStr = sw360ConfigurationsService.getSW360Configs()
                .get(SW360ConfigKeys.REST_API_TOKEN_LENGTH);
        if (CommonUtils.isNullEmptyOrWhitespace(tokenLengthStr)) {
            throw new BadRequestClientException(
                    "API token length is not configured. Please set '" + SW360ConfigKeys.REST_API_TOKEN_LENGTH + "' in SW360 configurations.");
        }
        int tokenLength = Integer.parseInt(tokenLengthStr);
        String token = RandomStringUtils.secure().nextAlphanumeric(tokenLength);
        restApiToken.setToken(BCrypt.hashpw(token, API_TOKEN_HASH_SALT));
        sw360User.addToRestApiTokens(restApiToken);
        userService.updateUser(sw360User);

        return new ResponseEntity<>(token, HttpStatus.CREATED);
    }

    @Operation(summary = "Delete rest api token.",
            description = "Delete rest api token by name for current user.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Revoke token successfully."),
                    @ApiResponse(responseCode = "404", description = "Token name not found.")},
            tags = {"Users"})
    @DeleteMapping(value = USERS_URL + "/tokens")
    public ResponseEntity<String> revokeUserRestApiToken(
            @Parameter(description = "Name of token to be revoked.",
                    example = "MyToken")
            @RequestParam("name") String tokenName
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        if (!userService.isTokenNameExisted(sw360User, tokenName)) {
            throw new ResourceNotFoundException("Token not found: " + StringEscapeUtils.escapeHtml4(tokenName));
        }

        sw360User.getRestApiTokens().removeIf(t -> t.getName().equals(tokenName));
        userService.updateUser(sw360User);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(UserController.class).slash("api/users").withRel("users"));
        return resource;
    }

    private HalResource<User> createHalUser(User sw360User) {
        return new HalResource<>(sw360User);
    }

    @Operation(summary = "Fetch group list of a user.",
            description = "Fetch the list of group for a particular user.", tags = {"Users"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User and its groups.",
            content = {
                    @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                                "primaryGrpList": ["DEPARTMENT"],
                                "secondaryGrpList": ["DEPARTMENT1","DEPARTMENT2"]
                            }
                            """))})})
    @GetMapping(value = USERS_URL + "/groupList")
    public ResponseEntity<Map<String, List<String>>> getGroupList() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<String> primaryGrpList = new ArrayList<>();
        List<String> secondaryGrpList = new ArrayList<>();
        Map<String, List<String>> userGroupMap = new HashMap<>();
        String mainDepartment = sw360User.getDepartment();
        if (mainDepartment != null && !mainDepartment.isEmpty()) {
            primaryGrpList.add(mainDepartment);
        }
        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles =
                sw360User.getSecondaryDepartmentsAndRoles();
        if (secondaryDepartmentsAndRoles != null) {
            secondaryGrpList.addAll(secondaryDepartmentsAndRoles.keySet());
        }
        userGroupMap.put("primaryGrpList", primaryGrpList);
        userGroupMap.put("secondaryGrpList", secondaryGrpList);
        return new ResponseEntity<>(userGroupMap, HttpStatus.OK);
    }

    @Operation(summary = "Update an existing user.", description = "Update an existing user",
            tags = {"Users"})
    @PatchMapping(value = USERS_URL + "/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<EntityModel<User>> patchUser(
            @Parameter(description = "The user data to be updated.", schema = @Schema(implementation = User.class)) @RequestBody @NotNull User user,
            @Parameter(description = "Id of updated user") @PathVariable String id
    ) throws TException {
        if (user.getPassword() != null && user.getPassword().isEmpty()) {
            user.unsetPassword();
        }
        if (user.getPassword() != null) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }

        User userToUpdate = userService.getUser(id);
        userToUpdate = this.restControllerHelper.updateUser(userToUpdate, user);

        userService.updateUser(userToUpdate);
        HalResource<User> halResource = createHalUser(userToUpdate);

        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @Operation(summary = "Get existing departments.", description = "Get existing departments from all users",
            tags = {"Users"})
    @GetMapping(value = USERS_URL + "/departments")
    public ResponseEntity<?> getExistingDepartments(
            @Parameter(description = "Type of department (primary, secondary)")
            @RequestParam(value = "type", required = false) String type
    ) {
        if (!CommonUtils.isNotNullEmptyOrWhitespace(type)) {
            return new ResponseEntity<>(userService.getAvailableDepartments(), HttpStatus.OK);
        }
        return switch (type.toLowerCase()) {
            case "primary" -> new ResponseEntity<>(userService.getExistingPrimaryDepartments(), HttpStatus.OK);
            case "secondary" -> new ResponseEntity<>(userService.getExistingSecondaryDepartments(), HttpStatus.OK);
            default -> new ResponseEntity<>("Type must be: primary or secondary", HttpStatus.BAD_REQUEST);
        };
    }

    /**
     * Create a map of filters with the field name in the key and expected value in the value (as set).
     * @return Filter map from the user's request.
     */
    private @NonNull Map<String, Set<String>> getFilterMap(
            String givenName, String lastName, String email, String department,
            UserGroup usergroup, boolean luceneSearch
    ) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(givenName)) {
            Set<String> values = CommonUtils.splitToSet(givenName);
            if (luceneSearch) {
                values = values.stream()
                        .map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
            }
            filterMap.put(User._Fields.GIVENNAME.getFieldName(), values);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(email)) {
            Set<String> values = CommonUtils.splitToSet(email);
            if (luceneSearch) {
                values = values.stream()
                        .map(NouveauLuceneAwareDatabaseConnector::prepareFuzzyQuery)
                        .collect(Collectors.toSet());
            }
            filterMap.put(User._Fields.EMAIL.getFieldName(), values);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(department)) {
            Set<String> values = CommonUtils.splitToSet(department);
            filterMap.put(User._Fields.DEPARTMENT.getFieldName(), values);
        }
        if (usergroup != null) {
            Set<String> values = CommonUtils.splitToSet(usergroup.toString());
            filterMap.put(User._Fields.USER_GROUP.getFieldName(), values);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(lastName)) {
            Set<String> values = CommonUtils.splitToSet(lastName);
            if (luceneSearch) {
                values = values.stream()
                        .map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
            }
            filterMap.put(User._Fields.LASTNAME.getFieldName(), values);
        }
        return filterMap;
    }
}
