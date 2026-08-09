/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival.bundle;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Serializes Thrift-generated structs to clean JSON. Thrift's defaults emit
 * internal noise (__isset_bitfield, metaDataMap, schemes); this helper hides
 * them and serializes only declared fields.
 */
public final class ThriftJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .registerModule(new SimpleModule()
                    .setMixInAnnotation(Object.class, ThriftMixin.class));

    private static final ObjectReader READER = MAPPER
            .reader()
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ThriftJson() {}

    public static byte[] toJsonBytes(Object thriftObject) throws IOException {
        return MAPPER.writeValueAsBytes(thriftObject);
    }

    /**
     * Reads bytes written by {@link #toJsonBytes} back into a Thrift struct. The
     * same field-based mapper is used so the JSON produced on archive round-trips
     * cleanly on restore. Unknown properties are ignored so a bundle written by an
     * older SW360 still restores when new fields have appeared.
     */
    public static <T> T fromJson(byte[] json, Class<T> type) throws IOException {
        return READER.readValue(json, type);
    }

    /**
     * Mixin applied to every value type. Hides Thrift-generated internals so
     * the JSON has only the real entity fields.
     */
    @SuppressWarnings("unused")
    private abstract static class ThriftMixin {
        @JsonIgnore abstract Object getMetaDataMap();
        @JsonIgnore abstract Object getSchemes();
        @JsonIgnore abstract Object get__isset_bitfield();
    }
}
