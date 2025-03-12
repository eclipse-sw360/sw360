/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.eclipse.sw360.rest.authserver.security.key.KeyManager;

import com.google.common.collect.Sets;

/**
 * This REST controller can be accessed by users having the authority
 * {@link Sw360GrantedAuthority#ADMIN}. Such users can perform CRUD operations
 * on the configured {@link OAuthClientResource}s via these REST endpoints.
 */
@RestController
@RequestMapping(path = "/" + OAuthClientController.ENDPOINT_URL)
@PreAuthorize("hasAuthority('ADMIN')")
public class OAuthClientController {

    public static final String ENDPOINT_URL = "client-management";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${security.oauth2.resource.id}")
    private String resourceId;

    @Autowired
    private KeyManager keyManager;

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
            try {
                repo.add(clientEntity);
            } catch (SW360Exception e) {
                return new ResponseEntity<String>("Unable to add client " + clientResource.getClientId(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Ensure the entity has a non-empty id; if not, generate one.
            if (StringUtils.isEmpty(clientEntity.getId())) {
                clientEntity.setId(UUID.randomUUID().toString());
            }
            clientEntity.setClientId(clientEntity.getId());
            clientEntity.setClientSecret(passwordEncoder.encode(UUID.randomUUID().toString()));
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
        clientEntity.setAuthorities(clientResource.getAuthorities());
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
