/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import com.google.common.collect.Sets;

import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This REST controller can be accessed by users having the authority
 * {@link Sw360GrantedAuthority#ADMIN}. Such users can perform CRUD operations
 * on the configured {@link OAuthClientResource}s via these REST endpoints.
 */
@Controller
@RequestMapping(path = "/" + OAuthClientController.ENDPOINT_URL)
@PreAuthorize("hasAuthority('ADMIN')")
public class OAuthClientController {

    public static final String ENDPOINT_URL = "client-management";

    @Value("${security.oauth2.resource.id}")
    private String resourceId;

    @Autowired
    private OAuthClientRepository repo;

    @RequestMapping(method = RequestMethod.GET, path = "")
    public ResponseEntity<List<OAuthClientResource>> getAllClients() {
        List<OAuthClientResource> clientResources;

        List<OAuthClientEntity> clients = repo.getAll();
        if (clients == null) {
            clients = new ArrayList<>();
        }

        clientResources = clients.stream().map(OAuthClientResource::new).collect(Collectors.toList());

        return new ResponseEntity<List<OAuthClientResource>>(clientResources, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, path = "", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> createOrUpdateClient(@RequestBody OAuthClientResource clientResource) {
        OAuthClientEntity clientEntity = null;

        if (StringUtils.isNotEmpty(clientResource.getClientId())) {
            clientEntity = repo.getByClientId(clientResource.getClientId());
            if (clientEntity == null) {
                return new ResponseEntity<String>("No client found for given clientId " + clientResource.getClientId(),
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            clientEntity = new OAuthClientEntity();

            // store entity to get a new id
            repo.add(clientEntity);

            clientEntity.setClientId(clientEntity.getId());
            clientEntity.setClientSecret(UUID.randomUUID().toString());
        }

        updateClientEntityFromResource(clientEntity, clientResource);
        repo.update(clientEntity);

        return new ResponseEntity<OAuthClientResource>(
                new OAuthClientResource(repo.getByClientId(clientEntity.getClientId())), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/{clientId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> deleteClient(@PathVariable("clientId") String clientId) {
        OAuthClientEntity clientEntity = null;

        if (StringUtils.isNotEmpty(clientId)) {
            clientEntity = repo.getByClientId(clientId);
            if (clientEntity == null) {
                return new ResponseEntity<String>("No client found for given clientId " + clientId,
                        HttpStatus.BAD_REQUEST);
            }

            repo.remove(clientEntity);
        }

        return new ResponseEntity<OAuthClientResource>(new OAuthClientResource(clientEntity), HttpStatus.OK);
    }

    private void updateClientEntityFromResource(OAuthClientEntity clientEntity, OAuthClientResource clientResource) {
        // updateable properties (clientId and clientSecret cannot be changed)
        clientEntity.setDescription(clientResource.getDescription());
        clientEntity.setAuthoritiesAsStrings(clientResource.getAuthorities());
        clientEntity.setScope(clientResource.getScope());
        clientEntity.setAccessTokenValiditySeconds(clientResource.getAccessTokenValidity());
        clientEntity.setRefreshTokenValiditySeconds(clientResource.getRefreshTokenValidity());

        // static properties
        clientEntity.setAuthorizedGrantTypes(
                Stream.of("client_credentials", "password", "refresh_token").collect(Collectors.toSet()));
        clientEntity.setAutoApproveScopes(Collections.singleton("true"));
        clientEntity.setResourceIds(Sets.newHashSet(resourceId));
        clientEntity.setRegisteredRedirectUri(null);
    }
}
