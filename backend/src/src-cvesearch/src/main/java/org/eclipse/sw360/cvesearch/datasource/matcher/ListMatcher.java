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

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.sw360.cvesearch.datasource.matcher.ModifiedLevenshteinDistance.levenshteinMatch;

public class ListMatcher {
    private Collection<String> needleList;
    Logger log = Logger.getLogger(ListMatcher.class);

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
