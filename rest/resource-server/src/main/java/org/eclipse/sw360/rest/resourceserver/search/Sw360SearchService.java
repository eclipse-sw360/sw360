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

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

@Service
public class Sw360SearchService {
    private static final Logger log = LogManager.getLogger(Sw360SearchService.class);

    private SearchService.Iface getThriftSearchClient() {
        return new ThriftClients().makeSearchClient();
    }

    public List<SearchResult> search(String searchText, User sw360User, Optional<List<String>> typeMaskOptional) throws TException {
        List<String> typeMasks = typeMaskOptional.orElseGet(Lists::newArrayList);
        return getThriftSearchClient().searchFiltered(searchText, sw360User, typeMasks);
    }
}
