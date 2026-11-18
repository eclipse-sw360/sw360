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

import static org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler.EDGE_NGRAM_MAX_LENGTH;

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
     * Emits edge n-grams for each non-alphanumeric-delimited segment of {@code doc.version}.
     * This allows partial infix searches to match version strings like {@code 2.4.1_conflict}
     * when the user searches for {@code conf}, which the standard whitespace-splitting ngram
     * function cannot support because the whole value has no whitespace.
     *
     * <p>Example: {@code 2.4.1_conflict} splits into {@code ["2","4","1","conflict"]};
     * the segment {@code conflict} generates ngrams {@code co, con, conf, …, conflict}.</p>
     */
    public static final String INDEX_VERSION_SEGMENTS = """
            if (doc.version && typeof(doc.version) == 'string') {
              var vParts = doc.version.toLowerCase().split(/[^a-z0-9]+/);
              for (var vi = 0; vi < vParts.length; vi++) {
                if (vParts[vi].length >= 1) {
                  emitEdgeNGrams('version_ngram', vParts[vi], 1, %d);
                }
              }
            }
            """.formatted(EDGE_NGRAM_MAX_LENGTH);

    /**
     * Defines the JS function {@code versionSortKey(value)}, used to fill the Nouveau
     * {@code version_sort} field. It returns a string whose lexicographic order is a single natural
     * version order for all values, whether or not they are strict semver (SW360 does not enforce
     * a version format; values like {@code "1"}, {@code "3.5 SP1"}, {@code "v1.1.1"},
     * {@code "4.0.0.0_conflict"} are common):
     * <ul>
     *   <li>the leading dotted numeric core is compared segment by segment, numerically
     *       ({@code 2 < 10.0.10 < 10.0.25}); a shorter core sorts first ({@code 1.2 < 1.2.0});</li>
     *   <li>a semver pre-release suffix sorts before the bare core, identifiers compared per semver
     *       ({@code 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0});</li>
     *   <li>any other suffix sorts after the bare core and before the next numeric segment
     *       ({@code 4.0.0 < 4.0.0_x < 4.0.0.0});</li>
     *   <li>a leading {@code v} before a digit and {@code +build} metadata are ignored;</li>
     *   <li>values not starting with a digit sort after all numeric versions, in natural order.</li>
     * </ul>
     * Numbers are encoded as a 4-digit length followed by the digits without leading zeros, so
     * string comparison equals numeric comparison. Marker characters after the core are ordered
     * {@code '!'} (pre-release) &lt; {@code '#'} (end) &lt; {@code '%'} (other suffix) &lt;
     * {@code '.'} (next segment).
     */
    public static final String NATURAL_VERSION_SORT_KEY = """
            function versionEncodeNumber(digits) {
              var stripped = digits.replace(/^0+(?=.)/, '');
              return stripped.length.toString().padStart(4, '0') + stripped;
            }

            function versionNaturalKey(str) {
              var parts = str.match(/\\d+|\\D+/g) || [];
              var key = '';
              for (var i = 0; i < parts.length; i++) {
                var first = parts[i].charAt(0);
                key += (first >= '0' && first <= '9') ? versionEncodeNumber(parts[i]) : parts[i];
              }
              return key;
            }

            function versionSortKey(value) {
              if (value === undefined || value === null) return '';
              var str = String(value).trim().toLowerCase();
              if (str.length === 0) return '';
              str = str.replace(/^v(?=\\d)/, '');
              var plus = str.indexOf('+');
              if (plus > 0) str = str.substring(0, plus);
              var core = /^\\d+(?:\\.\\d+)*/.exec(str);
              if (!core) return '1' + versionNaturalKey(str);
              var segments = core[0].split('.');
              var key = '0';
              for (var i = 0; i < segments.length; i++) {
                if (i > 0) key += '.';
                key += versionEncodeNumber(segments[i]);
              }
              var rest = str.substring(core[0].length);
              if (rest.length === 0) return key + '#';
              if (/^-[0-9a-z-]+(?:\\.[0-9a-z-]+)*$/.test(rest)) {
                key += '!';
                var ids = rest.substring(1).split('.');
                for (var j = 0; j < ids.length; j++) {
                  if (j > 0) key += ' ';
                  key += /^\\d+$/.test(ids[j]) ? '0' + versionEncodeNumber(ids[j]) : '1' + versionNaturalKey(ids[j]);
                }
                return key;
              }
              return key + '%' + versionNaturalKey(rest);
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
