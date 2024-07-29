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

import com.cloudant.client.internal.DatabaseURIHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.cloudant.common.SdkCommon;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.SearchAnalyzeResult;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

/**
 * CouchDB connector which allows us to run the Nouveau queries.
 */
public class LuceneAwareCouchDbConnector {

    public static String DEFAULT_NOUVEAU_PREFIX = "_nouveau";
    public static String DEFAULT_DESIGN_PREFIX = "_design";

    private final String lucenePrefix;
    private final String ddoc;
    private final NouveauAwareDatabase database;

    public LuceneAwareCouchDbConnector(Cloudant db, String ddoc) throws IOException {
        this.lucenePrefix = DEFAULT_NOUVEAU_PREFIX;
        this.ddoc = ddoc;
        this.database = new NouveauAwareDatabase(db, this.ddoc, this.lucenePrefix);
    }

    public static class NouveauAwareDatabase {
        private final String ddoc;
        private final Cloudant client;
        private final String lucenePrefix;

        protected NouveauAwareDatabase(@NotNull Cloudant dbClient,
                                       String ddoc, String lucenePrefix) {
            this.client = dbClient;
            this.ddoc = ddoc;
            this.lucenePrefix = lucenePrefix;
        }

        public <T> T queryNouveau(String index, @NotNull NouveauQuery query, Class<T> classOfT) {
//            RequestBuilder builder = RequestBuilder.post(
//                    RequestBuilder.resolveRequestUrl(
//                            this.client.getServiceUrl(),
//                            "/" + ensureDesignId(this.ddoc) + "/" + lucenePrefix + "/" + index
//                    ));
//            Map<String, String> sdkHeaders = SdkCommon.getSdkHeaders(
//                    "cloudant", "v1", "postNouveauQuery");
//            Iterator<Map.Entry<String, String>> var4 = sdkHeaders.entrySet().iterator();
//
//            while (var4.hasNext()) {
//                Map.Entry<String, String> header = var4.next();
//                builder.header(new Object[]{header.getKey(), header.getValue()});
//            }
//
//            builder.header(new Object[]{"Accept", "application/json"});
//            JsonObject contentJson = this.getGson().fromJson(query.buildQuery(this.getGson()), JsonObject.class);
//            builder.bodyJson(contentJson);
//            ResponseConverter<SearchAnalyzeResult> responseConverter = ResponseConverterUtils.getValue((new TypeToken<SearchAnalyzeResult>() {
//            }).getType());
//            this.client.createServiceCall(builder.build(), responseConverter);


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

//        private Gson getGson() {
//            GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
//            for (Class<?> c : ThriftUtils.THRIFT_CLASSES) {
//                gsonBuilder.registerTypeAdapter(c, new CustomThriftDeserializer());
//                gsonBuilder.registerTypeAdapter(c, new CustomThriftSerializer());
//            }
//            for (Class<?> c : ThriftUtils.THRIFT_NESTED_CLASSES) {
//                gsonBuilder.registerTypeAdapter(c, new CustomThriftSerializer());
//            }
//            return gsonBuilder.create();
//        }
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
