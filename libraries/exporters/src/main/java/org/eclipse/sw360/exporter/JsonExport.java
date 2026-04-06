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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JsonExport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonExport() {
    }

    @NotNull
    public static ByteArrayInputStream toJson(Object data) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MAPPER.writeValue(out, data);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
