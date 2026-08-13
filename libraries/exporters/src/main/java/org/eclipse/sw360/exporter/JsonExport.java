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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

public class JsonExport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonExport() {
    }

    /**
     * Returns a {@link ByteBuffer} backed directly by the internal write buffer — no
     * {@code Arrays.copyOf} is performed.
     */
    @NotNull
    public static ByteBuffer toByteBuffer(Object data) throws IOException {
        ExposedByteArrayOutputStream out = new ExposedByteArrayOutputStream();
        MAPPER.writeValue(out, data);
        return out.asByteBuffer();
    }
}
