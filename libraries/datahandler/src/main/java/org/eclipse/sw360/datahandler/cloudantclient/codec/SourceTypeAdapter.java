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

import org.eclipse.sw360.datahandler.services.common.Source;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Dual-read adapter for service-api {@link Source}:
 * <ul>
 *   <li>thrift union JSON: {@code {"setField_":"PROJECT_ID","value_":"..."}}</li>
 *   <li>clean POJO JSON: {@code {"projectId":"..."}}</li>
 * </ul>
 * Always writes the clean POJO shape (lazy migrate on save).
 */
public final class SourceTypeAdapter extends TypeAdapter<Source> {

    @Override
    public void write(JsonWriter out, Source value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        if (value.getProjectId() != null) {
            out.name("projectId").value(value.getProjectId());
        } else if (value.getComponentId() != null) {
            out.name("componentId").value(value.getComponentId());
        } else if (value.getReleaseId() != null) {
            out.name("releaseId").value(value.getReleaseId());
        }
        out.endObject();
    }

    @Override
    public Source read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        Source source = new Source();
        String setField = null;
        String value = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                continue;
            }
            switch (name) {
                case "setField_" -> setField = in.nextString();
                case "value_" -> value = readScalarString(in);
                case "projectId" -> source.setProjectId(in.nextString());
                case "componentId" -> source.setComponentId(in.nextString());
                case "releaseId" -> source.setReleaseId(in.nextString());
                default -> in.skipValue(); // e.g. issetBitfield / thrift meta
            }
        }
        in.endObject();

        if (setField != null) {
            applyThriftUnion(source, setField, value);
        }
        return source;
    }

    private static String readScalarString(JsonReader in) throws IOException {
        return switch (in.peek()) {
            case STRING, NUMBER, BOOLEAN -> in.nextString();
            case NULL -> {
                in.nextNull();
                yield null;
            }
            default -> throw new IOException("Source.value_ must be a scalar, got " + in.peek());
        };
    }

    private static void applyThriftUnion(Source source, String setField, String value) {
        if (value == null) {
            return;
        }
        switch (setField) {
            case "PROJECT_ID" -> source.setProjectId(value);
            case "COMPONENT_ID" -> source.setComponentId(value);
            case "RELEASE_ID" -> source.setReleaseId(value);
            default -> throw new IllegalArgumentException(
                    "Unknown Source thrift union arm '" + setField + "' — refusing to drop data");
        }
    }
}
