/*
 * Copyright Sandip Mandal <sandipmandal02.sm@gmail.com>, 2026.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.jetbrains.annotations.NotNull;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class JsonExport {

    private static final ObjectMapper MAPPER = JsonMapper.builderWithJackson2Defaults()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private JsonExport() {
    }

    /**
     * Returns a {@link ByteBuffer} backed directly by the internal write buffer — no
     * {@code Arrays.copyOf} is performed.
     */
    @NotNull
    public static ByteBuffer toByteBuffer(Object data) throws IOException {
        ExposedByteArrayOutputStream out = new ExposedByteArrayOutputStream();
        try {
            MAPPER.writeValue(out, data);
        } catch (JacksonException e) {
            throw new IOException("Unable to serialize JSON export", e);
        }
        return out.asByteBuffer();
    }
}
