/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.datasource.json;

import org.eclipse.sw360.cvesearch.datasource.CveSearchData;
import com.google.gson.*;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.util.function.Function;

public abstract class CveSearchJsonParser<T> implements Function<BufferedReader, T> {
    private Gson gson;

    public CveSearchJsonParser() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(CveSearchData.DateTimeObject.class, new DateTimeObjectDeserializer());
        gsonBuilder.registerTypeAdapter(CveSearchData.VulnerableConfigurationEntry.class, new VulnerableConfigurationEntryDeserializer());
        gson = gsonBuilder.create();
    }

    private class VulnerableConfigurationEntryDeserializer implements JsonDeserializer<CveSearchData.VulnerableConfigurationEntry> {
        @Override
        public CveSearchData.VulnerableConfigurationEntry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()){
                final String id = jsonElement.getAsString();
                return new CveSearchData.VulnerableConfigurationEntry(id);
            }else{
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                final String title = jsonObject.get("title").getAsString();
                final String id = jsonObject.get("id").getAsString();
                return new CveSearchData.VulnerableConfigurationEntry(title, id);
            }
        }
    }

    private class DateTimeObjectDeserializer implements JsonDeserializer<CveSearchData.DateTimeObject> {
        @Override
        public CveSearchData.DateTimeObject deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()){
                final String formattedDate = jsonElement.getAsString();
                return new CveSearchData.DateTimeObject(formattedDate);
            }else{
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.has("$date")){
                    return new CveSearchData.DateTimeObject(jsonObject.get("$date").getAsLong());
                }
                return null;
            }
        }
    }

    public abstract Type getType();

    @Override
    public T apply(BufferedReader json) {
        return gson.fromJson(json,getType());
    }
}
