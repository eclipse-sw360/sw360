/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
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
package org.eclipse.sw360.cvesearch;

import org.eclipse.sw360.cvesearch.datasource.CveSearchApi;
import org.eclipse.sw360.cvesearch.datasource.CveSearchApiImpl;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.cvesearch.service.CveSearchHandler;
import org.junit.Assume;
import org.junit.Before;

import java.util.Properties;

import static org.eclipse.sw360.cvesearch.datasource.CveSearchDataTestHelper.isUrlReachable;

public abstract class TestWithCveSearchConnection {
    private String PUBLIC_CVE_SEARCH_SERVER = "https://cve.circl.lu";

    protected CveSearchApi cveSearchApi;

    @Before
    public void setUpApi() {
        Properties props = CommonUtils.loadProperties(TestWithCveSearchConnection.class, "/cvesearch.properties");
        String host = props.getProperty(CveSearchHandler.CVESEARCH_HOST_PROPERTY, PUBLIC_CVE_SEARCH_SERVER);

        Assume.assumeTrue("The public CVE Search server is unreliable and tests are only executed if a different instance is configured.", ! PUBLIC_CVE_SEARCH_SERVER.equals(host));
        Assume.assumeTrue("CVE Search host is reachable", isUrlReachable(host));
        System.out.println("The CVE Search host is: " + host);

        cveSearchApi = new CveSearchApiImpl(host);
    }
}
