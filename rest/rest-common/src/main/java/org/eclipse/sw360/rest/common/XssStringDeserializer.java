/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.common;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.ALL_KNOWN_CONFIG_KEYS;
import static org.eclipse.sw360.rest.common.Sw360XSSRequestWrapper.stripXSS;

/**
 * Jackson deserializer for Strings which uses stripXSS, except for config keys.
 */
public class XssStringDeserializer extends StdScalarDeserializer<String> {

    public XssStringDeserializer() {
        super(String.class);
    }

    /**
     * While deserializing JSON, filter all the strings with stripXSS function. This makes sure if the value is coming
     * for the key in known SW360ConfigKeys, then we do not want to stripXSS to preserve JSON encoding in strings.
     */
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
