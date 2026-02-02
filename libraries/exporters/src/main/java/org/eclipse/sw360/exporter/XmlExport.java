/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 * Copyright Sandip Mandal <sandipmandal02.sm@gmail.com>, 2026.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class XmlExport {

    private static final XmlMapper XML_MAPPER = XmlMapper.builder()
            .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
            .build();

    private XmlExport() {
    }

    @NotNull
    public static ByteArrayInputStream toXml(Object data) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XML_MAPPER.writeValue(out, data);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
