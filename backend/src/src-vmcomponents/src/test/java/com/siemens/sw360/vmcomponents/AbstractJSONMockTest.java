/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package org.eclipse.sw360.vmcomponents;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;

import java.io.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author stefan.jaeger@evosoft.com
 */
public abstract class AbstractJSONMockTest {

    // mocking rest service
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);

    public void staticJSONResponse(String url, String body) {

        // mock preparation
        stubFor(get(urlEqualTo(url))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    public void staticJSONResponse(String url, File body) throws IOException {

        FileInputStream fis = new FileInputStream(body);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        int read;
        while ((read = br.read()) > -1){
            sb.append((char) read);
        }
        br.close();
        staticJSONResponse(url, sb.toString());
    }

}