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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.ALL_KNOWN_CONFIG_KEYS;
import static org.eclipse.sw360.rest.common.Sw360XSSRequestWrapper.stripXSS;

/**
 * Jackson deserializer for Strings which uses stripXSS, except for config keys.
 */
public class XssStringDeserializer extends JsonDeserializer<String> {

    /**
     * While deserializing JSON, filter all the strings with stripXSS function. This
     * makes sure if the value is coming
     * for the key in known SW360ConfigKeys, then we do not want to stripXSS to
     * preserve JSON encoding in strings.
     */
    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        String value = jsonParser.getText();

        if (value == null) {
            return null;
        }

        String currentName = jsonParser.getParsingContext().getCurrentName();

        // Do not strip config keys
        if (currentName != null && ALL_KNOWN_CONFIG_KEYS.contains(currentName)) {
            return value;
        }

        // Sanitize without HTML encoding - just remove dangerous patterns
        return sanitizeWithoutEncoding(value);
    }

    /**
     * Sanitizes input by removing XSS attack patterns without HTML encoding.
     * This allows clean text to be stored in the database while still preventing
     * XSS.
     */
    private String sanitizeWithoutEncoding(String value) {
        if (value == null) {
            return null;
        }

        // First unescape to get canonical form
        String canonical = StringEscapeUtils.unescapeHtml4(value);

        // Remove script tags and other dangerous patterns (case-insensitive)
        canonical = canonical.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        canonical = canonical.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");
        canonical = canonical.replaceAll("(?i)javascript:", "");
        canonical = canonical.replaceAll("(?i)vbscript:", "");
        canonical = canonical.replaceAll("(?i)data:", "");
        canonical = canonical.replaceAll("on\\w+\\s*=", ""); // Remove event handlers

        // Return clean text without HTML entity encoding
        return canonical;
    }
}
