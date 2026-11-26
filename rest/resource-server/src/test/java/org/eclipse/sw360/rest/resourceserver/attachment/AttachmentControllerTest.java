/*
 * Copyright Pratyay Patel, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class AttachmentControllerTest {

    @Mock
    private Sw360AttachmentService attachmentService;

    @Mock
    private Sw360ProjectService projectService;

    @Mock
    private Sw360ReleaseService releaseService;

    @Mock
    private Sw360ComponentService componentService;

    @Mock
    private RestControllerHelper<?> restControllerHelper;

    private AttachmentController controller;

    @BeforeEach
    public void setUp() {
        controller = new AttachmentController(attachmentService, projectService, releaseService, componentService,
                restControllerHelper);
    }

    @Test
    public void shouldRejectEmptyFileListAsBadRequest() {
        assertThatThrownBy(() -> controller.createAttachment(Collections.emptyList()))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessage("You must select at least one file for uploading");
        verifyNoInteractions(attachmentService);
    }

    @Test
    public void shouldRejectMissingFileListAsBadRequest() {
        assertThatThrownBy(() -> controller.createAttachment(null))
                .isInstanceOf(BadRequestClientException.class);
        verifyNoInteractions(attachmentService);
    }
}
