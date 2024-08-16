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
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import com.ibm.cloud.sdk.core.util.Validator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * CouchDB connector which allows us to run the Nouveau queries.
 */
public class LuceneAwareCouchDbConnector {
    public static String DEFAULT_NOUVEAU_PREFIX = "_nouveau";
    public static String DEFAULT_DESIGN_PREFIX = "_design";

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
            this.db = db;
            this.ddoc = ddoc;
            this.lucenePrefix = lucenePrefix;
            this.gson = gson;
        }

        public <T> ServiceCall<T> queryNouveau(String index, @NotNull NouveauQuery query,
                                               Class<T> ignoredClassOfT) {
            Validator.notEmpty(index, "index cannot be empty");
            Validator.notNull(query, "query cannot be null");

            Map<String, String> pathParamsMap = new HashMap<>();
            pathParamsMap.put("db", this.db);
            pathParamsMap.put("ddoc", ensureDesignId(this.ddoc));
            pathParamsMap.put("nouveauPrefix", this.lucenePrefix);
            pathParamsMap.put("index", index);

            RequestBuilder builder = RequestBuilder.post(RequestBuilder.resolveRequestUrl(
                    this.getServiceUrl(),
                    "/{db}/{ddoc}/{nouveauPrefix}/{index}",
                    pathParamsMap));
            Map<String, String> sdkHeaders = SdkCommon.getSdkHeaders("cloudant", "v1", "postNouveauQuery");

            for (Map.Entry<String, String> stringStringEntry : sdkHeaders.entrySet()) {
                builder.header(stringStringEntry.getKey(), stringStringEntry.getValue());
            }

            builder.header("Accept", "application/json");
            builder.header("Content-Type", "application/json");

            builder.bodyContent(query.buildQuery(this.gson), "application/json");
            ResponseConverter<T> responseConverter = ResponseConverterUtils.getValue((new TypeToken<T>() {
            }).getType());
            return this.createServiceCall(builder.build(), responseConverter);
        }
    }

    /**
     * Query the Nouveau index.
     * @param index The name of the index.
     * @param query The query to run.
     * @return The result of the query.
     */
    public NouveauResult queryNouveau(String index, @NotNull NouveauQuery query) {
        return this.database.queryNouveau(index, query, NouveauResult.class).execute().getResult();
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
