/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class AttachmentController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String ATTACHMENTS_URL = "/attachments";

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Operation(
            summary = "Get attachment information.",
            description = "Get attachment information.",
            tags = {"Attachments"}
    )
    @GetMapping(value = ATTACHMENTS_URL + "/{id}")
    public ResponseEntity<EntityModel<Attachment>> getAttachmentForId(
            @Parameter(description = "id of the attachment")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        AttachmentInfo attachmentInfo = attachmentService.getAttachmentById(id);
        HalResource<Attachment> attachmentResource = createHalAttachment(attachmentInfo, sw360User);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Get attachment information by sha1.",
            description = "Get attachment information by sha1 and the resource having it.",
            tags = {"Attachments"}
    )
    @GetMapping(value = ATTACHMENTS_URL)
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> getAttachments(
            @Parameter(description = "sha1 of the attachment", required = true)
            @RequestParam String sha1
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<AttachmentInfo> attachmentInfos = attachmentService.getAttachmentsBySha1(sha1);

        List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        for (AttachmentInfo sw360Attachment : attachmentInfos) {
            HalResource<Attachment> attachmentResource = createHalAttachment(sw360Attachment, sw360User);
            attachmentResources.add(attachmentResource);
        }
        CollectionModel<EntityModel<Attachment>> resources;
        if (!attachmentResources.isEmpty()) {
            resources = CollectionModel.of(attachmentResources);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } else {
            return new ResponseEntity(attachmentResources, HttpStatus.NO_CONTENT);
        }
    }

    @Operation(
            summary = "Create attachment.",
            description = "Create an attachment.",
            tags = {"Attachments"}
    )
    @RequestMapping(value = ATTACHMENTS_URL , method = RequestMethod.POST, consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> createAttachment(
            @Parameter(description = "List of files to attach",
                    schema = @Schema(
                            type = "string",
                            format = "binary",
                            description = "File to attach"
                    )
            )
            @RequestParam("files") List<MultipartFile> files
    ) throws TException, IOException {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("You must select at least one file for uploading");
        }
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<EntityModel<Attachment>> attachments = new ArrayList<>();
        for (MultipartFile file: files) {
            Attachment attachment = attachmentService.addAttachment(file, sw360User);
            attachments.add(EntityModel.of(attachment));
        }
        CollectionModel<EntityModel<Attachment>> attachmentsResponse = CollectionModel.of(attachments);
        return new ResponseEntity<>(attachmentsResponse, HttpStatus.OK);
    }

    private HalResource<Attachment> createHalAttachment(AttachmentInfo attachmentInfo, User sw360User) throws TException {
        HalResource<Attachment> halAttachment = new HalResource<>(attachmentInfo.getAttachment());
        Source owner = attachmentInfo.getOwner();
        String attachmentId = attachmentInfo.getAttachment().getAttachmentContentId();
        Link downloadLink = null;

        switch (owner.getSetField()) {
            case PROJECT_ID:
                Project sw360Project = projectService.getProjectForUserById(owner.getProjectId(), sw360User);
                restControllerHelper.addEmbeddedProject(halAttachment, sw360Project, false);
                downloadLink = linkTo(ProjectController.class).slash("/api/projects/" + sw360Project.getId() + "/attachments/" + attachmentId).withRel("downloadLink");
                break;
            case COMPONENT_ID:
                Component sw360Component = componentService.getComponentForUserById(owner.getComponentId(), sw360User);
                restControllerHelper.addEmbeddedComponent(halAttachment, sw360Component);
                downloadLink = linkTo(ComponentController.class).slash("/api/components/" + sw360Component.getId() + "/attachments/" + attachmentId).withRel("downloadLink");
                break;
            case RELEASE_ID:
                Release sw360Release = releaseService.getReleaseForUserById(owner.getReleaseId(), sw360User);
                restControllerHelper.addEmbeddedRelease(halAttachment, sw360Release);
                downloadLink = linkTo(ComponentController.class).slash("/api/releases/" + sw360Release.getId() + "/attachments/" + attachmentId).withRel("downloadLink");
                break;
        }

        halAttachment.add(downloadLink);

        if (sw360User != null) {
            restControllerHelper.addEmbeddedUser(halAttachment, sw360User, "createdBy");
        }

        return halAttachment;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        final WebMvcLinkBuilder controllerLinkBuilder = linkTo(AttachmentController.class);
        final Link attachments = Link.of(UriTemplate.of(controllerLinkBuilder.toUri().toString() + "/api/attachments{?sha1}"), "attachments");
        resource.add(attachments);
        return resource;
    }
}
