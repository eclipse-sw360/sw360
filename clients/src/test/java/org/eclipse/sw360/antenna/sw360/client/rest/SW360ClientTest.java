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
package org.eclipse.sw360.antenna.sw360.client.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.antenna.http.HttpClient;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessTokenProvider;
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SW360ClientTest {
    private static final String BASE = "https://scott:tiger@www.eclipse.org";
    private static final String BASE_REST_URI = BASE + "/sw360";

    private SW360Client client;

    @Before
    public void setUp() {
        SW360ClientConfig config = SW360ClientConfig.createConfig(BASE_REST_URI,
                BASE_REST_URI + "/auth/token",
                "scott", "tiger", "CLIENT_ID", "CLIENT_PASSWORD", "USER_TOKEN",
                mock(HttpClient.class), mock(ObjectMapper.class));
        client = new SW360ComponentClient(config, mock(AccessTokenProvider.class));
    }

    @Test
    public void testCorrectUriIsResolved() {
        final String uri = BASE_REST_URI + "/releases/1234567890/attachments";

        assertThat(client.resolveAgainstBase(uri).toString()).isEqualTo(uri);
    }

    @Test
    public void testBaseIsCorrectlySetWhenResolving() {
        final String path = "/some/relative/path?foo=bar&baz=blub#fragment";
        final String uri = "http://other.host.org" + path;

        assertThat(client.resolveAgainstBase(uri).toString()).isEqualTo(BASE_REST_URI + path);
    }

    @Test
    public void testRelativeUriCanBeResolved() {
        final String relativeUri = "/some/relative/path";

        assertThat(client.resolveAgainstBase(relativeUri).toString()).isEqualTo(BASE_REST_URI + relativeUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorHandling() {
        client.resolveAgainstBase(":");
    }
}
