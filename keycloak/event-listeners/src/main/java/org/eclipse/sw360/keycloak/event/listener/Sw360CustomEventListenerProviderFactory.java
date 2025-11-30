/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.listener;

import org.eclipse.sw360.keycloak.event.listener.service.Sw360UserService;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory class for creating instances of Sw360CustomEventListenerProvider.
 *
 * @author smruti.sahoo@siemens.com
 */
public class Sw360CustomEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger logger = Logger.getLogger(Sw360CustomEventListenerProviderFactory.class);

    public static final String SW360_ADD_USER_TO_COUCHDB = "sw360-add-user-to-couchdb";

    public EventListenerProvider create(KeycloakSession session) {
        logger.info("Creating Sw360CustomEventListenerProvider");
        return new Sw360CustomEventListenerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        logger.info("Initializing Sw360CustomEventListenerProviderFactory with config: " + config);
        if (config.get("thrift") != null && !config.get("thrift").isEmpty()) {
            logger.infof("In SPI %s, setting thrift server URL to: '%s'",
                    SW360_ADD_USER_TO_COUCHDB, config.get("thrift"));
            Sw360UserService.thriftServerUrl = config.get("thrift");
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("Post-initializing Sw360CustomEventListenerProviderFactory with factory: " + factory);
    }

    @Override
    public void close() {
        logger.info("Closing Sw360CustomEventListenerProviderFactory");
    }

    @Override
    public String getId() {
        logger.infof("ID of Sw360CustomEventListenerProviderFactory: %s", SW360_ADD_USER_TO_COUCHDB);
        return SW360_ADD_USER_TO_COUCHDB;
    }
}
