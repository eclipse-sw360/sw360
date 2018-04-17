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
package org.eclipse.sw360.cvesearch.datasource;

import org.eclipse.sw360.cvesearch.datasource.heuristics.Heuristic;
import org.eclipse.sw360.cvesearch.datasource.heuristics.SearchLevels;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

public class CveSearchWrapper {

    private static final Logger log = Logger.getLogger(CveSearchWrapper.class);

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
