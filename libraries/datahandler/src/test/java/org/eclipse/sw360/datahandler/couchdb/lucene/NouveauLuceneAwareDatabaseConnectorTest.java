/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb.lucene;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public class NouveauLuceneAwareDatabaseConnectorTest {

    @Test
    public void testFormatSubqueryWithValidQuotedPhrase() throws Exception {
        String query = formatSubquery(Set.of("\"AB CD E\""), "tag");

        Assert.assertEquals("( tag:\"AB CD E\" ) ", query);
    }

    @Test
    public void testFormatSubqueryEscapesMalformedOpeningQuote() throws Exception {
        String query = formatSubquery(Set.of("\"AB CD E"), "tag");

        Assert.assertEquals("( tag:\"\\\"AB CD E\" ) ", query);
    }

    @Test
    public void testFormatSubqueryEscapesMalformedClosingQuote() throws Exception {
        String query = formatSubquery(Set.of("AB CD E\""), "tag");

        Assert.assertEquals("( tag:\"AB CD E\\\"\" ) ", query);
    }

    @Test
    public void testFormatSubqueryEscapesEmbeddedRogueQuote() throws Exception {
        String query = formatSubquery(Set.of("AB \"CD E"), "tag");

        Assert.assertEquals("( tag:\"AB \\\"CD E\" ) ", query);
    }

    @Test
    public void testFormatSubqueryKeepsPreformattedWildcardClause() throws Exception {
        String wildcardClause = "(\"ab cd e\"^20 OR (ab* AND cd* AND e*)^5 OR (ab* OR cd* OR e*))";

        String query = formatSubquery(Collections.singleton(wildcardClause), "tag");

        String expected = "( tag:(\\\"ab cd e\\\"^20 OR (ab* AND cd* AND e*)^5 OR (ab* OR cd* OR e*)) ) ";

        Assert.assertEquals(expected, query);
    }

    @Test
    public void testNormalizeRestrictionInputCases() throws Exception {
        Assert.assertEquals("\"ABC\\\"XYZ\"", normalizeRestrictionInput("\"ABC\"XYZ\""));
        Assert.assertEquals("\\\"ABC\\\"XYZ", normalizeRestrictionInput("\"ABC\"XYZ"));
        Assert.assertEquals("\\\"ABC", normalizeRestrictionInput("\"ABC"));
        Assert.assertEquals("\"ABC\\\"X\\\"YZ\"", normalizeRestrictionInput("\"ABC\"X\"YZ\""));
        Assert.assertEquals("ABC\\\"", normalizeRestrictionInput("ABC\""));
        Assert.assertEquals("ABC", normalizeRestrictionInput("ABC"));
        Assert.assertEquals("\"ABC\"", normalizeRestrictionInput("\"ABC\""));
        Assert.assertEquals("A\\\"BC", normalizeRestrictionInput("A\"BC"));
    }

    private static String formatSubquery(Set<String> filterSet, String fieldName) throws Exception {
        Method method = NouveauLuceneAwareDatabaseConnector.class
                .getDeclaredMethod("formatSubquery", Set.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, filterSet, fieldName);
    }

    private static String normalizeRestrictionInput(String input) throws Exception {
        Method method = NouveauLuceneAwareDatabaseConnector.class
                .getDeclaredMethod("normalizeRestrictionInput", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, input);
    }
}
