/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseMergeSelector;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies that duplicate attachment results from the backend are exposed as HTTP 409 Conflict
 * for project, component and release creation and update.
 */
class DuplicateAttachmentConflictTest {

    private final User user = new User().setEmail("user@sw360.org");
    private final ProjectService.Iface projectClient = mock(ProjectService.Iface.class);
    private final ComponentService.Iface componentClient = mock(ComponentService.Iface.class);
    private final AddDocumentRequestSummary duplicateAttachmentSummary = new AddDocumentRequestSummary()
            .setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT)
            .setMessage(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE);

    private MockedStatic<ThriftClients> thriftClients;
    private Sw360ProjectService projectService;
    private Sw360ComponentService componentService;
    private Sw360ReleaseService releaseService;

    @BeforeEach
    void setUp() {
        thriftClients = mockStatic(ThriftClients.class);
        thriftClients.when(ThriftClients::makeProjectClient).thenReturn(projectClient);
        thriftClients.when(ThriftClients::makeComponentClient).thenReturn(componentClient);

        RestControllerHelper rch = mock(RestControllerHelper.class);
        projectService = new Sw360ProjectService(rch);
        componentService = new Sw360ComponentService(rch, projectService, mock(Sw360VulnerabilityService.class));
        releaseService = new Sw360ReleaseService(rch, projectService);
    }

    @AfterEach
    void tearDown() {
        thriftClients.close();
    }

    @Test
    void shouldReturnConflict_whenProjectCreationHasDuplicateAttachments() throws Exception {
        when(projectClient.addProject(any(), any())).thenReturn(duplicateAttachmentSummary);

        assertConflict(() -> projectService.createProject(new Project().setName("project"), user));
    }

    @Test
    void shouldReturnConflict_whenProjectUpdateIntroducesDuplicateAttachment() throws Exception {
        when(projectClient.updateProject(any(), any())).thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);
        when(projectClient.updateProjectWithForceFlag(any(), any(), anyBoolean()))
                .thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);

        assertConflict(() -> projectService.updateProject(new Project().setName("project"), user));
    }

    @Test
    void shouldReturnConflict_whenComponentCreationHasDuplicateAttachments() throws Exception {
        when(componentClient.addComponent(any(), any())).thenReturn(duplicateAttachmentSummary);

        assertConflict(() -> componentService.createComponent(new Component().setName("component"), user));
    }

    @Test
    void shouldReturnConflict_whenComponentUpdateIntroducesDuplicateAttachment() throws Exception {
        when(componentClient.updateComponent(any(), any())).thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);
        when(componentClient.updateComponentWithForceFlag(any(), any(), anyBoolean()))
                .thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);

        assertConflict(() -> componentService.updateComponent(new Component().setName("component"), user));
    }

    @Test
    void shouldReturnConflict_whenReleaseCreationHasDuplicateAttachments() throws Exception {
        when(componentClient.getComponentById(any(), any())).thenReturn(new Component().setName("component"));
        when(componentClient.addRelease(any(), any())).thenReturn(duplicateAttachmentSummary);

        assertConflict(() -> releaseService.createRelease(
                new Release().setComponentId("c1").setVersion("1.0"), user));
    }

    @Test
    void shouldReturnConflict_whenReleaseUpdateIntroducesDuplicateAttachment() throws Exception {
        when(componentClient.updateRelease(any(), any())).thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);
        when(componentClient.updateReleaseWithForceFlag(any(), any(), anyBoolean()))
                .thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);

        assertConflict(() -> releaseService.updateRelease(
                new Release().setName("component").setVersion("1.0"), user));
    }

    @Test
    void shouldReturnConflict_whenComponentMergeIntroducesDuplicateAttachment() throws Exception {
        when(componentClient.mergeComponents(any(), any(), any(), any())).thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);
        Component selection = new Component().setName("component").setCreatedBy(user.getEmail()).setCreatedOn("2026-11-11");

        assertConflict(() -> componentService.mergeComponents("target", "source", selection, user));
    }

    @Test
    void shouldReturnConflict_whenComponentSplitIntroducesDuplicateAttachment() throws Exception {
        when(componentClient.getComponentById(any(), any())).thenReturn(new Component().setName("component"));
        when(componentClient.splitComponent(any(), any(), any())).thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);

        assertConflict(() -> componentService.splitComponents(
                new Component().setId("source"), new Component().setId("target"), user));
    }

    @Test
    void shouldReturnConflict_whenReleaseMergeIntroducesDuplicateAttachment() throws Exception {
        when(componentClient.getReleaseById(any(), any())).thenReturn(new Release().setComponentId("c1"));
        when(componentClient.mergeReleases(any(), any(), any(), any())).thenReturn(RequestStatus.DUPLICATE_ATTACHMENT);
        Release selection = new Release().setName("component").setVersion("1.0")
                .setCreatedBy(user.getEmail()).setCreatedOn("2026-11-11").setAttachments(Collections.emptySet());

        assertConflict(() -> releaseService.mergeRelease("target", "source", ReleaseMergeSelector.from(selection), user));
    }

    private void assertConflict(ThrowingCallable call) {
        DataIntegrityViolationException exception =
                catchThrowableOfType(DataIntegrityViolationException.class, call);

        assertThat(exception).hasMessage(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE);
        assertThat(new RestExceptionHandler().handleConflict(exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
