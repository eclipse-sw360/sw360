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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ListMatcherTest {

    String needle1 = "needle1";
    String needle2 = "ndl2";
    String needle3 = "ndl3";
    ArrayList<String> needles;
    ListMatcher listMatcher;

    @Before
    public void prepare() {
        needles = new ArrayList<>();
        needles.add(needle1);
        needles.add(needle2);
        needles.add(needle3);
        listMatcher = new ListMatcher(needles);
    }

    @Test
    public void getMatchTestFullMatch() {

        List<Match> matches = listMatcher.getMatches(needle2);

        assert(matches.get(0).getNeedle().equals(needle2));
        assert(matches.get(0).getDistance() == 0);
        assert(matches.get(1).getDistance() != 0);
        assert(matches.get(2).getDistance() != 0);
    }
}
