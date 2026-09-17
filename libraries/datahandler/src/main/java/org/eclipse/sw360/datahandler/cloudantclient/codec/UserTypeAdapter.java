/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient.codec;

import java.io.IOException;

import org.eclipse.sw360.datahandler.services.users.User;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Dual-read adapter for service-api {@link User}:
 * <ul>
 *   <li>thrift-shaped CouchDB JSON (with {@code issetBitfield} metadata)</li>
 *   <li>clean POJO JSON</li>
 * </ul>
 * Always writes the clean POJO shape (lazy migrate on save).
 */
public final class UserTypeAdapter extends TypeAdapter<User> {

    private static final Gson PLAIN_GSON = new Gson();

    @Override
    public void write(JsonWriter out, User value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        PLAIN_GSON.toJson(value, User.class, out);
    }

    @Override
    public User read(JsonReader in) throws IOException {
        JsonElement element = JsonParser.parseReader(in);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        obj.remove("issetBitfield");
        obj.remove("__isset_bitfield");
        return PLAIN_GSON.fromJson(obj, User.class);
    }
}
