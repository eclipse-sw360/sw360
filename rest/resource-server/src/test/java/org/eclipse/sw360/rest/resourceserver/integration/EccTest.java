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
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@RunWith(SpringRunner.class)
public class EccTest extends TestIntegrationBase {

    @LocalServerPort
    private int port;

    @MockitoBean
    private Sw360ReleaseService releaseServiceMock;

    private List<Release> releaseList;

    @Before
    public void before() throws TException {
        releaseList = new ArrayList<>();

        // Create test releases with ECC information
        Release release1 = new Release();
        release1.setName("TestRelease1");
        release1.setVersion("1.0");
        release1.setId("rel123");

        EccInformation eccInfo1 = new EccInformation();
        eccInfo1.setAssessorContactPerson("john.doe@example.com");
        eccInfo1.setAssessmentDate("2025-03-15");
        eccInfo1.setAssessorDepartment("Security Department");
        release1.setEccInformation(eccInfo1);

        Release release2 = new Release();
        release2.setName("TestRelease2");
        release2.setVersion("2.0");
        release2.setId("rel456");

        EccInformation eccInfo2 = new EccInformation();
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

    @Test
    public void should_get_all_ecc_information() throws Exception {
        HttpHeaders headers = getHeaders(port);
        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/ecc",
                        HttpMethod.GET,
                        new HttpEntity<>(null, headers),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Using the embedded relationship name "sw360:releases" since ECC returns release objects
        TestHelper.checkResponse(response.getBody(), "releases", 2);
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
}
