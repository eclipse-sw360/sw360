/*
 * Copyright Rajnish Kumar<rk2452003@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.admin.attachment;

import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AttachmentCleanUpControllerTest {

    @Mock
    private RestControllerHelper restControllerHelper;

    @Mock
    private Sw360AttachmentCleanUpService attachmentCleanUpService;

    @InjectMocks
    private AttachmentCleanUpController attachmentCleanUpController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testProcess() {
        RepositoryLinksResource resource = new RepositoryLinksResource();
        RepositoryLinksResource result = attachmentCleanUpController.process(resource);
        Optional<Link> linkOptional = result.getLink("attachmentCleanUp");

        assertTrue("Link 'attachmentCleanUp' should be present", linkOptional.isPresent());

        Link link = linkOptional.get();
        assertTrue("Link href should contain '/api/attachmentCleanUp'", link.getHref().contains("/api/attachmentCleanUp"));
    }

    @Test
    public void testCleanUpAttachment() throws Exception {
        User mockUser = new User();
        RequestSummary mockSummary = new RequestSummary();

        when(restControllerHelper.getSw360UserFromAuthentication()).thenReturn(mockUser);
        when(attachmentCleanUpService.cleanUpAttachments(mockUser)).thenReturn(mockSummary);

        ResponseEntity<RequestSummary> response = attachmentCleanUpController.cleanUpAttachment();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockSummary, response.getBody());

        verify(restControllerHelper, times(1)).getSw360UserFromAuthentication();
        verify(attachmentCleanUpService, times(1)).cleanUpAttachments(mockUser);
    }
}
