/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

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
            "      index('text', indexName, result.trim());" +
            "      index('string', indexName + '_sort', result.trim());" +
            "    }" +
            "  }";

    /**
     * This function helps generate edge n_grams for a field. Example:
     * {@code ["aw", "awe", "awes", "aweso", "awesom", "awesome"]}. The
     * parameters:
     * <ul>
     *     <li><b>fieldName</b>: The index field name to store n-grams as.</li>
     *     <li><b>text</b>: The text to be processed into n-grams.</li>
     *     <li><b>minGram</b>: The minimum length of the n-grams.</li>
     *     <li><b>maxGram</b>: The maximum length of the n-grams.</li>
     * </ul>
     */
    public static final String EMIT_EDGE_N_GRAM_INDEX = """
              function emitEdgeNGrams(fieldName, text, minGram, maxGram) {
                if (!text) return;
                var words = text.toLowerCase().split(/\\\\s+/);
                for (var i = 0; i < words.length; i++) {
                  var word = words[i];
                  var limit = Math.min(word.length, maxGram);
                  for (var len = minGram; len <= limit; len++) {
                    index('text', fieldName, word.substring(0, len));
                  }
                }
              }
            """;

    /**
     * This function helps indexing dates as yyyymmdd long number. For example,
     * 2026-01-23 will be indexed as {@code double(20260123)} so it can be
     * compared and sorted.
     */
    public static final String INDEX_DATE_AS_DOUBLE = """
            function indexDateAsDouble(fieldName, createdOn) {
              if (createdOn) {
                var dt = new Date(createdOn);
                if (!isNaN(dt.getTime())) {
                  var yyyy = dt.getFullYear().toString();
                  var mm = (dt.getMonth() + 1).toString().padStart(2, '0');
                  var dd = dt.getDate().toString().padStart(2, '0');
                  index('double', fieldName, Number(yyyy + mm + dd));
                }
              }
            }
            """;

    /**
     * This function indexes {@code doc._id} as a string field with name {@code id}.
     */
    public static final String INDEX_ID_FIELD = """
            if(doc._id && typeof(doc._id) == 'string' && doc._id.length > 0) {
              index('string', 'id', doc._id);
            }
            """;

    /**
     * This function indexes every {@code releaseId} found in the stringified
     * {@code doc.releaseRelationNetwork} ({@code list<ReleaseNode>}), including nested
     * {@code releaseLink} nodes.
     */
    public static final String INDEX_PROJECT_RELEASE_RELATION_NETWORK = """
            function indexReleaseRelationNetwork(nodes) {
              if (typeof(nodes) === 'string' && nodes.length > 0) {
                try {
                  nodes = JSON.parse(nodes);
                } catch (e) {
                  return;
                }
              }

              if (!nodes || !Array.isArray(nodes)) return;

              for (var i = 0; i < nodes.length; i++) {
                var node = nodes[i];
                if (!node || typeof(node) !== 'object') continue;

                if (typeof(node['releaseId']) === 'string' && node['releaseId'].length > 0) {
                  index('string', 'releaseRelationNetwork', node['releaseId']);
                }

                if (node.releaseLink && Array.isArray(node.releaseLink) && node.releaseLink.length > 0) {
                  indexReleaseRelationNetwork(node.releaseLink);
                }
              }
            }

            if (doc.releaseRelationNetwork) {
              indexReleaseRelationNetwork(doc.releaseRelationNetwork);
            }
            """;
}
