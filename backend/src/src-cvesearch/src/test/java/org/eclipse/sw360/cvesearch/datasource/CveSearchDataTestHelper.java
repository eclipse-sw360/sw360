/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * With modifications by Siemens AG, 2016.
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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CveSearchDataTestHelper {


    private static final int CONNECT_TIMEOUT = 10000;

    public static boolean isEquivalent(List<CveSearchData> l1, List<CveSearchData> l2){
        int s = l1.size();
        if(s != l2.size()) return false;

        List<String> ids1 = l1.stream().map(CveSearchData::getId).sorted().collect(Collectors.toList());
        List<String> ids2 = l2.stream().map(CveSearchData::getId).sorted().collect(Collectors.toList());

        return IntStream.range(0,s)
                .mapToObj(i -> ids1.get(i).equals(ids2.get(i)))
                .reduce(true, Boolean::logicalAnd);
    }

    public static boolean isUrlReachable(String url) {
        try {
            final URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.connect();
            return true;
        } catch (final IOException e) {
            return false;
        }
    }
}
