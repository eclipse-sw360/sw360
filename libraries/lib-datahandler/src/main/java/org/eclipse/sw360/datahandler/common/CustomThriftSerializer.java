/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import java.lang.reflect.Type;

import org.apache.thrift.TBase;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CustomThriftSerializer implements JsonSerializer<TBase> {

    @Override
    public JsonElement serialize(TBase src, Type typeOfSrc, JsonSerializationContext context) {
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(src, typeOfSrc);
        JsonObject jObject = json.getAsJsonObject();
        JsonElement str = jObject.get("__isset_bitfield");
        JsonElement idElement = jObject.get("id");
        JsonElement revElement = jObject.get("revision");
        String id = null, rev = null;
        if (idElement != null) {
            id = idElement.getAsString();
            jObject.addProperty("_id", id);
            jObject.remove("id");
        }
        if (revElement != null) {
            rev = revElement.getAsString();
            jObject.addProperty("_rev", rev);
            jObject.remove("revision");
        }
        if (str != null) {
            jObject.addProperty("issetBitfield", str.getAsString());
            jObject.remove("__isset_bitfield");
        }
        return jObject.getAsJsonObject();
    }
}
