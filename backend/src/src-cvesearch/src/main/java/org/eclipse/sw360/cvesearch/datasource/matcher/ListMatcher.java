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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.sw360.cvesearch.datasource.matcher.ModifiedLevenshteinDistance.levenshteinMatch;

public class ListMatcher {
    private Collection<String> needleList;

    public ListMatcher(Collection<String> needleList){
        this.needleList = needleList;
    }

    public List<Match> getMatches(String haystack){
        return needleList.stream()
                .map(needle -> levenshteinMatch(needle, haystack))
                .sorted((sm1,sm2) -> sm1.compareTo(sm2))
                .collect(Collectors.toList());
    }
}
