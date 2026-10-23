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

import org.eclipse.sw360.datahandler.services.attachments.LicenseInfoUsage;
import org.eclipse.sw360.datahandler.services.attachments.ManuallySetUsage;
import org.eclipse.sw360.datahandler.services.attachments.SourcePackageUsage;
import org.eclipse.sw360.datahandler.services.attachments.UsageData;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Dual-read adapter for service-api {@link UsageData}:
 * <ul>
 *   <li>thrift union JSON: {@code {"setField_":"LICENSE_INFO","value_":{...}}}</li>
 *   <li>clean POJO JSON: {@code {"licenseInfo":{...}}}</li>
 * </ul>
 * Always writes the clean POJO shape (lazy migrate on save).
 */
public final class UsageDataTypeAdapter extends TypeAdapter<UsageData> {

    private static final Gson GSON = new Gson();

    @Override
    public void write(JsonWriter out, UsageData value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        if (value.getLicenseInfo() != null) {
            out.name("licenseInfo");
            GSON.toJson(value.getLicenseInfo(), LicenseInfoUsage.class, out);
        } else if (value.getSourcePackage() != null) {
            out.name("sourcePackage");
            GSON.toJson(value.getSourcePackage(), SourcePackageUsage.class, out);
        } else if (value.getManuallySet() != null) {
            out.name("manuallySet");
            GSON.toJson(value.getManuallySet(), ManuallySetUsage.class, out);
        }
        out.endObject();
    }

    @Override
    public UsageData read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        UsageData usageData = new UsageData();
        String setField = null;
        JsonElement thriftValue = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                continue;
            }
            switch (name) {
                case "setField_" -> setField = in.nextString();
                case "value_" -> thriftValue = JsonParser.parseReader(in);
                case "licenseInfo" -> usageData.setLicenseInfo(GSON.fromJson(in, LicenseInfoUsage.class));
                case "sourcePackage" -> usageData.setSourcePackage(GSON.fromJson(in, SourcePackageUsage.class));
                case "manuallySet" -> usageData.setManuallySet(GSON.fromJson(in, ManuallySetUsage.class));
                default -> in.skipValue();
            }
        }
        in.endObject();

        if (setField != null) {
            applyThriftUnion(usageData, setField, thriftValue);
        }
        return usageData;
    }

    private static void applyThriftUnion(UsageData usageData, String setField, JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return;
        }
        switch (setField) {
            case "LICENSE_INFO" -> usageData.setLicenseInfo(GSON.fromJson(value, LicenseInfoUsage.class));
            case "SOURCE_PACKAGE" -> usageData.setSourcePackage(GSON.fromJson(value, SourcePackageUsage.class));
            case "MANUALLY_SET" -> usageData.setManuallySet(GSON.fromJson(value, ManuallySetUsage.class));
            default -> throw new IllegalArgumentException(
                    "Unknown UsageData thrift union arm '" + setField + "' — refusing to drop data");
        }
    }
}
