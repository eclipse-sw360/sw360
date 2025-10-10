/*
 * Helper to create JSON exports using Jackson
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