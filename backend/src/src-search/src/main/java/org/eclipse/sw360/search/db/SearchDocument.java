/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.search.db;

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.sw360.search.common.SearchConstants.NAME_MAX_LENGTH;

/**
 * Helper class to help parse JSON documents from lucene-ektorp
 *
 * @author cedric.bodet@tngtech.com
 */
class SearchDocument {

    /**
     * Map representation of the document
     */
    private final Map<String, Object> document;

    private final String type;

    /**
     * Constructor, create empty hashmap if the one provided is null, avoiding null pointer exception
     */
    SearchDocument(Map<String, Object> document) {
        if (document != null) {
            this.document = Collections.unmodifiableMap(document);
        } else {
            this.document = new HashMap<>();
        }
        // Set the type of the document
        type = getProperty("type");
    }

    /**
     * Get document type
     */
    String getType() {
        return type;
    }

    /**
     * Get document name
     */
    String getName() {
        if (!SW360Constants.MAP_FULLTEXT_SEARCH_NAME.containsKey(type)) {
            return "";
        } else {
            String name = getProperty(SW360Constants.MAP_FULLTEXT_SEARCH_NAME.get(type));
            return StringUtils.abbreviate(name, NAME_MAX_LENGTH);
        }
    }

    /**
     * Get property from document hashmap, returning an empty string in case of error
     */
    String getProperty(String key) {
        if (key == null) {
            return "";
        }

        if (!key.contains(" ")) {
            // Get a single key
            Object value = document.get(key);

            if (value instanceof String) {
                return (String) value;
            } else {
                return "";
            }
        } else {
            // Build a name containing all keys
            StringBuilder builder = new StringBuilder("");
            String[] parts = key.split(" ");
            for (String part : parts) {
                builder.append(getProperty(part)).append(' ');
            }
            return builder.toString();
        }
    }

}
