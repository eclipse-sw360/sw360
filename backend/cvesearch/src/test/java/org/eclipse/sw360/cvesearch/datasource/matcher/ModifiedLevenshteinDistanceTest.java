/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.datasource.matcher;

import org.junit.Test;

import static org.eclipse.sw360.cvesearch.datasource.matcher.ModifiedLevenshteinDistance.levenshteinMatch;
import static org.junit.Assert.*;

public class ModifiedLevenshteinDistanceTest {

    @Test
    public void testBasicRules() {
        String needle = "needle";
        String neele = "neele";
        String needDle = "needDle";

        // equal strings have distance 0
        assertEquals(0, levenshteinMatch(needle,needle).getDistance());

        // appending or prepending adds 1 to the distance
        assertEquals(1, levenshteinMatch(needle,needle + "a").getDistance());
        assertEquals(1, levenshteinMatch(needle, "a" + needle ).getDistance());
        assertEquals(2, levenshteinMatch(needle, "a" + needle + "a").getDistance());
        assertEquals(1, levenshteinMatch(needle, needDle).getDistance());
        assertEquals(1, levenshteinMatch(needDle, needle).getDistance());

        // dropping adds 1 to the distance
        assertEquals(1, levenshteinMatch(needle, neele).getDistance());
        assertEquals(1, levenshteinMatch(neele, needle).getDistance());

        // should be able to find the best match
        assertEquals(0, levenshteinMatch(needle, needle + " " + neele).getDistance());
        assertEquals(0, levenshteinMatch(needle, neele + " " + needle).getDistance());

        // seperated by spaces does not change distance
        assertEquals(0, levenshteinMatch(needle,needle + " a").getDistance());
        assertEquals(0, levenshteinMatch(needle, "a " + needle ).getDistance());
        assertEquals(1, levenshteinMatch(needle, "a" + needle + " a").getDistance());
        assertEquals(1, levenshteinMatch(needle, "a " + needle + "a").getDistance());
        assertEquals(0, levenshteinMatch(needle, "a " + needle + " a").getDistance());
    }

    @Test
    public void getDistancesEmptyNeedle(){
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("", "haystack").getDistance());
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("", "haystack").getDistance());
    }

    @Test
    public void getDistancesEmptyHaystack(){
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("needle", "").getDistance());
    }

    @Test
    public void getDistanceXtoSomethingWithoutX() {
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("x","y").getDistance());
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("x","y ").getDistance());
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("x"," y").getDistance());
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("x"," y ").getDistance());
        assertEquals(Integer.MAX_VALUE, levenshteinMatch("x","lorem ipsum").getDistance());
    }

    @Test
    public void getDistanceXToXX() {
        assertEquals(1, levenshteinMatch("xx","x").getDistance());
        assertEquals(1, levenshteinMatch("xx","x ").getDistance());
        assertEquals(1, levenshteinMatch("xx"," x").getDistance());
        assertEquals(1, levenshteinMatch("xx"," x ").getDistance());
        assertEquals(1, levenshteinMatch("xx","y x ").getDistance());
        assertEquals(1, levenshteinMatch("xx"," x y").getDistance());
        assertEquals(1, levenshteinMatch("xx","y x y").getDistance());

        assertEquals(1, levenshteinMatch("Xx","x").getDistance());
        assertEquals(1, levenshteinMatch("xx","X").getDistance());
    }

    @Test
    public void getDistanceTest() {
        String needle = "needle";
        String haystack = "haystack";

        Match match = levenshteinMatch(needle,haystack);

        assertEquals(needle, match.getNeedle());
        assertTrue(match.getDistance() > 0);
    }

    @Test
    public void getDistanceTestFullMatch() {
        String needle = "needle";
        String haystack = "needle";

        Match match = levenshteinMatch(needle,haystack);

        assertEquals(0, match.getDistance());
    }

    @Test
    public void getDistanceTestPartialMatch() {
        String needle = "needle";
        String haystack = "ndle";

        Match match = levenshteinMatch(needle,haystack);

        assertEquals(2, match.getDistance());
    }

    @Test
    public void getDistanceTestUpToPrefixMatch() {
        String needle = "needle";
        String haystack = "prefix needle";

        Match match = levenshteinMatch(needle,haystack);

        assertEquals(0, match.getDistance());
    }

    @Test
    public void getDistanceTestUpToPostfixMatch() {
        String needle = "needle";
        String haystack = "needle postfix";

        Match match = levenshteinMatch(needle,haystack);

        assertEquals(0, match.getDistance());
    }

    @Test
    public void getDistanceTestFullSubstringMatch() {
        String needle = "needle";
        String noise = "bla";
        String haystack = "prefix needle " + noise + " postfix";

        Match match = levenshteinMatch(needle,haystack);

        assertEquals(0, match.getDistance());
    }

    @Test
    public void getDistanceTestPartialSubstringMatch() {
        String needle = "needle";
        String noise = "bla";
        String haystack = "prefix needle" + noise + " postfix";

        Match match = levenshteinMatch(needle,haystack);

        assertEquals(noise.length(), match.getDistance());
    }
}
