/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler.IndexField;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseNouveauSearchHandlerTest {

    @Test
    void versionField_shouldGenerateNormalizedVersionSortSnippet() {
        IndexField field = IndexField.version("version", 1, 50);
        String snippet = field.toJsSnippet("__EMPTY__");

        assertEquals(
                "    if(doc.version !== undefined && doc.version != null && typeof(doc.version) == 'string' && doc.version.length > 0) {" +
                "      index('text', 'version_exact', doc.version);" +
                "      emitEdgeNGrams('version_ngram', doc.version, 1, 50);" +
                "      index('string', 'version_sort', normalizeVersionForSort(doc.version));" +
                "    }", snippet);
    }

    @Test
    void versionNestedField_shouldGuardParentAndGenerateNormalizedVersionSortSnippet() {
        IndexField field = IndexField.version("subVersion", "nested.version", 2, 40);
        String snippet = field.toJsSnippet("__EMPTY__");

        assertTrue(snippet.contains("doc.nested !== undefined && doc.nested != null && "));
        assertTrue(snippet.contains("doc.nested.version !== undefined && doc.nested.version != null && "));
        assertTrue(snippet.contains("index('text', 'subVersion_exact', doc.nested.version)"));
        assertTrue(snippet.contains("emitEdgeNGrams('subVersion_ngram', doc.nested.version, 2, 40)"));
        assertTrue(snippet.contains("index('string', 'subVersion_sort', normalizeVersionForSort(doc.nested.version))"));
    }

    @Test
    void contributeAnalyzers_shouldRegisterNgramAndSortForVersionField() {
        IndexField field = IndexField.version("version");
        Map<String, String> analyzers = new HashMap<>();
        field.contributeAnalyzers(analyzers);

        assertEquals("whitespace", analyzers.get("version_ngram"));
        assertEquals("keyword", analyzers.get("version_sort"));
    }

    @Test
    void buildIndexFunction_shouldIncludeNormalizeVersionHelperWhenVersionFieldPresent() {
        var built = BaseNouveauSearchHandler.buildIndexFunction(
                "release",
                "__EMPTY__",
                List.of(IndexField.version("version", 1, 50)),
                null,
                Collections.emptyMap(),
                "standard"
        );

        String function = built.getIndexFunction().getIndex();
        assertTrue(function.contains("normalizeVersionForSort"));
        assertTrue(function.contains("match.length < 10"));
    }

    @Test
    void buildIndexFunction_shouldNotIncludeNormalizeVersionHelperWhenNoVersionField() {
        var built = BaseNouveauSearchHandler.buildIndexFunction(
                "release",
                "__EMPTY__",
                List.of(IndexField.standard("name", 2, 50)),
                null,
                Collections.emptyMap(),
                "standard"
        );

        String function = built.getIndexFunction().getIndex();
        assertFalse(function.contains("normalizeVersionForSort"));
        assertFalse(function.contains("match.length < 10"));
    }
}
