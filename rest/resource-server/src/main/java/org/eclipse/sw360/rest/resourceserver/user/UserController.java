/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.data.domain.Pageable;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
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

    private static final ImmutableSet<User._Fields> setOfUserProfileFields = ImmutableSet.<User._Fields>builder()
            .add(User._Fields.WANTS_MAIL_NOTIFICATION)
            .add(User._Fields.NOTIFICATION_PREFERENCES).build();

    @Operation(
            summary = "List all of the service's users.",
            description = "List all of the service's users.",
            tags = {"Users"}
    )
    @RequestMapping(value = USERS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<User>>> getUsers(
            Pageable pageable,
            HttpServletRequest request,
            @Parameter(description = "fullName of the users")
            @RequestParam(value = "givenname", required = false) String givenname,
            @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "luceneSearch parameter to filter the users.")
            @RequestParam(value = "luceneSearch", required = false) boolean luceneSearch,
            @RequestParam(value = "lastname", required = false) String lastname,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "usergroup", required = false) UserGroup usergroup
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        PaginationResult<User> paginationResult = null;
        List<User> sw360Users = new ArrayList<>();
        boolean isSearchByName = givenname != null && !givenname.isEmpty();
        boolean isSearchByLastName = lastname != null && !lastname.isEmpty();
        boolean isSearchByDepartment = CommonUtils.isNotNullEmptyOrWhitespace(department);
        boolean isUserGroup = usergroup != null && !Objects.equals(usergroup, "");
        boolean isSearchEmail = CommonUtils.isNotNullEmptyOrWhitespace(email);
        if (luceneSearch) {
            Map<String, Set<String>> filterMap = new HashMap<>();
            if (CommonUtils.isNotNullEmptyOrWhitespace(givenname)) {
                Set<String> values = CommonUtils.splitToSet(givenname);
                values = values.stream().map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
                filterMap.put(User._Fields.GIVENNAME.getFieldName(), values);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(email)) {
                Set<String> values = CommonUtils.splitToSet(email);
                values = values.stream().map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
                filterMap.put(User._Fields.EMAIL.getFieldName(), values);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(department)) {
                Set<String> values = CommonUtils.splitToSet(department);
                filterMap.put(User._Fields.DEPARTMENT.getFieldName(), values);
            }
            if (isUserGroup) {
                Set<String> values = CommonUtils.splitToSet(usergroup.toString());
                filterMap.put(User._Fields.USER_GROUP.getFieldName(), values);
            }
           if (CommonUtils.isNotNullEmptyOrWhitespace(lastname)) {
             Set<String> values = CommonUtils.splitToSet(lastname);
              values = values.stream().map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
                filterMap.put(User._Fields.LASTNAME.getFieldName(), values);
            }
            List<User> userByGivenName = userService.refineSearch(filterMap);
            paginationResult = restControllerHelper.createPaginationResult(request, pageable, userByGivenName,
                    SW360Constants.TYPE_USER);
        } else {
            if (isSearchByName) {
                sw360Users.addAll(userService.searchUserByName(givenname));
            } else if (isSearchByLastName) {
                sw360Users.addAll(userService.searchUserByLastName(lastname));
            } else if (isSearchByDepartment) {
                sw360Users.addAll(userService.searchUserByDepartment(department));
            } else if (isUserGroup) {
                sw360Users.addAll(userService.searchUserByUserGroup(usergroup));
            } else {
                sw360Users = userService.getAllUsers();
            }
            paginationResult = restControllerHelper.createPaginationResult(request, pageable, sw360Users,
                    SW360Constants.TYPE_USER);
        }
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
    @Operation(
            summary = "Get a single user.",
            description = "Get a single user by email.",
            tags = {"Users"}
    )
    @RequestMapping(value = USERS_URL + "/{email:.+}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<User>> getUserByEmail(
            @Parameter(description = "The email of the user to be retrieved.")
            @PathVariable("email") String email
    ) {
        String decodedEmail;
        try {
            decodedEmail = URLDecoder.decode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        User sw360User = userService.getUserByEmail(decodedEmail);
        HalResource<User> halResource = createHalUser(sw360User);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    // unusual URL mapping for compatibility with older version of the REST API (see getUserByEmail())
    @Operation(
            summary = "Get a single user.",
            description = "Get a single user by id.",
            tags = {"Users"}
    )
    @RequestMapping(value = USERS_URL + "/byid/{id:.+}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<User>> getUser(
            @Parameter(description = "The id of the user to be retrieved.")
            @PathVariable("id") String id
    ) {
        User sw360User = userService.getUser(id);
        HalResource<User> halResource = createHalUser(sw360User);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Create a new user.",
            description = "Create a user (not in Liferay).",
            tags = {"Users"}
    )
    @PostMapping(value = USERS_URL)
    public ResponseEntity<EntityModel<User>> createUser(
            @Parameter(description = "The user to be created.")
            @RequestBody User user
    ) throws TException {
        if(CommonUtils.isNullEmptyOrWhitespace(user.getPassword())) {
            throw new HttpMessageNotReadableException("Password can not be null or empty or whitespace!");
        }
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        User createdUser = userService.addUser(user);
        HalResource<User> halResource = createHalUser(createdUser);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdUser.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @Operation(
            summary = "Get current user's profile.",
            description = "Get current user's profile.",
            tags = {"Users"}
    )
    @GetMapping(value = USERS_URL + "/profile")
    public ResponseEntity<HalResource<User>> getUserProfile() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        HalResource<User> halUserResource = new HalResource<>(sw360User);
        return ResponseEntity.ok(halUserResource);
    }

    @Operation(
            summary = "Update user's profile.",
            description = "Update user's profile.",
            tags = {"Users"}
    )
    @PatchMapping(value = USERS_URL + "/profile")
    public ResponseEntity<HalResource<User>> updateUserProfile(
            @Parameter(description = "Updated user profile",
                    schema = @Schema(example = """
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
        sw360User = restControllerHelper.updateUserProfile(sw360User, userProfile, setOfUserProfileFields);
        userService.updateUser(sw360User);
        HalResource<User> halUserResource = new HalResource<>(sw360User);
        return ResponseEntity.ok(halUserResource);
    }

    @Operation(
            summary = "List all of rest api tokens.",
            description = "List all of rest api tokens of current user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of tokens.")
            },
            tags = {"Users"}
    )
    @RequestMapping(value = USERS_URL + "/tokens", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<RestApiToken>>> getUserRestApiTokens() {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<RestApiToken> restApiTokens = sw360User.getRestApiTokens();

        if (restApiTokens == null) {
            return new ResponseEntity<>(CollectionModel.of(Collections.emptyList()), HttpStatus.OK);
        }

        List<EntityModel<RestApiToken>> restApiResources = restApiTokens.stream()
                .map(EntityModel::of)
                .collect(Collectors.toList());
        return new ResponseEntity<>(CollectionModel.of(restApiResources), HttpStatus.OK);
    }

    @Operation(
            summary = "Create rest api token.",
            description = "Create rest api token for current user.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Create token successfully."),
                    @ApiResponse(responseCode = "500", description = "Create token failure.")
            },
            tags = {"Users"}
    )
    @RequestMapping(value = USERS_URL + "/tokens", method = RequestMethod.POST)
    public ResponseEntity<String> createUserRestApiToken(
            @Parameter(description = "Token request", schema = @Schema(implementation = RestApiToken.class))
            @RequestBody Map<String, Object> requestBody
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RestApiToken restApiToken = userService.convertToRestApiToken(requestBody, sw360User);
        String token = RandomStringUtils.random(20, true, true);
        restApiToken.setToken(BCrypt.hashpw(token, SW360Constants.REST_API_TOKEN_HASH_SALT));
        sw360User.addToRestApiTokens(restApiToken);
        userService.updateUser(sw360User);

        return new ResponseEntity<>(token, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Delete rest api token.",
            description = "Delete rest api token by name for current user.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Revoke token successfully."),
                    @ApiResponse(responseCode = "404", description = "Token name not found.")
            },
            tags = {"Users"}
    )
    @RequestMapping(value = USERS_URL + "/tokens", method = RequestMethod.DELETE)
    public ResponseEntity<String> revokeUserRestApiToken(
            @Parameter(description = "Name of token to be revoked.", example = "MyToken")
            @RequestParam("name") String tokenName
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        if (!userService.isTokenNameExisted(sw360User, tokenName)) {
            return new ResponseEntity<>("Token not found: " + StringEscapeUtils.escapeHtml(tokenName), HttpStatus.NOT_FOUND);
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

    @Operation(
            summary = "Fetch group list of a user.",
            description = "Fetch the list of group for a particular user.",
            tags = {"Users"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User and its groups.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                    {
                                        "primaryGrpList": ["DEPARTMENT"],
                                        "secondaryGrpList": ["DEPARTMENT1","DEPARTMENT2"]
                                    }
                                    """
                                    ))
                    }
            )
    })
    @RequestMapping(value = USERS_URL + "/groupList", method = RequestMethod.GET)
    public ResponseEntity<Map<String, List<String>>> getGroupList() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<String> primaryGrpList = new ArrayList<>();
        List<String> secondaryGrpList = new ArrayList<>();
        Map<String, List<String>> userGroupMap = new HashMap<>();
        String mainDepartment = sw360User.getDepartment();
        if (mainDepartment != null && !mainDepartment.isEmpty()) {
            primaryGrpList.add(mainDepartment);
        }
        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = sw360User.getSecondaryDepartmentsAndRoles();
        if (secondaryDepartmentsAndRoles != null) {
            secondaryGrpList.addAll(secondaryDepartmentsAndRoles.keySet());
        }
        userGroupMap.put("primaryGrpList", primaryGrpList);
        userGroupMap.put("secondaryGrpList", secondaryGrpList);
        return new ResponseEntity<>(userGroupMap, HttpStatus.OK);
    }
}
