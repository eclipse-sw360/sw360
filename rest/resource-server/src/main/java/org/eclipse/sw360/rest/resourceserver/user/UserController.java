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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController implements RepresentationModelProcessor<RepositoryLinksResource> {

    protected final EntityLinks entityLinks;

    static final String USERS_URL = "/users";

    @NonNull
    private final Sw360UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = USERS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<User>>> getUsers() {
        List<User> sw360Users = userService.getAllUsers();

        List<EntityModel<User>> userResources = new ArrayList<>();
        for (User sw360User : sw360Users) {
            User embeddedUser = restControllerHelper.convertToEmbeddedUser(sw360User);
            EntityModel<User> userResource = EntityModel.of(embeddedUser);
            userResources.add(userResource);
        }

        CollectionModel<EntityModel<User>> resources = CollectionModel.of(userResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    // '/users/{xyz}' searches by email, as opposed to by id, as is customary,
    // for compatibility with older version of the REST API
    @RequestMapping(value = USERS_URL + "/{email:.+}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<User>> getUserByEmail(
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
    public ResponseEntity<EntityModel<User>> getUser(
            @PathVariable("id") String id) {
        User sw360User = userService.getUser(id);
        HalResource<User> halResource = createHalUser(sw360User);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @PostMapping(value = USERS_URL)
    public ResponseEntity<EntityModel<User>> createUser(@RequestBody User user) throws TException {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        User createdUser = userService.addUser(user);
        HalResource<User> halResource = createHalUser(createdUser);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdUser.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
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
