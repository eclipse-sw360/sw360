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
package org.eclipse.sw360.http.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HttpClientConfigTest {
    @Test
    public void testBasicConfig() {
        HttpClientConfig basicConfig = HttpClientConfig.basicConfig();

        assertThat(basicConfig.customObjectMapper()).isNotPresent();
        assertThat(basicConfig.proxySettings()).isEqualTo(ProxySettings.defaultProxySelector());
    }

    @Test
    public void testBasicConfigIsSingleton() {
        HttpClientConfig config1 = HttpClientConfig.basicConfig();

        HttpClientConfig config2 = HttpClientConfig.basicConfig();
        assertThat(config2).isSameAs(config1);
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(HttpClientConfig.class)
                .withPrefabValues(ObjectMapper.class, new ObjectMapper(), new ObjectMapper())
                .verify();
    }

    @Test
    public void testWithObjectMapper() {
        ObjectMapper mapper = mock(ObjectMapper.class);

        HttpClientConfig config = HttpClientConfig.basicConfig().withObjectMapper(mapper);
        assertThat(config.proxySettings()).isSameAs(HttpClientConfig.basicConfig().proxySettings());
        assertThat(config.customObjectMapper()).contains(mapper);
    }

    @Test
    public void testWithObjectMapperNull() {
        HttpClientConfig orgConfig = HttpClientConfig.basicConfig().withObjectMapper(mock(ObjectMapper.class));

        HttpClientConfig config = orgConfig.withObjectMapper(null);
        assertThat(config.customObjectMapper()).isNotPresent();
    }

    @Test
    public void testWithProxySettings() {
        ProxySettings proxySettings = ProxySettings.useProxy("proxy.host", 12345);
        ObjectMapper mapper = mock(ObjectMapper.class);

        HttpClientConfig config = HttpClientConfig.basicConfig()
                .withObjectMapper(mapper)
                .withProxySettings(proxySettings);
        assertThat(config.proxySettings()).isSameAs(proxySettings);
        assertThat(config.customObjectMapper()).contains(mapper);
    }

    @Test
    public void testGetOrCreateObjectMapperUndefined() {
        assertThat(HttpClientConfig.basicConfig().getOrCreateObjectMapper()).isNotNull();
    }

    @Test
    public void testGetOrCreateObjectMapperCustomMapper() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        HttpClientConfig config = HttpClientConfig.basicConfig().withObjectMapper(mapper);

        assertThat(config.getOrCreateObjectMapper()).isEqualTo(mapper);
    }
}
