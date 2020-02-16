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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController implements ResourceProcessor<RepositoryLinksResource> {

    protected final EntityLinks entityLinks;

    static final String USERS_URL = "/users";

    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = USERS_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource<User>>> getUsers() {
        List<User> sw360Users = userService.getAllUsers();

        List<Resource<User>> userResources = new ArrayList<>();
        for (User sw360User : sw360Users) {
            User embeddedUser = restControllerHelper.convertToEmbeddedUser(sw360User);
            Resource<User> userResource = new Resource<>(embeddedUser);
            userResources.add(userResource);
        }

        Resources<Resource<User>> resources = new Resources<>(userResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    // '/users/{xyz}' searches by email, as opposed to by id, as is customary,
    // for compatibility with older version of the REST API
    @RequestMapping(value = USERS_URL + "/{email:.+}", method = RequestMethod.GET)
    public ResponseEntity<Resource<User>> getUserByEmail(
            @PathVariable("email") String email) {

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
    @RequestMapping(value = USERS_URL + "/byid/{id:.+}", method = RequestMethod.GET)
    public ResponseEntity<Resource<User>> getUser(
            @PathVariable("id") String id) {
        User sw360User = userService.getUser(id);
        HalResource<User> halResource = createHalUser(sw360User);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(UserController.class).slash("api/users").withRel("users"));
        return resource;
    }

    private HalResource<User> createHalUser(User sw360User) {
        return new HalResource<>(sw360User);
    }
}
