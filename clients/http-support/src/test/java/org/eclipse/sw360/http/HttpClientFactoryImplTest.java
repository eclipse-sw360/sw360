/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.http.config.HttpClientConfig;
import org.eclipse.sw360.http.config.ProxySettings;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HttpClientFactoryImplTest {
    /**
     * The factory to be tested.
     */
    private HttpClientFactoryImpl factory;

    @Before
    public void setUp() {
        factory = new HttpClientFactoryImpl();
    }

    /**
     * Creates a new client using the factory under test and checks whether it
     * is of the expected type.
     *
     * @param config the client configuration
     * @return the newly created client
     */
    private HttpClientImpl createClient(HttpClientConfig config) {
        HttpClient client = factory.newHttpClient(config);
        assertThat(client).isInstanceOf(HttpClientImpl.class);
        return (HttpClientImpl) client;
    }

    @Test
    public void testNewClient() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        HttpClientConfig config = HttpClientConfig.basicConfig()
                .withObjectMapper(mapper);

        HttpClientImpl client = createClient(config);
        assertThat(client.getMapper()).isSameAs(mapper);
        assertThat(client.getClient().proxy()).isNull();
        assertThat(client.getClient().proxySelector()).isEqualTo(ProxySelector.getDefault());
    }

    @Test
    public void testNewClientWithProxySettings() {
        String host = "localhost";
        int port = 8080;
        ProxySettings proxySettings = ProxySettings.useProxy(host, port);
        HttpClientConfig config = HttpClientConfig.basicConfig()
                .withProxySettings(proxySettings);

        HttpClientImpl client = createClient(config);
        Proxy proxy = client.getClient().proxy();
        assertThat(proxy).isNotNull();
        assertThat(proxy.address()).isInstanceOf(InetSocketAddress.class);
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        assertThat(address.getHostName()).isEqualTo(host);
        assertThat(address.getPort()).isEqualTo(port);
        assertThat(proxy.type()).isEqualTo(Proxy.Type.HTTP);
    }

    @Test
    public void testNewClientWithNoProxy() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        HttpClientConfig config = HttpClientConfig.basicConfig()
                .withObjectMapper(mapper)
                .withProxySettings(ProxySettings.noProxy());

        HttpClientImpl client = createClient(config);
        assertThat(client.getMapper()).isSameAs(mapper);
        assertThat(client.getClient().proxy()).isEqualTo(Proxy.NO_PROXY);
    }

    @Test
    public void testNewClientWithoutCertificateCheck() {
        try {
            System.setProperty(HttpClientFactoryImpl.CLIENT_ACCESS_UNVERIFIED_PROPERTY, "true");
            ObjectMapper mapper = mock(ObjectMapper.class);
            HttpClientConfig config = HttpClientConfig.basicConfig()
                    .withObjectMapper(mapper);

            HttpClientImpl client = createClient(config);
            assertThat(client.getClient().hostnameVerifier().verify("", null)).isTrue();
        } finally {
            System.clearProperty(HttpClientFactoryImpl.CLIENT_ACCESS_UNVERIFIED_PROPERTY);
        }
    }
}
