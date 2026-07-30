/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.search;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.search.SearchClient;
import org.eclipse.sw360.datahandler.search.SearchClients;
import org.eclipse.sw360.datahandler.services.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;


@Service
public class Sw360SearchService {
    private static final Logger log = LogManager.getLogger(Sw360SearchService.class);

    private SearchClient client() {
        return SearchClients.get();
    }

    public List<SearchResult> search(String searchText, User sw360User, Optional<List<String>> typeMaskOptional) {
        List<String> typeMasks = typeMaskOptional.orElse(Collections.emptyList());
        return client().search(searchText, UserConverter.fromThrift(sw360User), typeMasks);
    }
}
