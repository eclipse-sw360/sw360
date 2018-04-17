/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.search;

import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;

import java.io.IOException;
import java.util.List;

/**
 * Small client for testing a service
 *
 * @author cedric.bodet@tngtech.com
 */
public class TestSearchClient {

    private static final String searchtext = "s*";

    public static void main(String[] args) throws TException, IOException {
        THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080/search/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        SearchService.Iface client = new SearchService.Client(protocol);

        List<SearchResult> results = client.search(searchtext, null);
        //List<SearchResult> results = new SearchHandler().search(searchtext);


        //  http://localhost:5984/_fti/local/sw360db/_design/lucene/all?q=type:project%20AND%20P1*

        System.out.println("Fetched " + results.size() + " from search service");
        for (SearchResult result : results) {
            System.out.println(result.getId() + "(" + result.getType() + "): " + result.getName() + " (" + result.getScore() + ")");
        }
    }

}
