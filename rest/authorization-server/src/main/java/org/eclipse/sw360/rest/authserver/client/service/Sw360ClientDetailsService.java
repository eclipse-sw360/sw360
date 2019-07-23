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
package org.eclipse.sw360.rest.authserver.client.service;

import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

/**
 * This is our implementation of a {@link ClientDetailsService} which is able to
 * use the {@link OAuthClientRepository}.
 */
public class Sw360ClientDetailsService implements ClientDetailsService {

    @Autowired
    private OAuthClientRepository clientRepo;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        ClientDetails client = clientRepo.getByClientId(clientId);
        if (client == null) {
            throw new ClientRegistrationException("No client found for clientId " + clientId);
        }

        return client;
    }

}
