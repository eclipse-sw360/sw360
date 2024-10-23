/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Copyright Siemens AG, 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.datasource.heuristics;

import org.eclipse.sw360.cvesearch.datasource.CveSearchApi;
import org.eclipse.sw360.cvesearch.datasource.CveSearchData;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Heuristic {

    private final SearchLevels searchLevels;
    private final CveSearchApi cveSearchApi;
    private final int maxDepth;
    private Logger log = LogManager.getLogger(Heuristic.class);

    public Heuristic(SearchLevels searchLevels, CveSearchApi cveSearchApi) {
        this.searchLevels = searchLevels;
        this.cveSearchApi = cveSearchApi;
        this.maxDepth = 0;
    }

    public Heuristic(SearchLevels searchLevels, CveSearchApi cveSearchApi, int maxDepth) {
        this.searchLevels = searchLevels;
        this.cveSearchApi = cveSearchApi;
        this.maxDepth = maxDepth;
    }

    protected Stream<CveSearchData> runForNeedleWithMeta(SearchLevels.NeedleWithMeta needleWithMeta){
        try {
            return cveSearchApi.cvefor(needleWithMeta.needle)
                    .stream()
                    .map(cveSearchData -> cveSearchData
                            .setUsedNeedle(needleWithMeta.needle)
                            .setMatchedBy(needleWithMeta.description));
        } catch (IOException e) {
            log.error("IOException in searchlevel" +
                    "\n\twith description=" + needleWithMeta.description +
                    "\n\twith needle=" + needleWithMeta.needle +
                    "\n\twith exception message=" + e.getMessage(), e);
            return Stream.empty();
        }
    }

    public List<CveSearchData> run(Release release) throws IOException {
        return searchLevels.apply(release)
                .limit(maxDepth == 0 ? Integer.MAX_VALUE : maxDepth)
                .map(evaluatedSearchLevel -> evaluatedSearchLevel.stream()
                        .flatMap(this::runForNeedleWithMeta))
                .map(stream -> stream.collect(Collectors.toList()))
                .filter(list -> list.size() > 0)
                .findFirst()
                .orElse(new ArrayList<>());
    }
}
