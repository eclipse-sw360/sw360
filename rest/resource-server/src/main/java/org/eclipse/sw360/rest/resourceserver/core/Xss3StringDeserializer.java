/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.ALL_KNOWN_CONFIG_KEYS;
import static org.eclipse.sw360.rest.common.Sw360XSSRequestWrapper.stripXSS;

/**
 * Jackson 3.x String deserializer that applies XSS stripping.
 * <p>
 * This is the Jackson 3.x equivalent of the Jackson 2.x
 * {@link org.eclipse.sw360.rest.common.XssStringDeserializer}.
 * Spring Boot 4 uses Jackson 3.x ({@code tools.jackson}) by default for
 * HTTP message conversion, so XSS prevention must be registered with both
 * the Jackson 2.x {@code ObjectMapper} and the Jackson 3.x {@code JsonMapper}.
 */
public class Xss3StringDeserializer extends StdScalarDeserializer<String> {

    public Xss3StringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext context) {
        String value = jsonParser.getText();

        if (value == null) {
            return null;
        }

        String currentName = jsonParser.currentName();

        // Do not strip config keys
        if (currentName != null && ALL_KNOWN_CONFIG_KEYS.contains(currentName)) {
            return value;
        }

        return stripXSS(value);
    }
}
