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

import java.util.Comparator;

public class Match {
    private String needle;
    private int distance;

    public Match(String needle, int distance){
        this.needle = needle;
        this.distance = distance;
    }

    public String getNeedle() {
        return needle;
    }

    public int getDistance() {
        return distance;
    }

    public int compareTo(Match otherMatch) {
        Comparator<Match> byDistance     = (sm1, sm2) -> Integer.compare(sm1.getDistance(), sm2.getDistance());
        Comparator<Match> byNeedleLength = (sm1,sm2) -> Integer.compare(sm2.getNeedle().length(), sm1.getNeedle().length());

        return byDistance.thenComparing(byNeedleLength).compare(this, otherMatch);
    }

    public Match concat(Match otherMatch) {
        return new Match(this.needle + ":" + otherMatch.getNeedle(), this.distance + otherMatch.getDistance());
    }

    @Override
    public String toString() {
        return "[" + this.needle + ":" + this.distance + "]";
    }
}
