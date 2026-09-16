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
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class Sw360ProjectServiceTest {

    private Sw360ProjectService projectService;

    @BeforeEach
    public void setUp() {
        projectService = spy(new Sw360ProjectService(mock(RestControllerHelper.class)));
    }

    @Test
    public void should_return_zero_and_skip_Thrift_when_release_ids_are_empty()
            throws TException {
        Set<String> emptyReleaseIds = Collections.emptySet();

        int result = projectService.countProjectsByReleaseIds(emptyReleaseIds);

        assertEquals(0, result, "Expected 0 for empty release-id set");
        verify(projectService, never()).getThriftProjectClient();
    }

    @Test
    public void should_return_zero_and_skip_Thrift_when_release_ids_are_null()
            throws TException {
        Set<String> nullReleaseIds = null;

        int result = projectService.countProjectsByReleaseIds(nullReleaseIds);

        assertEquals(0, result, "Expected 0 for null release-id set");
        verify(projectService, never()).getThriftProjectClient();
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
    public void should_attempt_Thrift_call_for_non_empty_release_ids() throws TException {
        Set<String> releaseIds = new HashSet<>();
        releaseIds.add("release1");

        org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface projectClient = mock(org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).getThriftProjectClient();
        org.mockito.Mockito.when(projectClient.getCountByReleaseIds(releaseIds)).thenReturn(2);

        int result = projectService.countProjectsByReleaseIds(releaseIds);

        assertEquals(2, result, "Expected count from Thrift client for non-empty input");
        verify(projectService, times(1)).getThriftProjectClient();
        verify(projectClient, times(1)).getCountByReleaseIds(releaseIds);
    }

    @Test
    public void should_return_zero_for_multiple_release_ids_when_Thrift_fails() throws TException {
        Set<String> releaseIds = new HashSet<>();
        releaseIds.add("release1");
        releaseIds.add("release2");
        releaseIds.add("release3");

        org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface projectClient = mock(org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).getThriftProjectClient();
        org.mockito.Mockito.when(projectClient.getCountByReleaseIds(releaseIds)).thenThrow(new TException("forced failure for test"));

        int result = projectService.countProjectsByReleaseIds(releaseIds);

        assertEquals(0, result, "Expected 0 when Thrift call fails");
        verify(projectService, times(1)).getThriftProjectClient();
        verify(projectClient, times(1)).getCountByReleaseIds(releaseIds);
    }

    @Test
    public void should_delegate_getProjectsByReleaseIds() throws TException {
        Set<String> releaseIds = new HashSet<>();
        releaseIds.add("release1");
        org.eclipse.sw360.datahandler.thrift.users.User user =
                new org.eclipse.sw360.datahandler.thrift.users.User().setEmail("test@sw360.org");
        Set<org.eclipse.sw360.datahandler.thrift.projects.Project> expected = new HashSet<>();
        expected.add(new org.eclipse.sw360.datahandler.thrift.projects.Project().setId("project1"));

        org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface projectClient = mock(org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).getThriftProjectClient();
        org.mockito.Mockito.when(projectClient.searchByReleaseIds(releaseIds, user)).thenReturn(expected);

        Set<org.eclipse.sw360.datahandler.thrift.projects.Project> result =
                projectService.getProjectsByReleaseIds(releaseIds, user);

        assertEquals(1, result.size());
        verify(projectClient, times(1)).searchByReleaseIds(releaseIds, user);
    }

    @Test
    public void should_delegate_getProjectsByRelease() throws TException {
        String releaseId = "release1";
        org.eclipse.sw360.datahandler.thrift.users.User user =
                new org.eclipse.sw360.datahandler.thrift.users.User().setEmail("test@sw360.org");
        Set<org.eclipse.sw360.datahandler.thrift.projects.Project> expected = new HashSet<>();
        expected.add(new org.eclipse.sw360.datahandler.thrift.projects.Project().setId("project1"));

        org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface projectClient = mock(org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).getThriftProjectClient();
        org.mockito.Mockito.when(projectClient.searchByReleaseId(releaseId, user)).thenReturn(expected);

        Set<org.eclipse.sw360.datahandler.thrift.projects.Project> result =
                projectService.getProjectsByRelease(releaseId, user);

        assertEquals(1, result.size());
        verify(projectClient, times(1)).searchByReleaseId(releaseId, user);
    }

    @Test
    public void should_get_groups_sorted_with_empty_token_first() throws TException {
        Set<String> mockGroups = new HashSet<>(Arrays.asList(
                "",
                "group b",
                "Group A",
                "Group B"
        ));

        org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface projectClient =
                mock(org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).getThriftProjectClient();
        org.mockito.Mockito.when(projectClient.getGroups()).thenReturn(mockGroups);

        List<String> result = projectService.getGroups();

        assertEquals(
                Arrays.asList(SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN, "Group A", "Group B", "group b"),
                result
        );
        verify(projectClient, times(1)).getGroups();
    }

    @Test
    public void should_get_groups_when_thrift_returns_null() throws TException {
        org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface projectClient =
                mock(org.eclipse.sw360.datahandler.thrift.projects.ProjectService.Iface.class);
        org.mockito.Mockito.doReturn(projectClient).when(projectService).getThriftProjectClient();
        org.mockito.Mockito.when(projectClient.getGroups()).thenReturn(null);

        List<String> result = projectService.getGroups();

        assertEquals(
                Collections.singletonList(SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN),
                result
        );
        verify(projectClient, times(1)).getGroups();
    }

    @Test
    public void should_count_only_loaded_attachments_in_one_request() throws TException {
        Release first = new Release().setId("r1").setAttachments(Set.of(
                new Attachment().setAttachmentContentId("a1"),
                new Attachment().setAttachmentContentId("a2")));
        Release second = new Release().setId("r2").setAttachments(Set.of(
                new Attachment().setAttachmentContentId("a1")));
        UsageData filter = UsageData.licenseInfo(new LicenseInfoUsage(Set.of()));
        Map<Source, Set<String>> keys = Map.of(
                Source.releaseId("r1"), Set.of("a1", "a2"),
                Source.releaseId("r2"), Set.of("a1"));
        AttachmentService.Iface client = mock(AttachmentService.Iface.class);
        try (MockedStatic<ThriftClients> clients = mockStatic(ThriftClients.class)) {
            clients.when(ThriftClients::makeAttachmentClient).thenReturn(client);
            when(client.getAttachmentUsageCount(keys, filter)).thenReturn(Map.of(
                    Map.of(Source.releaseId("r1"), "a1"), 7,
                    Map.of(Source.releaseId("r2"), "a1"), 2));

            assertEquals(Map.of("r1_a1", 7, "r2_a1", 2),
                    projectService.getAttachmentUsageCountsForReleases(List.of(first, second, first), filter));
            verify(client).getAttachmentUsageCount(keys, filter);
            clients.verify(ThriftClients::makeAttachmentClient, times(1));
        }
    }

    @Test
    public void should_skip_attachment_count_request_when_no_attachments_exist() throws TException {
        try (MockedStatic<ThriftClients> clients = mockStatic(ThriftClients.class)) {
            assertTrue(projectService.getAttachmentUsageCountsForReleases(List.of(), null).isEmpty());
            assertTrue(projectService.getAttachmentUsageCountsForReleases(
                    List.of(new Release().setId("r1")), null).isEmpty());
            clients.verifyNoInteractions();
        }
    }

    @Test
    public void should_propagate_attachment_count_failure() throws TException {
        Release release = new Release().setId("r1").setAttachments(
                Set.of(new Attachment().setAttachmentContentId("a1")));
        Map<Source, Set<String>> keys = Map.of(Source.releaseId("r1"), Set.of("a1"));
        AttachmentService.Iface client = mock(AttachmentService.Iface.class);
        try (MockedStatic<ThriftClients> clients = mockStatic(ThriftClients.class)) {
            clients.when(ThriftClients::makeAttachmentClient).thenReturn(client);
            when(client.getAttachmentUsageCount(keys, null)).thenThrow(new TException("count unavailable"));
            assertThrows(TException.class,
                    () -> projectService.getAttachmentUsageCountsForReleases(List.of(release), null));
        }
    }

}
