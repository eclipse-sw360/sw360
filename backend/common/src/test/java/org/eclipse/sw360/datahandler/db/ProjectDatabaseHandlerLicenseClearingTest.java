/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ProjectDatabaseHandlerLicenseClearingTest {
    private final User user = new User().setEmail("reader@example.org").setUserGroup(UserGroup.USER);
    private ProjectDatabaseHandler handler;
    private ComponentDatabaseHandler components;

    @Before
    public void setUp() throws Exception {
        // Avoid constructing repositories: these regressions must not contact a database.
        handler = mock(ProjectDatabaseHandler.class, CALLS_REAL_METHODS);
        components = mock(ComponentDatabaseHandler.class);
        Field field = ProjectDatabaseHandler.class.getDeclaredField("componentDatabaseHandler");
        field.setAccessible(true);
        field.set(handler, components);
    }

    private void givenReleases(List<Release> releases) throws Exception {
        Map<String, ProjectReleaseRelationship> usages = releases.stream().collect(Collectors.toMap(
                Release::getId,
                release -> new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)));
        doReturn(new Project().setId("project").setReleaseIdToUsage(usages))
                .when(handler).getProjectById("project", user);
        if (!usages.isEmpty()) {
            when(components.getReleasesByIds(usages.keySet())).thenReturn(releases);
        }
    }

    @Test
    public void shouldFetchSharedComponentsOnceForThousandsOfReleases() throws Exception {
        List<Release> releases = IntStream.range(0, 3129)
                .mapToObj(i -> new Release().setId("r" + i).setComponentId("component"))
                .collect(Collectors.toCollection(ArrayList::new));
        givenReleases(releases);
        when(components.getComponentsByIds(Set.of("component"))).thenReturn(List.of(
                new Component().setId("component").setCreatedBy(user.getEmail()).setComponentType(ComponentType.OSS)));

        List<Release> result = handler.getReleasesForLicenseClearing("project", user, false, null, null, null);

        assertEquals(3129, result.size());
        assertTrue(result.stream().allMatch(release -> release.getComponentType() == ComponentType.OSS));
        verify(components).getReleasesByIds(releases.stream().map(Release::getId).collect(Collectors.toSet()));
        verify(components).getComponentsByIds(Set.of("component"));
        verify(components, never()).isReleaseActionAllowed(any(), any(), any());
    }

    @Test
    public void shouldStillEnforceParentComponentVisibility() throws Exception {
        givenReleases(List.of(
                new Release().setId("publicRelease").setComponentId("public"),
                new Release().setId("privateRelease").setComponentId("private")));
        when(components.getComponentsByIds(Set.of("public", "private"))).thenReturn(List.of(
                new Component().setId("public").setVisbility(Visibility.EVERYONE).setComponentType(ComponentType.OSS),
                new Component().setId("private").setVisbility(Visibility.PRIVATE).setCreatedBy("other@example.org")));
        try (MockedStatic<SW360Utils> utils = mockStatic(SW360Utils.class, CALLS_REAL_METHODS)) {
            utils.when(() -> SW360Utils.readConfig(IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED, false))
                    .thenReturn(true);

            List<Release> result = handler.getReleasesForLicenseClearing("project", user, false, null, null, null);

            assertEquals(List.of("publicRelease"), result.stream().map(Release::getId).toList());
        }
    }

    @Test
    public void shouldRetainClearingStateAndComponentTypeFilters() throws Exception {
        givenReleases(List.of(
                new Release().setId("approved").setComponentId("oss").setClearingState(ClearingState.APPROVED),
                new Release().setId("new").setComponentId("unused").setClearingState(ClearingState.NEW_CLEARING),
                new Release().setId("other").setComponentId("other").setClearingState(ClearingState.APPROVED)));
        when(components.getComponentsByIds(Set.of("oss", "other"))).thenReturn(List.of(
                new Component().setId("oss").setCreatedBy(user.getEmail()).setComponentType(ComponentType.OSS),
                new Component().setId("other").setCreatedBy(user.getEmail()).setComponentType(ComponentType.COTS)));

        List<Release> result = handler.getReleasesForLicenseClearing("project", user, false,
                List.of(ClearingState.APPROVED), List.of(ComponentType.OSS), null);

        assertEquals(List.of("approved"), result.stream().map(Release::getId).toList());
        verify(components).getComponentsByIds(Set.of("oss", "other"));
    }

    @Test
    public void shouldNotReadComponentsForEmptyProjects() throws Exception {
        givenReleases(List.of());
        assertTrue(handler.getReleasesForLicenseClearing("project", user, false, null, null, null).isEmpty());
        verifyNoInteractions(components);
    }

    @Test
    public void shouldNotExposeReleasesWhoseParentComponentIsMissing() throws Exception {
        givenReleases(List.of(new Release().setId("orphan").setComponentId("missing")));
        when(components.getComponentsByIds(Set.of("missing"))).thenReturn(List.of());
        assertTrue(handler.getReleasesForLicenseClearing("project", user, false, null, null, null).isEmpty());
    }
}
