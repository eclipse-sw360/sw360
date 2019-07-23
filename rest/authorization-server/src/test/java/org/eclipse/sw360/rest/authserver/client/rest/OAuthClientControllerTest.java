/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import com.google.common.collect.Lists;

import org.eclipse.sw360.rest.authserver.IntegrationTestBase;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class OAuthClientControllerTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate template;

    @Test
    public void testGetAll_basicAuth_unknownUser_fail() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth("my-unknown-user", "my-unknown-password").exchange(
                new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")), String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void testGetAll_basicAuth_admin_success() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth(adminTestUser.email, "password-not-checked-in-test-without-liferay")
                .exchange(new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")),
                        String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testGetAll_basicAuth_normal_fail() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));

        responseEntity = template.withBasicAuth(normalTestUser.email, "password-not-checked-in-test-without-liferay")
                .exchange(new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")),
                        String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    @Test
    public void testGetAll_headerAuth_unknownUser_fail() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        headers.set("authenticated-email", "my-unknown-user");

        responseEntity = template.exchange(
                new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")), String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void testGetAll_headerAuth_admin_success() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        headers.set("authenticated-email", adminTestUser.email);

        responseEntity = template.exchange(
                new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")), String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testGetAll_headerAuth_normal_fail() throws RestClientException, URISyntaxException {
        // given:
        when(clientRepo.getAll()).thenReturn(null);

        // when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
        headers.set("authenticated-email", normalTestUser.email);

        responseEntity = template.exchange(
                new RequestEntity<String>(headers, HttpMethod.GET, new URI("/client-management")), String.class);

        // then:
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }
}
