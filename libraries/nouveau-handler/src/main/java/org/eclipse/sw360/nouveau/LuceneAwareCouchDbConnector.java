/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.nouveau;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.internal.CouchDbUtil;
import com.cloudant.http.HttpConnection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

/**
 * CouchDB connector which allows us to run the Nouveau queries.
 */
public class LuceneAwareCouchDbConnector {

    public static String DEFAULT_NOUVEAU_PREFIX = "_nouveau";
    public static String DEFAULT_DESIGN_PREFIX = "_design";

    private final String lucenePrefix;
    private final String ddoc;
    private final NouveauAwareDatabase database;

    public LuceneAwareCouchDbConnector(Database db, Supplier<CloudantClient> dbClient, String ddoc) throws IOException {
        this.lucenePrefix = DEFAULT_NOUVEAU_PREFIX;
        this.ddoc = ddoc;
        this.database = new NouveauAwareDatabase(db, dbClient, this.ddoc, this.lucenePrefix);
    }

    public static class NouveauAwareDatabase extends Database {
        private final String ddoc;
        private final CloudantClient client;
        private final String lucenePrefix;

        protected NouveauAwareDatabase(Database db, @NotNull Supplier<CloudantClient> dbClient,
                                       String ddoc, String lucenePrefix) {
            super(db);
            this.client = dbClient.get();
            this.ddoc = ddoc;
            this.lucenePrefix = lucenePrefix;
        }

        public <T> T queryNouveau(String index, @NotNull NouveauQuery query, Class<T> classOfT) {
            URI uri = (new DatabaseURIHelper(this.getDBUri()))
                    .path(ensureDesignId(this.ddoc))
                    .path(lucenePrefix).path(index).build();

            InputStream response = null;

            T queryResponse;
            try {
                response = this.client.executeRequest(CouchDbUtil.createPost(uri, query.buildQuery(this.client.getGson()), "application/json")).responseAsInputStream();
                queryResponse = CouchDbUtil.getResponse(response, classOfT, this.client.getGson());
            } catch (IOException | CouchDbException e) {
                throw new RuntimeException(e);
            } finally {
                CouchDbUtil.close(response);
            }

            return queryResponse;
        }
    }

    /**
     * Query the Nouveau index.
     * @param index The name of the index.
     * @param query The query to run.
     * @return The result of the query.
     */
    public NouveauResult queryNouveau(String index, @NotNull NouveauQuery query) {
        return this.database.queryNouveau(index, query, NouveauResult.class);
    }

    /**
     * Ensure that the design ID is prefixed with the default design prefix.
     * @param designId The design ID to validate.
     * @return The design ID with the default design prefix.
     */
    @Contract(pure = true)
    public static @NotNull String ensureDesignId(@NotNull String designId) {
        if (designId.startsWith(DEFAULT_DESIGN_PREFIX)) {
            return designId;
        } else {
            return DEFAULT_DESIGN_PREFIX + "/" + designId;
        }
    }
}
