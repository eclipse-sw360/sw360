/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
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