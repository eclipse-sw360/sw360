/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AttachmentController implements ResourceProcessor<RepositoryLinksResource> {
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

    @RequestMapping(value = ATTACHMENTS_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource<Attachment>> getAttachmentForId(
            @PathVariable("id") String id) throws TException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        AttachmentInfo attachmentInfo = attachmentService.getAttachmentById(id);
        HalResource<Attachment> attachmentResource = createHalAttachment(attachmentInfo, sw360User);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    @RequestMapping(value = ATTACHMENTS_URL, params = "sha1", method = RequestMethod.GET)
    public ResponseEntity<Resource<Attachment>> getAttachmentForSha1(
            @RequestParam String sha1) throws TException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        AttachmentInfo attachmentInfo = attachmentService.getAttachmentBySha1(sha1);
        HalResource<Attachment> attachmentResource = createHalAttachment(attachmentInfo, sw360User);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    private HalResource<Attachment> createHalAttachment(AttachmentInfo attachmentInfo, User sw360User) throws TException {
        HalResource<Attachment> halAttachment = new HalResource<>(attachmentInfo.getAttachment());
        Source owner = attachmentInfo.getOwner();
        String attachmendId = attachmentInfo.getAttachment().getAttachmentContentId();
        Link downloadLink = null;

        switch (owner.getSetField()) {
            case PROJECT_ID:
                Project sw360Project = projectService.getProjectForUserById(owner.getProjectId(), sw360User);
                restControllerHelper.addEmbeddedProject(halAttachment, sw360Project);
                downloadLink = linkTo(ProjectController.class).slash("/api/projects/" + sw360Project.getId() + "/attachments/" + attachmendId).withRel("downloadLink");
                break;
            case COMPONENT_ID:
                Component sw360Component = componentService.getComponentForUserById(owner.getComponentId(), sw360User);
                restControllerHelper.addEmbeddedComponent(halAttachment, sw360Component);
                downloadLink = linkTo(ComponentController.class).slash("/api/components/" + sw360Component.getId() + "/attachments/" + attachmendId).withRel("downloadLink");
                break;
            case RELEASE_ID:
                Release sw360Release = releaseService.getReleaseForUserById(owner.getReleaseId(), sw360User);
                restControllerHelper.addEmbeddedRelease(halAttachment, sw360Release);
                downloadLink = linkTo(ComponentController.class).slash("/api/releases/" + sw360Release.getId() + "/attachments/" + attachmendId).withRel("downloadLink");
                break;
        }

        halAttachment.add(downloadLink);
        restControllerHelper.addEmbeddedUser(halAttachment, sw360User, "createdBy");
        return halAttachment;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        final ControllerLinkBuilder controllerLinkBuilder = linkTo(AttachmentController.class);
        final Link attachments = new Link(new UriTemplate(controllerLinkBuilder.toUri().toString() + "/api/attachments{?sha1}"), "attachments");
        resource.add(attachments);
        return resource;
    }
}
