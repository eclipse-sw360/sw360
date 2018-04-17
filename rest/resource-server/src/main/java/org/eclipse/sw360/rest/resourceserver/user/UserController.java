/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.ArrayList;
import java.util.Base64;
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

    @RequestMapping(value = USERS_URL + "/{id:.+}", method = RequestMethod.GET)
    public ResponseEntity<Resource<User>> getUser(
            @PathVariable("id") String id) {
        byte[] base64decodedBytes = Base64.getDecoder().decode(id);
        String decodedId;
        try {
            decodedId = new String(base64decodedBytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw (new RuntimeException(e));
        }

        User sw360User = userService.getUserByEmail(decodedId);
        HalResource<User> halResource = createHalUser(sw360User);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(UserController.class).slash("api/users").withRel("users"));
        return resource;
    }

    private HalResource<User> createHalUser(User sw360User) {
        HalResource<User> halResource = new HalResource<>(sw360User);
        return halResource;
    }
}
