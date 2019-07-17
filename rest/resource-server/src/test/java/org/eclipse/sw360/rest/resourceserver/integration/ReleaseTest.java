/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReleaseTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ReleaseService releaseServiceMock;

    @MockBean
    private Sw360LicenseService licenseServiceMock;

    private Release release;
    private String releaseId = "121831bjh1v2j";

    @Before
    public void before() throws TException {
        List<Release> releaseList = new ArrayList<>();
        release = new Release();
        release.setName("Release 1");
        release.setId(releaseId);
        release.setComponentId("component123");
        release.setVersion("1.0.4");
        release.setCpeid("cpe:id-1231");
        release.setMainLicenseIds(new HashSet<>(Arrays.asList("GPL-2.0-or-later", "Apache-2.0")));
        releaseList.add(release);

        given(this.releaseServiceMock.getReleasesForUser(anyObject())).willReturn(releaseList);

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");

        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        given(this.licenseServiceMock.getLicenseById("Apache-2.0")).willReturn(
                new License("Apache 2.0 License")
                        .setText("Dummy License Text")
                        .setShortname("Apache-2.0")
                        .setId(UUID.randomUUID().toString()));
        given(this.licenseServiceMock.getLicenseById("GPL-2.0-or-later")).willReturn(
                new License("GNU General Public License 2.0")
                        .setText("GNU General Public License 2.0 Text")
                        .setShortname("GPL-2.0-or-later")
                        .setId(UUID.randomUUID().toString()));
    }

    @Test
    public void should_update_release_valid() throws IOException, TException {
        String updatedReleaseName = "updatedReleaseName";
        given(this.releaseServiceMock.updateRelease(anyObject(), anyObject())).willReturn(RequestStatus.SUCCESS);
        given(this.releaseServiceMock.getReleaseForUserById(eq(releaseId), anyObject())).willReturn(release);
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        body.put("mainLicenseIds", new String[] { "Apache-2.0" });
        body.put("wrong_prop", "abc123");
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + releaseId,
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(responseBody.get("name").textValue(), updatedReleaseName);
        JsonNode licenses = responseBody.get("_embedded").get("sw360:licenses");
        assertTrue(licenses.isArray());
        List<String> mainLicenseIds = new ArrayList<>();
        for (JsonNode license : licenses) {
            mainLicenseIds.add(license.get("fullName").textValue());
        }
        assertEquals(1, mainLicenseIds.size());
        assertEquals("Apache 2.0 License", mainLicenseIds.get(0));
        assertNull(responseBody.get("wrong_prop"));
    }

    @Test
    public void should_update_release_invalid() throws IOException, TException {
        doThrow(TException.class).when(this.releaseServiceMock).getReleaseForUserById(anyObject(), anyObject());
        String updatedReleaseName = "updatedReleaseName";
        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("name", updatedReleaseName);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/unknownId123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void should_get_all_releases_with_fields() throws IOException {
        String extraField = "cpeId";
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases?fields=" + extraField,
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 1, Collections.singletonList(extraField));
    }

    @Test
    public void should_delete_releases() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(eq(releaseId), anyObject())).willReturn(RequestStatus.SUCCESS);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + releaseId + "," + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(releaseId, HttpStatus.OK));
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

    @Test
    public void should_delete_release() throws IOException, TException {
        String unknownReleaseId = "abcde12345";
        given(this.releaseServiceMock.deleteRelease(anyObject(), anyObject())).willReturn(RequestStatus.FAILURE);
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/releases/" + unknownReleaseId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(null, headers),
                        String.class);
        List<MultiStatus> multiStatusList = new ArrayList<>();
        multiStatusList.add(new MultiStatus(unknownReleaseId, HttpStatus.INTERNAL_SERVER_ERROR));
        TestHelper.handleBatchDeleteResourcesResponse(response, multiStatusList);
    }

}