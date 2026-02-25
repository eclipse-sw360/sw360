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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class XmlExport {

    private XmlExport() {
    }

    @NotNull
    public static ByteArrayInputStream toXml(List<Map<String, String>> records) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(out, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("records");

            for (Map<String, String> record : records) {
                writer.writeStartElement("record");
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    String key = sanitizeXmlTag(entry.getKey());
                    writer.writeStartElement(key);
                    writer.writeCharacters(entry.getValue() != null ? entry.getValue() : "");
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();

            return new ByteArrayInputStream(out.toByteArray());
        } catch (XMLStreamException e) {
            throw new IOException("Failed to generate XML", e);
        }
    }

    private static String sanitizeXmlTag(String tag) {
        return tag.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
