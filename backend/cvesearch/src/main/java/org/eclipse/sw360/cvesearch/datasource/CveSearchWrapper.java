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
package org.eclipse.sw360.cvesearch.datasource;

import org.eclipse.sw360.cvesearch.datasource.heuristics.Heuristic;
import org.eclipse.sw360.cvesearch.datasource.heuristics.SearchLevels;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CveSearchWrapper {

    private static final Logger log = LogManager.getLogger(CveSearchWrapper.class);

    private final Heuristic heuristic;

    public CveSearchWrapper(CveSearchApi cveSearchApi) {
        SearchLevels searchLevels = new SearchLevels(cveSearchApi);
        heuristic = new Heuristic(searchLevels, cveSearchApi);
    }

    public Optional<List<CveSearchData>> searchForRelease(Release release) {
        try {
            return Optional.of(heuristic.run(release));
        } catch (IOException e) {
            log.error("Was not able to search for release with name=" + release.getName() + " and id=" + release.getId(), e);
        }
        return Optional.empty();
    }
}
