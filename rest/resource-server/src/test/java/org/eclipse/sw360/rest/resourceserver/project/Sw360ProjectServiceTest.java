/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.projects.ProjectClient;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.rest.resourceserver.attachment.SW360AttachmentBackendService;
import org.eclipse.sw360.rest.resourceserver.component.ComponentServiceRestAdapter;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.license.LicenseServiceRestAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class Sw360ProjectServiceTest {

    private Sw360ProjectService projectService;

    @BeforeEach
    public void setUp() {
        projectService = spy(new Sw360ProjectService(mock(RestControllerHelper.class),
                mock(SW360AttachmentBackendService.class),
                mock(LicenseServiceRestAdapter.class),
                mock(ComponentServiceRestAdapter.class)));
    }

    @Test
    public void should_return_zero_and_skip_client_when_release_ids_are_empty() {
        Set<String> emptyReleaseIds = Collections.emptySet();

        int result = projectService.countProjectsByReleaseIds(emptyReleaseIds);

        assertEquals(0, result, "Expected 0 for empty release-id set");
        verify(projectService, never()).projectClient();
    }

    @Test
    public void should_return_zero_and_skip_client_when_release_ids_are_null() {
        Set<String> nullReleaseIds = null;

        int result = projectService.countProjectsByReleaseIds(nullReleaseIds);

        assertEquals(0, result, "Expected 0 for null release-id set");
        verify(projectService, never()).projectClient();
    }

    @Test
    public void should_verify_CommonUtils_identifies_null_and_empty_collections() {
        assertTrue(CommonUtils.isNullOrEmptyCollection(Collections.emptySet()),
                "Empty set should be identified as empty");

        assertTrue(CommonUtils.isNullOrEmptyCollection(null),
                "Null collection should be identified as empty");

        Set<String> nonEmpty = new HashSet<>();
        nonEmpty.add("item");
        assertFalse(CommonUtils.isNullOrEmptyCollection(nonEmpty),
                "Non-empty set should not be identified as empty");
    }

    @Test
    public void should_not_trigger_guard_for_valid_non_empty_release_ids() {
        Set<String> releaseIds = new HashSet<>();
        releaseIds.add("release1");
        releaseIds.add("release2");

        boolean isEmptyOrNull = CommonUtils.isNullOrEmptyCollection(releaseIds);

        assertFalse(isEmptyOrNull, "Non-empty release IDs should NOT trigger guard");
    }

    @Test
    public void should_attempt_client_call_for_non_empty_release_ids() {
        Set<String> releaseIds = new HashSet<>();
        releaseIds.add("release1");

        ProjectClient projectClient = mock(ProjectClient.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).projectClient();
        org.mockito.Mockito.when(projectClient.getCountByReleaseIds(releaseIds)).thenReturn(2);

        int result = projectService.countProjectsByReleaseIds(releaseIds);

        assertEquals(2, result, "Expected count from project client for non-empty input");
        verify(projectService, times(1)).projectClient();
        verify(projectClient, times(1)).getCountByReleaseIds(releaseIds);
    }

    @Test
    public void should_return_zero_for_multiple_release_ids_when_client_fails() {
        Set<String> releaseIds = new HashSet<>();
        releaseIds.add("release1");
        releaseIds.add("release2");
        releaseIds.add("release3");

        ProjectClient projectClient = mock(ProjectClient.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).projectClient();
        org.mockito.Mockito.when(projectClient.getCountByReleaseIds(releaseIds))
                .thenThrow(new RuntimeException("forced failure for test"));

        int result = projectService.countProjectsByReleaseIds(releaseIds);

        assertEquals(0, result, "Expected 0 when project client call fails");
        verify(projectService, times(1)).projectClient();
        verify(projectClient, times(1)).getCountByReleaseIds(releaseIds);
    }

    @Test
    public void should_delegate_getProjectsByReleaseIds() throws TException {
        Set<String> releaseIds = new HashSet<>();
        releaseIds.add("release1");
        User user = new User().setEmail("test@sw360.org");
        Set<Project> expected = new HashSet<>();
        expected.add(new Project().setId("project1"));

        ProjectClient projectClient = mock(ProjectClient.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).projectClient();
        org.mockito.Mockito.when(projectClient.searchByReleaseIds(releaseIds, user)).thenReturn(expected);

        Set<Project> result = projectService.getProjectsByReleaseIds(releaseIds, user);

        assertEquals(1, result.size());
        verify(projectClient, times(1)).searchByReleaseIds(releaseIds, user);
    }

    @Test
    public void should_delegate_getProjectsByRelease() throws TException {
        String releaseId = "release1";
        User user = new User().setEmail("test@sw360.org");
        Set<Project> expected = new HashSet<>();
        expected.add(new Project().setId("project1"));

        ProjectClient projectClient = mock(ProjectClient.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).projectClient();
        org.mockito.Mockito.when(projectClient.searchByReleaseId(releaseId, user)).thenReturn(expected);

        Set<Project> result = projectService.getProjectsByRelease(releaseId, user);

        assertEquals(1, result.size());
        verify(projectClient, times(1)).searchByReleaseId(releaseId, user);
    }

}
