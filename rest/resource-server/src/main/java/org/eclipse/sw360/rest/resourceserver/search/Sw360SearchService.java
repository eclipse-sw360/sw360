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
import org.eclipse.sw360.datahandler.services.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class Sw360SearchService {
    private static final Logger log = LogManager.getLogger(Sw360SearchService.class);

    private final RestClient restClient;
    private final String SEARCH_URI = "/search/api/search";

    public Sw360SearchService(RestClient restClient){
        this.restClient = restClient;
    }

    public List<SearchResult> search(String searchText, User sw360User, Optional<List<String>> typeMaskOptional) throws Exception {
        List<String> typeMasks = typeMaskOptional.orElse(Collections.emptyList());
        return restClient.get()
        .uri(uriBuidler -> uriBuidler
            .path(SEARCH_URI)
            .queryParam("text", searchText)
            .queryParam("typeMask", typeMasks)
            .build())
        .header("X-User-Email", sw360User.getEmail())
        .retrieve()
        .body(new ParameterizedTypeReference<List<SearchResult>>() {});
    }
}
