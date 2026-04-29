/*
 * Copyright 2025 Pranay Heda pranayheda24@gmail.com
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
public class EccTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private Sw360ReleaseService releaseServiceMock;

    private List<Release> releaseList;
    private Release release1;
    private Release release2;

    @Before
    public void before() throws TException {
        releaseList = new ArrayList<>();

        release1 = new Release();
        release1.setName("TestRelease1");
        release1.setVersion("1.0");
        release1.setId("rel123");

        EccInformation eccInfo1 = new EccInformation();
        eccInfo1.setEccStatus(ECCStatus.OPEN);
        eccInfo1.setAssessorContactPerson("john.doe@example.com");
        eccInfo1.setAssessmentDate("2025-03-15");
        eccInfo1.setAssessorDepartment("Security Department");
        release1.setEccInformation(eccInfo1);

        release2 = new Release();
        release2.setName("TestRelease2");
        release2.setVersion("2.0");
        release2.setId("rel456");

        EccInformation eccInfo2 = new EccInformation();
        eccInfo2.setEccStatus(ECCStatus.APPROVED);
        eccInfo2.setAssessorContactPerson("jane.smith@example.com");
        eccInfo2.setAssessmentDate("2025-03-20");
        eccInfo2.setAssessorDepartment("Export Control Team");
        release2.setEccInformation(eccInfo2);

        releaseList.add(release1);
        releaseList.add(release2);

        // Mock user service with TestHelper
        User user = TestHelper.getTestUser();
        given(this.userServiceMock.getUserByEmailOrExternalId("admin@sw360.org")).willReturn(user);

        // Mock release service
        given(this.releaseServiceMock.getReleasesForUser(any())).willReturn(releaseList);
    }

    // ── GET /ecc ─────────────────────────────────────────────────────────────

    @Test
    public void should_get_all_ecc_information() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Using the embedded relationship name "sw360:releases" since ECC returns releases
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_return_all_ecc_when_no_status_filter() throws Exception {
        // Omitting eccStatus must return all releases — backward-compatible behaviour
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 2);
    }

    @Test
    public void should_filter_ecc_by_status_open() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc?eccStatus=OPEN",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 1);
    }

    @Test
    public void should_filter_ecc_by_status_approved() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc?eccStatus=APPROVED",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 1);
    }

    @Test
    public void should_get_ecc_information_with_pagination() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc?page=0&page_entries=1",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 1);
    }

    @Test
    public void should_get_no_ecc_information_when_empty() throws Exception {
        // Mock empty release list
        given(this.releaseServiceMock.getReleasesForUser(any())).willReturn(new ArrayList<>());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TestHelper.checkResponse(response.getBody(), "releases", 0);
    }

    @Test
    public void should_handle_exception_when_getting_ecc_information() throws Exception {
        // Mock exception in release service
        doThrow(new TException("Test exception")).when(this.releaseServiceMock).getReleasesForUser(any());

        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ── PATCH /ecc/{releaseId} ────────────────────────────────────────────────

    @Test
    public void should_patch_ecc_information_successfully() throws Exception {
        given(this.releaseServiceMock.getReleaseForUserById(eq("rel123"), any())).willReturn(release1);
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SUCCESS);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"eccStatus\":\"APPROVED\",\"assessmentDate\":\"2026-04-25\"}";

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc/rel123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue("Response should contain eccInformation", response.getBody().contains("eccInformation"));
        assertTrue("Untouched assessorContactPerson should survive", response.getBody().contains("john.doe@example.com"));
    }

    @Test
    public void should_patch_ecc_information_sent_to_moderator() throws Exception {
        given(this.releaseServiceMock.getReleaseForUserById(eq("rel123"), any())).willReturn(release1);
        given(this.releaseServiceMock.updateRelease(any(), any())).willReturn(RequestStatus.SENT_TO_MODERATOR);

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"eccStatus\":\"APPROVED\"}";

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc/rel123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    public void should_return_403_when_access_denied_on_patch() throws Exception {
        given(this.releaseServiceMock.getReleaseForUserById(eq("rel123"), any())).willReturn(release1);
        doThrow(new AccessDeniedException("Not allowed to update release 'TestRelease1 1.0'."))
                .when(this.releaseServiceMock).updateRelease(any(), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"eccStatus\":\"APPROVED\"}";

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc/rel123",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void should_return_404_when_release_not_found_on_patch() throws Exception {
        doThrow(new ResourceNotFoundException("Release not found"))
                .when(this.releaseServiceMock).getReleaseForUserById(eq("nonexistent"), any());

        HttpHeaders headers = getHeaders(port);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"eccStatus\":\"APPROVED\"}";

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc/nonexistent",
                        HttpMethod.PATCH,
                        new HttpEntity<>(body, headers),
                        String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
