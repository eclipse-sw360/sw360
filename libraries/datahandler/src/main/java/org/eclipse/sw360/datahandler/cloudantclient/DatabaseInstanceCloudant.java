/*
 * Copyright Siemens AG, 2021, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DeleteDatabaseOptions;
import com.ibm.cloud.cloudant.v1.model.PutDatabaseOptions;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.eclipse.sw360.datahandler.common.CustomThriftDeserializer;
import org.eclipse.sw360.datahandler.common.CustomThriftSerializer;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * Class for connecting to a given CouchDB instance
 */
public class DatabaseInstanceCloudant {

    public DatabaseInstanceCloudant() {
        DatabaseInstanceTrackerCloudant.track(this);
    }

    Cloudant client = null;

    private static Gson gson;

    public DatabaseInstanceCloudant(Cloudant client) {
        this.client = client;
    }

    public void createDB(String dbName) {
        if (!checkIfDbExists(dbName)) {
            PutDatabaseOptions putDbOptions = new PutDatabaseOptions.Builder().db(dbName).build();
            try {
                client.putDatabase(putDbOptions).execute().getResult();
            } catch (ServiceResponseException e) {
                if (e.getStatusCode() != 412) {
                    throw e;
                }
            }
        }
    }

    public boolean checkIfDbExists(String dbName) {
        return client.getAllDbs().execute().getResult().contains(dbName);
    }

    public void destroy() {
        if (client == null) {
            return;
        }
        // As per https://javadoc.io/static/com.squareup.okhttp3/okhttp/3.9.1/okhttp3/OkHttpClient.html
        OkHttpClient okhttpClient = client.getClient();
        try (ExecutorService executor = okhttpClient.dispatcher().executorService()) {
            executor.shutdown();
        }
        okhttpClient.connectionPool().evictAll();
        try (Cache cache = okhttpClient.cache()) {
            if (cache != null) {
                cache.close();
            }
        } catch (IOException ignored) {
            // Nothing to do, safe to exit
        }
        client = null;
    }

    public void deleteDatabase(String dbName) {
        DeleteDatabaseOptions deleteDbOptions = new DeleteDatabaseOptions.Builder().db(dbName).build();
        try {
            client.deleteDatabase(deleteDbOptions).execute().getResult();
        } catch (ServiceResponseException e) {
            if (e.getStatusCode() != 404) {
                throw e;
            }
        }
    }

    public Cloudant getClient() {
        return client;
    }

    public Gson getGson() {
        if (gson == null) {
            GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            for (Class<?> c : ThriftUtils.THRIFT_CLASSES) {
                gsonBuilder.registerTypeAdapter(c, new CustomThriftDeserializer());
                gsonBuilder.registerTypeAdapter(c, new CustomThriftSerializer());
            }
            for (Class<?> c : ThriftUtils.THRIFT_NESTED_CLASSES) {
                gsonBuilder.registerTypeAdapter(c, new CustomThriftSerializer());
            }
            gson = gsonBuilder.create();
        }
        return gson;
    }
}
