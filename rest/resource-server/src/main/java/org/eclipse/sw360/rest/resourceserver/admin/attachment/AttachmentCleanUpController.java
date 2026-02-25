/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.admin.attachment;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;


@RestController
@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AttachmentCleanUpController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String ATTACHMENT_CLEANUP_URL = "/attachmentCleanUp";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360AttachmentCleanUpService attachmentCleanUpService;

	@Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(AttachmentCleanUpController.class).slash("api" + ATTACHMENT_CLEANUP_URL).withRel("attachmentCleanUp"));
        return resource;
    }

    @Operation(
            summary = "Clean up all the attachment.",
            description = "Cleanup all the unused attachments.",
            tags = {"Admin"}
    )
    @DeleteMapping(value = ATTACHMENT_CLEANUP_URL + "/deleteAll")
    public ResponseEntity<RequestSummary> cleanUpAttachment() throws TException  {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = attachmentCleanUpService.cleanUpAttachments(sw360User);
        return new ResponseEntity<>(requestSummary,HttpStatus.OK);
    }
}
