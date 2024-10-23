/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils;

public class SearchUtils {
    /*
     * This function returns the entire document as a string which can then be
     * indexed as a text field for 'default' index in Nouveau.
     * Possible values for `typeof` are documented at
     * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/typeof#description
     * We entertain only following types:
     * - number, bigint, string -> Directly converted to string with `+`
     * - boolean -> Converted to string using `toString()`
     * - object -> Recursively converted to string.
     * - function & others -> skip
     */
    public static final String OBJ_TO_DEFAULT_INDEX = "  function getObjAsString(obj) {" +
            "    let result = '';" +
            "    for (var key in obj) {" +
            "      if (key == '_rev' || key == 'type') continue;" +
            "      switch (typeof(obj[key])) {" +
            "        case 'object':" +
            "          if (obj[key] !== null) {" +
            "            result += ' ' + getObjAsString(obj[key]);" +
            "          }" +
            "          break;" +
            "        case 'number':" +
            "        case 'bigint':" +
            "        case 'string':" +
            "          result += ' ' + obj[key];" +
            "          break;" +
            "        case 'boolean':" +
            "          result += ' ' + obj[key].toString();" +
            "          break;" +
            "        case 'function':" +
            "        default:" +
            "          break;" +
            "      }" +
            "    }" +
            "    return result.trim();" +
            "  };";

    /*
     * This function takes an array (or object) and traverse through it. Get all
     * the values and index them as a text index.
     */
    public static final String OBJ_ARRAY_TO_STRING_INDEX = " function arrayToStringIndex(arr, indexName) {" +
            "    let result = '';" +
            "    for (let i in arr) {" +
            "      if (arr[i] && typeof(arr[i]) == 'string' && arr[i].length > 0) {" +
            "        result += ' ' + arr[i];" +
            "      }" +
            "    }" +
            "    if (result.trim().length > 0) {" +
            "      index('text', indexName, result.trim(), {'store': true});" +
            "    }" +
            "  }";
}
