/*
 * Helper to create XML exports using Jackson XmlMapper
 */
package org.eclipse.sw360.exporter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class XmlExport {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    @NotNull
    public static ByteArrayInputStream toXml(Object data, Class<?> rootType) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // When writing a generic object, XmlMapper will infer names. For collections you may pass a wrapper object.
            XML_MAPPER.writeValue(out, data);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            out.close();
            throw e;
        }
    }
}