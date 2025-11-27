/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.persistence;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This repository can perform CRUD operations on a CouchDB for
 * {@link OAuthClientEntity}s. The necessary configuration for the CouchDB
 * connection has to be available to Spring's {@link Value} infrastructure.
 */
@Component
public class OAuthClientRepository extends DatabaseRepositoryCloudantClient<OAuthClientEntity> {

    private static final String ALL =
            "function(doc) {" +
                    "  emit(null, doc._id); " +
                    "}";

    private static final String BY_IDs_VIEW =
            "function(doc) {" +
                    "  emit(doc._id, null); " +
                    "}";

    private static final String BY_CLIENT_ID_VIEW =
            "function(doc) {" +
                    "  emit(doc.client_id, null); " +
                    "}";

    @Autowired
    public OAuthClientRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_OAUTHCLIENTS") DatabaseConnectorCloudant db
    ) {
        super(db, OAuthClientEntity.class);

        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byids", createMapReduce(BY_IDs_VIEW, null));
        views.put("byClientId", createMapReduce(BY_CLIENT_ID_VIEW, null));
        initStandardDesignDocument(views, db);
    }

    public OAuthClientEntity getByClientId(String clientId) {
        final Set<String> idList = queryForIds("byClientId", clientId);

        List<OAuthClientEntity> clients = Lists.newArrayList();
        if (idList != null) {
            for (String id : idList) {
                clients.add(super.get(id));
            }
        }

        if (clients.isEmpty()) {
            log.warn("No clients found for clientId <{}>.", clientId);
            return null;
        } else if (clients.size() > 1) {
            log.warn("More than one client found ({}) for clientId <{}>.", clients.size(), clientId);
            return clients.get(0);
        } else {
            log.debug("Client found for clientId <{}>", clientId);
            return clients.get(0);
        }
    }

}
