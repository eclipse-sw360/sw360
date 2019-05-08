/*
 * Copyright Bosch Software Innovations GmbH, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.security.keycloak;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.JwtAccessTokenConverterConfigurer;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Profile("!SECURITY_MOCK")
@Component
public class KeycloakAccessTokenConverter extends DefaultAccessTokenConverter implements JwtAccessTokenConverterConfigurer {

    private static final Logger log = Logger.getLogger(KeycloakAccessTokenConverter.class);

    private static final String JWT_RESOURCE_ACCESS = "resource_access";
    private static final String JWT_ROLES = "roles";
    private static final String JWT_AUTHORITIES = "authorities";

    @Autowired
    private ResourceServerProperties resourceServerProperties;

    @Override
    public void configure(JwtAccessTokenConverter converter) {
        converter.setAccessTokenConverter(this);
        log.info("Configured  KeycloakAccessTokenConverter");
    }

    /***
     * Expects a token which has the keycloak format.
     * The token contains a resource_access claim which is a list of resources and the granted roles on this resources.
     * The expectation is that the token has resource client roles (WRITE/READ) of the sw360-REST-API client.
     * INFO: the SecurityContextHolder.getContext().getAuthentication().getPrincipal(); will return user_name of the jwt
     * @param tokenMap the raw jwt token
     * @return the processed OAuth2Authentication
     */
    @Override
    @SuppressWarnings("unchecked")
    public OAuth2Authentication extractAuthentication(Map<String, ?> tokenMap) {
        log.debug("extract authentication: tokenMap = " + tokenMap.toString());
        Map<String, Object> jwtToken = (Map<String, Object>) tokenMap;

        // Map the roles of resource_access.sw360-REST-API.roles.* into authorities.*
        if (tokenMap.containsKey(JWT_RESOURCE_ACCESS)) {
            Map<String, Object> resourceAccess = (Map<String, Object>) jwtToken.get(JWT_RESOURCE_ACCESS);
            if (resourceAccess.containsKey(resourceServerProperties.getResourceId())) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(resourceServerProperties.getResourceId());
                if (clientAccess.containsKey(JWT_ROLES)) {
                    ArrayList<String> clientRoles = (ArrayList<String>) clientAccess.get(JWT_ROLES);
                    jwtToken.put(JWT_AUTHORITIES, Collections.unmodifiableList(clientRoles));
                }
            }
        }


        //TODO: Right now only the userId is present in the session. But it may not exists in the database.
        // implement user creation/mapping here. So new users in keycloak can directly query the restapi without liferay
        // The idea is to create the users on the fly if they do no exist.
        // based on the keycloak token which contains, name, surename and deparment already
        // !!! -> This needs to be done with care.
        // 1. Consider caching to avoid a lot of calls for the user object
        // 2. Consider caching only for as long the token is the same to ensure valid user info
        //User user = new User();
        //user.setId("test2");
        //authentication.setDetails(user);

        OAuth2Authentication authentication = super.extractAuthentication(jwtToken);
        return authentication;
    }


}
