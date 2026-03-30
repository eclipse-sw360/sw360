/*
 * Copyright amandx36 2026.
 * Copyright (c) Bosch Software Innovations GmbH 2016-2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JsonExport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NotNull
    public static ByteArrayInputStream toJson(Object data) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            MAPPER.writeValue(out, data);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            out.close();
            throw e;
        }
    }
}