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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.cloudant.common.SdkCommon;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.ibm.cloud.cloudant.v1.model.PutDesignDocumentOptions;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import com.ibm.cloud.sdk.core.util.Validator;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * CouchDB connector which allows us to run the Nouveau queries.
 */
public class LuceneAwareCouchDbConnector {
    public static String DEFAULT_NOUVEAU_PREFIX = "_nouveau";
    public static String DEFAULT_DESIGN_PREFIX = ""; // '_design/' not needed with Cloudant SDK

    private final NouveauAwareDatabase database;

    public LuceneAwareCouchDbConnector(Cloudant dbClient, String ddoc, String db,
                                       Gson gson) {
        String lucenePrefix = DEFAULT_NOUVEAU_PREFIX;
        this.database = new NouveauAwareDatabase(dbClient, db, ddoc,
                lucenePrefix, gson);
    }

    public static class NouveauAwareDatabase extends Cloudant {
        private final String db;
        private final String ddoc;
        private final String lucenePrefix;
        private final Gson gson;

        public NouveauAwareDatabase(@NotNull Cloudant client, String db,
                                    String ddoc, String lucenePrefix, Gson gson) {
            super(client.getName(), client.getAuthenticator());
            this.setServiceUrl(client.getServiceUrl());
            this.db = db;
            this.ddoc = ddoc;
            this.lucenePrefix = lucenePrefix;
            this.gson = gson;
        }

        public ServiceCall<NouveauResult> queryNouveau(String index,
                                                       @NotNull NouveauQuery query) {
            Validator.notEmpty(index, "index cannot be empty");
            Validator.notNull(query, "query cannot be null");

            Map<String, String> pathParamsMap = new HashMap<>();
            pathParamsMap.put("db", this.db);
            pathParamsMap.put("ddoc", ensureDesignId(this.ddoc));
            pathParamsMap.put("nouveauPrefix", this.lucenePrefix);
            pathParamsMap.put("index", index);

            RequestBuilder builder = RequestBuilder.post(RequestBuilder.resolveRequestUrl(
                    this.getServiceUrl(),
                    "/{db}/_design/{ddoc}/{nouveauPrefix}/{index}",
                    pathParamsMap));
            Map<String, String> sdkHeaders = SdkCommon.getSdkHeaders("cloudant", "v1", "postNouveauQuery");

            for (Map.Entry<String, String> stringStringEntry : sdkHeaders.entrySet()) {
                builder.header(stringStringEntry.getKey(), stringStringEntry.getValue());
            }

            builder.header("Accept", "application/json");
            builder.header("Content-Type", "application/json");

            builder.bodyContent(query.buildQuery(this.gson), "application/json");
            ResponseConverter<NouveauResult> responseConverter = ResponseConverterUtils.getValue((new TypeToken<NouveauResult>() {
            }).getType());
            return this.createServiceCall(builder.build(), responseConverter);
        }

        public ServiceCall<NouveauDesignDocument> getNouveauDesignDocument(String ddoc) {
            Validator.notEmpty(ddoc, "ddoc cannot be empty");

            Map<String, String> pathParamsMap = new HashMap<>();
            pathParamsMap.put("db", this.db);
            pathParamsMap.put("ddoc", ensureDesignId(this.ddoc));

            RequestBuilder builder = RequestBuilder.get(RequestBuilder.resolveRequestUrl(
                    this.getServiceUrl(),
                    "/{db}/_design/{ddoc}",
                    pathParamsMap));
            Map<String, String> sdkHeaders = SdkCommon.getSdkHeaders("cloudant", "v1", "getNouveauDesignDocument");

            for (Map.Entry<String, String> stringStringEntry : sdkHeaders.entrySet()) {
                builder.header(stringStringEntry.getKey(), stringStringEntry.getValue());
            }

            builder.header("Accept", "application/json");
            builder.header("Content-Type", "application/json");

            builder.query("latest", String.valueOf(true));

            ResponseConverter<NouveauDesignDocument> responseConverter = ResponseConverterUtils.getValue((new TypeToken<NouveauDesignDocument>() {
            }).getType());
            return this.createServiceCall(builder.build(), responseConverter);
        }

        /**
         * Put a NouveauDesignDocument to DB.
         * First checks if the document already exists to get the ID and
         * revision update. If not, then creates a new one.
         * @param designDocument Design document to create/update
         * @return true on success
         * @throws RuntimeException If there is some error.
         */
        public boolean putNouveauDesignDocument(
                @NotNull NouveauDesignDocument designDocument
        ) throws RuntimeException {
            NouveauDesignDocument existingDoc;
            try {
                existingDoc = this.getNouveauDesignDocument(designDocument.getId())
                        .execute()
                        .getResult();
            } catch (NotFoundException e) {
                existingDoc = null;
            }
            if (existingDoc != null) {
                designDocument.setId(existingDoc.getId());
                designDocument.setRev(existingDoc.getRev());
            }
            PutDesignDocumentOptions designDocumentOptions =
                    new PutDesignDocumentOptions.Builder()
                            .db(this.db)
                            .designDocument(designDocument)
                            .ddoc(ddoc)
                            .build();

            DocumentResult response =
                    this.putDesignDocument(designDocumentOptions)
                            .execute().getResult();
            boolean success = response.isOk();
            if (!success) {
                throw new RuntimeException(
                        "Unable to put design document " + designDocument.getId()
                        + " to " + ddoc + ". Error: " + response.getError());
            } else {
                designDocument.setId(response.getId());
                designDocument.setRev(response.getRev());
            }
            return true;
        }
    }

    /**
     * Query the Nouveau index.
     * @param index The name of the index.
     * @param query The query to run.
     * @return The result of the query.
     */
    public NouveauResult queryNouveau(String index, @NotNull NouveauQuery query)
            throws ServiceResponseException {
        return this.database.queryNouveau(index, query).execute().getResult();
    }

    /**
     * Ensure that the design ID is prefixed with the default design prefix.
     * @param designId The design ID to validate.
     * @return The design ID with the default design prefix.
     */
    @Contract(pure = true)
    public static @NotNull String ensureDesignId(@NotNull String designId) {
        if (!DEFAULT_DESIGN_PREFIX.isEmpty() && !designId.startsWith(DEFAULT_DESIGN_PREFIX)) {
            return DEFAULT_DESIGN_PREFIX + designId;
        } else {
            return designId;
        }
    }

    /**
     * Get a NouveauDesignDocument from DB.
     * @param ddoc ddoc to get
     * @return Design document if found. Null otherwise.
     */
    protected NouveauDesignDocument getNouveauDesignDocument(@NotNull String ddoc) {
        if (ddoc.isEmpty()) {
            throw new IllegalArgumentException("ddoc cannot be empty");
        }
        try {
            return this.database.getNouveauDesignDocument(ddoc)
                    .execute()
                    .getResult();
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Create/update Nouveau design document.
     * @param designDocument Design document to create/update.
     * @return True on success.
     * @throws RuntimeException If something goes wrong.
     */
    public boolean putNouveauDesignDocument(
            @NotNull NouveauDesignDocument designDocument
    ) throws RuntimeException {
        return this.database.putNouveauDesignDocument(designDocument);
    }
}
