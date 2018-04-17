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
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
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
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = ATTACHMENTS_URL, params = "sha1", method = RequestMethod.GET)
    public ResponseEntity<Resource<Attachment>> getAttachmentForSha1(
            OAuth2Authentication oAuth2Authentication,
            @RequestParam String sha1) throws TException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        AttachmentInfo attachmentInfo = attachmentService.getAttachmentBySha1ForUser(sha1, sw360User);
        HalResource<Attachment> attachmentResource =
                createHalAttachment(
                        attachmentInfo.getAttachment(),
                        attachmentInfo.getRelease(),
                        sw360User);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    @RequestMapping(value = ATTACHMENTS_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource<Attachment>> getAttachmentForId(
            @PathVariable("id") String id,
            OAuth2Authentication oAuth2Authentication) throws TException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication(oAuth2Authentication);
        AttachmentInfo attachmentInfo =
                attachmentService.getAttachmentByIdForUser(id, sw360User);

        HalResource<Attachment> attachmentResource =
                createHalAttachment(attachmentInfo.getAttachment(),
                        attachmentInfo.getRelease(),
                        sw360User);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    private HalResource<Attachment> createHalAttachment(
            Attachment sw360Attachment,
            Release sw360Release,
            User sw360User) {

        HalResource<Attachment> halAttachment = new HalResource<>(sw360Attachment);
        String componentUUID = sw360Attachment.getAttachmentContentId();

        Link releaseLink = linkTo(AttachmentController.class).slash("api/releases/" + sw360Release.getId()).withRel("release");
        halAttachment.add(releaseLink);

        restControllerHelper.addEmbeddedRelease(halAttachment, sw360Release);
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
