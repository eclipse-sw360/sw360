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

import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import java.net.MalformedURLException;
import java.util.List;

/**
 * This repository can perform CRUD operations on a CouchDB for
 * {@link OAuthClientEntity}s. The necessary configuration for the CouchDB
 * connection has to be available to Spring's {@link Value} infrastructure.
 */
@Component
@Views({
        @View(name = "all", map = "function(doc) { emit(null, doc._id); }"),
        @View(name = "byId", map = "function(doc) { emit(doc._id, doc); }"),
        @View(name = "byClientId", map = "function(doc) { emit(doc.client_id, doc); }")
})
public class OAuthClientRepository extends CouchDbRepositorySupport<OAuthClientEntity> {

    protected OAuthClientRepository(
            @Value("${couchdb.url}") final String dbUrl,
            @Value("${couchdb.database}") final String dbName,
            @Value("${couchdb.username:#{null}}") final String dbUsername,
            @Value("${couchdb.password:#{null}}") final String dbPassword) throws MalformedURLException {

        super(OAuthClientEntity.class, new StdCouchDbConnector(dbName,
                        new StdCouchDbInstance(
                                new StdHttpClient.Builder()
                                        .url(dbUrl)
                                        .username(dbUsername)
                                        .password(dbPassword)
                                        .build()
                                )
                        )
                );

        initStandardDesignDocument();
    }

    public OAuthClientEntity getByClientId(String clientId) {
        ViewQuery query = createQuery("byClientId");
        query.setIgnoreNotFound(true);
        query.key(clientId);

        List<OAuthClientEntity> clients = Lists.newArrayList();
        ViewResult result = db.queryView(query);
        for (ViewResult.Row row : result.getRows()) {
            String id = row.getId();
            clients.add(super.get(id));
        }

        if (clients.size() < 1) {
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
