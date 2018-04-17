/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.cvesearch.datasource.matcher;

import org.junit.Test;

import static org.eclipse.sw360.cvesearch.datasource.matcher.ModifiedLevenshteinDistance.levenshteinMatch;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ModifiedLevenshteinDistanceTest {

    @Test
    public void testBasicRules() {
        String needle = "needle";
        String neele = "neele";
        String needDle = "needDle";

        // equal strings have distance 0
        assertThat(levenshteinMatch(needle,needle).getDistance(), is(0));

        // appending or prepending adds 1 to the distance
        assertThat(levenshteinMatch(needle,needle + "a").getDistance(), is(1));
        assertThat(levenshteinMatch(needle, "a" + needle ).getDistance(), is(1));
        assertThat(levenshteinMatch(needle, "a" + needle + "a").getDistance(), is(2));
        assertThat(levenshteinMatch(needle, needDle).getDistance(), is(1));
        assertThat(levenshteinMatch(needDle, needle).getDistance(), is(1));

        // dropping adds 1 to the distance
        assertThat(levenshteinMatch(needle, neele).getDistance(), is(1));
        assertThat(levenshteinMatch(neele, needle).getDistance(), is(1));

        // should be able to find the best match
        assertThat(levenshteinMatch(needle, needle + " " + neele).getDistance(), is(0));
        assertThat(levenshteinMatch(needle, neele + " " + needle).getDistance(), is(0));

        // seperated by spaces does not change distance
        assertThat(levenshteinMatch(needle,needle + " a").getDistance(), is(0));
        assertThat(levenshteinMatch(needle, "a " + needle ).getDistance(), is(0));
        assertThat(levenshteinMatch(needle, "a" + needle + " a").getDistance(), is(1));
        assertThat(levenshteinMatch(needle, "a " + needle + "a").getDistance(), is(1));
        assertThat(levenshteinMatch(needle, "a " + needle + " a").getDistance(), is(0));
    }

    @Test
    public void getDistancesEmptyNeedle(){
        assertThat(levenshteinMatch("", "haystack").getDistance(), is(Integer.MAX_VALUE));
        assertThat(levenshteinMatch("", "haystack").getDistance(), is(Integer.MAX_VALUE));
    }

    @Test
    public void getDistancesEmptyHaystack(){
        assertThat(levenshteinMatch("needle", "").getDistance(), is(Integer.MAX_VALUE));
    }

    @Test
    public void getDistanceXtoSomethingWithoutX() {
        assertThat(levenshteinMatch("x","y").getDistance(), is(Integer.MAX_VALUE));
        assertThat(levenshteinMatch("x","y ").getDistance(), is(Integer.MAX_VALUE));
        assertThat(levenshteinMatch("x"," y").getDistance(), is(Integer.MAX_VALUE));
        assertThat(levenshteinMatch("x"," y ").getDistance(), is(Integer.MAX_VALUE));
        assertThat(levenshteinMatch("x","lorem ipsum").getDistance(), is(Integer.MAX_VALUE));
    }

    @Test
    public void getDistanceXToXX() {
        assertThat(levenshteinMatch("xx","x").getDistance(), is(1));
        assertThat(levenshteinMatch("xx","x ").getDistance(), is(1));
        assertThat(levenshteinMatch("xx"," x").getDistance(), is(1));
        assertThat(levenshteinMatch("xx"," x ").getDistance(), is(1));
        assertThat(levenshteinMatch("xx","y x ").getDistance(), is(1));
        assertThat(levenshteinMatch("xx"," x y").getDistance(), is(1));
        assertThat(levenshteinMatch("xx","y x y").getDistance(), is(1));

        assertThat(levenshteinMatch("Xx","x").getDistance(), is(1));
        assertThat(levenshteinMatch("xx","X").getDistance(), is(1));
    }

    @Test
    public void getDistanceTest() {
        String needle = "needle";
        String haystack = "haystack";

        Match match = levenshteinMatch(needle,haystack);

        assertThat(match.getNeedle(), is(needle));
        assertThat(match.getDistance(), is(greaterThan(0)));
    }

    @Test
    public void getDistanceTestFullMatch() {
        String needle = "needle";
        String haystack = "needle";

        Match match = levenshteinMatch(needle,haystack);

        assertThat(match.getDistance(), is(0));
    }

    @Test
    public void getDistanceTestPartialMatch() {
        String needle = "needle";
        String haystack = "ndle";

        Match match = levenshteinMatch(needle,haystack);

        assertThat(match.getDistance(), is(2));
    }

    @Test
    public void getDistanceTestUpToPrefixMatch() {
        String needle = "needle";
        String haystack = "prefix needle";

        Match match = levenshteinMatch(needle,haystack);

        assertThat(match.getDistance(), is(0));
    }

    @Test
    public void getDistanceTestUpToPostfixMatch() {
        String needle = "needle";
        String haystack = "needle postfix";

        Match match = levenshteinMatch(needle,haystack);

        assertThat(match.getDistance(), is(0));
    }

    @Test
    public void getDistanceTestFullSubstringMatch() {
        String needle = "needle";
        String noise = "bla";
        String haystack = "prefix needle " + noise + " postfix";

        Match match = levenshteinMatch(needle,haystack);

        assertThat(match.getDistance(), is(0));
    }

    @Test
    public void getDistanceTestPartialSubstringMatch() {
        String needle = "needle";
        String noise = "bla";
        String haystack = "prefix needle" + noise + " postfix";

        Match match = levenshteinMatch(needle,haystack);

        assertThat(match.getDistance(), is(noise.length())) ;
    }
}
