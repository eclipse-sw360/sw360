/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bodet on 11/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class JacksonUtils {

    private JacksonUtils() {
        // Utility class with only static functions
    }

    public static boolean arrayContains(ArrayNode array, String needle) {
        for (JsonNode jsonNode : array) {
            if (jsonNode.isTextual() && needle.equals(jsonNode.textValue())) {
                return true;
            }
        }
        return false;
    }

    public static int arrayPosition(ArrayNode array, String needle) {
        for (int i = 0; i < array.size(); i++) {
            JsonNode jsonNode = array.get(i);
            if (jsonNode.isTextual() && needle.equals(jsonNode.textValue())) {
                return i;
            }
        }
        return -1;
    }

    public static Set<String> extractSet(ArrayNode array) throws SW360Exception {
        Set<String> result = new HashSet<>();

        for (JsonNode jsonNode : array) {
            if (jsonNode.isTextual())
                result.add(jsonNode.textValue());
            else
                throw new SW360Exception("Non textual string ?!");
        }
        return result;
    }

}
