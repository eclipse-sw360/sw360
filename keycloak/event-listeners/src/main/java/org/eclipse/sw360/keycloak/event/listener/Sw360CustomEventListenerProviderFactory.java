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
        
        // Read CouchDB configuration from Keycloak SPI config
        String couchdbUrl = config.get("couchdbUrl");
        if (couchdbUrl != null && !couchdbUrl.isEmpty()) {
            logger.info("In SPI " + SW360_ADD_USER_TO_COUCHDB + ", setting CouchDB URL to: '" + couchdbUrl + "'");
            Sw360UserService.couchdbUrl = couchdbUrl;
        } else {
            logger.info("No 'couchdbUrl' found in config, using default: '" + Sw360UserService.couchdbUrl + "'");
        }
        
        String couchdbUsername = config.get("couchdbUsername");
        if (couchdbUsername != null && !couchdbUsername.isEmpty()) {
            logger.info("In SPI " + SW360_ADD_USER_TO_COUCHDB + ", setting CouchDB username to: '" + couchdbUsername + "'");
            Sw360UserService.couchdbUsername = couchdbUsername;
        } else {
            logger.info("No 'couchdbUsername' found in config, using default: '" + Sw360UserService.couchdbUsername + "'");
        }
        
        String couchdbPassword = config.get("couchdbPassword");
        if (couchdbPassword != null && !couchdbPassword.isEmpty()) {
            logger.info("In SPI " + SW360_ADD_USER_TO_COUCHDB + ", setting CouchDB password");
            Sw360UserService.couchdbPassword = couchdbPassword;
        } else {
            logger.info("No 'couchdbPassword' found in config, using default");
        }
        
        String couchdbDatabase = config.get("couchdbDatabase");
        if (couchdbDatabase != null && !couchdbDatabase.isEmpty()) {
            logger.info("In SPI " + SW360_ADD_USER_TO_COUCHDB + ", setting CouchDB database to: '" + couchdbDatabase + "'");
            Sw360UserService.couchdbDatabase = couchdbDatabase;
        } else {
            logger.info("No 'couchdbDatabase' found in config, using default: '" + Sw360UserService.couchdbDatabase + "'");
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
